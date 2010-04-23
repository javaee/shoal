package org.shoal.gf.store;

import org.glassfish.ha.store.spi.BackingStore;
import org.glassfish.ha.store.spi.BackingStoreException;
import org.glassfish.ha.store.spi.BackingStoreFactory;
import org.glassfish.ha.store.spi.BatchBackingStore;
import org.shoal.ha.cache.api.DataStore;
import org.shoal.ha.cache.api.DataStoreFactory;

import java.util.Properties;

/**
 * @author Mahesh Kannan
 */
public class ReplicationBackingStoreFactory
    implements BackingStoreFactory {

    private String instanceName;

    private String groupName;

    @Override
    public <K, V> BackingStore<K, V> createBackingStore(String storeName, Class<K> keyClazz, Class<V> vClazz, Properties env) throws BackingStoreException {
        DataStore<K, V> ds = (DataStore<K, V>) DataStoreFactory.createDataStore(storeName, instanceName, groupName);

        return null;
    }

    @Override
    public BatchBackingStore createBatchBackingStore(Properties env) throws BackingStoreException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
