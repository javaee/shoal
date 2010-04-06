package org.shoal.ha.cache.impl.store;

import org.shoal.ha.cache.api.DataStoreContext;
import org.shoal.ha.cache.api.DataStoreEntry;
import org.shoal.ha.cache.api.DataStoreEntryHelper;
import org.shoal.ha.cache.api.DataStoreException;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mahesh Kannan
 */
public class ReplicaStore<K, V> {

    private DataStoreContext<K, V> ctx;

    private ConcurrentHashMap<K, DataStoreEntry<K, V>> map =
            new ConcurrentHashMap<K, DataStoreEntry<K, V>>();

    public ReplicaStore(DataStoreContext<K, V> ctx) {
        this.ctx = ctx;
    }

    public void put(K k, V v) {
        DataStoreEntryHelper<K, V> helper = ctx.getDataStoreEntryHelper();
        DataStoreEntry<K, V> e = helper.createDataStoreEntry(k, v);
        map.put(k, e);
        System.out.println("SaveCommand["+ctx.getServiceName() +"] put(" + k + ", " + e.getClass().getName() + ");");
        //TODO Need to take care of out of order messages
    }

    public V get(K k) {
        System.out.println("ReplicaStore.get["+ctx.getServiceName() +"] get(" + k + ") : size: " + map.size() + ");");
        DataStoreEntryHelper<K, V> helper = ctx.getDataStoreEntryHelper();
        DataStoreEntry<K, V> e = map.get(k);

        System.out.println("ReplicaStore.get.2["+ctx.getServiceName() +"] get(" + k + ") : e: " + e + ");");

        V v = null;
        try {
            v = helper.getV(e);
        } catch (DataStoreException dsEx) {
            dsEx.printStackTrace();
        }
        System.out.println("Load["+ctx.getServiceName() +"] get(" + k + ", " + v + ");");

        return v;
    }
}
