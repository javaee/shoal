package org.shoal.ha.store.impl.command;

import org.shoal.ha.store.api.DataStoreContext;
import org.shoal.ha.store.impl.command.Command;
import org.shoal.ha.store.impl.command.ReplicationFramePayloadCommand;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mahesh Kannan
 */
public class ReplicationCommandTransmitter<K, V>
        implements Runnable {

    private AtomicInteger outgoingSeqno = new AtomicInteger();

    private AtomicInteger acknowledgedSeqno = new AtomicInteger();

    private DataStoreContext<K, V> dsc;

    private String targetName;

    private Thread thread;

    private ConcurrentLinkedQueue<Command<K, V>> list
            = new ConcurrentLinkedQueue<Command<K, V>>();

    public void initialize(String targetName, DataStoreContext<K, V> rsInfo) {
        this.targetName = targetName;
        this.dsc = rsInfo;

        thread = new Thread(this, "ReplicationCommandTransmitter[" + targetName + "]");
        thread.setDaemon(true);
        thread.start();
    }

    public void send(String targetName, Command<K, V> cmd) {
        list.add(cmd);
        System.out.println("ReplicationCommandTransmitter[" + targetName + "] just "
        + " accumulated: " + cmd.getOpcode());
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(15);
                if (list.peek() != null) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ReplicationFrame<K, V> frame = new ReplicationFrame<K, V>((byte)255,
                            dsc.getServiceName(), dsc.getInstanceName());

                    Command<K, V> cmd = list.poll();
                    while (cmd != null) {
                        frame.addCommand(cmd);
                        cmd = list.poll();

                        if (frame.getCommands().size() >= 20) {
                            transmitFramePayload(frame);
                            frame = new ReplicationFrame<K, V>((byte)255,
                            dsc.getServiceName(), dsc.getInstanceName());
                        }
                    }

                    if (frame.getCommands().size() > 0) {
                        transmitFramePayload(frame);
                    }


                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void transmitFramePayload(ReplicationFrame<K, V> frame) {
        frame.setSeqNo(outgoingSeqno.incrementAndGet());
        frame.setMinOutstandingPacketNumber(acknowledgedSeqno.get());
        frame.setTargetInstanceName(targetName);
        ReplicationFramePayloadCommand<K, V> cmd = new ReplicationFramePayloadCommand<K, V>();
        cmd.setReplicationFrame(frame);

        dsc.getCommandManager().execute(cmd);


    }

}
