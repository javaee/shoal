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

import com.sun.enterprise.ee.cms.core.GMSException;
import com.sun.enterprise.ee.cms.core.MemberNotInViewException;
import com.sun.enterprise.ee.cms.impl.common.GMSContextFactory;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import com.sun.enterprise.ee.cms.spi.GMSMessage;
import com.sun.enterprise.ee.cms.spi.GroupCommunicationProvider;
import com.sun.enterprise.ee.cms.spi.MemberStates;
import com.sun.enterprise.jxtamgmt.*;
import net.jxta.id.ID;

import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements the GroupCommunicationProvider interface to plug in
 * JxtaClusterManagement layer as a Group Communication Provider for GMS.
 *
 * @author Shreedhar Ganapathy
 *         Date: Jun 26, 2006
 * @version $Revision$
 */
public class GroupCommunicationProviderImpl implements
        GroupCommunicationProvider,
        ClusterViewEventListener,
        ClusterMessageListener {
    private ClusterManager clusterManager;
    private final String groupName;
    private GMSContext ctx;
    private Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    private final ExecutorService msgSendPool;
    private  Map<ID, CallableMessageSend> instanceCache = new Hashtable<ID, CallableMessageSend>();

    public GroupCommunicationProviderImpl(final String groupName) {
        this.groupName = groupName;
        System.setProperty("JXTA_MGMT_LOGGER", logger.getName());
        msgSendPool = Executors.newCachedThreadPool();
    }

    private GMSContext getGMSContext() {
        if (ctx == null) {
            ctx = (GMSContext) GMSContextFactory.getGMSContext(groupName);
        }
        return ctx;
    }

    public void clusterViewEvent(final ClusterViewEvent clusterViewEvent,
                                 final ClusterView clusterView) {
        if (!getGMSContext().isShuttingDown()) {
            logger.log(Level.FINER, "Received Cluster View Event..." + clusterViewEvent.getEvent().toString() +
                   " from " + clusterViewEvent.getAdvertisement().getName());
            logger.log(Level.FINER, clusterView.getView().toString());
            final EventPacket ePacket = new EventPacket(clusterViewEvent.getEvent(),
                    clusterViewEvent.getAdvertisement(),
                    clusterView);
            final ArrayBlockingQueue<EventPacket> viewQueue = getGMSContext().getViewQueue();
            try {
                viewQueue.put(ePacket);
                logger.log(Level.FINER, "Adding " + clusterViewEvent.getEvent() + " to viewQueue");
            } catch (InterruptedException e) {
                //TODO: Examine all InterruptedException and thread.interrupt cases for better logging.
                logger.log(Level.WARNING, "interruptedexception.occurred",
                        new Object[] {e.getLocalizedMessage()});
            }
        }
    }

    /**
     * Initializes the Group Communication Service Provider with the requisite
     * values of group identity, member(self) identity, and a Map containing
     * recognized and valid configuration properties that can be set/overriden
     * by the employing application. The valid property keys must be specified
     * in a datastructure that is available to the implementation and to GMS.
     *
     * @param memberName member name
     * @param groupName group name
     * @param identityMap valid configuration properties
     * @param configProperties configuration properties
     */
    public void initializeGroupCommunicationProvider(final String memberName,
                                                     final String groupName,
                                                     final Map<String, String> identityMap,
                                                     final Map configProperties) {
        final List<ClusterViewEventListener> cvListeners =
                new ArrayList<ClusterViewEventListener>();
        cvListeners.add(this);
        final List<ClusterMessageListener> cmListeners =
                new ArrayList<ClusterMessageListener>();
        cmListeners.add(this);
        clusterManager = new ClusterManager(groupName,
                memberName,
                identityMap,
                configProperties,
                cvListeners,//View Listener
                cmListeners);//MessageListener
    }

    /**
     * Joins the group using semantics specified by the underlying GCP system
     */
    public void join() {
        logger.log(Level.FINE, "starting.cluster");
        clusterManager.start();
    }

    public void announceClusterShutdown(final GMSMessage gmsMessage) {
        try {
            clusterManager.send(null, gmsMessage);                  
        } catch (IOException e) {
            logger.log(Level.WARNING, "ioexception.occurred.cluster.shutdown", new Object[] {e});
        } catch (MemberNotInViewException e) {
            //ignore since this is a broadcast
        }
    }

    /**
     * Leaves the group as a result of a planned administrative action to
     * shutdown.
     */
    public void leave(final boolean isClusterShutdown) {
        clusterManager.stop(isClusterShutdown);
    }

    /**
     * Sends a message using the underlying group communication
     * providers'(GCP's) APIs. Requires the users' message to be wrapped into a
     * GMSMessage object.
     *
     * @param targetMemberIdentityToken The member token string that identifies
     *                      the target member to which this message is addressed.
     *                      The implementation is expected to provide a mapping
     *                      the member token to the GCP's addressing semantics.
     *                      If null, the entire group would receive this message.
     * @param message       a Serializable object that wraps the user specified
     *                      message in order to allow remote GMS instances to
     *                      unpack this message appropriately.
     * @param synchronous   setting true here will call the underlying GCP's api
     *                      that corresponds to a synchronous message, if
     *                      available.
     * @throws com.sun.enterprise.ee.cms.core.GMSException
     */
    public void sendMessage(final String targetMemberIdentityToken,
                            final Serializable message,
                            final boolean synchronous) throws GMSException, MemberNotInViewException {
        try {
            if (targetMemberIdentityToken == null) {
                if (synchronous) {
                    /*
                    Use point-to-point communication with all instances instead of the group-wide (udp) based message.
                    Since we don't have reliable multicast yet, this approach will ensure reliability.
                    Ideally, when a message is sent to the group via point-to-point,
                    the message to each member should be on a separate thread to get concurrency.
                     */
                    List<SystemAdvertisement> currentMemberAdvs = clusterManager.getClusterViewManager().
                                                    getLocalView().getView();

                    for (SystemAdvertisement currentMemberAdv : currentMemberAdvs) {
                        final ID id = currentMemberAdv.getID();

                        //TODO : make this multi-threaded via Callable
                        /* final CallableMessageSend task = getInstanceOfCallableMessageSend(id);
                        logger.fine("Message is = " + message.toString());
                        task.setMessage(message);
                        msgSendPool.submit(task);
                        */
                        logger.log(Level.FINE, "sending message to member: " + currentMemberAdv.getName());
                        try {
                            clusterManager.send(id, message);
                        } catch (MemberNotInViewException e) {
                            logger.warning("MemberNotInViewException : " + e.toString());
                        }
                    }
                } else {
                    clusterManager.send(null, message);//sends to whole group
                }
            } else {
                final ID id = clusterManager.getID(targetMemberIdentityToken);
                if (clusterManager.getClusterViewManager().containsKey(id)) {
                    logger.log(Level.FINE, "sending message to PeerID: " + id);
                    clusterManager.send(id, message);                    
                } else {
                    logger.log(Level.FINE, "message not sent to  " + targetMemberIdentityToken +
                            " since it is not in the View");
                    throw new MemberNotInViewException("Member " + targetMemberIdentityToken +
                            " is not in the View anymore. Hence not performing sendMessage operation");
                }
            }
        } catch (IOException e) {
            if (logger.isLoggable(Level.FINE)) {
                e.printStackTrace();
            }
            throw new GMSException(e);
        }
    }

    public void sendMessage(Serializable message) throws GMSException, MemberNotInViewException {
        sendMessage(null, message, false);
    }

    /**
     * Returns the address representing the peer identified by this process. The
     * address object is of the type corresponding to the underlying GCP. In
     * this case, the jxta ID of this peer is returned.
     *
     * @return Object - representing this peer's address.
     */
    public Object getLocalAddress() {
        return clusterManager.getSystemAdvertisement().getID();
    }

    /**
     * returns a list of members that are currently alive in the group
     *
     * @return list of current live members
     */
    public List<String> getMembers() {//TODO: BUG. This will result in viewID increment.
        return clusterManager.
                getClusterViewManager().
                getLocalView().
                getPeerNamesInView();
    }

    public boolean isGroupLeader() {
        return clusterManager.isMaster();
    }

    public MemberStates getMemberState(final String memberIdentityToken) {
        //TODO/FIXME lowercase is always recommended over uppercase for non localized strings
        return MemberStates.valueOf(clusterManager.getNodeState(clusterManager.getID(memberIdentityToken)).toUpperCase());
    }

    public String getGroupLeader() {
        return clusterManager.getClusterViewManager().getMaster().getName();
    }

    public void handleClusterMessage(final SystemAdvertisement adv,
                                     final Object message) {
        try {
            //logger.log(Level.FINE, "Received AppMessage Notification, placing in message queue = " + new String(((GMSMessage)message).getMessage()));
            getGMSContext().getMessageQueue().put(new MessagePacket(adv, message));
        } catch (InterruptedException e) {
            logger.log(Level.WARNING,
                    MessageFormat.format("Interrupted Exception occured while adding message to Shoal MessageQueue :{0}",
                            e.getLocalizedMessage()));
        }
    }
    
    public void assumeGroupLeadership() {
        clusterManager.takeOverMasterRole();
    }

    public void setGroupStoppingState() {
        clusterManager.setClusterStopping();  
    }

    public void reportJoinedAndReadyState() {
        clusterManager.reportJoinedAndReadyState();
    }

    private CallableMessageSend getInstanceOfCallableMessageSend(ID id) {
        if (instanceCache.get(id) == null) {
            CallableMessageSend c = new CallableMessageSend(id);
            instanceCache.put(id, c);
            return c;
        } else {
            return instanceCache.get(id);
        }
    }

    /**
     * implements Callable.
     * Used for handing off the job of calling sendMessage() method to a ThreadPool.
     * REVISIT
     */
    private class CallableMessageSend implements Callable<Object> {
        private ID member;
        private Serializable msg;

           private CallableMessageSend(final ID member) {
            this.member = member;
        }

         public void setMessage(Serializable msg) {
             this.msg = null;
             this.msg = msg;
         }

        public Object call() throws Exception {
            clusterManager.send(member, msg);
            return null;
        }
    }

}

