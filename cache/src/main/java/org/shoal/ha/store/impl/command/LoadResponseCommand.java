package org.shoal.ha.store.impl.command;

import org.shoal.ha.store.api.DataStoreContext;
import org.shoal.ha.store.api.DataStoreEntryHelper;
import org.shoal.ha.store.api.DataStoreException;
import org.shoal.ha.store.impl.util.ReplicationState;
import org.shoal.ha.store.impl.command.Command;
import org.shoal.ha.store.impl.util.*;
import org.shoal.ha.store.impl.command.ReplicationCommandOpcode;

import java.io.IOException;

/**
 * @author Mahesh Kannan
 */
public class LoadResponseCommand<K, V>
    extends Command<K, V> {

    private K key;

    private ReplicationState<K, V> state;

    public LoadResponseCommand() {
        this(null);
    }

    public LoadResponseCommand(K key) {
        super(ReplicationCommandOpcode.LOAD_RESPONSE);
        this.key = key;
    }

    public ReplicationState<K, V> getReplicationState() {
        return state;
    }

    public void setReplicationState(ReplicationState<K, V> state) {
        this.state = state;
    }

    public void setDestinationName(String target) {
        super.setTargetName(target);
    }

    @Override
    protected LoadResponseCommand<K, V> createNewInstance() {
        return new LoadResponseCommand<K, V>();
    }

    @Override
    public void writeCommandPayload(DataStoreContext<K, V> trans, ReplicationOutputStream ros) throws IOException {
        ros.write(Utility.longToBytes(getTokenId()));
//        System.out.println("** LoadResponseCommand: wrote key: " +
//                key + "; token: " + getTokenId());

        trans.getDataStoreKeyHelper().writeKey(ros, key);
        ros.write(state == null ? (new byte[] {0}) : (new byte[] {1}));
        if (state != null) {
            state.writeDataStoreEntry(trans, ros);
        }
    }

    @Override
    public void readCommandPayload(DataStoreContext<K, V> trans, byte[] data, int offset)
        throws IOException, DataStoreException {
        setTokenId(Utility.bytesToLong(data, offset));
        int transKeyLen = Utility.bytesToInt(data, offset+8);
        key = (K) trans.getDataStoreKeyHelper().readKey(data, offset+12);
        offset += 12 + (key == null ? 0 : transKeyLen);

        if (data[offset] != 0) {
            ReplicationState<K, V> state = new ReplicationState<K, V>();
            state.readDataStoreEntry(trans, data, offset+1);
            setReplicationState(state);
        }
    }

    @Override
    public void execute() {
        ResponseMediator respMed = getReplicationService().getResponseMediator();
        CommandResponse resp = respMed.getCommandResponse(getTokenId());
        if (resp != null) {
            System.out.println("RECEIVED LOAD RESPONSE: " + getTokenId());
            resp.setResult(getReplicationState());
        }
    }


}