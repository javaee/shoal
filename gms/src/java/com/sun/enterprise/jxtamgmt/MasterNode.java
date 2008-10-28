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
package com.sun.enterprise.jxtamgmt;

import static com.sun.enterprise.jxtamgmt.ClusterViewEvents.ADD_EVENT;
import static com.sun.enterprise.jxtamgmt.JxtaUtil.getObjectFromByteArray;
import com.sun.enterprise.ee.cms.impl.jxta.CustomTagNames;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.MessageTransport;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.id.ID;
import net.jxta.impl.endpoint.router.EndpointRouter;
import net.jxta.impl.endpoint.router.RouteControl;
import net.jxta.impl.pipe.BlockingWireOutputPipe;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.protocol.RouteAdvertisement;
import net.jxta.peer.PeerID;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
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
class MasterNode implements PipeMsgListener, Runnable {
    private static final Logger LOG = JxtaUtil.getLogger(MasterNode.class.getName());
    private final ClusterManager manager;
    private InputPipe inputPipe;
    private OutputPipe outputPipe;

    private boolean masterAssigned = false;
    private volatile boolean discoveryInProgress = true;
    private ID localNodeID = ID.nullID;
    private final SystemAdvertisement sysAdv;
    private PipeAdvertisement pipeAdv = null;
    private final PipeService pipeService;
    private final MessageElement sysAdvElement;
    private MessageElement routeAdvElement = null;
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
    private static final String ROUTEADV = "ROUTE";
    private static final String AMASTERVIEW = "AMV";
    private static final String MASTERVIEWSEQ = "SEQ";

    private int interval = 6;
    // Default master node discovery timeout
    private long timeout = 10 * 1000L;
    private static final String VIEW_CHANGE_EVENT = "VCE";
    private RouteControl routeControl = null;
    private MessageTransport endpointRouter = null;
    private transient ConcurrentHashMap<ID, OutputPipe> pipeCache = new ConcurrentHashMap<ID, OutputPipe>();

