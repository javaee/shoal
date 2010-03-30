package org.shoal.ha.store.impl.command;

import org.shoal.ha.store.api.DataStoreContext;
import org.shoal.ha.store.api.DataStoreEntry;
import org.shoal.ha.store.api.DataStoreEntryHelper;
import org.shoal.ha.store.impl.util.ReplicationOutputStream;

import java.io.IOException;

/**
 * @author Mahesh Kannan
 */
public class SaveCommand<K, V>
    extends Command<K, V> {

    private V v;

    public SaveCommand() {
        super(ReplicationCommandOpcode.SAVE);
    }

    public void setValue(V v) {
        this.v = v;
    }

    @Override
    protected SaveCommand<K, V> createNewInstance() {
        return new SaveCommand<K, V>();
    }

    @Override
    public void writeCommandPayload(DataStoreContext<K, V> trans, ReplicationOutputStream bos)
        throws IOException {
        trans.getDataStoreEntryHelper().writeObject(bos, v);
    }

    @Override
    public void readCommandPayload(DataStoreContext<K, V> ctx, byte[] data, int offset)
        throws IOException {
        DataStoreEntry<K, V> entry = ctx.getDataStoreEntryHelper().createDataStoreEntry();
        entry.readDataStoreEntry(ctx, data, offset);
        //setReplicationEntry(entry);
    }

    @Override
    public void execute() {
        //getReplicaCache().put(entry);
//        System.out.println("SaveCommand["+getReplicationService().getMyName()+"] received: " + entry);
    }

}
