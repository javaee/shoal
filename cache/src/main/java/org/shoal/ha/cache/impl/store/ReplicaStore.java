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
        //TODO Need to take care of out of order messages
    }

    public V get(K k) {
        DataStoreEntryHelper<K, V> helper = ctx.getDataStoreEntryHelper();
        DataStoreEntry<K, V> e = map.get(k);

        V v = null;
        try {
            if (e != null) {
                v = helper.getV(e);
            }
        } catch (DataStoreException dsEx) {
            dsEx.printStackTrace();
        }

        return v;
    }

    public void remove(K k) {
        map.remove(k);
    }

    public void touch(K k, long ts, long version, long ttl) {
        DataStoreEntry<K, V> e = map.get(k);
        if (e != null) {
            e.setLastAccessedAt(ts);
            e.setMaxIdleTime(ttl);
            e.setVersion(version);
        }
    }
}
