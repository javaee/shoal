package org.shoal.ha.store.impl.store;

import org.shoal.ha.group.GroupService;
import org.shoal.ha.mapper.KeyMapper;
import org.shoal.ha.store.api.*;
import org.shoal.ha.store.impl.command.*;
import org.shoal.ha.store.impl.util.StringKeyMapper;

import java.util.Collection;

/**
 * @author Mahesh Kannan
 */
public class ReplicatedDataStore<K, V>
        implements DataStore<K, V> {

    private String storeName;

    private String instanceName;

    private String groupName;

    private GroupService gs;

    private CommandManager<K, V> cm;

    private DataStoreEntryHelper<K, V> transformer;

    public ReplicatedDataStore(String storeName, GroupService gs) {
        this(storeName, gs,
                new DefaultDataStoreEntryHelper<K, V>(Thread.currentThread().getContextClassLoader()),
                new StringKeyMapper<K>(gs.getGroupName()));
    }

    public ReplicatedDataStore(String storeName, GroupService gs,
                               DataStoreEntryHelper<K, V> helper) {
        this(storeName, gs, helper,
                new StringKeyMapper<K>(gs.getGroupName()));
    }

    public ReplicatedDataStore(String storeName, GroupService gs,
                               DataStoreEntryHelper<K, V> helper, KeyMapper<K> keyMapper) {
        this.storeName = storeName;
        this.gs = gs;
        this.instanceName = gs.getMemberName();
        this.groupName = gs.getGroupName();

        DataStoreContext<K, V> dsc = new DataStoreContext<K, V>(storeName, gs);
        this.transformer = helper;
        dsc.setDataStoreEntryHelper(helper);
        cm = new CommandManager<K, V>(dsc);
    }

    @Override
    public void put(K k, V v) {
        SaveCommand<K, V> cmd = new SaveCommand<K, V>();
        cmd.setValue(v);
        cm.execute(cmd);
    }

    @Override
    public void updateDelta(K k, Object obj) {
        UpdateDeltaCommand<K, V> cmd = new UpdateDeltaCommand<K, V>();
        cmd.setObject(obj);
        cm.execute(cmd);
    }

    @Override
    public V get(K k) {
        LoadRequestCommand<K, V> cmd = new LoadRequestCommand<K, V>(k);
        cm.execute(cmd);

        try {
            return transformer.getV(cmd.getReplicationEntry());
        } catch (DataStoreException dsEx) {
            //TODO Log?
            return null;
        }
    }

    @Override
    public void remove(K k) {
        RemoveCommand<K, V> cmd = new RemoveCommand<K, V>();
        cmd.setKey(k);
        cm.execute(cmd);
    }

    @Override
    public void touch(K k, long timestamp) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeIdleEntries(long idleFor) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection find(DataStoreEntryEvaluator<K, V> kvDataStoreEntryEvaluator) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void update(DataStoreEntryEvaluator<K, V> kvDataStoreEntryEvaluator) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
