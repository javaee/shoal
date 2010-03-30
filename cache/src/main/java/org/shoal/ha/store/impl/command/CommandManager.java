package org.shoal.ha.store.impl.command;

import com.sun.enterprise.ee.cms.core.*;
import org.shoal.ha.group.GroupMessageReceiver;
import org.shoal.ha.store.api.DataStoreContext;
import org.shoal.ha.store.impl.interceptor.ExecutionInterceptor;
import org.shoal.ha.store.impl.util.ResponseMediator;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Mahesh Kannan
 */
public class CommandManager<K, V>
        implements GroupMessageReceiver, CallBack {

    private final static Logger logger = Logger.getLogger("ReplicationLogger");

    private String myName;

    private String groupName;

    private DataStoreContext<K, V> dsc;

    private Command<K, V>[] commands = (Command<K, V>[]) Array.newInstance(Command.class, 256);

    private volatile ExecutionInterceptor<K, V> head;

    private volatile ExecutionInterceptor<K, V> tail;

    public CommandManager(DataStoreContext<K, V> dsc) {
        this.dsc = dsc;
        this.myName = dsc.getInstanceName();
        this.groupName = dsc.getGroupName();
    }

    public void registerCommand(Command<K, V> command) {
        commands[command.getOpcode()] = command;
        command.initialize(dsc);
    }

    public void unregisterCommand(byte opcode) {
        commands[opcode] = null;
    }

    public synchronized void registerExecutionInterceptor(ExecutionInterceptor<K, V> interceptor) {
        interceptor.initialize(dsc);
        if (head == null) {
            head = interceptor;
        } else {
            tail.setNext(interceptor);
        }
        interceptor.setPrev(tail);
        interceptor.setNext(null);
        tail = interceptor;
    }

    public Command getCommand(byte opcode) {
        return commands[opcode];
    }

    //Initiated to transmit

    public void execute(Command<K, V> cmd) {
        execute(cmd, true, myName);
    }

    //Initiated to transmit

    public void execute(Command<K, V> cmd, boolean forward, String initiator) {
        cmd.initialize(dsc);
        if (head != null) {
            if (forward) {
                head.onTransmit(cmd);
            } else {
                tail.onReceive(cmd);
            }
        }
    }

    public Command<K, V> createNewInstance(byte opcode, byte[] data, int offset)
            throws IOException {
        Command<K, V> cmd2 = commands[opcode];
        Command<K, V> cmd = null;
        if (cmd2 != null) {
            cmd = cmd2.createNewInstance();
            cmd.initialize(dsc);
            cmd.readCommandState(data, offset);
        }

        return cmd;
    }

    public void transmit(Command<K, V> cmd) {

    }

    public ResponseMediator getResponseMediator() {
        return dsc.getResponseMediator();
    }

    public void handleMessage(String sourceMemberName, String token, byte[] frameData) {

        byte opCode = frameData[0];
        Command<K, V> cmd2 = commands[opCode];
        if (cmd2 != null) {
            Command<K, V> cmd = cmd2.createNewInstance();
            System.out.println("RECEIVED MEEESSAGE FOR: " + cmd.getClass().getName());
            cmd.initialize(dsc);
            try {
                cmd.readCommandState(frameData, 0);
                execute(cmd, false, sourceMemberName);
            } catch (IOException dse) {
                //TODO
            }
        }
    }

    @Override
    public void processNotification(Signal signal) {
        Object message = null;
        try {
            MessageSignal messageSignal = null;
            signal.acquire();
            //logger.log(Level.INFO, "Source Member: " + signal.getMemberToken() + " group : " + signal.getGroupName());
            if (signal instanceof MessageSignal) {
                messageSignal = (MessageSignal) signal;
                message = ((MessageSignal) signal).getMessage();
//                logger.log(Level.INFO, "\t\t***  Message received: "
//                        + ((MessageSignal) signal).getTargetComponent() + "; "
//                        + ((MessageSignal) signal).getMemberToken());

                if (messageSignal != null) {
                    this.handleMessage(messageSignal.getMemberToken(), messageSignal.getTargetComponent(),
                            (byte[]) message);
                }
            }
            signal.release();


        } catch (SignalAcquireException e) {
            logger.log(Level.WARNING, "Exception occured while acquiring signal" + e);
        } catch (SignalReleaseException e) {
            logger.log(Level.WARNING, "Exception occured while releasing signal" + e);
        }
    }
}