package org.shoal.ha.store.impl.command;

import org.shoal.ha.store.api.DataStoreContext;
import org.shoal.ha.store.api.DataStoreEntryEvaluator;
import org.shoal.ha.store.api.DataStoreEntryHelper;
import org.shoal.ha.store.api.DataStoreException;
import org.shoal.ha.store.impl.util.ReplicationOutputStream;

import java.io.IOException;

/**
 * @author Mahesh Kannan
 */
public class UpdateDeltaCommand<K, V>
    extends Command<K, V> {

    private Object obj;

    public UpdateDeltaCommand() {
        super(ReplicationCommandOpcode.SAVE_WITH_DSEE);
    }

    public void setObject(Object obj) {
        this.obj = obj;
    }

    @Override
    protected UpdateDeltaCommand<K, V> createNewInstance() {
        return new UpdateDeltaCommand<K, V>();
    }

    @Override
    public void writeCommandPayload(DataStoreContext<K, V> trans, ReplicationOutputStream ros)
        throws IOException {
        trans.getDataStoreEntryHelper().writeObject(ros, obj);
    }

    @Override
    public void readCommandPayload(DataStoreContext<K,V> trans, byte[] data, int offset)
        throws DataStoreException {
        trans.getDataStoreEntryHelper().readObject(data, offset);
    }

    @Override
    public void execute() {
        //getReplicaCache().put(eval);
//        System.out.println("SaveCommand["+getReplicationService().getMyName()+"] received: " + eval);
    }

}