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
package com.sun.enterprise.mgmt;

import static com.sun.enterprise.mgmt.ClusterViewEvents.ADD_EVENT;
import com.sun.enterprise.ee.cms.core.GMSConstants;
import com.sun.enterprise.ee.cms.core.GMSMember;
import com.sun.enterprise.ee.cms.impl.base.CustomTagNames;
import com.sun.enterprise.ee.cms.impl.base.PeerID;
import com.sun.enterprise.ee.cms.impl.base.SystemAdvertisement;
import com.sun.enterprise.ee.cms.impl.base.Utility;
import com.sun.enterprise.ee.cms.impl.common.GMSContext;
import com.sun.enterprise.ee.cms.impl.common.GMSContextFactory;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import com.sun.enterprise.mgmt.transport.Message;
import com.sun.enterprise.mgmt.transport.MessageEvent;
import com.sun.enterprise.mgmt.transport.MessageImpl;
import com.sun.enterprise.mgmt.transport.MessageIOException;
import com.sun.enterprise.mgmt.transport.MessageListener;

import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Master Node defines a protocol by which a set of nodes connected to a JXTA infrastructure group
 * may dynamically organize into a group with a determinically self elected master. The protocol is
 * composed of a JXTA message containing a "NAD", and "ROUTE" and one of the following :
 * <p/>
 * -"MQ", is used to query for a master node
 * <p/>
 * -"NR", is used to respond to a "MQ", which also contains the following
 * <p/>
 * -"AMV", is the MasterView of the cluster
 * <p/>
 * -"GS", true if MasterNode is aware that all members in group are starting as part of a GroupStarting, sent as part
 * of MasterNodeResponse NR
 * <p/>
 * -"GSC", sent when GroupStarting phase has completed.  Subsequent JOIN & JoinedAndReady are considered INSTANCE_STARTUP.
 * <p/>
 * -"CCNTL", is used to indicate collision between two nodes
 * <p/>
 * -"NADV", contains a node's <code>SystemAdvertisement</code>
 * <p/>
 * -"ROUTE", contains a node's list of current physical addresses, which is used to issue ioctl to the JXTA
 * endpoint to update any existing routes to the nodes.  (useful when a node changes physical addresses.
 *
 * <p/>
 * MasterNode will attempt to discover a master node with the specified timeout (timeout * number of iterations)
 * after which, it determine whether to become a master, if it happens to be first node in the ordered list of discovered nodes.
 * Note: due to startup time, a master node may not always be the first node node.  However if the master node fails,
 * the first node is expected to assert it's designation as the master, otherwise, all nodes will repeat the master node discovery
 * process.
 */
class MasterNode implements MessageListener, Runnable {
    private static final Logger LOG = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    private static final Logger masterLogger = Logger.getLogger("ShoalLogger.MasterNode");
    private final ClusterManager manager;

    private boolean masterAssigned = false;
    private volatile boolean discoveryInProgress = true;
    private PeerID localNodeID;
    private final SystemAdvertisement sysAdv;
    private volatile boolean started = false;
    private volatile boolean stop = false;
    private Thread thread = null;
    private ClusterViewManager clusterViewManager;
    private ClusterView discoveryView;
    private final AtomicLong masterViewID = new AtomicLong();
    //Collision control
    final Object MASTERLOCK = new Object();
    private static final String CCNTL = "CCNTL";
    private static final String MASTERNODE = "MN";
    private static final String MASTERQUERY = "MQ";
    private static final String NODEQUERY = "NQ";
    private static final String MASTERNODERESPONSE = "MR";
    private static final String NODERESPONSE = "NR";
    private static final String NAMESPACE = "MASTER";
    private static final String NODEADV = "NAD";
    private static final String AMASTERVIEW = "AMV";
    private static final String MASTERVIEWSEQ = "SEQ";
    private static final String GROUPSTARTING = "GS";
    private static final String GROUPSTARTUPCOMPLETE = "GSC";

    private int interval = 6;
    // Default master node discovery timeout
    private long timeout = 10 * 1000L;
    private static final String VIEW_CHANGE_EVENT = "VCE";

    private boolean groupStarting = false;
    private List<String> groupStartingMembers = null;
    private final Timer timer;
    private DelayedSetGroupStartingCompleteTask groupStartingTask = null;
    static final private long MAX_GROUPSTARTING_TIME = 240000;  // 4 minute limit for group starting duration.
    static final private long GROUPSTARTING_COMPLETE_DELAY = 30000;   // delay before completing group startup.  Allow late arriving JoinedAndReady notifications
                                                                     // to be group starting.
    private boolean clusterStopping = false;
    final Object discoveryLock = new Object();
    private GMSContext ctx = null;
    final private SortedSet<MasterNodeMessageEvent> outstandingMasterNodeMessages;
    private Thread processOutstandingMessagesThread = null;

    /**
     * Constructor for the MasterNode object
     *
     * @param timeout  - waiting intreval to receive a response to a master
     *                 node discovery
     * @param interval - number of iterations to perform master node discovery
     * @param manager  the cluster manager
     */
    MasterNode(final ClusterManager manager,
               final long timeout,
               final int interval) {
        localNodeID = manager.getPeerID();
        if (timeout > 0) {
            this.timeout = timeout;
        }
        this.interval = interval;
        this.manager = manager;
        sysAdv = manager.getSystemAdvertisement();
        discoveryView = new ClusterView(sysAdv);
        timer = new Timer(true);
        outstandingMasterNodeMessages = new TreeSet<MasterNodeMessageEvent>();
    }

    /**
     * returns the cumulative MasterNode timeout
     *
     * @return timeout
     */
    long getTimeout() {
        return timeout * interval;
    }

    /**
     * Sets the Master Node peer ID, also checks for collisions at which event
     * A Conflict Message is sent to the conflicting node, and master designation
     * is reiterated over the wire after a short timeout
     *
     * @param systemAdv the system advertisement
     * @return true if no collisions detected, false otherwise
     */
    boolean checkMaster(final SystemAdvertisement systemAdv) {
        if (masterAssigned && isMaster()) {
              LOG.log(Level.FINER,"checkMaster : clusterStopping() = " + clusterStopping);
            if (clusterStopping) {
                //accept the DAS as the new Master
                LOG.log(Level.FINER, "Resigning Master Node role in anticipation of a master node announcement");
                LOG.log(Level.FINER, "Accepting DAS as new master in the event of cluster stopping...");
                clusterViewManager.setMaster(systemAdv, false);
                masterAssigned = true;
                return false;
            }
            LOG.log(Level.FINER,
                    "Master node role collision with " + systemAdv.getName() +
                            " .... attempting to resolve");
            send(systemAdv.getID(), systemAdv.getName(),
                    createMasterCollisionMessage());

            //TODO add code to ensure whether this node should remain as master or resign (basically noop)
            if (manager.getPeerID().compareTo(systemAdv.getID()) >= 0) {
                LOG.log(Level.FINER, "Affirming Master Node role");
            } else {
                LOG.log(Level.FINER, "Resigning Master Node role in anticipation of a master node announcement");
                clusterViewManager.setMaster(systemAdv, false);
            }

            return false;
        } else {
            clusterViewManager.setMaster(systemAdv, true);
            masterAssigned = true;
            synchronized (MASTERLOCK) {
                MASTERLOCK.notifyAll();
            }
            LOG.log(Level.FINE, "Discovered a Master node :" + systemAdv.getName());
        }
        return true;
    }

    /**
     * Creates a Master Collision Message. A collision message is used
     * to indicate the conflict. Nodes receiving this message then required to
     * assess the candidate master node based on their knowledge of the network
     * should await for an assertion of the master node candidate
     *
     * @return Master Collision Message
     */
    private Message createMasterCollisionMessage() {
        final Message msg = createSelfNodeAdvertisement();
        msg.addMessageElement(CCNTL, localNodeID);
        LOG.log(Level.FINER, "Created a Master Collision Message");
        return msg;
    }

    private Message createSelfNodeAdvertisement() {
        Message msg = new MessageImpl(Message.TYPE_MASTER_NODE_MESSAGE);
        msg.addMessageElement(NODEADV, sysAdv);
        return msg;
    }

    private void sendSelfNodeAdvertisement(final PeerID id, final String name) {
        final Message msg = createSelfNodeAdvertisement();
        LOG.log(Level.FINER, "Sending a Node Response Message ");
        msg.addMessageElement(NODERESPONSE, "noderesponse");
        send(id, name, msg);
    }

    private void sendGroupStartupComplete() {
        final Message msg = createSelfNodeAdvertisement();
        LOG.log(Level.FINER, "Sending GroupStartupComplete Message for group:" + manager.getGroupName());
        msg.addMessageElement(GROUPSTARTUPCOMPLETE, "true");
        send(null, null, msg);
    }

    /**
     * Creates a Master Query Message
     *
     * @return a message containing a master query
     */
    private Message createMasterQuery() {
        final Message msg = createSelfNodeAdvertisement();
        msg.addMessageElement(MASTERQUERY, "query");
        LOG.log(Level.FINER, "Created a Master Node Query Message ");
        return msg;
    }

    /**
     * Creates a Node Query Message
     *
     * @return a message containing a node query
     */
    private Message createNodeQuery() {
        final Message msg = createSelfNodeAdvertisement();
        msg.addMessageElement(NODEQUERY, "nodequery");
        LOG.log(Level.FINER, "Created a Node Query Message ");
        return msg;
    }

    /**
     * Creates a Master Response Message
     *
     * @param masterID     the MasterNode ID
     * @param announcement if true, creates an anouncement type message, otherwise it creates a response type.
     * @return a message containing a MasterResponse element
     */
    private Message createMasterResponse(boolean announcement, final PeerID masterID) {
        final Message msg = createSelfNodeAdvertisement();
        String type = MASTERNODE;
        if (!announcement) {
            type = MASTERNODERESPONSE;
        }
        msg.addMessageElement(type, masterID);
        if (groupStarting) {
            // note that we are currently not sending static list of known group members in MasterNodeResponse at this time
            msg.addMessageElement(GROUPSTARTING, Boolean.toString(groupStarting));
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Created a Master Response Message with masterId = " + masterID.toString() + " groupStarting=" + Boolean.toString(groupStarting));
        }
        return msg;
    }

    /**
     * Returns the ID of a discovered Master node
     *
     * @return the MasterNode ID
     */
    boolean discoverMaster() {
        masterViewID.set(clusterViewManager.getMasterViewID());
        final long timeToWait = timeout;
        LOG.log(Level.FINER, "Attempting to discover a master node");
        Message query = createMasterQuery();
        send(null, null, query);
        LOG.log(Level.FINER, " waiting for " + timeout + " ms");
        try {
            synchronized (MASTERLOCK) {
                MASTERLOCK.wait(timeToWait);
            }
        } catch (InterruptedException intr) {
            Thread.interrupted();
            LOG.log(Level.FINER, "Thread interrupted", intr);
        }
        LOG.log(Level.FINE, "masterAssigned=" + masterAssigned);
        return masterAssigned;
    }

    /**
     * Returns true if this node is the master node
     *
     * @return The master value
     */
    boolean isMaster() {
        if (masterLogger.isLoggable(Level.FINER)) {
            masterLogger.log(Level.FINER, "isMaster :" + clusterViewManager.isMaster() + " MasterAssigned :" + masterAssigned + " View Size :" + clusterViewManager.getViewSize());
        } else {
            LOG.log(Level.FINER, "isMaster :" + clusterViewManager.isMaster() + " MasterAssigned :" + masterAssigned + " View Size :" + clusterViewManager.getViewSize());
        }
        return clusterViewManager.isMaster();
    }

    /**
     * Returns true if this node is the master node
     *
     * @return The master value
     */
    boolean isMasterAssigned() {
        return masterAssigned;
    }

    /**
     * Returns master node ID
     *
     * @return The master node ID
     */
    PeerID getMasterNodeID() {
        return clusterViewManager.getMaster().getID();
    }

    /**
     * return true if this service has been started, false otherwise
     *
     * @return true if this service has been started, false otherwise
     */
    synchronized boolean isStarted() {
        return started;
    }

    /**
     * Resets the master node designation to the original state. This is typically
     * done when an existing master leaves or fails and a new master node is to
     * selected.
     */
    void resetMaster() {
        LOG.log(Level.FINER, "Resetting Master view");
        masterAssigned = false;
    }

    /**
     * Parseses out the source SystemAdvertisement
     *
     * @param msg the Message
     * @return true if the message is a MasterNode announcement message
     * @throws IOException if an io error occurs
     */
    SystemAdvertisement processNodeAdvertisement(final Message msg) throws IOException {
        final Object msgElement = msg.getMessageElement(NODEADV);
        if (msgElement == null) {
            // no need to go any further
            LOG.log(Level.WARNING, "Missing NODEADV message element. message = " + msg);
            return null;
        }
        final SystemAdvertisement adv;
        if( msgElement instanceof SystemAdvertisement ) {
            adv = (SystemAdvertisement) msgElement;
            if( !adv.getID().equals( localNodeID ) ) {
                LOG.log( Level.FINER, "Received a System advertisment Name :" + adv.getName() );
            }
        } else {
            LOG.log(Level.WARNING, "Received an unknown message");
            adv = null;
        }
        return adv;
    }

    /**
     * Processes a MasterNode announcement.
     *
     * @param msg    the Message
     * @param source the source node SystemAdvertisement
     * @return true if the message is a MasterNode announcement message
     * @throws IOException if an io error occurs
     */
    @SuppressWarnings("unchecked")
    boolean processMasterNodeAnnouncement(final Message msg, final SystemAdvertisement source) throws IOException {
        Object msgElement = msg.getMessageElement(MASTERNODE);
        if (msgElement == null) {
            return false;
        }

        GMSMember member = Utility.getGMSMember(source);
        long seqID = getLongFromMessage(msg, NAMESPACE, MASTERVIEWSEQ);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Received a Master Node Announcement from  member:" + member.getMemberToken() +
                     " of group:" + member.getGroupName() +
                     " masterViewSeqId:" + seqID + " masterAssigned:" + masterAssigned + " isMaster:" + isMaster());
        }
        if (checkMaster(source)) {
            msgElement = msg.getMessageElement(AMASTERVIEW);
            if (msgElement != null && msgElement instanceof List) {
                final List<SystemAdvertisement> newLocalView = (List<SystemAdvertisement>)msgElement;
                if (newLocalView != null) {
                    LOG.log(Level.FINER, MessageFormat.format("Received an authoritative view from {0}, of size {1}" +
                            " resetting local view containing {2}",
                            source.getName(), newLocalView.size(), clusterViewManager.getLocalView().getSize()));
                }
                msgElement = msg.getMessageElement(VIEW_CHANGE_EVENT);
                if (msgElement != null && msgElement instanceof ClusterViewEvent) {
                    LOG.log(Level.FINE, "MasterNode:PMNA: Received Master View with Seq Id="+seqID +
                    "Current sequence is "+clusterViewManager.getMasterViewID());
                    if (!isDiscoveryInProgress() && seqID <= clusterViewManager.getMasterViewID()) {
                        LOG.log(Level.WARNING, MessageFormat.format("Received an older clusterView sequence {0}." +
                                " Current sequence :{1} discarding out of sequence view", seqID, clusterViewManager.getMasterViewID()));
                        return true;
                    }
                    final ClusterViewEvent cvEvent = (ClusterViewEvent)msgElement;
                    assert newLocalView != null;
                    if (!newLocalView.contains(manager.getSystemAdvertisement())) {
                        LOG.log(Level.FINE, "New ClusterViewManager does not contain self. Publishing Self");
                        sendSelfNodeAdvertisement(source.getID(), null);
                        //update the view once the the master node includes this node
                        return true;
                    }
                    clusterViewManager.setMasterViewID(seqID);
                    masterViewID.set(seqID);
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "MN: New MasterViewID = " + clusterViewManager.getMasterViewID());
                    }
                    clusterViewManager.addToView(newLocalView, true, cvEvent);
                }
            } else {
                LOG.log(Level.WARNING, "New View Received without corresponding ViewChangeEvent details");
                //TODO according to the implementation MasterNode does not include VIEW_CHANGE_EVENT
                //when it announces a Authortative master view
                //throw new IOException("New View Received without corresponding ViewChangeEvent details");
            }
        }
        synchronized (MASTERLOCK) {
            MASTERLOCK.notifyAll();
        }
        return true;
    }

    /**
     * Processes a MasterNode response.
     *
     * @param msg    the Message
     * @param source the source node SystemAdvertisement
     * @return true if the message is a master node response message
     * @throws IOException if an io error occurs
     */
    @SuppressWarnings("unchecked")
    boolean processMasterNodeResponse(final Message msg,
                                      final SystemAdvertisement source) throws IOException {
        Object msgElement = msg.getMessageElement(MASTERNODERESPONSE);
        if ( msgElement == null )
            return false;
        long seqID = getLongFromMessage(msg, NAMESPACE, MASTERVIEWSEQ);
        if (LOG.isLoggable(Level.FINE)){
            LOG.log(Level.FINE, "Received a MasterNode Response from Member:" + source.getName() + " PMNR masterViewSeqId:" + seqID +
                                " current MasterViewSeqId:" + masterViewID.get() + " masterAssigned=" + masterAssigned +
                                " isMaster=" + isMaster() + " discoveryInProgress:" + isDiscoveryInProgress());
        }
        msgElement = msg.getMessageElement(GROUPSTARTING);
        if (msgElement != null) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "MNR indicates GroupStart for group: " + manager.getGroupName());
            }
            setGroupStarting(true);
            delayedSetGroupStarting(false, MAX_GROUPSTARTING_TIME);  // place a boundary on length of time that GroupStarting state is true.  Default is 10 minutes.
        }
        msgElement = msg.getMessageElement(AMASTERVIEW);
        if ( msgElement == null || !(msgElement instanceof List) ) {
            clusterViewManager.setMaster( source, true );
            masterAssigned = true;
            return true;
        }
        final List<SystemAdvertisement> newLocalView = (List<SystemAdvertisement>) msgElement;
        msgElement = msg.getMessageElement(VIEW_CHANGE_EVENT);
        if ( msgElement == null || !(msgElement instanceof ClusterViewEvent)) {
            clusterViewManager.setMaster( source, true );
            masterAssigned = true;
            return true;
        }
        if (!isDiscoveryInProgress() && seqID <= clusterViewManager.getMasterViewID()) {
            clusterViewManager.setMaster(source, true);
            masterAssigned = true;
            LOG.log(Level.WARNING,
                    MessageFormat.format("Received an older clusterView sequence {0} of size :{1}" +
                           " Current sequence :{2} discarding out of sequence view",
                            seqID, newLocalView.size(), clusterViewManager.getMasterViewID()));
            return true;
        }
        final ClusterViewEvent cvEvent = (ClusterViewEvent)msgElement;
        if (!newLocalView.contains(manager.getSystemAdvertisement())) {
            clusterViewManager.setMaster( source, true );
            masterAssigned = true;

            // During start cluster,  each starting instance sends out a MasterNodeQuery.
            // Quite likely that one receives a MNR due to other instances MNQ.
            // If one does not contain this instance,  another one surely will.
            // No need to send out the self advertisement.
            //LOG.log(Level.FINE, "Received view does not contain self. Publishing self");
            //sendSelfNodeAdvertisement(source.getID(), null);
            //update the view once the the master node includes this node
            return true;
        }
        clusterViewManager.setMasterViewID(seqID);
        masterViewID.set(seqID);
        boolean masterChanged = clusterViewManager.setMaster( newLocalView, source );
        masterAssigned = true;
        if ( masterChanged )
            clusterViewManager.notifyListeners( cvEvent );
        else
            clusterViewManager.addToView(newLocalView, true, cvEvent);
        synchronized (MASTERLOCK) {
            MASTERLOCK.notifyAll();
        }
        return true;
    }

    /**
     * Processes a Group Startup Complete message
     *
     * @param msg    the Message
     * @param source the source node SystemAdvertisement
     * @return true if the message is a Group Startup Complete message
     * @throws IOException if an io error occurs
     */
    boolean processGroupStartupComplete(final Message msg,
                                        final SystemAdvertisement source) throws IOException {
        Object msgElement = msg.getMessageElement(GROUPSTARTUPCOMPLETE);
        if ( msgElement == null )
            return false;

        // provide wiggle room to enable JoinedAndReady Notifications to be considered part of group startup.
        // have a small delay before transitioning out of GROUP_STARTUP state.
        delayedSetGroupStarting(false, GROUPSTARTING_COMPLETE_DELAY);
        return true;
    }

    /**
     * Processes a cluster change event. This results in adding the new
     * members to the cluster view, and subsequenlty in application notification.
     *
     * @param msg    the Message
     * @param source the source node SystemAdvertisement
     * @return true if the message is a change event message
     * @throws IOException if an io error occurs
     */
    @SuppressWarnings("unchecked")
    boolean processChangeEvent(final Message msg,
                               final SystemAdvertisement source) throws IOException {

        Object msgElement = msg.getMessageElement(VIEW_CHANGE_EVENT);
        LOG.log(Level.FINER,"Inside processChangeEvent for group: " + manager.getGroupName());
        if (msgElement != null && msgElement instanceof ClusterViewEvent) {
            final ClusterViewEvent cvEvent = (ClusterViewEvent)msgElement;
            msgElement = msg.getMessageElement(AMASTERVIEW);
            if (msgElement != null && msgElement instanceof List && cvEvent != null) {
                if (cvEvent.getEvent()  == ClusterViewEvents.JOINED_AND_READY_EVENT &&
                    cvEvent.getAdvertisement().getID().equals(localNodeID)){

                    // after receiving JOINED_AND_READY_EVENT from Master, stop sending READY heartbeat.
                    manager.getHealthMonitor().setJoinedAndReadyReceived();
                }
                long seqID = getLongFromMessage(msg, NAMESPACE, MASTERVIEWSEQ);
                if (seqID <= clusterViewManager.getMasterViewID()) {
                    LOG.log(Level.WARNING, MessageFormat.format("Received a stale clusterview, older clusterview sequence {0}." +
                            " Current sequence :{1} discarding out of sequence view.  Notified listeners of ChangeEvent={2} from {3} for group: {4}",
                            seqID, clusterViewManager.getMasterViewID(), cvEvent.getEvent().toString(),
                             cvEvent.getAdvertisement().getName(), manager.getGroupName()));
                    clusterViewManager.notifyListeners(cvEvent);
                    return true;
                }
                final List<SystemAdvertisement> newLocalView = (List<SystemAdvertisement>)msgElement;
                LOG.log(Level.FINER,
                        MessageFormat.format("Received a new view of size :{0}, event :{1}",
                                newLocalView.size(), cvEvent.getEvent().toString()));
                if (!newLocalView.contains(manager.getSystemAdvertisement())) {
                    LOG.log(Level.FINER, "Received ClusterViewManager does not contain self. Publishing Self");
                    sendSelfNodeAdvertisement(source.getID(), null);
                    //update the view once the the master node includes this node
                    return true;
                }
                clusterViewManager.setMasterViewID(seqID);
                masterViewID.set(seqID);
                clusterViewManager.addToView(newLocalView, true, cvEvent);
                return true;
            }
        }
        return false;
    }

    /**
     * Processes a Masternode Query message. This results in a master node
     * response if this node is a master node.
     *
     * @param msg the Message
     * @param adv the source node SystemAdvertisement
     * @return true if the message is a query message
     * @throws IOException if an io error occurs
     */
    boolean processMasterNodeQuery(final Message msg, final SystemAdvertisement adv, boolean isAdvAddedToView) throws IOException {

        final Object msgElement = msg.getMessageElement(MASTERQUERY);

        if (msgElement == null || adv == null) {
            return false;
        }
        if (isMaster() && masterAssigned) {
            LOG.log(Level.FINE, MessageFormat.format("Received a MasterNode Query from Name :{0} ID :{1}", adv.getName(), adv.getID()));
            if (isAdvAddedToView) {
                final ClusterViewEvent cvEvent = new ClusterViewEvent(ADD_EVENT, adv);
                Message masterResponseMsg = createMasterResponse(false, localNodeID);
                synchronized(masterViewID) {
                    clusterViewManager.setMasterViewID(masterViewID.incrementAndGet());
                    addAuthoritativeView(masterResponseMsg);
                }
                clusterViewManager.notifyListeners(cvEvent);
                if (LOG.isLoggable(Level.INFO)) {
                    LOG.log(Level.INFO, "Master " + manager.getInstanceName() +  " broadcasting ADD_EVENT  of member: " + adv.getName() + " to GMS group: " + manager.getGroupName());
                }
                sendNewView(null, cvEvent, masterResponseMsg, false);
            } else  LOG.log(Level.FINER, "Node " + adv.getName() + " is already in the view. Hence not sending ADD_EVENT.");
        }
        //for issue 484
        //when the master is killed and restarted very quickly
        //there is no failure notification sent out and no new master elected
        //this results in the instance coming back up and assuming group leadership
        //instance which is the master never sends out a join notif for itself.
        //this will get fixed with the following code

        //check if this instance has an older start time of the restarted instance
        //i.e. check if the restarted instance is in the cache of this instance

        //check if the adv.getID was the master before ...
        SystemAdvertisement madv = clusterViewManager.getMaster();
        SystemAdvertisement oldSysAdv = clusterViewManager.get(adv.getID());
        if (madv != null && adv != null && madv.getID().equals(adv.getID())) {
            //master restarted
            //check for the start times for both advs i.e. the one in the view and the one passed into this method.
            //If they are different that means the master has restarted for sure
            //put a warning that master has restarted without failure

            if (confirmInstanceHasRestarted(oldSysAdv, adv)) {
                LOG.warning("Previously elected Master node " + madv.getName() +
                        " has restarted. There was no failure notification sent out for it.");
                //re-elect a new master so that a join and joinedandready event
                //can be sent for the restarted master
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.finer("MasterNode.processMasterNodeQuery() : clusterViewManager.getMaster().getID() = " +
                            clusterViewManager.getMaster().getID());
                    LOG.finer("MasterNode.processMasterNodeQuery() : adv.getID() = " + adv.getID());
                    LOG.finer("MasterNode.processMasterNodeQuery() : clusterViewManager.getMaster().getname() = " +
                            clusterViewManager.getMaster().getName());
                    LOG.finer("MasterNode.processMasterNodeQuery() : adv.getID() = " + adv.getName());
                }
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("MasterNode.processMasterNodeQuery() : re-electing the master...");
                }
                // remove outdated advertisement for failed previous master (with start time of previous master)
                manager.getClusterViewManager().remove(adv);
                // add back in the restarted member with the new start time.
                // otherwise, restarted member will receive a view without itself in it.
                manager.getClusterViewManager().add(adv);
                resetMaster();
                appointMasterNode();
            } else {
                LOG.fine("MasterNode.processMasterNodeQuery() : master node did not restart as suspected");
            }
        } else {
            //some instance other than the master has restarted
            //without a failure notification
             confirmInstanceHasRestarted(oldSysAdv, adv);

        }
        return true;
    }

    boolean confirmInstanceHasRestarted(SystemAdvertisement oldSysAdv, SystemAdvertisement newSysAdv) {
        if (oldSysAdv != null && newSysAdv != null) {
            LOG.fine("MasterNode.confirmInstanceHasRestarted() : oldSysAdv.getName() = " + oldSysAdv.getName());
            long cachedAdvStartTime = -1;
            try {
                cachedAdvStartTime = Long.valueOf(oldSysAdv.getCustomTagValue(CustomTagNames.START_TIME.toString()));
                LOG.fine("MasterNode.confirmInstanceHasRestarted() : cachedAdvStartTime = " + cachedAdvStartTime);
            } catch (NoSuchFieldException ex) {
                LOG.fine("MasterNode.confirmInstanceHasRestarted : Could not find the START_TIME field in the cached system advertisement");
                return false;
            }

            if (cachedAdvStartTime != -1) {
                //that means this instance already had the restarted instance in its view
                //get the new start time for the restarted instance
                long currentAdvStartTime = -1;
                try {
                    currentAdvStartTime = Long.valueOf(newSysAdv.getCustomTagValue(CustomTagNames.START_TIME.toString()));
                } catch (NoSuchFieldException ex) {
                    LOG.fine("MasterNode.confirmInstanceHasRestarted : Could not find the START_TIME field in the current system advertisement");
                    return false;
                }
                LOG.fine("MasterNode.confirmInstanceHasRestarted() : currentAdvStartTime = " + currentAdvStartTime);
                if (currentAdvStartTime != cachedAdvStartTime) {
                    // previous instance has restarted w/o a FAILURE detection.  Clean cache of references to previous instantiation of the instance.
                    manager.getHealthMonitor().cleanAllCaches(oldSysAdv.getName());
                    //that means the instance has really restarted
                    LOG.log(Level.WARNING, MessageFormat.format("Instance {0} was restarted at  {1,time,full} on {1,date}.",
                            newSysAdv.getName(), new Date(currentAdvStartTime)));
                    LOG.log(Level.WARNING, MessageFormat.format("Note that there was no Failure notification sent out for " +
                            "this instance that was previously started at  {0,time,full} on {0,date}", new Date(cachedAdvStartTime)));
                    return true;
                } else {
                    LOG.fine("MasterNode.confirmInstanceHasRestarted : currentAdvStartTime and cachedAdvStartTime have the same value = " +
                    new Date(cachedAdvStartTime) + " .Instance " + newSysAdv.getName() + "was not restarted.");
                    return false;
                }
            } else {
                LOG.fine("MasterNode.confirmInstanceHasRestarted : cachedAdvStartTime does not havea valid value = " + cachedAdvStartTime);
                return false;
            }
        } else {
            LOG.fine("MasterNode.confirmInstanceHasRestarted : oldSysAdv or newSysAdv is null");
            return false;
        }
    }

    /**
     * Processes a Masternode Query message. This results in a master node
     * response if this node is a master node.
     *
     * @param msg the Message
     * @param adv the source node SystemAdvertisement
     * @return true if the message is a query message
     * @throws IOException if an io error occurs
     */
    boolean processNodeQuery(final Message msg, final SystemAdvertisement adv, boolean isAdvAddedToView) throws IOException {
        final Object msgElement = msg.getMessageElement(NODEQUERY);

        if (msgElement == null || adv == null) {
            return false;
        }
        LOG.log(Level.FINER, MessageFormat.format("Received a Node Query from Name :{0} ID :{1}", adv.getName(), adv.getID()));

        if (isMaster() && masterAssigned) {
            LOG.log(Level.FINE, MessageFormat.format("Received a Node Query from Name :{0} ID :{1}", adv.getName(), adv.getID()));
            if(isAdvAddedToView) {
                final ClusterViewEvent cvEvent = new ClusterViewEvent(ADD_EVENT, adv);
                Message responseMsg = createMasterResponse(false, localNodeID);
                synchronized(masterViewID) {
                    clusterViewManager.setMasterViewID(masterViewID.incrementAndGet());
                    addAuthoritativeView(responseMsg);
                }
                clusterViewManager.notifyListeners(cvEvent);
                sendNewView(null, cvEvent, responseMsg, false);
            } else LOG.log(Level.FINER, "Node " + adv.getName() + " is already in the view. Hence not sending ADD_EVENT.");
        } else {
            final Message response = createSelfNodeAdvertisement();
            response.addMessageElement(NODERESPONSE, "noderesponse");
            LOG.log(Level.FINER, "Sending Node response to  :" + adv.getName());
            send(adv.getID(), null, response);
        }
        return true;
    }

    /**
     * Processes a Node Response message.
     *
     * @param msg the Message
     * @param adv the source node SystemAdvertisement
     * @return true if the message is a response message
     * @throws IOException if an io error occurs
     */
    boolean processNodeResponse(final Message msg, final SystemAdvertisement adv, boolean isAdvAddedToView) throws IOException {
        final Object msgElement = msg.getMessageElement(NODERESPONSE);

        if (msgElement == null || adv == null) {
            return false;
        }
        if (isMaster() && masterAssigned) {
            LOG.log(Level.FINE, MessageFormat.format("Received a Node Response from Name :{0} ID :{1}", adv.getName(), adv.getID()));
            if(isAdvAddedToView) {
                final ClusterViewEvent cvEvent = new ClusterViewEvent(ADD_EVENT, adv);
                Message responseMsg = createMasterResponse(false, localNodeID);
                synchronized(masterViewID) {
                    clusterViewManager.setMasterViewID(masterViewID.incrementAndGet());
                    addAuthoritativeView(responseMsg);
                }
                clusterViewManager.notifyListeners(cvEvent);
                sendNewView(null, cvEvent, responseMsg, false);
            } else LOG.log(Level.FINER, "Node " + adv.getName() + " is already in the view. Hence not sending ADD_EVENT.");
        }
        return true;
    }

    /**
     * Processes a MasterNode Collision.  When two nodes assume a master role (by assertion through
     * a master node announcement), each node can indepedentaly and deterministically elect the master node.
     * This is done through electing the node atop of the NodeID sort order. If there are more than two
     * nodes in collision, this same process is repeated.
     *
     * @param msg the Message
     * @param adv the source node SystemAdvertisement
     * @return true if the message was indeed a collision message
     * @throws IOException if an io error occurs
     */
    boolean processMasterNodeCollision(final Message msg,
                                       final SystemAdvertisement adv) throws IOException {

        final Object msgElement = msg.getMessageElement(CCNTL);
        if (msgElement == null) {
            return false;
        }
        final SystemAdvertisement madv = manager.getSystemAdvertisement();
        LOG.log(Level.FINER, MessageFormat.format("Candidate Master: " + madv.getName() + "received a MasterNode Collision from Name :{0} ID :{1}", adv.getName(), adv.getID()));
        if (madv.getID().compareTo(adv.getID()) >= 0) {
            LOG.log(Level.FINER, "Member " + madv.getName() + " affirming Master Node role over member:" + adv.getName());
            synchronized (MASTERLOCK) {
                //Ensure the view SeqID is incremented by 2
                clusterViewManager.setMasterViewID(masterViewID.incrementAndGet());
                announceMaster(manager.getSystemAdvertisement());
                MASTERLOCK.notifyAll();
            }
        } else {
            LOG.log(Level.FINER, "Resigning Master Node role");
            clusterViewManager.setMaster(adv, true);
        }
        return true;
    }

    /**
     * Probes a node. Used when a node does not exist in local view
     *
     * @param entry node entry
     * @throws IOException if an io error occurs sending the message
     */
    void probeNode(final HealthMessage.Entry entry) throws IOException {
        if (isMaster() && masterAssigned) {
            LOG.log(Level.FINER, "Probing ID = " + entry.id + ", name = " + entry.adv.getName());
            send(entry.id, null, createNodeQuery());
        }
    }

    public void receiveMessageEvent(final MessageEvent event) throws MessageIOException {
        final Message msg = event.getMessage();
        if (msg == null) {
            return;
        }
        final MasterNodeMessageEvent mnme = new MasterNodeMessageEvent(event);
        if (mnme.seqId == -1) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("receiveMessageEvent: process master node message masterViewSeqId:" + mnme.seqId + " from member:" + event.getSourcePeerID());
            }
            processNextMessageEvent(mnme);
        } else {
            final boolean added;
            synchronized(outstandingMasterNodeMessages) {

                //place message into set ordered via MasterViewSequenceId.
                added = outstandingMasterNodeMessages.add(mnme);
                outstandingMasterNodeMessages.notify();
            }
            if (added) {
                LOG.config("receiveMessageEvent: added master node message masterViewSeqId:" + mnme.seqId + " from member:" + event.getSourcePeerID());
            } else {
                LOG.warning("receiveMessageEvent: ignored duplicate master node message masterViewSeqId:" + mnme.seqId + " from member:" + event.getSourcePeerID());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void processNextMessageEvent(final MasterNodeMessageEvent masterNodeMessage) throws MessageIOException {
        boolean result = false;
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "Received a message inside  pipeMsgEvent");
        }
        if (manager.isStopping()) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Since this Peer is Stopping, returning without processing incoming master node message. ");
            }
            return;
        }

        if (isStarted()) {
            final Message msg;
            // grab the message from the event
            msg = masterNodeMessage.msg;
            long seqId = masterNodeMessage.seqId;
            if (msg == null) {
                LOG.log(Level.WARNING, "Received a null message");
                return;
            }
            try {
                final SystemAdvertisement adv = processNodeAdvertisement(msg);
                if (adv != null && adv.getID().equals(localNodeID)) {
                    LOG.log(Level.FINEST, "Discarding loopback message");
                    return;
                }
                // add the advertisement to the list
                if (adv != null) {
                    if (isMaster() && masterAssigned) {
                        result = clusterViewManager.add(adv);
                    } else if (discoveryInProgress) {
                        result = false;  // never report Join event during discovery mode.
                        discoveryView.add(adv);
                    }
                }
                if (processMasterNodeQuery(msg, adv, result)) {
                    return;
                }
                if (processNodeQuery(msg, adv, result)) {
                    return;
                }
                if (processNodeResponse(msg, adv, result)) {
                    return;
                }
                if (processGroupStartupComplete(msg, adv)) {
                    return;
                }
                if (processMasterNodeResponse(msg, adv)) {
                    return;
                }
                if (processMasterNodeAnnouncement(msg, adv)) {
                    return;
                }
                if (processMasterNodeCollision(msg, adv)) {
                    return;
                }
                if (processChangeEvent(msg, adv)) {
                    return;
                }
            } catch (IOException e) {
                LOG.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            LOG.log(Level.FINER, MessageFormat.format("ClusterViewManager contains {0} entries", clusterViewManager.getViewSize()));
        } else {
            LOG.log(Level.FINER, "Started : " + isStarted());
        }
    }

    public int getType() {
        return Message.TYPE_MASTER_NODE_MESSAGE;
    }

    private void announceMaster(SystemAdvertisement adv) {
        final Message msg = createMasterResponse(true, adv.getID());
        final ClusterViewEvent cvEvent = new ClusterViewEvent(ClusterViewEvents.MASTER_CHANGE_EVENT, adv);
        if(masterAssigned && isMaster()){
            LOG.log(Level.INFO, MessageFormat.format("Announcing Master Node designation for member: {1} of group: {2}. Local view contains " +
                           "{0} entries", clusterViewManager.getViewSize(), manager.getInstanceName(), manager.getGroupName()));
            
            sendNewView(null, cvEvent, msg, true);
        }
    }

    /**
     * MasterNode discovery thread. Starts the master node discovery protocol
     */
    public void run() {
        startMasterNodeDiscovery();
    }

    /**
     * Starts the master node discovery protocol
     */
    void startMasterNodeDiscovery() {
        int count = 0;
        //assumes self as master node
        synchronized (this) {
            clusterViewManager.start();
        }
        if (masterAssigned) {
            discoveryInProgress = false;
            LOG.log(Level.FINE, "startMasterNodeDiscovery: discovery completed. masterSequenceId:" + this.masterViewID.get() +
                    " clusterViewManager.masterViewID:" + clusterViewManager.getMasterViewID());
            synchronized (discoveryLock) { discoveryLock.notifyAll(); }
            return;
        }
        while (!stop && count < interval) {
            if (!discoverMaster()) {
                // TODO: Consider changing this approach to a background reaper
                // that would reconcole the group from time to time, consider
                // using an incremental timeout interval ex. 800, 1200, 2400,
                // 4800, 9600 ms for iteration periods, then revert to 800
                count++;
            } else {
                break;
            }
        }
        // timed out
        if (!masterAssigned) {
            LOG.log(Level.FINER, "MN Discovery timeout, appointing master");
            appointMasterNode();
        }
       LOG.log(Level.FINEST,"startMasterNodeDiscovery making discoveryInProgress false");
       LOG.log(Level.FINE, "startMasterNodeDiscovery: after discoverMaster polling, discovery completed. masterSequenceId:" + this.masterViewID.get() +
                    " clusterViewManager.masterViewID:" + clusterViewManager.getMasterViewID());
       discoveryInProgress = false;
       synchronized (discoveryLock) { discoveryLock.notifyAll(); }
    }

    /**
     * determines a master node candidate, if the result turns to be this node
     * then a master node announcement is made to assert such state
     */
    void appointMasterNode() {
        if (masterAssigned) {
            return;
        }
        final SystemAdvertisement madv;
        LOG.log(Level.FINER, "MasterNode: discoveryInProgress="+discoveryInProgress);
        if (discoveryInProgress) {
            madv = discoveryView.getMasterCandidate();
        } else {
            madv = clusterViewManager.getMasterCandidate();
        }
        LOG.log(Level.FINER, "MasterNode: Master Candidate="+madv.getName());
        //avoid notifying listeners
        clusterViewManager.setMaster(madv, false);
        masterAssigned = true;
        if (madv.getID().equals(localNodeID)) {
            LOG.log(Level.FINER, "MasterNode: Setting myself as MasterNode ");
            clusterViewManager.setMasterViewID(masterViewID.incrementAndGet());
            LOG.log(Level.FINER, "MasterNode: masterViewId ="+masterViewID );
            // generate view change event
            if (discoveryInProgress) {
                List<SystemAdvertisement> list = discoveryView.getView();
                final ClusterViewEvent cvEvent = new ClusterViewEvent(ClusterViewEvents.MASTER_CHANGE_EVENT, madv);
                clusterViewManager.addToView(list, true, cvEvent);
            } else {
                LOG.log(Level.FINER, "MasterNode: Notifying Local Listeners of  Master Change");
                final ClusterViewEvent cvEvent = new ClusterViewEvent(ClusterViewEvents.MASTER_CHANGE_EVENT, madv);
                clusterViewManager.notifyListeners(cvEvent);
            }

        }
        discoveryView.clear();
        discoveryView.add(sysAdv);
        synchronized (MASTERLOCK) {
            if (madv.getID().equals(localNodeID)) {
                // this thread's job is done
                LOG.log(Level.INFO, "Assuming Master Node designation member:" +
                        madv.getName() + " for group:" + manager.getGroupName());
                //broadcast we are the masternode for the cluster
                LOG.log(Level.FINER, "MasterNode: announcing MasterNode assumption ");
                announceMaster(manager.getSystemAdvertisement());
                MASTERLOCK.notifyAll();
            }
        }
    }

    /**
     * Send a message to a specific node. In the case where the id is null the
     * message multicast
     *
     * @param peerid the destination node, if null, the message is sent to the cluster
     * @param msg    the message to send
     * @param name   name used for debugging messages
     */
    private void send(final PeerID peerid, final String name, final Message msg) {
        try {
            if (peerid != null) {
                // Unicast datagram
                // create a op pipe to the destination peer
                LOG.log(Level.FINER, "Unicasting Message to :" + name + "ID=" + peerid);
                final boolean sent = manager.getNetworkManager().send( peerid, msg );
                if (!sent) {
                    LOG.log(Level.WARNING, "OutputPipe.send unexpectedly returned false sending messge " + msg +
                            " to instance " + name);
                }
            } else {
                // multicast
                LOG.log(Level.FINER, "Broadcasting Message");
                final boolean sent = manager.getNetworkManager().broadcast(msg);
                if (!sent) {
                    LOG.log(Level.WARNING, "OutputPipe.send unexpectedly returned false broadcasting messge " + msg +
                            " to cluster");
                }
            }
        } catch (IOException io) {
            LOG.log(Level.FINEST, "Failed to send message", io);
        }
    }

    /**
     * Sends the discovered view to the group indicating a new membership snapshot has been
     * created. This will lead to all members replacing their localviews to
     * this new view.
     *
     * @param event       The ClusterViewEvent object containing details of the event.
     * @param msg         The message to send
     * @param toID        receipient ID
     * @param includeView if true view will be included in the message
     */
    void sendNewView(final PeerID toID,
                     final ClusterViewEvent event,
                     final Message msg,
                     final boolean includeView) {
        if (includeView) {
            addAuthoritativeView(msg);
        }
        //LOG.log(Level.FINER, MessageFormat.format("Created a view element of size {0}bytes", cvEvent.getByteLength()));
        msg.addMessageElement(VIEW_CHANGE_EVENT, event);
        LOG.log(Level.FINER, "Sending new authoritative cluster view to group, event :" + event.getEvent().toString()+" viewSeqId: "+clusterViewManager.getMasterViewID());
        send(toID, null, msg);
    }

    /**
     * Adds an authoritative message element to a Message
     *
     * @param msg The message to add the view to
     */
    void addAuthoritativeView(final Message msg) {
        final List<SystemAdvertisement> view;
        ClusterView cv = clusterViewManager.getLocalView();
        view = cv.getView();
        LOG.log(Level.FINE, "MasterNode: Adding Authoritative View of size "+view.size()+ "  to view message masterSeqId=" + cv.masterViewId);
        msg.addMessageElement(AMASTERVIEW, (Serializable)view);
        addLongToMessage(msg, NAMESPACE, MASTERVIEWSEQ, cv.masterViewId);
    }

    /**
     * Stops this service
     */
    synchronized void stop() {
        LOG.log(Level.FINER, "Stopping MasterNode");
        discoveryView.clear();
        thread = null;
        masterAssigned = false;
        started = false;
        stop = true;
        discoveryInProgress = false;
        synchronized (discoveryLock){
            discoveryLock.notifyAll();
        }
        manager.getNetworkManager().removeMessageListener( this );
        processOutstandingMessagesThread.interrupt();
    }

    /**
     * Starts this service. Creates the communication channels, and the MasterNode discovery thread.
     */
    synchronized void start() {
        LOG.log(Level.FINER, "Starting MasterNode");
        this.clusterViewManager = manager.getClusterViewManager();
        started = true;
        manager.getNetworkManager().addMessageListener( this );
        LOG.info("MasterNode message listener is registered for member: " + manager.getInstanceName() + " group:" + manager.getGroupName());
        processOutstandingMessagesThread = new Thread(new ProcessOutstandingMessagesTask(), "MasterNode processOutStandingMessages");
        processOutstandingMessagesThread.setDaemon(true);
        processOutstandingMessagesThread.start();
        thread = new Thread(this, "MasterNode");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Sends a ViewChange event to the cluster.
     *
     * @param event VievChange event
     */
    void viewChanged(final ClusterViewEvent event) {
        if (isMaster() && masterAssigned) {
            clusterViewManager.notifyListeners( event );
            Message msg = createSelfNodeAdvertisement();
            synchronized(masterViewID) {
                //increment the view seqID
                clusterViewManager.setMasterViewID(masterViewID.incrementAndGet());
                addAuthoritativeView(msg);
            }
            sendNewView(null, event, msg, false);
        }
    }

    /**
     * Adds a long to a message
     *
     * @param message   The message to add to
     * @param nameSpace The namespace of the element to add. a null value assumes default namespace.
     * @param elemName  Name of the Element.
     * @param data      The feature to be added to the LongToMessage attribute
     */
    private static void addLongToMessage(Message message, String nameSpace, String elemName, long data) {
        message.addMessageElement(elemName, data);
    }

    /**
     * Returns an long from a message
     *
     * @param message   The message to retrieve from
     * @param nameSpace The namespace of the element to get.
     * @param elemName  Name of the Element.
     * @return The long value, -1 if element does not exist in the message
     * @throws NumberFormatException If the String does not contain a parsable int.
     */
    private static long getLongFromMessage(Message message, String nameSpace, String elemName) throws NumberFormatException {
        Object value = message.getMessageElement(elemName);
        if (value != null) {
            return Long.parseLong(value.toString());
        } else {
            return -1;
        }
    }

    /**
     * This method allows the DAS to become a master by force. This
     * is especially important when the the DAS is going down and then
     * coming back up. This way only the DAS will ever be the master.
     */
    void takeOverMasterRole() {
        synchronized (MASTERLOCK) {
            final SystemAdvertisement madv = clusterViewManager.get(localNodeID);
            LOG.log(Level.FINER, "MasterNode: Forcefully becoming the Master..." + madv.getName());
            //avoid notifying listeners
            clusterViewManager.setMaster(madv, false);
            masterAssigned = true;

            clusterViewManager.setMasterViewID(masterViewID.incrementAndGet());
            LOG.log(Level.FINER, "MasterNode: becomeMaster () : masterViewId ="+masterViewID );
            // generate view change event
            LOG.log(Level.FINER, "MasterNode: becomeMaster () : Notifying Local Listeners of  Master Change");
            final ClusterViewEvent cvEvent = new ClusterViewEvent(ClusterViewEvents.MASTER_CHANGE_EVENT, madv);
            clusterViewManager.notifyListeners(cvEvent);

            discoveryView.clear();
            discoveryView.add(sysAdv);

            //broadcast we are the masternode
            LOG.log(Level.FINER, "MasterNode: becomeMaster () : announcing MasterNode assumption ");
            announceMaster(manager.getSystemAdvertisement());
            MASTERLOCK.notifyAll();
            manager.notifyNewMaster();

        }
    }

    void setClusterStopping() {
        clusterStopping = true;
    }

    ClusterViewEvent sendReadyEventView(final SystemAdvertisement adv) {
        final ClusterViewEvent cvEvent = new ClusterViewEvent(ClusterViewEvents.JOINED_AND_READY_EVENT, adv);
        LOG.log(Level.FINEST, MessageFormat.format("Sending to Group, Joined and Ready Event View for peer :{0}", adv.getName()));
        Message msg = this.createSelfNodeAdvertisement();
        synchronized(masterViewID) {
            clusterViewManager.setMasterViewID(masterViewID.incrementAndGet());
            this.addAuthoritativeView(msg);
        }
        sendNewView(null, cvEvent, msg, false);
        return cvEvent;
    }

    boolean isDiscoveryInProgress() {
        return discoveryInProgress;
    }

    void setGroupStarting(boolean value) {
        groupStarting = value;
        getGMSContext().setGroupStartup(value);
    }

    /**
     * Avoid boundary conditions for group startup completed.  Delay setting to false for delaySet time.
     *
     * @param value
     * @param delaySet in milliseconds, time to delay setting group starting to value
     */
    private void delayedSetGroupStarting(boolean value, long delaySet) {
        synchronized(timer) {
            if (groupStartingTask != null) {
                groupStartingTask.cancel();
            }
            groupStartingTask = new DelayedSetGroupStartingCompleteTask(delaySet);

            // place a delay duration on group startup duration to allow possibly late arriving JoinedAndReady notifications to be considered part of group.
            timer.schedule(groupStartingTask, delaySet);
        }
    }

    public boolean isGroupStartup() {
        return groupStarting;
    }

    /**
     * Demarcate start and end of group startup.
     *
     * All members started with in this duration of time are considered part of GROUP_STARTUP in GMS JoinNotification and JoinAndReadyNotifications.
     * All other members restarted after this duration are INSTANCE_STARTUP.
     *
     * Method is called once when group startup is initiated with <code>INITIATED</code> as startupState.
     * Method is called again when group startup has completed and indication of success or failure is
     * indicated by startupState of <code>COMPLETED_SUCCESS</code> or <code>COMPLATED_FAILED</code>.
     *
     * @param startupState  either the start or successful or failed completion of group startup.
     * @param memberTokens  list of members associated with <code>startupState</code>
     */
    void groupStartup(GMSConstants.groupStartupState startupState, List<String> memberTokens) {
        StringBuffer sb = new StringBuffer();
        groupStartingMembers = memberTokens;
        switch(startupState) {
            case INITIATED:
                // Optimization:  Rather than broadcast to members of cluster (that have not been started yet when INIATIATED group startup),
                // assume this is called by a static administration utility that is running in same process as master node.
                // This call toggles state of whether gms MasterNode is currently part of group startup depending on the value of startupState.
                // See createMasterResponse() for how this group starting is sent from Master to members of the group.
                // See processMasterNodeResponse() for how members process this sent info.
                setGroupStarting(true);
                sb.append(" Starting Members: ");
                break;
            case COMPLETED_FAILED:
                setGroupStarting(false);
                sb.append(" Failed Members: ");
                if (this.isMaster() && this.isMasterAssigned()) {
                    // send a message to other instances in cluster that group startup has completed
                    sendGroupStartupComplete();
                }
                break;
            case COMPLETED_SUCCESS:
                setGroupStarting(false);
                sb.append(" Started Members: ");
                if (this.isMaster() && this.isMasterAssigned()) {
                    // send a message to other instances in cluster that group startup has completed
                    sendGroupStartupComplete();
                }
                break;
        }
        for (String member: memberTokens) {
            sb.append(member).append(",");
        }
        LOG.info("GroupStart for group: " +  getGMSContext().getGroupName()  + " State:" + startupState.toString() + sb);
    }

    private GMSContext getGMSContext() {
        if (ctx == null) {
            ctx = (GMSContext) GMSContextFactory.getGMSContext(manager.getGroupName());
        }
        return ctx;
    }

    class DelayedSetGroupStartingCompleteTask extends TimerTask {
        final private long delay;  //millisecs

        public DelayedSetGroupStartingCompleteTask(long delay) {
            this.delay = delay;
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer("GroupStartupCompleteTask scheduled in " + delay + " ms");
            }
        }

        public void run() {
            if (delay == MasterNode.MAX_GROUPSTARTING_TIME) {
                LOG.warning("missed GroupStartupComplete message. Timed out group startup after " +
                            MasterNode.MAX_GROUPSTARTING_TIME / 1000 + " secs");
            }
            synchronized (timer) {
                setGroupStarting(false);
                groupStartingTask = null;
            }
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Received GroupStartupComplete for group:" + manager.getGroupName() + " delay(ms)=" + delay);
            }
        }
    }

    static public class MasterNodeMessageEvent implements Comparable {
        public final long seqId;
        public final Message msg;
        public final MessageEvent event;

        public MasterNodeMessageEvent(MessageEvent event) {
            this.event = event;
            msg = event.getMessage();
            seqId = getLongFromMessage(msg, NAMESPACE, MASTERVIEWSEQ);
        }

        public int compareTo(Object o) {
            if (o instanceof MasterNodeMessageEvent) {
                MasterNodeMessageEvent e = (MasterNodeMessageEvent)o;
                int peerCompareToResult = event.getSourcePeerID().compareTo(e.event.getSourcePeerID());
                //return peerCompareToResult + (int)(seqId - ((MasterNodeMessageEvent)o).seqId);
                return (int)(seqId - ((MasterNodeMessageEvent)o).seqId);
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Process incoming Master Node messages in sorted order via MasterViewSequenceId.
     * Serializes processing to one at a time.
     */
    public class ProcessOutstandingMessagesTask implements Runnable {

        public ProcessOutstandingMessagesTask() {
        }

        public void run() {
            MasterNodeMessageEvent msg;

            while (manager != null && !manager.isStopping()) {
                msg = null;
                try {
                    synchronized(outstandingMasterNodeMessages) {

                        // Only process one outstanding message at a time.  Allow incoming handlers to add new events to be processed.
                        if (outstandingMasterNodeMessages.size() > 0) {
                            msg = outstandingMasterNodeMessages.first();
                            if (msg != null) {
                                outstandingMasterNodeMessages.remove(msg);
                            }
                        } else {
                            outstandingMasterNodeMessages.wait();
                        }
                    } // only synchronize removing first item from set.

                    if (msg != null) {
                        processNextMessageEvent(msg);
                    }

                    if (isDiscoveryInProgress() || ! isMaster()) {
                        // delay window before processing next message. allow messages received out of order to be ordered.
                        Thread.sleep(30);
                    }
                } catch (InterruptedException ie) {
                } catch (Throwable t) {
                    LOG.log(Level.CONFIG, "MasterNode.ProcessOutstandingMessages.run: ignoring exception " + t.getLocalizedMessage(), t);
                }
            }
            LOG.config("Completed processing outstanding master node messages for member:" + manager.getInstanceName() + " group:" + manager.getGroupName() +
                       " oustandingMessages to process:" + outstandingMasterNodeMessages.size());
        }
    }
}

