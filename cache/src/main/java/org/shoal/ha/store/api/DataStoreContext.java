package org.shoal.ha.store.api;

import org.shoal.ha.group.GroupService;
import org.shoal.ha.mapper.KeyMapper;
import org.shoal.ha.store.impl.command.CommandManager;
import org.shoal.ha.store.impl.util.ResponseMediator;

/**
 * @author Mahesh Kannan
 */
public class DataStoreContext<K, V> {

    private String serviceName;

    private String instanceName;

    private String groupName;

    private DataStoreKeyHelper<K> dataStoreKeyHelper;

    private DataStoreEntryHelper<K, V> dataStoreEntryHelper;

    private CommandManager<K, V> cm;

    private KeyMapper keyMapper;

    private ResponseMediator responseMediator;


    public DataStoreContext(String serviceName, GroupService gs) {
        this.serviceName = serviceName;
        this.instanceName = gs.getMemberName();
        this.groupName = gs.getGroupName();
        this.cm = new CommandManager<K, V>(this);
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public String getGroupName() {
        return groupName;
    }

    public DataStoreKeyHelper<K> getDataStoreKeyHelper() {
        return dataStoreKeyHelper;
    }

    public void setDataStoreKeyHelper(DataStoreKeyHelper<K> dataStoreKeyHelper) {
        this.dataStoreKeyHelper = dataStoreKeyHelper;
    }

    public DataStoreEntryHelper<K, V> getDataStoreEntryHelper() {
        return dataStoreEntryHelper;
    }

    public void setDataStoreEntryHelper(DataStoreEntryHelper<K, V> dataStoreEntryHelper) {
        this.dataStoreEntryHelper = dataStoreEntryHelper;
    }

    public KeyMapper getKeyMapper() {
        return keyMapper;
    }

    public void setKeyMapper(KeyMapper keyMapper) {
        this.keyMapper = keyMapper;
    }

    public CommandManager<K, V> getCommandManager() {
        return cm;
    }

    public ResponseMediator getResponseMediator() {
        return responseMediator;
    }
}
