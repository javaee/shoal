package org.shoal.ha.cache.impl.util;

import com.sun.enterprise.ee.cms.core.*;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public abstract class MessageReceiver
    implements CallBack {

    private final static Logger logger = Logger.getLogger("ReplicationLogger");

    private String storeName;

    protected MessageReceiver(String storeName) {
        this.storeName = storeName;
    }

    @Override
    public void processNotification(Signal signal) {
        Object message = null;
        try {
            MessageSignal messageSignal = null;
            signal.acquire();
            logger.log(Level.INFO, "Source Member: " + signal.getMemberToken() + " group : " + signal.getGroupName());
            if (signal instanceof MessageSignal) {
                messageSignal = (MessageSignal) signal;
                message = ((MessageSignal) signal).getMessage();
//                logger.log(Level.INFO, "\t\t***  Message received: "
//                        + ((MessageSignal) signal).getTargetComponent() + "; "
//                        + ((MessageSignal) signal).getMemberToken());

                if (messageSignal != null) {
                    handleMessage(messageSignal.getMemberToken(), messageSignal.getTargetComponent(),
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

    protected abstract void handleMessage(String senderName, String messageToken, byte[] data);
}
