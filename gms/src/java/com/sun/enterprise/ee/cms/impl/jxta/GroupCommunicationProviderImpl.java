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

import com.sun.enterprise.ee.cms.core.GMSException;
import com.sun.enterprise.ee.cms.impl.common.GMSContextFactory;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import com.sun.enterprise.ee.cms.spi.GroupCommunicationProvider;
import com.sun.enterprise.ee.cms.spi.MemberStates;
import com.sun.enterprise.jxtamgmt.*;
import net.jxta.id.ID;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
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
    private String certPass;
    private final String groupName;
    private GMSContext ctx;
    private Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);

    public GroupCommunicationProviderImpl(final String groupName) {
        this.groupName = groupName;
        System.setProperty("JXTA_MGMT_LOGGER", logger.getName());
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
            logger.log(Level.FINER, "Received Cluster View Event...");
            logger.log(Level.FINER, clusterView.getView().toString());
            final EventPacket ePacket = new EventPacket(clusterViewEvent.getEvent(),
                    clusterViewEvent.getAdvertisement(),
                    clusterView);
            final ArrayBlockingQueue<EventPacket> viewQueue = getGMSContext().getViewQueue();
            try {
                viewQueue.put(ePacket);
            } catch (InterruptedException e) {
                //TODO: Examine all InterruptedException and thread.interrupt cases for better logging.
                logger.log(Level.WARNING, "InteruptedException occurred when " +
                        "putting EventPacket into viewQueue:"
                        + e.getLocalizedMessage());
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
        logger.log(Level.INFO, "Starting ClusterManager...");
        clusterManager.start();
    }

    public void announceClusterShutdown() {
        clusterManager.announceStop(true);
    }

    /**
     * Leaves the group as a result of a planned administrative action to
     * shutdown.
     */
    public void leave() {
        clusterManager.stop();
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
                            final boolean synchronous) throws GMSException {
        //TODO: support synchronous mode for now the boolean is ignored and message sent asynchronously
        try {
            if(targetMemberIdentityToken == null){
                clusterManager.send(null, message);//sends to whole group
            }
            else {
                final ID id = getAddress(targetMemberIdentityToken);
                logger.log(Level.FINER, "sending message to PeerID: " + id);
                clusterManager.send(id, message);
            }
        } catch (IOException e) {
            throw new GMSException(
                    new StringBuffer()
                            .append("IOException thrown when sending message")
                            .toString(), e);
        }
    }

    public void sendMessage(Serializable message) throws GMSException {
        sendMessage(null, message, false);
    }

    /**
     * Provides the address object corresponding to the underlying GCP's Address
     * object. In this case the jxta ID of the named peer is returned

     * @param memberIdentityToken ID of the named peer
     *
     * @return Object  - representing address of a given member.
     */
    ID getAddress(final String memberIdentityToken) {
        return clusterManager.getClusterViewManager().getID(memberIdentityToken);
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
        return MemberStates.valueOf(clusterManager.
                getPeerState(
                        getAddress(memberIdentityToken)).
                toUpperCase());
    }

    public String getGroupLeader() {
        return clusterManager.getClusterViewManager().getMaster().getName();
    }

    public void handleClusterMessage(final SystemAdvertisement adv,
                                     final Object message) {
        try {
            logger.log(Level.FINER, "Received AppMessage Notification, placing in message queue");
            getGMSContext().getMessageQueue().put(new MessagePacket(adv, message));
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted Exception occured while " +
                    "adding message to Shoal MessageQueue:" +
                    e.getLocalizedMessage());
        }
    }
}
