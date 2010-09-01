/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
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
import org.shoal.ha.mapper.DefaultKeyMapper;
import org.shoal.ha.group.GroupService;
import org.shoal.ha.mapper.KeyMapper;
import org.shoal.ha.cache.api.*;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.command.CommandManager;
import org.shoal.ha.cache.impl.command.ReplicationCommandOpcode;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public class ReplicatedDataStore<K, V extends Serializable>
        implements DataStore<K, V> {

    private static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_CONFIG);

    private String storeName;

    private String instanceName;

    private String groupName;

    private GroupService gs;

    private CommandManager<K, V> cm;

    private DataStoreEntryHelper<K, V> transformer;

    private DataStoreContext<K, V> dsc;

    private DataStoreConfigurator<K, V> conf;

    private ReplicaStore<K, V> replicaStore;

    public ReplicatedDataStore(DataStoreConfigurator<K, V> conf, GroupService gs) {
        this.conf = conf;
        this.storeName = conf.getStoreName();
        this.gs = gs;
        this.instanceName = gs.getMemberName();
        this.groupName = gs.getGroupName();
        
        initialize(conf);
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

        //if (conf.isDoASyncReplication()) {
            cm.registerExecutionInterceptor(new ReplicationCommandTransmitterManager<K, V>());
            cm.registerCommand(new ReplicationFramePayloadCommand<K, V>());
            _logger.log(Level.INFO, "ASync replication enabled...");
        //}
        
        
        KeyMapper keyMapper = conf.getKeyMapper();
        if ((keyMapper != null) && (keyMapper instanceof DefaultKeyMapper)) {
            gs.registerGroupMemberEventListener((DefaultKeyMapper) keyMapper);
        }

        gs.registerGroupMessageReceiver(storeName, cm);

        replicaStore = dsc.getReplicaStore();

        Logger logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_CONFIG);
        logger.log(Level.INFO, "Created ReplicatedDataStore with config: " + conf);

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
            if (! entry.isRemoved()) {
                SaveCommand<K, V> cmd = new SaveCommand<K, V>(k, v);
                cm.execute(cmd);
                if (conf.isCacheLocally()) {
                    entry.setV(v);
                }

                String staleLocation = entry.setReplicaInstanceName(cmd.getTargetName());

                result = cmd.getKeyMappingInfo();


                if (staleLocation != null) {
                    StaleCopyRemoveCommand<K, V> staleCmd = new StaleCopyRemoveCommand<K, V>();
                    staleCmd.setKey(k);
                    staleCmd.setStaleTargetName(staleLocation);
                    cm.execute(staleCmd);
                }
            } else {
                _logger.log(Level.WARNING, "ReplicatedDataStore.put(" + k + ") AFTER remove?");
                return "";
            }
        }

        return result;
    }

    @Override
    public String updateDelta(K k, V obj)
        throws DataStoreException {
        UpdateDeltaCommand<K, V> cmd = new UpdateDeltaCommand<K, V>(k, obj);
        cm.execute(cmd);

        return cmd.getKeyMappingInfo();
    }

    @Override
    public V get(K key)
            throws DataStoreException {
        return get(key, null);
    }

    @Override
    public V get(K key, String cookie)
            throws DataStoreException {

        DataStoreEntry<K, V> entry = replicaStore.getOrCreateEntry(key);
        V v = null;
        synchronized (entry) {
            if (! entry.isRemoved()) {
                v = entry.getV();
            } else {
                return null; //Because it is already removed
            }
        }

        if (v == null) {
            try {
                String respondingInstanceName = null;
                if (cookie == null) {
                    BroadcastLoadRequestCommand<K, V> cmd = new BroadcastLoadRequestCommand<K, V>(key);
                    cm.execute(cmd);
                    v = cmd.getResult(3, TimeUnit.SECONDS);
                    respondingInstanceName = cmd.getRespondingInstanceName();
                } else {
                    LoadRequestCommand<K, V> cmd = new LoadRequestCommand<K, V>(key, cookie);
                    cm.execute(cmd);
                    v = cmd.getResult(3, TimeUnit.SECONDS);
                    respondingInstanceName = cmd.getRespondingInstanceName();
                }

                entry = replicaStore.getOrCreateEntry(key);
                synchronized (entry) {
                    if (! entry.isRemoved()) {
                        entry.setReplicaInstanceName(respondingInstanceName);

                        if (conf.isCacheLocally()) {
                            entry.setV(v);
                        }
                    }
                }
            } catch (DataStoreException dseEx) {
                throw dseEx;
            }

        }

        return v;
    }

    @Override
    public void remove(K k)
            throws DataStoreException {
        DataStoreEntry<K, V> entry = replicaStore.getOrCreateEntry(k);
        synchronized (entry) {
            entry.markAsRemoved("Removed by ReplicatedDataStore.remove");
        }

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
        return null;
    }

    @Override
    public int removeIdleEntries(long idleFor) {
        return 0;
    }

    @Override
    public Collection find(DataStoreEntryEvaluator<K, V> kvDataStoreEntryEvaluator) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void update(DataStoreEntryEvaluator<K, V> kvDataStoreEntryEvaluator) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }


}
