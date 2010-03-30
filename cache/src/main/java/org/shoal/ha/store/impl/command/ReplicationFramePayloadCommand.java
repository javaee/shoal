package org.shoal.ha.store.impl.command;

import org.shoal.ha.store.api.DataStoreContext;
import org.shoal.ha.store.api.DataStoreEntryHelper;
import org.shoal.ha.store.api.DataStoreException;
import org.shoal.ha.store.impl.command.Command;
import org.shoal.ha.store.impl.util.ReplicationOutputStream;
import org.shoal.ha.store.impl.command.ReplicationFrame;

import java.io.IOException;
import java.util.List;

/**
 * @author Mahesh Kannan
 */
public class ReplicationFramePayloadCommand<K, V>
    extends Command<K, V> {

    private ReplicationFrame<K, V> frame;

    public ReplicationFramePayloadCommand() {
        super(ReplicationCommandOpcode.REPLICATION_FRAME_PAYLOAD);
    }

    public void setReplicationFrame(ReplicationFrame<K, V> frame) {
        this.frame = frame;
    }

    @Override
    protected ReplicationFramePayloadCommand<K, V> createNewInstance() {
        return new ReplicationFramePayloadCommand<K, V>();
    }

    @Override
    public void writeCommandPayload(DataStoreContext<K, V> trans, ReplicationOutputStream bos) throws IOException {
        bos.write(frame.getSerializedData());
    }

    @Override
    public void readCommandPayload(DataStoreContext<K, V> trans, byte[] data, int offset)
        throws DataStoreException {
        ReplicationFrame<K, V> frame = ReplicationFrame.toReplicationFrame(trans, data, offset);
        setReplicationFrame(frame);
    }

    @Override
    public void execute() {
//        System.out.println("ReplicationFramePayloadCommand["+getReplicationService().getMyName()+"] received: " + frame);
        List<Command<K, V>> commands = frame.getCommands();
        for (Command<K, V> cmd : commands) {
            getCommandManager().execute(cmd, false, frame.getSourceInstanceName());
        }
    }

    public String toString() {
        return "ReplicationFramePayloadCommand: contains "
                + frame.getCommands().size() + " commands";
    }
}