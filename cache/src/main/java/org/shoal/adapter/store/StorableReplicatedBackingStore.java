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
import org.shoal.ha.cache.api.*;
import org.shoal.ha.cache.impl.store.ReplicaStore;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Mahesh Kannan
 */
public class StorableReplicatedBackingStore<K extends Serializable, V extends Storeable>
        extends BackingStore<K, V> {

    private ReplicationFramework<K, V> framework;

    private ReplicaStore<K, V> replicaStore;

    boolean localCachingEnabled;

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

        boolean doSyncReplication = Boolean.valueOf((String) vendorSpecificMap.get("synchronous.replication"));
        dsConf.setDoSyncReplication(doSyncReplication);

        dsConf.setObjectInputOutputStreamFactory(new DefaultObjectInputOutputStreamFactory());

        dsConf.addCommand(new StoreableSaveCommand<K, V>());
        dsConf.addCommand(new StoreableRemoveCommand<K, V>());
        dsConf.addCommand(new StoreableTouchCommand<K, V>());
        dsConf.addCommand(new StoreableBroadcastLoadRequestCommand<K, V>());
        dsConf.addCommand(new StoreableLoadResponseCommand<K, V>());
        dsConf.addCommand(new StaleCopyRemoveCommand<K, V>());

        framework = new ReplicationFramework<K, V>(dsConf);

        replicaStore = framework.getReplicaStore();

        localCachingEnabled = framework.getDataStoreConfigurator().isCacheLocally();
    }

    @Override
    public V load(K key, String strVersion) throws BackingStoreException {
        DataStoreEntry<K, V> entry = replicaStore.getOrCreateEntry(key);
        Long version = strVersion == null ? Long.MIN_VALUE : Long.valueOf(strVersion);
        V v = null;
        synchronized (entry) {
            if (!entry.isRemoved()) {
                v = entry.getV();
                if (v == null || v._storeable_getVersion() < version) {
                    v = null;
                }
            } else {
                //Because it is already removed
                return null;
            }
        }

        if (v == null) {
            try {
                //NOTE: Be careful with LoadREquest command. Do not synchronize while executing the command
                //  as the response may want to set the result on the entry
                StoreableBroadcastLoadRequestCommand<K, V> cmd =
                        new StoreableBroadcastLoadRequestCommand<K, V>(key, version == null ? null : Long.valueOf(version));
                framework.execute(cmd);
                v = cmd.getResult(6, TimeUnit.SECONDS);
                entry.setV(v);

                entry = replicaStore.getOrCreateEntry(key);

                synchronized (entry) {
                    if (!entry.isRemoved()) {
                        entry.setReplicaInstanceName(cmd.getRespondingInstanceName());

                        if (localCachingEnabled) {
                            entry.setV(v);
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

                if (!entry.isRemoved()) {
                    if (localCachingEnabled) {
                        entry.setV(value);
                    }
                    entry.setReplicaInstanceName(cmd.getTargetName());

                    result = cmd.getTargetName();
                }
            }

            return result;
        } catch (DataStoreException dsEx) {
            throw new BackingStoreException("Error during save: " + key, dsEx);
        }
    }

    @Override
    public void remove(K key) throws BackingStoreException {
        try {
            DataStoreEntry<K, V> entry = replicaStore.getOrCreateEntry(key);
            synchronized (entry) {
                entry.markAsRemoved("Removed by BackingStore.remove");
            }
            StoreableRemoveCommand<K, V> cmd = new StoreableRemoveCommand<K, V>(key);
            framework.execute(cmd);
        } catch (DataStoreException dsEx) {
            throw new BackingStoreException("Error during remove: " + key, dsEx);
        }
    }

    @Override
    public int removeExpired(long idleTime) throws BackingStoreException {
        return 0; //TODO
    }

    @Override
    public int size() throws BackingStoreException {
        return framework.getReplicaStore().size();
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
            DataStoreEntry<K, V> entry = replicaStore.getOrCreateEntry(key);
            synchronized (entry) {
                if (! entry.isRemoved()) {
                    if (entry.getReplicaInstanceName() != null) {
                        StoreableTouchCommand<K, V> cmd = new StoreableTouchCommand<K, V>(key, version, accessTime, maxIdleTime);
                        framework.execute(cmd);

                        result = entry.getReplicaInstanceName().equals(cmd.getTargetName())
                                ? cmd.getTargetName() : "";
                    }
                } else {
                    //entry already removed
                }
            }

        } catch (DataStoreException dsEx) {
            throw new BackingStoreException("Error during load: " + key, dsEx);
        }

        return result;
    }

}