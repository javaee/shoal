package org.shoal.ha.store.impl.command;

import org.shoal.ha.store.api.DataStoreContext;
import org.shoal.ha.store.api.DataStoreEntryHelper;
import org.shoal.ha.store.api.DataStoreException;
import org.shoal.ha.store.impl.util.ReplicationOutputStream;
import org.shoal.ha.store.impl.command.Command;
import org.shoal.ha.store.impl.command.ReplicationCommandOpcode;
import org.shoal.ha.store.impl.util.ReplicationIOUtils;
import org.shoal.ha.store.impl.util.Utility;

import java.io.IOException;

/**
 * @author Mahesh Kannan
 */
public class RemoveCommand<K, V>
    extends Command<K, V> {

    private K key;

    public RemoveCommand() {
        super(ReplicationCommandOpcode.REMOVE);
    }

    public void setKey(K key) {
        this.key = key;
    }

    @Override
    protected RemoveCommand<K, V> createNewInstance() {
        return new RemoveCommand<K, V>();
    }

    @Override
    public void writeCommandPayload(DataStoreContext<K, V> trans, ReplicationOutputStream ros) throws IOException {
        trans.getDataStoreKeyHelper().writeKey(ros, key);
        System.out.println("Just wrote REMOVE Command: " + key);
    }

    @Override
    public void readCommandPayload(DataStoreContext<K, V> trans, byte[] data, int offset)
        throws DataStoreException {
        int transKeyLen = Utility.bytesToInt(data, offset);
        key = (K) trans.getDataStoreKeyHelper().readKey(data, offset+4);
    }

    @Override
    public void execute() {
        //getReplicaCache().remove(key);
        System.out.println("RemoveCommand ["+getReplicationService().getServiceName()
                +"] received: " + key);
    }

}