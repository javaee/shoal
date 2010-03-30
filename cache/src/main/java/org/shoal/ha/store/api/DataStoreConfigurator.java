package org.shoal.ha.store.api;

import org.shoal.ha.mapper.KeyMapper;

/**
 * @author Mahesh Kannan
 */
public class DataStoreConfigurator<K, V> {

    private K kClazz;

    private V vClazz;

    private KeyMapper keyMapper;

    private DataStoreKeyHelper<K> dataStoreKeyHelper;

    private DataStoreEntryHelper<K, V> dataStoreEntryHelper;

}
