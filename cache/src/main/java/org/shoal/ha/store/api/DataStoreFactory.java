package org.shoal.ha.store.api;

import org.shoal.ha.group.GroupService;
import org.shoal.ha.group.GroupServiceFactory;
import org.shoal.ha.mapper.KeyMapper;
import org.shoal.ha.store.impl.store.ReplicatedDataStore;

/**
 * @author Mahesh Kannan
 */
public class DataStoreFactory {

    public static <K, V> DataStore<K, V> createDataStore(String storeName, String instanceName, String groupName) {
        GroupService gs = GroupServiceFactory.getInstance().getGroupService(instanceName, groupName);
        return new ReplicatedDataStore<K, V>(storeName, gs);
    }

    public static <K, V> DataStore<K, V> createDataStore(String storeName, String instanceName, String groupName,
                                                  DataStoreEntryHelper<K, V> helper) {
        GroupService gs = GroupServiceFactory.getInstance().getGroupService(instanceName, groupName);
        return new ReplicatedDataStore<K, V>(storeName, gs, helper);
    }

    public static <K, V> DataStore<K, V> createDataStore(String storeName, String instanceName, String groupName,
                                                  DataStoreEntryHelper<K, V> helper, KeyMapper<K> keyMapper) {
        GroupService gs = GroupServiceFactory.getInstance().getGroupService(instanceName, groupName);
        return new ReplicatedDataStore<K, V>(storeName, gs, helper, keyMapper);
    }

}
