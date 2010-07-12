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

import org.shoal.ha.cache.api.*;
import org.shoal.ha.cache.impl.command.DirectedRemoveCommand;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mahesh Kannan
 */
public class ReplicaLocationHolder<K, V>
        implements Runnable {

    private DataStoreConfigurator<K, V> conf;

    private DataStoreContext<K, V> dsc;

    private ConcurrentHashMap<K, LocationInfo<K, V>> localCache =
            new ConcurrentHashMap<K, LocationInfo<K, V>>();

    private boolean cacheV;

    private DataStoreEntryHelper<K, V> dseHelper;

    public ReplicaLocationHolder(DataStoreConfigurator<K, V> conf, DataStoreContext<K, V> dsc) {
        this.conf = conf;
        this.dsc = dsc;

        this.cacheV = conf.isCacheLocally();
        System.out.println("*** ReplicaLocationHolder ==> " + cacheV);
        this.dseHelper = conf.getDataStoreEntryHelper();
    }

    public String put(K k, V v, String newLocation)
            throws DataStoreException {
        LocationInfo<K, V> le = localCache.get(k);
        DataStoreEntry<K, V> entry = null;
        if (le == null) {
            le = new LocationInfo<K, V>(k, null);
            entry = new DataStoreEntry<K, V>();
            le.setEntry(entry);
            LocationInfo<K, V> oldLE = localCache.putIfAbsent(k, le);
            if (oldLE != null) {
                le = oldLE;
            }
        } else {
            entry = le.getEntry();
        }

        if (cacheV) {
            dseHelper.updateState(k, entry, v);
        } else {
            dseHelper.updateMetadata(k, entry, v);
        }
        le.setUseV(cacheV);

        String staleLocation = le.setReplicaInstanceName(newLocation);
        if (staleLocation != null) {
            removeStaleData(k, staleLocation);
        }

        return le.getReplicaInstanceName();
    }

    public V get(K k)
            throws DataStoreException {
        V v = null;
        LocationInfo<K, V> entry = localCache.get(k);

        if ((entry != null) && (cacheV)) {
            if (entry.isUseV()) {
                v = dseHelper.getV(entry.getEntry());
            } else {
                v = conf.getDataStoreEntryHelper().getV(entry.getEntry());
            }

            System.out.println("**Loaded from local (non replica) cache: " + v);
        }

        return v;
    }

    public void remove(K k)
            throws DataStoreException {
        localCache.remove(k);
        System.out.println("*** ReplicaLocationHolder:remove ==> " + k);
    }

    public String touch(K k, long version, long accessTime, long maxIdle, String newLocation)
            throws DataStoreException {

        LocationInfo<K, V> le = localCache.get(k);
        DataStoreEntry<K, V> entry = null;
        if (le == null) {
            le = new LocationInfo<K, V>(k, null);
            entry = new DataStoreEntry<K, V>();
            le.setEntry(entry);
            LocationInfo<K, V> oldLE = localCache.putIfAbsent(k, le);
            if (oldLE != null) {
                le = oldLE;
            }
        } else {
            entry = le.getEntry();
        }

        entry.setVersion(version);
        entry.setLastAccessedAt(accessTime);
        entry.setMaxIdleTime(maxIdle);

        String staleLocation = le.setReplicaInstanceName(newLocation);
        le.setUseV(false);

        if (staleLocation != null) {
            removeStaleData(k, staleLocation);
        }

        return newLocation;
    }

    public int removeIdleEntries(long idleFor) {
        Thread th = new Thread(this);
        th.start();
        return 0;
    }

    public void close() {
        localCache = null;
    }

    public void run() {
        long now = System.currentTimeMillis();
        for (LocationInfo<K, V> le : localCache.values()) {
            DataStoreEntry<K, V> dse = le.getEntry();
            if (dse.getLastAccessedAt() < (now - dse.getMaxIdleTime())) {
                localCache.remove(le.getK());
            }
        }
    }

    private void removeStaleData(K k, String staleLocation)
        throws DataStoreException {
        DirectedRemoveCommand<K, V> cmd = new DirectedRemoveCommand<K, V>();
        cmd.setTargetName(staleLocation);
        cmd.setKey(k);
        dsc.getCommandManager().execute(cmd);
        System.out.println("*!!=> REMOVING STALE DATA from : " + staleLocation);
    }

    private static final class LocationInfo<K, V> {

        private K k;

        private V v;

        private DataStoreEntry<K, V> entry;

        private boolean useV = true;

        private String replicaInstanceName;

        private LocationInfo(K k, V v) {
            this.k = k;
            this.v = v;
            useV = true;
        }

        public K getK() {
            return k;
        }

        public V getV() {
            return v;
        }

        public void setV(V v) {
            this.v = v;
            useV = (v != null);
        }

        public DataStoreEntry<K, V> getEntry() {
            return entry;
        }

        public void setEntry(DataStoreEntry<K, V> entry) {
            this.entry = entry;
            useV = false;
        }

        public boolean isUseV() {
            return useV;
        }

        public void setUseV(boolean useV) {
            this.useV = useV;
        }

        public String getReplicaInstanceName() {
            return replicaInstanceName;
        }

        public String setReplicaInstanceName(String replicaInstanceName) {
            String staleLocation = this.replicaInstanceName;
            this.replicaInstanceName = replicaInstanceName;

            if (staleLocation != null) {
                staleLocation = staleLocation.equals(replicaInstanceName) ? null : staleLocation;
            }

            return staleLocation;
        }
    }
}