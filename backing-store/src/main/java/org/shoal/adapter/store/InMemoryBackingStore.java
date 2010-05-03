package org.shoal.adapter.store;

import org.glassfish.ha.store.spi.BackingStore;
import org.glassfish.ha.store.spi.BackingStoreException;
import org.shoal.ha.cache.api.DataStore;
import org.shoal.ha.cache.api.DataStoreException;
import org.shoal.ha.cache.api.DataStoreFactory;
import org.shoal.ha.cache.api.DataStoreKeyHelper;
import org.shoal.ha.cache.impl.util.ReplicationOutputStream;
import org.shoal.ha.cache.impl.util.StringKeyHelper;

import java.io.IOException;
import java.util.Properties;

/**
 * @author Mahesh Kannan
 *
 */
public class InMemoryBackingStore<K, V>
    extends BackingStore<K, V> {

    DataStore<K, V> dataStore;

    @Override
    protected void initialize(String storeName, Class<K> keyClazz, Class<V> vClazz, Properties props) {
        super.initialize(storeName, keyClazz, vClazz, props);
        String instName = props.getProperty("instance.name");
        String groupName = props.getProperty("cluster.name");

        DataStoreKeyHelper<K> keyHelper;

        if (keyClazz == String.class) {
            keyHelper = (DataStoreKeyHelper<K>) new StringKeyHelper();
        } else {

        }
        dataStore = DataStoreFactory.createDataStore(storeName, instName, groupName,
                keyClazz, vClazz, Thread.currentThread().getContextClassLoader());
    }

    @Override
    public V load(K key, String version) throws BackingStoreException {
        return dataStore.get(key);
    }

    @Override
    public void save(K key, V value, boolean isNew) throws BackingStoreException {
        dataStore.put(key, value);
    }

    @Override
    public void remove(K key) throws BackingStoreException {
        dataStore.remove(key);
    }

    @Override
    public int removeExpired() throws BackingStoreException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int size() throws BackingStoreException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void destroy() throws BackingStoreException {
        dataStore.close();
    }



    private static class BackingStoreKeyHelper
        implements DataStoreKeyHelper<Object> {

        @Override
        public void writeKey(ReplicationOutputStream ros, Object o) throws IOException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public Object readKey(byte[] data, int index) throws DataStoreException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    
}