    private boolean clusterStopping = false;
    final Object discoveryLock = new Object();

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
        PeerGroup group = manager.getNetPeerGroup();
        pipeService = group.getPipeService();
        localNodeID = group.getPeerID();
        if (timeout > 0) {
            this.timeout = timeout;
        }
        this.interval = interval;
        this.manager = manager;
        sysAdv = manager.getSystemAdvertisement();
        discoveryView = new ClusterView(sysAdv);
        sysAdvElement = new TextDocumentMessageElement(NODEADV,
                (XMLDocument) manager.getSystemAdvertisement()
                        .getDocument(MimeMediaType.XMLUTF8), null);
        //used to ensure up to date routes are used
        endpointRouter = (group.getEndpointService()).getMessageTransport("jxta");
        if (endpointRouter != null) {
            routeControl = (RouteControl) endpointRouter.transportControl(EndpointRouter.GET_ROUTE_CONTROL, null);
            RouteAdvertisement route = routeControl.getMyLocalRoute();
            if (route != null) {
                routeAdvElement = new TextDocumentMessageElement(ROUTEADV,
                        (XMLDocument) route.getDocument(MimeMediaType.XMLUTF8), null);
            }
        }
        if (routeAdvElement == null) {
            LOG.warning("MasterNode constructor: bad constraints endpointRouter= " + endpointRouter +
                   " routeControl=" + routeControl + " routeAdvElement=" + routeAdvElement);
        } else if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("MasterNode() routeAdvElement=" + routeAdvElement);
        }

        try {
            // create the pipe advertisement, to be used in creating the pipe
            pipeAdv = createPipeAdv();
            //create output
            outputPipe = pipeService.createOutputPipe(pipeAdv, 0);
        } catch (IOException io) {
            io.printStackTrace();
            LOG.log(Level.WARNING, "Failed to create master outputPipe : " + io);
        }
    }

    /**
     * in the event of a failure or planned shutdown, remove the
     * pipe from the pipeCache
     */
     
    void removePipeFromCache(ID token) {
        pipeCache.remove(token);
    }

    void clearPipeCache() {
        pipeCache.clear();
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
            if (manager.getNodeID().toString().compareTo(systemAdv.getID().toString()) >= 0) {
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
        final MessageElement el = new StringMessageElement(CCNTL, localNodeID.toString(), null);
        msg.addMessageElement(NAMESPACE, el);
        LOG.log(Level.FINER, "Created a Master Collision Message");
        return msg;
    }

    private Message createSelfNodeAdvertisement() {
        Message msg = new Message();
        msg.addMessageElement(NAMESPACE, sysAdvElement);
        return msg;
    }

    private void sendSelfNodeAdvertisement(final ID id, final String name) {
        final Message msg = createSelfNodeAdvertisement();
        LOG.log(Level.FINER, "Sending a Node Response Message ");
        final MessageElement el = new StringMessageElement(NODERESPONSE, "noderesponse", null);
        msg.addMessageElement(NAMESPACE, el);
        send(id, name, msg);
    }

    /**
     * Creates a Master Query Message
     *
     * @return a message containing a master query
     */
    private Message createMasterQuery() {
        final Message msg = createSelfNodeAdvertisement();
        final MessageElement el = new StringMessageElement(MASTERQUERY, "query", null);
        msg.addMessageElement(NAMESPACE, el);
        addRoute(msg);
        LOG.log(Level.FINER, "Created a Master Node Query Message ");
        return msg;
    }

    void addRoute(Message msg) {
        if (routeAdvElement != null && routeControl != null) {
            msg.addMessageElement(NAMESPACE, routeAdvElement);
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("addRoute(): Did not add route to msg " + msg + " routeAdvElement=" + routeAdvElement +
                        " routeControl=" + routeControl);
            }
        }
    }

    /**
     * Creates a Node Query Message
     *
     * @return a message containing a node query
     */
    private Message createNodeQuery() {
        final Message msg = createSelfNodeAdvertisement();
        final MessageElement el = new StringMessageElement(NODEQUERY, "nodequery", null);
        msg.addMessageElement(NAMESPACE, el);
        addRoute(msg);
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
    private Message createMasterResponse(boolean announcement, final ID masterID) {
        final Message msg = createSelfNodeAdvertisement();
        String type = MASTERNODE;
        if (!announcement) {
            type = MASTERNODERESPONSE;
        }
        final MessageElement el = new StringMessageElement(type, masterID.toString(), null);
        msg.addMessageElement(NAMESPACE, el);
        addRoute(msg);
        LOG.log(Level.FINER, "Created a Master Response Message with masterId = " + masterID.toString());
        return msg;
    }

    /**
     * Constructs a propagated PipeAdvertisement for the MasterNode discovery
     * protocol
     *
     * @return MasterNode discovery protocol PipeAdvertisement
     */
    private PipeAdvertisement createPipeAdv() {
        final PipeAdvertisement pipeAdv;
        // create the pipe advertisement, to be used in creating the pipe
        pipeAdv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        pipeAdv.setPipeID(manager.getNetworkManager().getMasterPipeID());
        pipeAdv.setType(PipeService.PropagateType);
        return pipeAdv;
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
        LOG.log(Level.FINER, "isMaster :" + clusterViewManager.isMaster() + " MasterAssigned :" + masterAssigned + " View Size :" + clusterViewManager.getViewSize());
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
    ID getMasterNodeID() {
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
        final MessageElement msgElement = msg.getMessageElement(NAMESPACE, NODEADV);
        if (msgElement == null) {
            // no need to go any further
            LOG.log(Level.WARNING, "Missing NODEADV message element");
            JxtaUtil.printMessageStats(msg, false);
            return null;
        }

        final StructuredDocument asDoc;
        asDoc = StructuredDocumentFactory.newStructuredDocument(msgElement.getMimeType(), msgElement.getStream());
        final SystemAdvertisement adv = new SystemAdvertisement(asDoc);
        if (!adv.getID().equals(localNodeID)) {
            LOG.log(Level.FINER, "Received a System advertisment Name :" + adv.getName());
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
    boolean processMasterNodeAnnouncement(final Message msg, final SystemAdvertisement source) throws IOException {

        MessageElement msgElement = msg.getMessageElement(NAMESPACE, MASTERNODE);
        if (msgElement == null) {
            return false;
        }
        processRoute(msg);
        LOG.log(Level.FINER, "Received a Master Node Announcement from Name :" + source.getName());
        if (checkMaster(source)) {
            msgElement = msg.getMessageElement(NAMESPACE, AMASTERVIEW);
            if (msgElement != null) {
                final ArrayList<SystemAdvertisement> newLocalView = getObjectFromByteArray(msgElement);
                if (newLocalView != null) {
                    LOG.log(Level.FINER, MessageFormat.format("Received an authoritative view from {0}, of size {1}" +
                            " resetting local view containing {2}",
                            source.getName(), newLocalView.size(), clusterViewManager.getLocalView().getSize()));
                }
                long seqID = getLongFromMessage(msg, NAMESPACE, MASTERVIEWSEQ);
                msgElement = msg.getMessageElement(NAMESPACE, VIEW_CHANGE_EVENT);
                if (msgElement != null) {
                    LOG.log(Level.FINEST, "MasterNode:PMNA: Received Master View with Seq Id="+seqID +
                    "Current sequence is "+clusterViewManager.getMasterViewID());
                    if (seqID <= clusterViewManager.getMasterViewID()) {
                        LOG.log(Level.FINER, MessageFormat.format("Received an older clusterView sequence {0}." +
                                " Current sequence :{1} discarding out of sequence view", seqID, clusterViewManager.getMasterViewID()));
                        return true;
                    }
                    final ClusterViewEvent cvEvent = getObjectFromByteArray(msgElement);
                    assert newLocalView != null;
                    if (!newLocalView.contains(manager.getSystemAdvertisement())) {
                        LOG.log(Level.FINER, "New ClusterViewManager does not contain self. Publishing Self");
                        sendSelfNodeAdvertisement(source.getID(), null);
                        //update the view once the the master node includes this node
                        return true;
                    }
                    clusterViewManager.setMasterViewID(seqID);
                    masterViewID.set(seqID);
                    LOG.log(Level.FINER, "MN: New MasterViewID = "+clusterViewManager.getMasterViewID());
                    clusterViewManager.addToView(newLocalView, true, cvEvent);
                } else {
                    LOG.log(Level.WARNING, "New View Received without corresponding ViewChangeEvent details");
                    //TODO according to the implementation MasterNode does not include VIEW_CHANGE_EVENT
                    //when it announces a Authortative master view
                    //throw new IOException("New View Received without corresponding ViewChangeEvent details");
                }
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
    boolean processMasterNodeResponse(final Message msg,
                                      final SystemAdvertisement source) throws IOException {
        MessageElement msgElement = msg.getMessageElement(NAMESPACE, MASTERNODERESPONSE);
        if ( msgElement == null )
            return false;
        LOG.log(Level.FINE, "Received a MasterNode Response from Name :" + source.getName());
        processRoute(msg);
        msgElement = msg.getMessageElement(NAMESPACE, AMASTERVIEW);
        if ( msgElement == null ) {
            clusterViewManager.setMaster( source, true );
            masterAssigned = true;
            return true;
        }
        final ArrayList<SystemAdvertisement> newLocalView = getObjectFromByteArray(msgElement);
        msgElement = msg.getMessageElement(NAMESPACE, VIEW_CHANGE_EVENT);
        if ( msgElement == null ) {
            clusterViewManager.setMaster( source, true );
            masterAssigned = true;
            return true;
        }
        long seqID = getLongFromMessage(msg, NAMESPACE, MASTERVIEWSEQ);
        LOG.log(Level.FINEST, "MasterNode:PMNR Received Master View with Seq Id="+seqID);
        if (seqID <= clusterViewManager.getMasterViewID()) {
            clusterViewManager.setMaster( source, true );
            masterAssigned = true;
            LOG.log(Level.FINER,
                    MessageFormat.format("Received an older clusterView sequence {0} of size :{1}" +
                            " Current sequence :{2} discarding out of sequence view",
                            seqID, newLocalView.size(), clusterViewManager.getMasterViewID()));
            return true;
        } else {
            LOG.log(Level.FINER,
                    MessageFormat.format("Received a VIEW_CHANGE_EVENT from : {0}, seqID of :{1}, size :{2}",
                            source.getName(), seqID, newLocalView.size()));
        }
        final ClusterViewEvent cvEvent = getObjectFromByteArray(msgElement);
        if (!newLocalView.contains(manager.getSystemAdvertisement())) {
            clusterViewManager.setMaster( source, true );
            masterAssigned = true;
            LOG.log(Level.FINER, "Received view does not contain self. Publishing self");
            sendSelfNodeAdvertisement(source.getID(), null);
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
     * Processes a cluster change event. This results in adding the new
     * members to the cluster view, and subsequenlty in application notification.
     *
     * @param msg    the Message
     * @param source the source node SystemAdvertisement
     * @return true if the message is a change event message
     * @throws IOException if an io error occurs
     */
    boolean processChangeEvent(final Message msg,
                               final SystemAdvertisement source) throws IOException {

        MessageElement msgElement = msg.getMessageElement(NAMESPACE, VIEW_CHANGE_EVENT);
        LOG.log(Level.FINER,"Inside processChangeEvent..." );
        if (msgElement != null) {
            final ClusterViewEvent cvEvent = getObjectFromByteArray(msgElement);
            msgElement = msg.getMessageElement(NAMESPACE, AMASTERVIEW);
            if (msgElement != null && cvEvent != null) {
                long seqID = getLongFromMessage(msg, NAMESPACE, MASTERVIEWSEQ);
                if (seqID <= clusterViewManager.getMasterViewID()) {
                    LOG.log(Level.FINER, MessageFormat.format("Received an older clusterView sequence {0}." +
                            " Current sequence :{1} discarding out of sequence view",
                            seqID, clusterViewManager.getMasterViewID()));
                    return true;
                }
                final ArrayList<SystemAdvertisement> newLocalView = getObjectFromByteArray(msgElement);
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
    boolean processMasterNodeQuery(final Message msg, final SystemAdvertisement adv) throws IOException {

        final MessageElement msgElement = msg.getMessageElement(NAMESPACE, MASTERQUERY);

        if (msgElement == null || adv == null) {
            return false;
        }
        processRoute(msg);
        if (isMaster() && masterAssigned) {
            LOG.log(Level.FINER, MessageFormat.format("Received a MasterNode Query from Name :{0} ID :{1}", adv.getName(), adv.getID()));
            final ClusterViewEvent cvEvent = new ClusterViewEvent(ADD_EVENT, adv);
            clusterViewManager.setMasterViewID(masterViewID.incrementAndGet());
            clusterViewManager.notifyListeners(cvEvent);
            sendNewView(null, cvEvent, createMasterResponse(false, localNodeID), true);
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
                manager.getClusterViewManager().remove(adv);
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
    boolean processNodeQuery(final Message msg, final SystemAdvertisement adv) throws IOException {
        final MessageElement msgElement = msg.getMessageElement(NAMESPACE, NODEQUERY);

        if (msgElement == null || adv == null) {
            return false;
        }
        processRoute(msg);
        LOG.log(Level.FINER, MessageFormat.format("Received a Node Query from Name :{0} ID :{1}", adv.getName(), adv.getID()));

        if (isMaster() && masterAssigned) {
            LOG.log(Level.FINER, MessageFormat.format("Received a Node Query from Name :{0} ID :{1}", adv.getName(), adv.getID()));
            final ClusterViewEvent cvEvent = new ClusterViewEvent(ADD_EVENT, adv);
            clusterViewManager.setMasterViewID(masterViewID.incrementAndGet());
            clusterViewManager.notifyListeners(cvEvent);
            sendNewView(null, cvEvent, createMasterResponse(false, localNodeID), true);
        } else {
            final Message response = createSelfNodeAdvertisement();
            final MessageElement el = new StringMessageElement(NODERESPONSE, "noderesponse", null);
            response.addMessageElement(NAMESPACE, el);
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
    boolean processNodeResponse(final Message msg, final SystemAdvertisement adv) throws IOException {
        final MessageElement msgElement = msg.getMessageElement(NAMESPACE, NODERESPONSE);

        if (msgElement == null || adv == null) {
            return false;
        }
        processRoute(msg);
        if (isMaster() && masterAssigned) {
            LOG.log(Level.FINER, MessageFormat.format("Received a Node Response from Name :{0} ID :{1}", adv.getName(), adv.getID()));
            final ClusterViewEvent cvEvent = new ClusterViewEvent(ADD_EVENT, adv);
            clusterViewManager.setMasterViewID(masterViewID.incrementAndGet());
            clusterViewManager.notifyListeners(cvEvent);
            sendNewView(null, cvEvent, createMasterResponse(false, localNodeID), true);
        }
        return true;
    }

    void processRoute(final Message msg) {
        LOG.log(Level.FINER,"Inside processRoute...");

        try {
            final MessageElement routeElement = msg.getMessageElement(NAMESPACE, ROUTEADV);
            if (routeElement != null) {
                XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(
                        routeElement.getMimeType(), routeElement.getStream());
                final RouteAdvertisement route = (RouteAdvertisement)
                        AdvertisementFactory.newAdvertisement(asDoc);
                manager.cacheRoute(route);
                if (routeControl != null) {
                    routeControl.addRoute(route);
                }
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.finer("cached following route from msg " + msg + " route=" + route);
                }
            }
        } catch (IOException io) {
            io.printStackTrace();
            LOG.log(Level.WARNING, io.getLocalizedMessage());
        }
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

        final MessageElement msgElement = msg.getMessageElement(NAMESPACE, CCNTL);
        if (msgElement == null) {
            return false;
        }
        LOG.log(Level.FINER, MessageFormat.format("Received a MasterNode Collision from Name :{0} ID :{1}", adv.getName(), adv.getID()));
        final SystemAdvertisement madv = manager.getSystemAdvertisement();
        LOG.log(Level.FINER, "Candidate Master :" + madv.getName());
        if (madv.getID().toString().compareTo(adv.getID().toString()) >= 0) {
            LOG.log(Level.FINER, "Affirming Master Node role");
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

    /**
     * {@inheritDoc}
     */
    public void pipeMsgEvent(final PipeMsgEvent event) {
        LOG.log(Level.FINEST, "Received a message inside  pipeMsgEvent");

        if (manager.isStopping()) {
            LOG.log(Level.FINE, "Since this Peer is Stopping, returning without processing incoming master node message. ");
            return;
        }

        if (isStarted()) {
            final Message msg;
            // grab the message from the event
            msg = event.getMessage();
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
                        clusterViewManager.add(adv);
                    } else if (discoveryInProgress) {
                        discoveryView.add(adv);
                    }
                }
                if (processMasterNodeQuery(msg, adv)) {
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
                if (processNodeQuery(msg, adv)) {
                    return;
                }
                if (processNodeResponse(msg, adv)) {
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                LOG.log(Level.WARNING, e.getLocalizedMessage());
            }
            LOG.log(Level.FINER, MessageFormat.format("ClusterViewManager contains {0} entries", clusterViewManager.getViewSize()));
        } else {
            LOG.log(Level.FINER, "Started : " + isStarted());
        }
    }

    private void announceMaster(SystemAdvertisement adv) {
        final Message msg = createMasterResponse(true, adv.getID());
        final ClusterViewEvent cvEvent = new ClusterViewEvent(ClusterViewEvents.MASTER_CHANGE_EVENT, adv);
        if(masterAssigned && isMaster()){
            LOG.log(Level.FINER, MessageFormat.format("Announcing Master Node designation Local view contains" +
                "                                      {0} entries", clusterViewManager.getViewSize()));
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
                LOG.log(Level.FINER, "Assuming Master Node designation ...");
                //broadcast we are the masternode if view size is more than one
                if (clusterViewManager.getViewSize() > 1) {
                    LOG.log(Level.FINER, "MasterNode: announcing MasterNode assumption ");
                    announceMaster(manager.getSystemAdvertisement());
                }
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
    private void send(final ID peerid, final String name, final Message msg) {
        try {
            if (peerid != null) {
                // Unicast datagram
                // create a op pipe to the destination peer
                LOG.log(Level.FINER, "Unicasting Message to :" + name + "ID=" + peerid);
                OutputPipe output = pipeCache.get(peerid);
                if (output == null) {
                    RouteAdvertisement route = manager.getCachedRoute((PeerID) peerid);
                    if (route != null) {
                       output = new BlockingWireOutputPipe(manager.getNetPeerGroup(), pipeAdv, (PeerID) peerid, route);
                    }
                    if (output == null) {
                        // Unicast datagram
                        // create a op pipe to the destination peer
                        output = pipeService.createOutputPipe(pipeAdv, Collections.singleton(peerid), 1);
                    }
                    pipeCache.putIfAbsent(peerid, output);
                } else if (output.isClosed()) {
                    output = pipeService.createOutputPipe(pipeAdv, Collections.singleton(peerid), 1);
                    pipeCache.put(peerid, output);
                }
                final boolean sent = output.send(msg);
                if (!sent) {
                    LOG.log(Level.WARNING, "OutputPipe.send unexpectedly returned false sending messge " + msg +
                            " to instance " + name);
                }
            } else {
                // multicast
                LOG.log(Level.FINER, "Broadcasting Message");
                final boolean sent = outputPipe.send(msg);
                if (!sent) {
                    LOG.log(Level.WARNING, "OutputPipe.send unexpectedly returned false sending messge " + msg +
                            " to instance " + name);
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
    void sendNewView(final ID toID,
                     final ClusterViewEvent event,
                     final Message msg,
                     final boolean includeView) {
        if (includeView) {
            addAuthoritativeView(msg);
        }

        final ByteArrayMessageElement cvEvent =
                new ByteArrayMessageElement(VIEW_CHANGE_EVENT,
                        MimeMediaType.AOS, JxtaUtil.
                        createByteArrayFromObject(event),
                        null);

        LOG.log(Level.FINER, MessageFormat.format("Created a view element of size {0}bytes", cvEvent.getByteLength()));
        msg.addMessageElement(NAMESPACE, cvEvent);
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
        view = clusterViewManager.getLocalView().getView();
        LOG.log(Level.FINER, "MasterNode: Adding Authoritative View of size "+view.size()+ "  to view message");
        final ByteArrayMessageElement bame =
                new ByteArrayMessageElement(AMASTERVIEW,
                        MimeMediaType.AOS,
                        JxtaUtil.createByteArrayFromObject(view),
                        null);
        msg.addMessageElement(NAMESPACE, bame);
        LOG.log(Level.FINER, MessageFormat.format("Created an Authoritative view element of size {0}bytes", bame.getByteLength()));
        addLongToMessage(msg, NAMESPACE, MASTERVIEWSEQ, masterViewID.longValue());
    }

    /**
     * Stops this service
     */
    synchronized void stop() {
        LOG.log(Level.FINER, "Stopping MasterNode");
        outputPipe.close();
        inputPipe.close();
        pipeCache.clear();
        discoveryView.clear();
        thread = null;
        masterAssigned = false;
        started = false;
        stop = true;
        discoveryInProgress = false;
        synchronized (discoveryLock){
            discoveryLock.notifyAll();
        }

    }

    /**
     * Starts this service. Creates the communication channels, and the MasterNode discovery thread.
     */
    synchronized void start() {
        LOG.log(Level.FINER, "Starting MasterNode");
        this.clusterViewManager = manager.getClusterViewManager();

        try {
            //better set the started flag before the pipe is open
            //in case messages arrive
            started = true;
            inputPipe = pipeService.createInputPipe(pipeAdv, this);
        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, "Failed to create service input pipe" + ioe);
        }
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
            //increment the view seqID
            clusterViewManager.setMasterViewID(masterViewID.incrementAndGet());
            Message msg = createSelfNodeAdvertisement();
            sendNewView(null, event, msg, true);
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
        message.addMessageElement(nameSpace, new StringMessageElement(elemName, Long.toString(data), null));
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
        String seqStr = message.getMessageElement(nameSpace, elemName).toString();
        if (seqStr != null) {
            return Long.parseLong(message.getMessageElement(nameSpace, elemName).toString());
        } else {
            return -1;
        }
    }
    RouteControl getRouteControl() {
        if (routeControl == null) {
        routeControl = (RouteControl) endpointRouter.transportControl(EndpointRouter.GET_ROUTE_CONTROL, null);
        }
        return routeControl;
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

            //broadcast we are the masternode if view size is more than one
            if (clusterViewManager.getViewSize() > 1) {
                LOG.log(Level.FINER, "MasterNode: becomeMaster () : announcing MasterNode assumption ");
                announceMaster(manager.getSystemAdvertisement());
            }
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
        clusterViewManager.setMasterViewID(masterViewID.incrementAndGet());
        sendNewView(null, cvEvent, createSelfNodeAdvertisement(), true);
        return cvEvent;
    }

    boolean isDiscoveryInProgress() {
        return discoveryInProgress;
    }
}

