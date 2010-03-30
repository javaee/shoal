package org.shoal.ha.store.impl.command;

import org.shoal.ha.store.api.DataStoreContext;
import org.shoal.ha.store.api.DataStoreEntry;
import org.shoal.ha.store.api.DataStoreEntryHelper;
import org.shoal.ha.store.api.DataStoreException;
import org.shoal.ha.store.impl.util.ReplicationState;
import org.shoal.ha.store.impl.command.Command;
import org.shoal.ha.store.impl.util.ReplicationOutputStream;
import org.shoal.ha.store.impl.command.ReplicationCommandOpcode;

import java.io.IOException;

/**
 * @author Mahesh Kannan
 */
public class LoadRequestCommand<K, V>
        extends Command<K, V> {

    private K key;

    private DataStoreEntry<K, V> entry;

    public LoadRequestCommand() {
        this(null);
    }

    public LoadRequestCommand(K key) {
        super(ReplicationCommandOpcode.LOAD_REQUEST);
        this.key = key;
    }

    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public DataStoreEntry<K, V> getReplicationEntry() {
        return entry;
    }

    @Override
    protected LoadRequestCommand<K, V> createNewInstance() {
        return new LoadRequestCommand<K, V>();
    }

    @Override
    public void writeCommandPayload(DataStoreContext<K, V> trans, ReplicationOutputStream ros) throws IOException {
        trans.getDataStoreKeyHelper().writeKey(ros, key);
    }

    @Override
    public void readCommandPayload(DataStoreContext<K, V> trans, byte[] data, int offset)
        throws DataStoreException {
        setKey((K) trans.getDataStoreKeyHelper().readKey(data, offset));
    }

    @Override
    public void execute() {
        //System.out.println("LoadRequestCommand: Received FROM: " + ctx.getInitiatorName() + " - " + this);
        //ReplicationState result = getReplicationService().getReplicaCache().get(key);
        LoadResponseCommand<K, V> rsp = new LoadResponseCommand<K, V>(key);
        if (isMarkedForResponseRequired()) {
            rsp.setTokenId(getTokenId());
        }
        rsp.setReplicationState((ReplicationState<K, V>) result);
        //rsp.setDestinationName(ctx.getInitiatorName());
        getCommandManager().execute(rsp);
    }

    public DataStoreEntry<K, V> getResult() {
        if (isMarkedForResponseRequired()) {
            entry = (DataStoreEntry) getResult(15000);
        }

        return entry;
    }
}