/*
* The contents of this file are subject to the terms
* of the Common Development and Distribution License
* (the License).  You may not use this file except in
* compliance with the License.
*
* You can obtain a copy of the license at
* https://shoal.dev.java.net/public/CDDLv1.0.html
*
* See the License for the specific language governing
* permissions and limitations under the License.
*
* When distributing Covered Code, include this CDDL
* Header Notice in each file and include the License file
* at
* If applicable, add the following below the CDDL Header,
* with the fields enclosed by brackets [] replaced by
* you own identifying information:
* "Portions Copyrighted [year] [name of copyright owner]"
*
* Copyright 2006 Sun Microsystems, Inc. All rights reserved.
*/
package com.sun.enterprise.ee.cms.impl.jxta;

import com.sun.enterprise.ee.cms.core.GMSConstants;
import com.sun.enterprise.ee.cms.core.GMSException;
import com.sun.enterprise.ee.cms.core.MessageSignal;
import com.sun.enterprise.ee.cms.impl.common.*;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import com.sun.enterprise.ee.cms.spi.GMSMessage;
import com.sun.enterprise.jxtamgmt.SystemAdvertisement;

import java.text.MessageFormat;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles messages from the message queue and dispatches them to the
 * interested parties. Also specially handles messages sent for
 * DistributedStateCacheImpl (the default implementation) for synchronization
 * actions.
 *
 * @author Shreedhar Ganapathy
 *         Date: Jul 11, 2006
 * @version $Revision$
 */
public class MessageWindow implements Runnable {
    private Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    private GMSContext ctx;
    private static final int MESSAGE_WAIT_TIMEOUT = 2000;
    private ArrayBlockingQueue<MessagePacket> messageQueue;
    private final String groupName;

    public MessageWindow(final String groupName, final ArrayBlockingQueue<MessagePacket> messageQueue) {
        this.groupName = groupName;
        this.messageQueue = messageQueue;
    }

    private GMSContext getGMSContext() {
        if (ctx == null) {
            ctx = (GMSContext) GMSContextFactory.getGMSContext(groupName);
        }
        return ctx;
    }

    public void run() {
        while (!getGMSContext().isShuttingDown()) {
            try {
                final MessagePacket packet = messageQueue.poll(MESSAGE_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
                if (packet != null) {
                    logger.log(Level.FINER,"Processing received message .... ");
                    newMessageReceived(packet);
                }
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, e.getLocalizedMessage());
            }
        }
    }

    private void newMessageReceived(final MessagePacket packet) {
        final Object message = packet.getMessage();
        final SystemAdvertisement adv = packet.getAdvertisement();
        final String sender = adv.getName();

        if (message instanceof GMSMessage) {
            handleGMSMessage((GMSMessage) message, sender);
        } else if (message instanceof DSCMessage) {
            handleDSCMessage((DSCMessage) message, sender);
        }
    }

    private void handleDSCMessage(final DSCMessage dMsg, final String token) {

        final String ops = dMsg.getOperation();
        logger.log(Level.FINER, MessageFormat.format("DSCMessageReceived from :{0}, Operation :{1}", token, ops));
        final DistributedStateCacheImpl dsc =
                (DistributedStateCacheImpl) getGMSContext().getDistributedStateCache();
        if (ops.equals(DSCMessage.OPERATION.ADD.toString())) {
            logger.log(Level.FINER, "Adding Message: " + dMsg.getKey()+ ":" + dMsg.getValue());
            dsc.addToLocalCache(dMsg.getKey(), dMsg.getValue());
        } else if (ops.equals(DSCMessage.OPERATION.REMOVE.toString())) {
            logger.log(Level.FINER, "Removing Values with Key: " + dMsg.getKey());
            dsc.removeFromLocalCache(dMsg.getKey());
        } else if (ops.equals(DSCMessage.OPERATION.ADDALLLOCAL.toString())) {
            if (dMsg.isCoordinator()) {
                try {
                    logger.log(Level.FINER,"Syncing local cache with group ...");
                    dsc.addAllToRemoteCache();
                    logger.log(Level.FINER, "done with local to group sync...");
                } catch (GMSException e) {
                    logger.log(Level.WARNING, e.getLocalizedMessage());
                }
                logger.log(Level.FINER,"adding group cache state to local cache..");
                dsc.addAllToLocalCache(dMsg.getCache());
            }
        } else if (ops.equals(DSCMessage.OPERATION.ADDALLREMOTE.toString())) {
            dsc.addAllToLocalCache(dMsg.getCache());
        }//TODO: determine if the following is needed.
        /*else if( ops.equals( DSCMessage.OPERATION.REMOVEALL.toString()) ) {
            dsc.removeAllFromCache( dMsg. );
        }*/
    }

    private void handleGMSMessage(final GMSMessage gMsg, final String sender) {
        if(gMsg.getComponentName().equals(GMSConstants.shutdownType.GROUP_SHUTDOWN.toString())){
            final ShutdownHelper sh = GMSContextFactory.getGMSContext(gMsg.getGroupName()).getShutdownHelper();
            sh.addToGroupShutdownList(gMsg.getGroupName());
        }
        else {
            if (getRouter().isMessageAFRegistered()) {
                writeLog(sender, gMsg);
                final MessageSignal ms = new MessageSignalImpl(
                        gMsg.getMessage(), gMsg.getComponentName(), sender,
                        gMsg.getGroupName(), gMsg.getStartTime());
                final SignalPacket signalPacket = new SignalPacket(ms);
                getRouter().queueSignal(signalPacket);
            }
        }
    }

    private Router getRouter() {
        return getGMSContext().getRouter();
    }

    private void writeLog(final String sender, final com.sun.enterprise.ee.cms.spi.GMSMessage message) {
        final String localId = getGMSContext().getServerIdentityToken();
        logger.log(Level.FINER, MessageFormat.format("Sender:{0}, Receiver :{1}, TargetComponent :{2}, Message :{3}",
                sender, localId, message.getComponentName(), new String(message.getMessage())));
    }
}
