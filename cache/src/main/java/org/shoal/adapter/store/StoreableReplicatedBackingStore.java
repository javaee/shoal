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

package org.shoal.adapter.store;

import org.glassfish.ha.store.api.BackingStore;
import org.glassfish.ha.store.api.BackingStoreConfiguration;
import org.glassfish.ha.store.api.BackingStoreException;
import org.glassfish.ha.store.api.Storeable;
import org.shoal.adapter.store.commands.*;
import org.shoal.adapter.store.commands.monitor.ListBackingStoreConfigurationCommand;
import org.shoal.adapter.store.commands.monitor.ListBackingStoreConfigurationResponseCommand;
import org.shoal.adapter.store.commands.monitor.ListReplicaStoreEntriesCommand;
import org.shoal.ha.cache.api.*;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.store.ReplicaStore;
import org.shoal.ha.cache.impl.util.CommandResponse;
import org.shoal.ha.cache.impl.util.ResponseMediator;
import org.shoal.ha.mapper.KeyMapper;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public class StoreableReplicatedBackingStore<K extends Serializable, V extends Storeable>
        extends BackingStore<K, V> {

    private static final int MAX_REPLICA_TRIES = 2;

    private static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE);

    private ReplicationFramework<K, V> framework;

    private ReplicaStore<K, V> replicaStore;

    boolean localCachingEnabled;

    private ReplicatedBackingStoreFactory factory;

    private AtomicInteger broadcastLoadRequestCount = new AtomicInteger(0);

    private AtomicInteger simpleBroadcastCount = new AtomicInteger(0);

    private AtomicInteger foundLocallyCount = new AtomicInteger(0);

    /*package*/ void setBackingStoreFactory(ReplicatedBackingStoreFactory factory) {
        this.factory = factory;
    }
    
    @Override
    public void initialize(BackingStoreConfiguration<K, V> conf)
            throws BackingStoreException {
        super.initialize(conf);
        DataStoreConfigurator<K, V> dsConf = new DataStoreConfigurator<K, V>();
        dsConf.setInstanceName(conf.getInstanceName())
                .setGroupName(conf.getClusterName())
                .setStoreName(conf.getStoreName())
                .setKeyClazz(conf.getKeyClazz())
                .setValueClazz(conf.getValueClazz());
        Map<String, Object> vendorSpecificMap = conf.getVendorSpecificSettings();

        Object stGMS = vendorSpecificMap.get("start.gms");
        boolean startGMS = false;
        if (stGMS != null) {
            if (stGMS instanceof String) {
                try {
                    startGMS = Boolean.valueOf((String) stGMS);
                } catch (Throwable th) {
                    //Ignore
                }
            } else if (stGMS instanceof Boolean) {
                startGMS = (Boolean) stGMS;
            }
        }

        Object cacheLocally = vendorSpecificMap.get("local.caching");
        boolean enableLocalCaching = false;
        if (cacheLocally != null) {
            if (cacheLocally instanceof String) {
                try {
                    enableLocalCaching = Boolean.valueOf((String) cacheLocally);
                } catch (Throwable th) {
                    //Ignore
                }
            } else if (cacheLocally instanceof Boolean) {
                enableLocalCaching = (Boolean) stGMS;
            }
        }

        ClassLoader cl = (ClassLoader) vendorSpecificMap.get("class.loader");
        if (cl == null) {
            cl = conf.getValueClazz().getClassLoader();
        }
        dsConf.setClassLoader(cl)
                .setStartGMS(startGMS)
                .setCacheLocally(enableLocalCaching);

        boolean asyncReplication = vendorSpecificMap.get("async.replication") == null
                ? true : (Boolean) vendorSpecificMap.get("async.replication");
        dsConf.setDoASyncReplication(asyncReplication);

        dsConf.setObjectInputOutputStreamFactory(new DefaultObjectInputOutputStreamFactory());

        dsConf.addCommand(new StoreableSaveCommand<K, V>());
        dsConf.addCommand(new StoreableFullSaveCommand<K, V>());
        dsConf.addCommand(new SimpleAckCommand<K, V>());
        dsConf.addCommand(new StoreableRemoveCommand<K, V>());
        dsConf.addCommand(new StoreableTouchCommand<K, V>());
        dsConf.addCommand(new StoreableLoadRequestCommand<K, V>());
        dsConf.addCommand(new StoreableBroadcastLoadRequestCommand<K, V>());
        dsConf.addCommand(new StoreableLoadResponseCommand<K, V>());
        dsConf.addCommand(new StoreableRemoveCommand<K, V>());
        dsConf.addCommand(new StaleCopyRemoveCommand<K, V>());
        dsConf.addCommand(new SizeRequestCommand<K, V>());
        dsConf.addCommand(new SizeResponseCommand<K, V>());

        dsConf.addCommand(new ListBackingStoreConfigurationCommand());
        dsConf.addCommand(new ListBackingStoreConfigurationResponseCommand());
        dsConf.addCommand(new ListReplicaStoreEntriesCommand(null));

        KeyMapper keyMapper = (KeyMapper) vendorSpecificMap.get("key.mapper");
        if (keyMapper != null) {
            dsConf.setKeyMapper(keyMapper);
        }

        dsConf.setIdleEntryDetector(
                new IdleEntryDetector<K, V>() {
                    @Override
                    public boolean isIdle(DataStoreEntry<K, V> kvDataStoreEntry, long nowInMillis) {
                        V v = kvDataStoreEntry.getV();
                        boolean result = v != null && v._storeable_getMaxIdleTime() > 0
                            && v._storeable_getLastAccessTime() + v._storeable_getMaxIdleTime() < nowInMillis;

                        if (result) {
                            System.out.println("**Removing expired entries: " + v);
                        } else {

                            System.out.println("**Entry " + v + " is still active...");
                        }
                        return result;
                    }
                }
        );

        framework = new ReplicationFramework<K, V>(dsConf);

        replicaStore = framework.getReplicaStore();

        localCachingEnabled = framework.getDataStoreConfigurator().isCacheLocally();
        
        RepliatedBackingStoreRegistry.registerStore(conf.getStoreName(), conf, framework.getDataStoreContext());
    }

    @Override
    public V load(K key, String versionInfo) throws BackingStoreException {
        long version = Long.MIN_VALUE;
        try {
            version = Long.valueOf(versionInfo);
        } catch (Exception ex) {
            //TODO
        }

        return doLoad(key, Long.valueOf(versionInfo));
    }

    private V doLoad(K key, long version)
            throws BackingStoreException {

        V v = null;
        DataStoreEntry<K, V> entry = replicaStore.getEntry(key);
        if (entry != null) {
            if (!entry.isRemoved()) {
                v = entry.getV();
                if (v == null || v._storeable_getVersion() < version) {
                    entry.setV(null);
                    v = null;
                } else {
                    foundLocallyCount.incrementAndGet();
                }
            } else {
                return null; //Because it is already removed
            }
        }

        if (v == null) {
            String replicachoices = framework.getKeyMapper().getReplicaChoices(framework.getGroupName(), key);
            String[] replicaHint = replicachoices.split(":");
            _logger.log(Level.INFO, "ReplicatedDataStore: For Key=" + key
                                            + "; ReplicaChoices: " + replicachoices);
            try {
                String respondingInstance = null;
                for (int replicaIndex = 0; (replicaIndex < replicaHint.length) && (replicaIndex < MAX_REPLICA_TRIES); replicaIndex++) {
                    String target = replicaHint[replicaIndex];
                    if (target == null || target.trim().length() == 0) {
                        continue;
                    }
                    simpleBroadcastCount.incrementAndGet();

                    StoreableLoadRequestCommand<K, V> command
                            = new StoreableLoadRequestCommand<K, V>(key, version, target);

                    _logger.log(Level.INFO, "StoreableReplicatedBackingStore: For Key=" + key
                            + "; Trying to load from Replica[" + replicaIndex + "]: " + replicaHint[replicaIndex]);

                    framework.execute(command);
                    v = command.getResult(3, TimeUnit.SECONDS);
                    if (v != null) {
                        respondingInstance = command.getRespondingInstanceName();
                        break;
                    }
                }

                if (v == null) {
                    broadcastLoadRequestCount.incrementAndGet();
                    StoreableBroadcastLoadRequestCommand<K, V> command
                            = new StoreableBroadcastLoadRequestCommand<K, V>(key, version);

                    _logger.log(Level.WARNING, "StoreableReplicatedBackingStore: For Key=" + key
                            + "; Performing load using broadcast ");
                    
                    framework.execute(command);
                    v = command.getResult(3, TimeUnit.SECONDS);
                    if (v != null) {
                        respondingInstance = command.getRespondingInstanceName();
                    }
                }

                if (v != null) {
                    entry = replicaStore.getEntry(key);
                    if (entry != null) {
                        synchronized (entry) {
                            if (!entry.isRemoved()) {
                                if (localCachingEnabled) {
                                    entry.setV(v);
                                }
                                _logger.log(Level.INFO, "StoreableReplicatedBackingStore: For Key=" + key
                                    + "; Successfully loaded data from " + respondingInstance);
                                entry.setReplicaInstanceName(respondingInstance);
                                //Note: Do not remove the stale replica now. We will
                                //  do that in save
                            } else {
                                _logger.log(Level.INFO, "StoreableReplicatedBackingStore: For Key=" + key
                                    + "; Got data from " + respondingInstance + ", but another concurrent thread removed the entry");
                            }
                        }
                    }
                }
            } catch (DataStoreException dseEx) {
                throw new BackingStoreException("Error during load", dseEx);
            }
        }

        return v;
    }

    @Override
    public String save(K key, V value, boolean isNew) throws BackingStoreException {
        String result = null;
        try {
            DataStoreEntry<K, V> entry = replicaStore.getOrCreateEntry(key);

            //Note: synchronizing the entire method or during framework.execute
            //  could result in deadlock if we support concurrent save from different VMs!!
            //  (because both entries will be locked by diff threads)
            synchronized (entry) {
                StoreableSaveCommand<K, V> cmd = new StoreableSaveCommand<K, V>(key, value);
                cmd.setEntry(entry);
                framework.execute(cmd);

                if (localCachingEnabled) {
                    entry.setV(value);
                }

                String oldLocation = entry.setReplicaInstanceName(cmd.getTargetName());

                result = cmd.getTargetName();

                if (oldLocation != null) {
                    StaleCopyRemoveCommand<K, V> staleCmd = new StaleCopyRemoveCommand<K, V>();
                    staleCmd.setKey(key);
                    staleCmd.setStaleTargetName(oldLocation);
                    framework.execute(staleCmd);
                }

                result = cmd.getKeyMappingInfo();
            }

            return result;
        } catch (DataStoreException dsEx) {
            throw new BackingStoreException("Error during save: " + key, dsEx);
        }
    }

    @Override
    public void remove(K key) throws BackingStoreException {
        try {
            DataStoreEntry<K, V> entry = replicaStore.getEntry(key);
            if (entry != null) {
                synchronized (entry) {
                    entry.markAsRemoved("Removed by BackingStore.remove");
                }
            }
            String[] targets = framework.getDataStoreContext().getKeyMapper().getCurrentMembers();

            if (targets != null) {
                for (String target : targets) {
                    StoreableRemoveCommand<K, V> cmd = new StoreableRemoveCommand<K, V>(key);
                    cmd.setTarget(target);
                    framework.execute(cmd);
                }
            }
        } catch (DataStoreException dsEx) {
            throw new BackingStoreException("Error during remove: " + key, dsEx);
        }
    }

    @Override
    public int removeExpired(long idleTime) throws BackingStoreException {
        return replicaStore.removeExpired();
    }

    @Override
    public int size() throws BackingStoreException {

        int result = 0;
        KeyMapper km = framework.getKeyMapper();
        String[] targets = km.getCurrentMembers();

        int targetCount = targets.length;
        SizeRequestCommand[] commands = new SizeRequestCommand[targetCount];

        for (int i=0; i<targetCount; i++) {
            commands[i] = new SizeRequestCommand(targets[i]);
            try {
                framework.execute(commands[i]);
            } catch (DataStoreException dse) {
                //TODO:
            }
        }

        for (int i=0; i<targetCount; i++) {
            result += commands[i].getResult();
        }
        
        return result;
    }

    @Override
    public void destroy() throws BackingStoreException {
        framework = null;
    }

    @Override
    public void updateTimestamp(K key, long t) {
        //Will be removed shortly
    }

    //TODO: @Override

    public String updateTimestamp(K key, Long version, Long accessTime, Long maxIdleTime) throws BackingStoreException {
        String result = "";
        try {
            DataStoreEntry<K, V> entry = replicaStore.getEntry(key);
            if (entry != null) {
                synchronized (entry) {
                    if (! entry.isRemoved()) {
                        if (entry.getReplicaInstanceName() != null) {
                            StoreableTouchCommand<K, V> cmd = new StoreableTouchCommand<K, V>(key, version, accessTime, maxIdleTime);
                            framework.execute(cmd);

                            result = entry.getReplicaInstanceName().equals(cmd.getTargetName())
                                    ? cmd.getTargetName() : "";
                        }
                    } else {
                        _logger.log(Level.WARNING, "Ignored updateTimeStamp as the entry is already removed. Key = " + key);
                    }
                }
            }
        } catch (DataStoreException dsEx) {
            throw new BackingStoreException("Error during load: " + key, dsEx);
        }

        return result;
    }

    public ReplicationFramework<K, V> getFramework() {
        return framework;
    }

    public DataStoreContext<K, V> getDataStoreContext() {
        return framework.getDataStoreContext();
    }
}