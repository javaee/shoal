/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.enterprise.ee.cms.impl.base;

import com.sun.enterprise.ee.cms.core.GMSConstants;
import com.sun.enterprise.ee.cms.core.GMSException;
import com.sun.enterprise.ee.cms.core.MessageSignal;
import com.sun.enterprise.ee.cms.impl.common.*;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import com.sun.enterprise.ee.cms.spi.GMSMessage;

import java.text.MessageFormat;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Iterator;

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
    static private Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    private final Logger monitorLogger = GMSLogDomain.getMonitorLogger();
    private GMSContext ctx;
    private ArrayBlockingQueue<MessagePacket> messageQueue;
    private AtomicInteger messageQueueHighWaterMark = new AtomicInteger(0);
    private final String groupName;
    private final ExecutorService dscExecutor;

    public MessageWindow(final String groupName, final ArrayBlockingQueue<MessagePacket> messageQueue) {
        this.groupName = groupName;
        this.messageQueue = messageQueue;
        GMSThreadFactory gtf = new GMSThreadFactory("GMS-DistributedStateCache-Group-" + groupName + "-thread");
        this.dscExecutor = Executors.newSingleThreadExecutor(gtf);
    }

   void stop() {
        dscExecutor.shutdown();
   }

    private GMSContext getGMSContext() {
        if (ctx == null) {
            ctx = (GMSContext) GMSContextFactory.getGMSContext(groupName);
        }
        return ctx;
    }

    private void recordMessageQueueHighWaterMark() {
        if (monitorLogger.isLoggable(Level.FINE)) {
            int currentQueueSize = messageQueue.size();
            int localHighWater = messageQueueHighWaterMark.get();
            if (currentQueueSize > localHighWater) {
                messageQueueHighWaterMark.compareAndSet(localHighWater, currentQueueSize);
            }
        }
    }

    public void run() {
        while (!getGMSContext().isShuttingDown()) {
            try {
                recordMessageQueueHighWaterMark();
                final MessagePacket packet = messageQueue.take();
                if (packet != null) {
                    if (logger.isLoggable(Level.FINER)){
                        logger.log(Level.FINER, "Processing received message .... "+ packet.getMessage());
                    }
                    newMessageReceived(packet);
                }
            } catch (InterruptedException e) {
                logger.log(Level.FINE, e.getLocalizedMessage());
            } catch (Throwable t) {
                logger.log(Level.WARNING, "msg.wdw.exception.processing.msg", t);
            }
        }
        if (monitorLogger.isLoggable(Level.FINE)) {
            int msgQueueCapacity =  (messageQueue == null ? 0 : messageQueue.remainingCapacity());
            monitorLogger.log(Level.FINE, "message queue high water mark:" + messageQueueHighWaterMark.get() +
                                           " msg queue remaining capacity:" + msgQueueCapacity);
        }
        if (messageQueue != null && messageQueue.size() > 0) {
            int messageQueueSize = messageQueue.size();
            logger.log(Level.WARNING, "msg.wdw.thread.shutdown", new Object[]{groupName, messageQueueSize});
            if (messageQueueSize > 0 && logger.isLoggable(Level.FINER)) {
                Iterator<MessagePacket> mqIter = messageQueue.iterator();
                if (logger.isLoggable(Level.FINER)){
                    logger.finer("Dumping received but unprocessed messages for group: " + groupName);
                }
                while (mqIter.hasNext()) {
                    MessagePacket mp = mqIter.next();
                    Object message = mp.getMessage();
                    String sender = mp.getAdvertisement().getName();
                    if (message instanceof GMSMessage) {
                        writeLog(sender, (GMSMessage)mp.getMessage());
                    } else if (message instanceof DSCMessage && logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, MessageFormat.format("Unprocessed DSCMessageReceived from :{0}, Operation :{1}", sender, ((DSCMessage)message).getOperation()));
                    }
                }
            }

        } else {
            logger.log(Level.INFO, "msg.wdw.thread.terminated", new Object[]{groupName});
        }
    }

    private void newMessageReceived(final MessagePacket packet) {
        final Object message = packet.getMessage();
        final SystemAdvertisement adv = packet.getAdvertisement();
        final String sender = adv.getName();

        if (message instanceof GMSMessage) {
            handleGMSMessage((GMSMessage) message, sender);
        } else if (message instanceof DSCMessage) {
            try {
                dscExecutor.submit(new ProcessDSCMessageTask(this, (DSCMessage)message, sender));
            } catch (RejectedExecutionException ree) {
                logger.log(Level.WARNING, "failed to schedule processDSCMessageTask for mesasge " + message);

            }
        }
    }

    private void handleDSCMessage(final DSCMessage dMsg, final String token) {
        if (ctx.isWatchdog()) {
            // Distributed State Cache is disabled for WATCHDOG member.
            return;
        }

        final String ops = dMsg.getOperation();
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, MessageFormat.format("DSCMessageReceived from :{0}, Operation :{1}", token, ops));
        }
        final DistributedStateCacheImpl dsc =
                (DistributedStateCacheImpl) getGMSContext().getDistributedStateCache();
        if (ops.equals(DSCMessage.OPERATION.ADD.toString())) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "Adding Message: " + dMsg.getKey() + ":" + dMsg.getValue());
            }
            dsc.addToLocalCache(dMsg.getKey(), dMsg.getValue());
        } else if (ops.equals(DSCMessage.OPERATION.REMOVE.toString())) {
            if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "Removing Values with Key: " + dMsg.getKey());
            }
            dsc.removeFromLocalCache(dMsg.getKey());
        } else if (ops.equals(DSCMessage.OPERATION.ADDALLLOCAL.toString())) {
            if (dMsg.isCoordinator()) {
                try {
                    logger.log(Level.FINER, "Syncing local cache with group ...");
                    dsc.addAllToRemoteCache();
                    logger.log(Level.FINER, "done with local to group sync...");
                } catch (GMSException e) {
                    logger.log(Level.WARNING, e.getLocalizedMessage());
                }
                logger.log(Level.FINER, "adding group cache state to local cache..");
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
        if (gMsg.getComponentName() != null &&
                gMsg.getComponentName().equals(GMSConstants.shutdownType.GROUP_SHUTDOWN.toString())) {
            final ShutdownHelper sh = GMSContextFactory.getGMSContext(gMsg.getGroupName()).getShutdownHelper();
            logger.log(Level.INFO, "member.groupshutdown", new Object[]{sender, groupName});
            sh.addToGroupShutdownList(gMsg.getGroupName());
            logger.log(Level.FINE, "setting clusterStopping variable to true");
            GMSContextFactory.getGMSContext(gMsg.getGroupName()).getGroupCommunicationProvider().setGroupStoppingState();
        } else {
            if (getRouter().isMessageAFRegistered()) {
                writeLog(sender, gMsg);
                final MessageSignal ms = new MessageSignalImpl(gMsg.getMessage(), gMsg.getComponentName(), sender,
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
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, MessageFormat.format("Sender:{0}, Receiver :{1}, TargetComponent :{2}, Message :{3}",
                    sender, localId, message.getComponentName(), new String(message.getMessage())));
        }
    }

    private static class ProcessDSCMessageTask implements Runnable {
        final private MessageWindow mw;
        final private DSCMessage dMsg;
        final private String fromMember;

        public ProcessDSCMessageTask(MessageWindow mw, final DSCMessage dMsg, final String token  ) {
            this.mw = mw;
            this.dMsg = dMsg;
            this.fromMember = token;
        }

        public void run() {
            try {
                mw.handleDSCMessage(dMsg, fromMember);
            } catch (Throwable t) {
                mw.logger.log(Level.SEVERE, "failed to handleDSCMessage", t);
            }
        }
    }
}
