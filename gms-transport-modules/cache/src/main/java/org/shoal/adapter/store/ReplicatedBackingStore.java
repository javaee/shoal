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

package org.shoal.adapter.store;

import org.glassfish.ha.store.api.*;
import org.shoal.adapter.store.commands.*;
import org.shoal.ha.cache.api.*;
import org.shoal.ha.cache.impl.store.DataStoreEntry;
import org.shoal.ha.cache.impl.store.ReplicatedDataStore;
import org.shoal.ha.mapper.KeyMapper;

import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public class ReplicatedBackingStore<K extends Serializable, V extends Serializable>
        extends BackingStore<K, V> {

    private static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_DATA_STORE);

    private String storeName = "";

    private DataStore<K, V> dataStore;

    private ReplicatedBackingStoreFactory factory;

    private long defaultMaxIdleTimeInMillis;

    /*package*/ void setBackingStoreFactory(ReplicatedBackingStoreFactory factory) {
        this.factory = factory;
    }

    public BackingStoreFactory getBackingStoreFactory() {
        return factory;
    }

    public DataStoreContext<K, V> getDataStoreContext() {
        return dataStore == null ? null : ((ReplicatedDataStore) dataStore).getDataStoreContext();
    }

    @Override
    public void initialize(BackingStoreConfiguration<K, V> conf)
            throws BackingStoreException {
        super.initialize(conf);
        
        DataStoreContext<K, V> dsConf = new DataStoreContext<K, V>(conf);
        dataStore = DataStoreFactory.createDataStore(dsConf);

        storeName = dsConf.getStoreName();
    }

    @Override
    public V load(K key, String versionInfo) throws BackingStoreException {
        try {
            return dataStore.get(key);
        } catch (DataStoreException dsEx) {
            throw new BackingStoreException("Error during load: " + key, dsEx);
        }
    }

    @Override
    public String save(K key, V value, boolean isNew) throws BackingStoreException {
        try {
            return dataStore.put(key, value);
        } catch (DataStoreException dsEx) {
            throw new BackingStoreException("Error during save: " + key, dsEx);
        }
    }

    @Override
    public void remove(K key) throws BackingStoreException {
        try {
            if (dataStore != null) {
                dataStore.remove(key);
            }
        } catch (DataStoreException dsEx) {
            throw new BackingStoreException("Error during remove: " + key, dsEx);
        }
    }

    @Override
    public int removeExpired(long idleTime) throws BackingStoreException {
        return dataStore.removeIdleEntries(idleTime);
    }

    @Override
    public int size() throws BackingStoreException {
        return dataStore.size();
    }

    @Override
    public void close() throws BackingStoreException {
        destroy();    
    }

    @Override
    public void destroy() throws BackingStoreException {
        if (dataStore != null) {
            dataStore.close();
            _logger.log(Level.FINE, "** StoreName = " + storeName + " is destroyed ");
        } else {
            _logger.log(Level.FINE, "** StoreName = " + storeName + " is already destroyed ");
        }
        dataStore = null;
        factory = null;
    }

    @Override
    public void updateTimestamp(K key, long time) throws BackingStoreException {
        try {
            dataStore.touch(key, Long.MAX_VALUE - 1, time, defaultMaxIdleTimeInMillis);
        } catch (DataStoreException dsEx) {
            throw new BackingStoreException("Error during updateTimestamp: " + key, dsEx);
        }
    }

}
