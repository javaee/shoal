/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.shoal.ha.cache.impl.store;

import org.shoal.adapter.store.commands.*;
import org.shoal.ha.cache.impl.interceptor.ReplicationCommandTransmitterManager;
import org.shoal.ha.cache.impl.interceptor.ReplicationFramePayloadCommand;
import org.shoal.ha.cache.impl.util.CommandResponse;
import org.shoal.ha.cache.impl.util.ResponseMediator;
import org.shoal.ha.mapper.DefaultKeyMapper;
import org.shoal.ha.group.GroupService;
import org.shoal.ha.mapper.KeyMapper;
import org.shoal.ha.cache.api.*;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.command.CommandManager;

import java.io.Serializable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public class ReplicatedDataStore<K, V extends Serializable>
        implements DataStore<K, V> {

    private static final int MAX_REPLICA_TRIES = 1;

    private static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_DATA_STORE);

    private static final Logger _loadLogger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_LOAD_REQUEST_COMMAND);

    private static final Logger _saveLogger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_SAVE_COMMAND);

    private String storeName;

    private String instanceName;

    private String groupName;

    private GroupService gs;

    private CommandManager<K, V> cm;

    private DataStoreEntryHelper<K, V> transformer;

    private DataStoreContext<K, V> dsc;

    private DataStoreConfigurator<K, V> conf;

    private ReplicaStore<K, V> replicaStore;

    private AtomicInteger broadcastLoadRequestCount = new AtomicInteger(0);

    private AtomicInteger simpleBroadcastCount = new AtomicInteger(0);

    private AtomicInteger foundLocallyCount = new AtomicInteger(0);

    private long defaultIdleTimeoutInMillis;


    private String debugName = "ReplicatedDataStore";

    public ReplicatedDataStore(DataStoreConfigurator<K, V> conf, GroupService gs) {
        this.conf = conf;
        this.storeName = conf.getStoreName();
        this.gs = gs;
        this.instanceName = gs.getMemberName();
        this.groupName = gs.getGroupName();

        initialize(conf);

        debugName = conf.getStoreName() + ": ";
    }

    private void initialize(DataStoreConfigurator<K, V> conf) {
        this.dsc = new DataStoreContext<K, V>(
                storeName, gs, conf.getClassLoader());

        this.transformer = conf.getDataStoreEntryHelper();
        dsc.setDataStoreEntryHelper(transformer);
        dsc.setDataStoreKeyHelper(conf.getDataStoreKeyHelper());
        dsc.setKeyMapper(conf.getKeyMapper());
        cm = dsc.getCommandManager();


        if (conf.getCommands() != null) {
            for (Command<K, ? super V> cmd : conf.getCommands()) {
                cm.registerCommand(cmd);
            }
        }

        cm.registerExecutionInterceptor(new ReplicationCommandTransmitterManager<K, V>());
        cm.registerCommand(new ReplicationFramePayloadCommand<K, V>());


        KeyMapper keyMapper = conf.getKeyMapper();
        if ((keyMapper != null) && (keyMapper instanceof DefaultKeyMapper)) {
            gs.registerGroupMemberEventListener((DefaultKeyMapper) keyMapper);
        }

        gs.registerGroupMessageReceiver(storeName, cm);

        replicaStore = dsc.getReplicaStore();
        replicaStore.setIdleEntryDetector(conf.getIdleEntryDetector());

        _logger.log(Level.INFO, "Created ReplicatedDataStore with config: " + conf);

    }

    public DataStoreContext<K, V> getDataStoreContext() {
        return dsc;
    }

    @Override
    public String put(K k, V v)
            throws DataStoreException {

        String result = null;
        DataStoreEntry<K, V> entry = replicaStore.getOrCreateEntry(k);
        synchronized (entry) {
            if (!entry.isRemoved()) {
                entry.setLastAccessedAt(System.currentTimeMillis());
                entry.incrementAndGetVersion();
                SaveCommand<K, V> cmd = new SaveCommand<K, V>(k, v,
                        entry.getVersion(), entry.getLastAccessedAt());
                cm.execute(cmd);
                if (conf.isCacheLocally()) {
                    entry.setV(v);
                }

                String staleLocation = entry.setReplicaInstanceName(cmd.getTargetName());

                result = cmd.getKeyMappingInfo();


                if ((staleLocation != null) && (! staleLocation.equals(cmd.getTargetName()))) {
                    StaleCopyRemoveCommand<K, V> staleCmd = new StaleCopyRemoveCommand<K, V>();
                    staleCmd.setKey(k);
                    staleCmd.setStaleTargetName(staleLocation);
                    cm.execute(staleCmd);
                    _saveLogger.log(Level.WARNING, debugName + "put(" + k + ") sent stale_remove to stale data to " + staleLocation);
                }
            } else {
                _logger.log(Level.WARNING, "ReplicatedDataStore.put(" + k + ") AFTER remove?");
                return "";
            }
        }

        _saveLogger.log(Level.FINE, debugName + " done save(" + k + ") to " + result);
        return result;
    }

    @Override
    public V get(K key)
            throws DataStoreException {
        V v = null;
        DataStoreEntry<K, V> entry = replicaStore.getEntry(key);
        if (entry != null) {
            if (!entry.isRemoved()) {
                v = entry.getV();
                if (v != null) {
                    foundLocallyCount.incrementAndGet();
                    if (_loadLogger.isLoggable(Level.FINE)) {
                        _loadLogger.log(Level.FINE, debugName + "load(" + key
                                + "); FOUND IN LOCAL CACHE!!");
                    }
                }
            } else {
                return null; //Because it is already removed
            }
        }

        if (v == null) {
            KeyMapper keyMapper = dsc.getKeyMapper();
            String replicachoices = keyMapper.getReplicaChoices(dsc.getGroupName(), key);
            String[] replicaHint = replicachoices.split(":");
            if (_loadLogger.isLoggable(Level.FINE)) {
                _loadLogger.log(Level.FINE, debugName + "load(" + key
                        + "); ReplicaChoices: " + replicachoices);
            }

            String respondingInstance = null;
            for (int replicaIndex = 0; (replicaIndex < replicaHint.length) && (replicaIndex < MAX_REPLICA_TRIES); replicaIndex++) {
                String target = replicaHint[replicaIndex];
                if (target == null || target.trim().length() == 0 || target.equals(dsc.getInstanceName())) {
                    continue;
                }
                simpleBroadcastCount.incrementAndGet();
                LoadRequestCommand<K, V> command
                        = new LoadRequestCommand<K, V>(key, target);
                if (_loadLogger.isLoggable(Level.FINE)) {
                    _loadLogger.log(Level.FINE, debugName + "load(" + key
                        + ") Trying to load from Replica[" + replicaIndex + "]: " + replicaHint[replicaIndex]);
                }

                cm.execute(command);
                v = command.getResult(3, TimeUnit.SECONDS);
                if (v != null) {
                    respondingInstance = command.getRespondingInstanceName();
                    break;
                }
            }

            if (v == null) {
                if (_loadLogger.isLoggable(Level.FINE)) {
                    _loadLogger.log(Level.FINE, debugName + "*load(" + key
                        + ") Performing broadcast load");
                }
                broadcastLoadRequestCount.incrementAndGet();
                String[] targetInstances = dsc.getKeyMapper().getCurrentMembers();
                for (String targetInstance : targetInstances) {
                    if (targetInstance.equals(dsc.getInstanceName())) {
                        continue;
                    }
                    LoadRequestCommand<K, V> lrCmd
                            = new LoadRequestCommand<K, V>(key, targetInstance);
                    if (_loadLogger.isLoggable(Level.FINE)) {
                        _loadLogger.log(Level.FINE, debugName + "*load(" + key
                            + ") Trying to load from " + targetInstance);
                    }

                    cm.execute(lrCmd);
                    v = lrCmd.getResult(3, TimeUnit.SECONDS);
                    if (v != null) {
                        respondingInstance = targetInstance;
                        break;
                    }
                }
            }

            if (v != null) {
                entry = replicaStore.getEntry(key);
                if (entry != null) {
                    synchronized (entry) {
                        if (!entry.isRemoved()) {
                            if (conf.isCacheLocally()) {
                                entry.setV(v);
                            }

                            entry.setLastAccessedAt(System.currentTimeMillis());
                            entry.setReplicaInstanceName(respondingInstance);
                            //Note: Do not remove the stale replica now. We will
                            //  do that in save
                            if (_loadLogger.isLoggable(Level.FINE)) {
                                _loadLogger.log(Level.FINE, debugName + "load(" + key
                                        + "; Successfully loaded data from " + respondingInstance);
                            }
                        } else {
                            if (_loadLogger.isLoggable(Level.FINE)) {
                                _loadLogger.log(Level.FINE, debugName + "load(" + key
                                        + "; Got data from " + respondingInstance + ", but another concurrent thread removed the entry");
                            }
                        }
                    }
                }
            }
        }

        if (_loadLogger.isLoggable(Level.FINE)) {
            _loadLogger.log(Level.FINE, debugName + "load(" + key
                    + ") Final result: " + v);
        }
        return v;
    }

    @Override
    public void remove(K k)
            throws DataStoreException {

        replicaStore.remove(k);

        String[] targets = dsc.getKeyMapper().getCurrentMembers();

        if (targets != null) {
            for (String target : targets) {
                RemoveCommand<K, V> cmd = new RemoveCommand<K, V>();
                cmd.setKey(k);
                cmd.setTarget(target);
                cm.execute(cmd);
            }
        }

    }

    @Override
    public String touch(K k, long version, long ts, long ttl)
            throws DataStoreException {
        String location = "";
        DataStoreEntry<K, V> entry = replicaStore.getEntry(k);
        if (entry != null) {
            synchronized (entry) {
                long now = System.currentTimeMillis();
                entry.setLastAccessedAt(now);
                String target = entry.getReplicaInstanceName();
                TouchCommand<K, V> cmd = new TouchCommand<K, V>(k, version, now, defaultIdleTimeoutInMillis);
                cm.execute(cmd);

                location = cmd.getKeyMappingInfo();
            }
        }
        return location;
    }

    @Override
    public int removeIdleEntries(long idleFor) {

        String[] targets = dsc.getKeyMapper().getCurrentMembers();

        ResponseMediator respMed = dsc.getResponseMediator();
        CommandResponse resp = respMed.createCommandResponse();
        long tokenId = resp.getTokenId();
        Future<Integer> future = resp.getFuture();
        resp.setTransientResult(new Integer(0));

        int finalResult = 0;
        try {
            if (targets != null) {
                resp.setExpectedUpdateCount(targets.length);
                for (String target : targets) {
                    RemoveExpiredCommand<K, V> cmd = new RemoveExpiredCommand<K, V>(idleFor, tokenId);
                    cmd.setTarget(target);
                    try {
                        cm.execute(cmd);
                    } catch (DataStoreException dse) {
                        _logger.log(Level.INFO, "Exception during removeIdleEntries...",dse);
                    }
                }
            }

            int localResult = replicaStore.removeExpired();
            synchronized (resp) {
                Integer existingValue = (Integer) resp.getTransientResult();
                Integer newResult = new Integer(existingValue.intValue() + localResult);
                resp.setTransientResult(newResult);
            }

            finalResult = (Integer) resp.getTransientResult();

            finalResult = future.get(6, TimeUnit.SECONDS);
        } catch (Exception ex) {
            //TODO
        } finally {
            respMed.removeCommandResponse(tokenId);
        }

        return finalResult;
    }

    @Override
    public void close() {
    }

    public int size() {
        int result = 0;
        KeyMapper km = dsc.getKeyMapper();
        String[] targets = km.getCurrentMembers();

        int targetCount = targets.length;
        SizeRequestCommand[] commands = new SizeRequestCommand[targetCount];

        for (int i = 0; i < targetCount; i++) {
            commands[i] = new SizeRequestCommand(targets[i]);
            try {
                dsc.getCommandManager().execute(commands[i]);
            } catch (DataStoreException dse) {
                //TODO:
            }
        }

        for (int i = 0; i < targetCount; i++) {
            result += commands[i].getResult();
        }

        return result;
    }
}
