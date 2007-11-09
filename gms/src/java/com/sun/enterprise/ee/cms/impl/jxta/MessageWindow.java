/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
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

package com.sun.enterprise.ee.cms.impl.jxta;

import com.sun.enterprise.ee.cms.core.GMSConstants;
import com.sun.enterprise.ee.cms.core.GMSException;
import com.sun.enterprise.ee.cms.core.MessageSignal;
import com.sun.enterprise.ee.cms.impl.common.DSCMessage;
import com.sun.enterprise.ee.cms.impl.common.GMSContextFactory;
import com.sun.enterprise.ee.cms.impl.common.MessageSignalImpl;
import com.sun.enterprise.ee.cms.impl.common.Router;
import com.sun.enterprise.ee.cms.impl.common.ShutdownHelper;
import com.sun.enterprise.ee.cms.impl.common.SignalPacket;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import com.sun.enterprise.ee.cms.spi.GMSMessage;
import com.sun.enterprise.jxtamgmt.SystemAdvertisement;

import java.text.MessageFormat;
import java.util.concurrent.ArrayBlockingQueue;
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
                final MessagePacket packet = messageQueue.take();
                if (packet != null) {
                    logger.log(Level.FINER, "Processing received message .... "+ packet.getMessage());
                    newMessageReceived(packet);
                }
            } catch (InterruptedException e) {
                logger.log(Level.FINEST, e.getLocalizedMessage());
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
            logger.log(Level.FINER, "Adding Message: " + dMsg.getKey() + ":" + dMsg.getValue());
            dsc.addToLocalCache(dMsg.getKey(), dMsg.getValue());
        } else if (ops.equals(DSCMessage.OPERATION.REMOVE.toString())) {
            logger.log(Level.FINER, "Removing Values with Key: " + dMsg.getKey());
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
        if (gMsg.getComponentName().equals(GMSConstants.shutdownType.GROUP_SHUTDOWN.toString())) {
            final ShutdownHelper sh = GMSContextFactory.getGMSContext(gMsg.getGroupName()).getShutdownHelper();
            logger.log(Level.INFO, "member.groupshutdown", new Object[]{sender});
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
        logger.log(Level.FINER, MessageFormat.format("Sender:{0}, Receiver :{1}, TargetComponent :{2}, Message :{3}",
                sender, localId, message.getComponentName(), new String(message.getMessage())));
    }
}
