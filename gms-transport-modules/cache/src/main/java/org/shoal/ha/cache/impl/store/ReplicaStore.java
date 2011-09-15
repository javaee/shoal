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

import org.shoal.ha.cache.api.*;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public class ReplicaStore<K, V> {

    private static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_COMMAND);

    private DataStoreContext<K, V> ctx;

    private ConcurrentHashMap<K, DataStoreEntry<K, V>> map =
            new ConcurrentHashMap<K, DataStoreEntry<K, V>>();

    private AtomicInteger replicaEntries = new AtomicInteger(0);

    private IdleEntryDetector<K, V> idleEntryDetector;

    private AtomicBoolean expiredEntryRemovalInProgress = new AtomicBoolean(false);

    public ReplicaStore(DataStoreContext<K, V> ctx) {
        this.ctx = ctx;
    }

    public void setIdleEntryDetector(IdleEntryDetector<K, V> idleEntryDetector) {
        this.idleEntryDetector = idleEntryDetector;
    }

    //This is called during loadRequest. We do not want LoadRequests
    //  to call getOrCreateEntry()
    public DataStoreEntry<K, V> getEntry(K k) {
        return map.get(k);

    }

    public DataStoreEntry<K, V> getOrCreateEntry(K k) {
        DataStoreEntry<K, V> entry = map.get(k);
        if (entry == null) {
            entry = new DataStoreEntry<K, V>();
            entry.setKey(k);
            DataStoreEntry<K, V> tEntry = map.putIfAbsent(k, entry);
            if (tEntry != null) {
                entry = tEntry;
            } else {
                replicaEntries.incrementAndGet();
            }
        }

        return entry;
    }

    public V getV(K k, ClassLoader cl)
        throws DataStoreException {

        V result = null;
        DataStoreEntry<K, V> entry = map.get(k);
        synchronized (entry) {
            result = ctx.getDataStoreEntryUpdater().getV(entry);
        }
        
        return result;

    }

    public void remove(K k) {
        DataStoreEntry<K, V> dse = map.remove(k);
        if (dse != null) {
            synchronized (dse) {
                dse.markAsRemoved("Removed");
            }

            replicaEntries.decrementAndGet();
        }
    }

    public int size() {
        return map.size();
    }

    public int removeExpired() {
        int result = 0;
        ctx.getDataStoreMBean().incrementRemoveExpiredCallCount();
        if (expiredEntryRemovalInProgress.compareAndSet(false, true)) {
            try {
                if (idleEntryDetector != null) {
                    long now = System.currentTimeMillis();
                    Iterator<DataStoreEntry<K, V>> iterator = map.values().iterator();
                    while (iterator.hasNext()) {
                        DataStoreEntry<K, V> entry = iterator.next();
                        synchronized (entry) {
                            if (idleEntryDetector.isIdle(entry, now)) {
                                entry.markAsRemoved("Idle");
                                _logger.log(Level.FINE, "ReplicaStore removing (idle) key: " + entry.getKey());
                                iterator.remove();
                                result++;
                            }
                        }
                    }
                } else {
                    //System.out.println("ReplicaStore.removeExpired idleEntryDetector is EMPTY");
                }
            } finally {
                expiredEntryRemovalInProgress.set(false);
            }

            ctx.getDataStoreMBean().incrementRemoveExpiredEntriesCount(result);
        } else {
            _logger.log(Level.FINEST, "ReplicaStore.removeExpired(). Skipping since there is already another thread running");
        }

        return result;
    }

    public Collection<K> keys() {
        return map.keySet();
    }

    public Collection<DataStoreEntry<K, V>> values() {
        return map.values();
    }

}
