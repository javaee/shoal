package org.shoal.ha.store.api;

import java.util.Collection;

/**
 * A DataStore allows (#{Serializable} / #{Storable}) objects to be placed in the store. The
 * store itself is created and configured using a #{DataStoreFactory}.
 * 
 * @author Mahesh Kannan
 */
public interface DataStore<K, V> {

    /**
     * Creates or Replaces the object associated with key k.
     *
     * @param k The Key
     * @param v The value. The value must be either serializable of the DataStoreEntryHelper
     *  that is associated with this store must be able to transform this into a serializable.
     */
    public void put(K k, V v);

    /**
     * Returns the value to which the specified key is mapped in this store.
     *
     * @param k  The key
     * @return The value if the association exists or null.
     */
    public V get(K k);

    /**
     * Removes the mapping between the key and the object.
     *
     * @param k The key
     */
    public void remove(K k);

    /**
     * Updates the timestamp associated with this entry. see #{removeIdleEntries}
     *
     * @param k The key
     * @param timestamp The current time
     */
    public void touch(K k, long timestamp);

    /**
     * Performs an incremental update of the value associated with this key. The
     *  DataStore's DataStoreEntryHelper.updateDelta() method will be called with the key,
     *  the DataStoreEnrty and the delta. If no value is associated with K, then a
     *  hollow DataStoreEntry will be passed to DataStoreEntryHelper.updateDelta().
     *
     * @param k The key
     * @param delta The object whose eval() method will be called.
     *
     * @see #{DataStoreEntryEvaluator.eval}
     */
    public void updateDelta(K k, Object delta);

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
    public void removeIdleEntries(long idleFor);

    /**
     * Close this datastore. This causes all data to be removed(?)
     */
    public void close();

}
