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

package org.shoal.ha.cache.api;

import java.io.Serializable;
import java.util.Collection;

/**
 * A DataStore allows (#{Serializable} / #{Storable}) objects to be placed in the cache. The
 * cache itself is created and configured using a #{DataStoreFactory}.
 * 
 * @author Mahesh Kannan
 */
public interface DataStore<K, V> {

    /**
     * Creates or Replaces the object associated with key k.
     *
     * @param k The Key
     * @param v The value. The value must be either serializable of the DataStoreEntryHelper
     *  that is associated with this cache must be able to transform this into a serializable.
     */
    public String put(K k, V v)
            throws DataStoreException;

    /**
     * Returns the value to which the specified key is mapped in this cache.
     *
     * @param k  The key
     * @return The value if the association exists or null.
     */
    public V get(K k)
            throws DataStoreException;
    /**
     * Returns the value to which the specified key is mapped in this cache.
     *
     * @param k  The key
     * @return The value if the association exists or null.
     */
    public V get(K k, String[] replicaHint)
            throws DataStoreException;

    /**
     * Removes the mapping between the key and the object.
     *
     * @param k The key
     */
    public void remove(K k)
            throws DataStoreException;

    /**
     * Updates the timestamp associated with this entry. see #{removeIdleEntries}
     *
     * @param k The key
     */
    public String touch(K k, long version, long timeStamp, long ttl)
            throws DataStoreException;

    /**
     * Performs an incremental update of the value associated with this key. The
     *  DataStore's DataStoreEntryHelper.updateState() method will be called with the key,
     *  the DataStoreEnrty and the delta. If no value is associated with K, then a
     *  hollow DataStoreEntry will be passed to DataStoreEntryHelper.updateState().
     *
     * @param k The key
     * @param delta The object whose eval() method will be called.
     *
     * @see #{DataStoreEntryEvaluator.eval}
     */
    public String updateDelta(K k, V delta)
                throws DataStoreException;

    /**
     * Finds / Retrieves values that satisfy a criteria as dictated by the evaluator.
     *  The evaluator's eval() method will be called with both the key and the
     *  existing value.
     *
     * @param evaluator The StoreEntryEvaluator whose eval() method will be called.
     *
     * @see #{DataStoreEntryEvaluator.eval}
     */
    public Collection find(DataStoreEntryEvaluator<K, V> evaluator);

    //TODO: findByCriteria() will be ported from Sailfin soon.
    
    /**
     * Updates all existing values associated with a key using the evaluator. The
     *  evaluator's eval() method will be called with both the key and the
     *  existing value.
     *
     * @param evaluator The StoreEntryEvaluator whose eval() method will be called.
     *
     * @see #{DataStoreEntryEvaluator.eval}
     */
    public void update(DataStoreEntryEvaluator<K, V> evaluator);

    /**
     * Removes all entries that were not accessed for more than 'idlefor' millis
     *
     * @param idleFor Time in milli seconds
     */
    public int removeIdleEntries(long idleFor);

    /**
     * Close this datastore. This causes all data to be removed(?)
     */
    public void close();

}
