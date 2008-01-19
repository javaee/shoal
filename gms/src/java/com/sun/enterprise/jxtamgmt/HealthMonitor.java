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

import net.jxta.document.*;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.id.ID;
import net.jxta.peer.PeerID;
import net.jxta.pipe.*;
import net.jxta.protocol.PipeAdvertisement;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HealthMonitor utilizes MasterNode to determine self designation. All nodes
 * cache other node's states, and can act as a master node at any given
 * point in time.  The intention behind the designation is that no node other than
 * the master node should determine collective state and communicate it to
 * group members.
 * <p/>
 * TODO: Convert the InDoubt Peer Determination and Failure Verification into
 * Callable FutureTask using java.util.concurrent
 */
public class HealthMonitor implements PipeMsgListener, Runnable {
    private static final Logger LOG = JxtaUtil.getLogger(HealthMonitor.class.getName());
    // Default health reporting period
    private long timeout = 10 * 1000L;
    private long verifyTimeout = 10 * 1000L;
    private int maxMissedBeats = 3;
    private final String threadLock = new String("threadLock");
    private final ConcurrentHashMap<PeerID, HealthMessage.Entry> cache = new ConcurrentHashMap<PeerID, HealthMessage.Entry>();
    private MasterNode masterNode = null;
    private ClusterManager manager = null;
    private final PeerID localPeerID;
    InputPipe inputPipe = null;
    private OutputPipe outputPipe = null;
    private PipeAdvertisement pipeAdv = null;
    private final PipeService pipeService;
    private volatile boolean started = false;
    private volatile boolean stop = false;

    private Thread healthMonitorThread = null;
    private Thread failureDetectorThread = null;

    private InDoubtPeerDetector inDoubtPeerDetector;
    private final String[] states = {"starting",
            "started",
            "alive",
            "clusterstopping",
            "peerstopping",
            "stopped",
            "dead",
            "indoubt",
            "unknown",
            "ready"};
    private static final short STARTING = 0;    
    private static final short ALIVE = 2;
    private static final short CLUSTERSTOPPING = 3;
    private static final short PEERSTOPPING = 4;
    private static final short STOPPED = 5;
    private static final short DEAD = 6;
    private static final short INDOUBT = 7;
    private static final short UNKNOWN = 8;
    private static final short READY = 9;
    private static final String HEALTHM = "HM";
    private static final String NAMESPACE = "HEALTH";
    private static final String cacheLock = "cacheLock";
    private static final String verifierLock = "verifierLock";

    private Message aliveMsg = null;
    private transient Map<ID, OutputPipe> pipeCache = new Hashtable<ID, OutputPipe>();

    //counter for keeping track of the seq ids of the health messages
    AtomicLong hmSeqID = new AtomicLong();

    //private ShutdownHook shutdownHook;

    /**
     * Constructor for the HealthMonitor object
     *
     * @param manager        the ClusterManager
     * @param maxMissedBeats Maximum retries before failure
     * @param verifyTimeout  timeout in milliseconds that the health monitor
     *                       waits before finalizing that the in doubt peer is dead.
     * @param timeout        in milliseconds that the health monitor waits before
     *                       retrying an indoubt peer's availability.
     */
    public HealthMonitor(final ClusterManager manager, final long timeout,
                         final int maxMissedBeats, final long verifyTimeout) {
        this.timeout = timeout;
        this.maxMissedBeats = maxMissedBeats;
        this.verifyTimeout = verifyTimeout;
        this.manager = manager;
        this.masterNode = manager.getMasterNode();
        this.localPeerID = manager.getNetPeerGroup().getPeerID();
        this.pipeService = manager.getNetPeerGroup().getPipeService();
        //this.shutdownHook = new ShutdownHook();
        //Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * Creates a Message containing this node's state
     *
     * @param state member state
     * @return a Message containing this node's state
     */
    private Message createHealthMessage(final short state) {
        Message msg = createMessage(state, HEALTHM, manager.getSystemAdvertisement());
        masterNode.addRoute(msg);
        return msg;
    }

    private Message createMessage(final short state, final String tag,
                                  final SystemAdvertisement adv) {
        final Message msg = new Message();
        final HealthMessage hm = new HealthMessage();
        hm.setSrcID(localPeerID);
        final HealthMessage.Entry entry = new HealthMessage.Entry(adv, states[state], hmSeqID.incrementAndGet());
        hm.add(entry);
        msg.addMessageElement(NAMESPACE, new TextDocumentMessageElement(tag,
                (XMLDocument) hm.getDocument(MimeMediaType.XMLUTF8), null));
        //add this state to the local health cache. 
        cache.put(entry.id, entry);
        return msg;
    }

    private Message getAliveMessage() {
        if (aliveMsg == null) {
            aliveMsg = createHealthMessage(ALIVE);
        }
        return aliveMsg;
    }

    /**
     * Given a pipeid it returns a HealthMonitor pipe advertisement of propagate type
     *
     * @return a HealthMonitor pipe advertisement of propagate type
     */
    private PipeAdvertisement createPipeAdv() {
        final PipeAdvertisement pipeAdv;
        // create the pipe advertisement, to be used in creating the pipe
        pipeAdv = (PipeAdvertisement)
                AdvertisementFactory.newAdvertisement(
                        PipeAdvertisement.getAdvertisementType());
        pipeAdv.setPipeID(manager.getNetworkManager().getHealthPipeID());
        pipeAdv.setType(PipeService.PropagateType);
        return pipeAdv;
    }

    /**
     * {@inheritDoc}
     */
    public void pipeMsgEvent(final PipeMsgEvent event) {
        //if this peer is stopping, then stop processing incoming health messages.
        if (manager.isStopping()) {
            return;
        }
        if (started) {
            final Message msg;
            MessageElement msgElement;
            try {
                // grab the message from the event
                msg = event.getMessage();
                if (msg != null) {
                    final Message.ElementIterator iter = msg.getMessageElements();
                    while (iter.hasNext()) {
                        msgElement = iter.next();
                        if (msgElement != null && msgElement.getElementName().equals(HEALTHM)) {
                            HealthMessage hm = getHealthMessage(msgElement);
                            if (!hm.getSrcID().equals(localPeerID)) {
                                masterNode.processRoute(msg);
                            }
			                process(hm);
			            }
		            }
		        }
            } catch (IOException ex) {
                ex.printStackTrace();
                LOG.log(Level.WARNING, "HealthMonitor:Caught IOException : " + ex.getLocalizedMessage());
            } catch (Throwable e) {
                e.printStackTrace();
                LOG.log(Level.WARNING, e.getLocalizedMessage());
            }
        }
    }

    /**
     * process a health message received on the wire into cache
     *
     * @param hm Health message to process
     */
    private void process(final HealthMessage hm) {

        //discard loopback messages
        if (!hm.getSrcID().equals(localPeerID)) {
            //current assumption is that there is only 1 entry
            //per health message
            for (HealthMessage.Entry entry : hm.getEntries()) {

                LOG.log(Level.FINEST, "Processing Health Message " + entry.getSeqID() + " for entry " + entry.adv.getName());
                LOG.log(Level.FINEST, "Getting the cachedEntry " + entry.id);

                HealthMessage.Entry cachedEntry = cache.get(entry.id);
                if (cachedEntry != null) {                 
                    LOG.log(Level.FINEST, "cachedEntry is not null");
                    if (entry.getSeqID() <= cachedEntry.getSeqID()) {
                        LOG.log(Level.FINER, MessageFormat.format("Received an older health message sequence {0}." +
                                " Current sequence id is {1}. ",
                                entry.getSeqID(), cachedEntry.getSeqID()));
                        if (entry.state.equals(states[CLUSTERSTOPPING]) || entry.state.equals(states[PEERSTOPPING])) {
                            //dont discard the message
                            //and don't alter the state if the cachedEntry's state is stopped
                            LOG.log(Level.FINER, "Received out of order health message " +
                                    "with clusterstopping state." +
                                    " Calling handleStopEvent() to handle shutdown state.");
                            handleStopEvent(entry);
                        } else if (entry.state.equals(states[READY])) {
                            LOG.finer("Received out of order health message with Joined and Ready state. " +
                                    "Calling handleReadyEvent() for handling the peer's ready state");
                            handleReadyEvent(entry);
                        } else {
                            LOG.log(Level.FINER, "Discarding out of sequence health message");
                        }
                        return;
                    }
                }
                LOG.log(Level.FINEST, "Putting into cache " + entry.adv.getName() + " state = " + entry.state + " peerid = " + entry.id);
                cache.put(entry.id, entry);
                if (!manager.getClusterViewManager().containsKey(entry.id)) {
                    try {
                        masterNode.probeNode(entry);
                    } catch (IOException e) {
                        e.printStackTrace();
                        LOG.warning("IOException occured while sending probeNode() Message in HealthMonitor:"+e.getLocalizedMessage());
                    }
                }
                if (entry.state.equals(states[READY])) {
                    handleReadyEvent( entry );
                }
                if (entry.state.equals(states[PEERSTOPPING]) || entry.state.equals(states[CLUSTERSTOPPING])) {
                    handleStopEvent(entry);
                }
                if (entry.state.equals(states[INDOUBT]) || entry.state.equals(states[DEAD])) {
                    if (entry.id.equals(localPeerID)) {
                        reportMyState(ALIVE, hm.getSrcID());
                    } else {
                        if (entry.state.equals(states[INDOUBT])) {
                            LOG.log(Level.FINE, "Peer " + entry.id.toString() + " is suspected failed. Its state is " + entry.state);
                            notifyLocalListeners(entry.state, entry.adv);
                        }
                        if (entry.state.equals(states[DEAD])) {
                            LOG.log(Level.FINE, "Peer " + entry.id.toString() + " has failed. Its state is " + entry.state);
                        }
                    }
                } 
            }
        }
    }

    private void handleReadyEvent(final HealthMessage.Entry entry) {
        cache.put(entry.id, entry);
        if(entry.id.equals(masterNode.getMasterNodeID())){
            //if this is a ready state sent out by master, take no action here as master would send out a view to the group.
            return;
        }
        //if this is a ready state sent by a non-master, and if I am the assigned master, send out a new view and notify local listeners.
        if(masterNode.isMaster() && masterNode.isMasterAssigned()){
            LOG.log(Level.FINEST, MessageFormat.format("Handling Ready Event for peer :{0}", entry.adv.getName()));
            final ClusterViewEvent cvEvent = masterNode.sendReadyEventView(entry.adv);
            manager.getClusterViewManager().notifyListeners(cvEvent);
        }
    }

    private void handleStopEvent(final HealthMessage.Entry entry) {
        LOG.log(Level.FINEST, MessageFormat.format("Handling Stop Event for peer :{0}", entry.adv.getName()));
        short stateByte = PEERSTOPPING;
        if (entry.state.equals(states[CLUSTERSTOPPING])) {
            stateByte = CLUSTERSTOPPING;
        }
        if (entry.adv.getID().equals(masterNode.getMasterNodeID())) {
            //if masternode is resigning, remove master node from view and start discovery
            LOG.log(Level.FINER, MessageFormat.format("Removing master node {0} from view as it has stopped.", entry.adv.getName()));
            removeMasterAdv(entry, stateByte);
            masterNode.resetMaster();
            masterNode.appointMasterNode();
        } else if (masterNode.isMaster() && masterNode.isMasterAssigned()) {
            removeMasterAdv(entry, stateByte);
            LOG.log(Level.FINE, "Announcing Peer Stop Event of " + entry.adv.getName() + " to group ...");
            final ClusterViewEvent cvEvent;
            if (entry.state.equals(states[CLUSTERSTOPPING])) {
                cvEvent = new ClusterViewEvent(ClusterViewEvents.CLUSTER_STOP_EVENT, entry.adv);
            } else {
                cvEvent = new ClusterViewEvent(ClusterViewEvents.PEER_STOP_EVENT, entry.adv);
            }
            masterNode.viewChanged(cvEvent);
        }
    }

    private Map<PeerID, HealthMessage.Entry> getCacheCopy() {
        ConcurrentHashMap<PeerID, HealthMessage.Entry> clone = new ConcurrentHashMap<PeerID, HealthMessage.Entry>();
        clone.putAll(cache);
        return clone;
    }

    /**
     * Reports on the wire the specified state
     *
     * @param state specified state can be
     *              ALIVE|SLEEP|HIBERNATING|SHUTDOWN|DEAD
     * @param id    destination node ID, if null broadcast to group
     */
    private void reportMyState(final short state, final PeerID id) {
        LOG.log(Level.FINER, MessageFormat.format("Sending state {0} to {1}", states[state], id));
        if (state == ALIVE) {
            send(id, getAliveMessage());
        } else {
            send(id, createHealthMessage(state));
        }
    }

    private void reportOtherPeerState(final short state, final SystemAdvertisement adv) {
        final Message msg = createMessage(state, HEALTHM, adv);
        LOG.log(Level.FINEST, MessageFormat.format("Reporting {0} health state as {1}", adv.getName(), states[state]));
        send(null, msg);
    }

    /**
     * Main processing method for the HealthMonitor object
     */
    public void run() {
        //At start, the starting state is reported. But reporting ready state is dependent on
        //the parent application making the call that it is in the ready state, which is not
        //necessarily deterministic. Hence we move on from starting state to reporting
        //this peer's alive state. For join notification event, the liveness state of a peer
        // would be one of starting or ready or alive (other than peerstopping, etc). 
        reportMyState(STARTING, null);
        //System.out.println("Running HealthMonitor Thread at interval :"+actualto);
        while (!stop) {
            try {
                synchronized (threadLock) {
                    threadLock.wait(timeout);
                }
                if (!stop) {
                    reportMyState(ALIVE, null);
                }
            } catch (InterruptedException e) {
                stop = true;
                LOG.log(Level.FINEST, "Shoal Health Monitor Thread Stopping as the thread is now interrupted...:"+e.getLocalizedMessage());
                break;
            } catch (Throwable all) {
                LOG.log(Level.WARNING, "Uncaught Throwable in healthMonitorThread " + Thread.currentThread().getName() + ":" + all);
            }
        }
    }

    /**
     * Send a message to a specific node. In case the peerId is null the
     * message is multicast to the group
     *
     * @param peerid Peer ID to send massage to
     * @param msg    the message to send
     */
    private void send(final PeerID peerid, final Message msg) {
        try {
            if (peerid != null) {
                // Unicast datagram
                // create a op pipe to the destination peer
                LOG.log(Level.FINE, "Unicasting Message to :" + peerid.toString());
                OutputPipe output;
                if (!pipeCache.containsKey(peerid)) {
                    // Unicast datagram
                    // create a op pipe to the destination peer
                    output = pipeService.createOutputPipe(pipeAdv, Collections.singleton(peerid), 1);
                    pipeCache.put(peerid, output);
                } else {
                    output = pipeCache.get(peerid);
                    if (output.isClosed()) {
                        output = pipeService.createOutputPipe(pipeAdv, Collections.singleton(peerid), 1);
                        pipeCache.put(peerid, output);
                    }
                }
                output.send(msg);
            } else {
                outputPipe.send(msg);
            }
        } catch (IOException io) {
            LOG.log(Level.WARNING, "Failed to send message", io);
        }
    }

    /**
     * Creates both input/output pipes, and starts monitoring service
     */
    void start() {

        if (!started) {
            LOG.log(Level.FINE, "Starting HealthMonitor");
            try {
                // create the pipe advertisement, to be used in creating the pipe
                pipeAdv = createPipeAdv();
                // create input
                inputPipe = pipeService.createInputPipe(pipeAdv, this);
                // create output
                outputPipe = pipeService.createOutputPipe(pipeAdv, 1);

                this.healthMonitorThread = new Thread(this, "HealthMonitor");
                healthMonitorThread.start();
                inDoubtPeerDetector = new InDoubtPeerDetector();
                inDoubtPeerDetector.start();
                started = true;
            } catch (IOException ioe) {
                LOG.log(Level.WARNING, "Failed to create health monitoring pipe advertisement :" + ioe);
            }
        }
    }

    /**
     * Announces Stop event to all members indicating that this peer
     * will gracefully exit the group.
     * TODO: Make this a synchronous call or a simulated synchronous call so that responses from majority of members can be collected before returning from this method
     *
     * @param isClusterShutdown boolean value indicating whether this
     *                          announcement is in the context of a clusterwide shutdown or a shutdown of
     *                          this peer only.
     */
    void announceStop(final boolean isClusterShutdown) {
        //System.out.println("Announcing  Shutdown");
        LOG.log(Level.FINE, MessageFormat.format("Announcing stop event to group with clusterShutdown set to {0}", isClusterShutdown));
        if (isClusterShutdown) {
            reportMyState(CLUSTERSTOPPING, null);
        } else {
            reportMyState(PEERSTOPPING, null);
        }

    }

    /**
     * Stops this service
     * @param isClusterShutdown  true if the cluster is shutting down
     */
    void stop(boolean isClusterShutdown) {
        stop = true;
        started = false;
        announceStop(isClusterShutdown);
        reportMyState(STOPPED, null);
        LOG.log(Level.FINE, "Stopping HealthMonitor");
        final Thread tmpThread = healthMonitorThread;
        healthMonitorThread = null;
        if (tmpThread != null) {
            tmpThread.interrupt();
        }
        inDoubtPeerDetector.stop();
        inputPipe.close();
        outputPipe.close();
        pipeCache.clear();
    }

    private HealthMessage getHealthMessage(final MessageElement msgElement) throws IOException {
        final HealthMessage hm;
        hm = new HealthMessage(getStructuredDocument(msgElement), hmSeqID.incrementAndGet());
        return hm;
    }

    private static StructuredTextDocument getStructuredDocument(
            final MessageElement msgElement) throws IOException {
        return (StructuredTextDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8,
                msgElement.getStream());
    }


    private void notifyLocalListeners(final String state, final SystemAdvertisement adv) {
        if (state.equals(states[INDOUBT])) {
            manager.getClusterViewManager().setInDoubtPeerState(adv);
        } else if (state.equals(states[ALIVE])) {
            manager.getClusterViewManager().setPeerNoLongerInDoubtState(adv);
        } else if (state.equals(states[CLUSTERSTOPPING])) {
            manager.getClusterViewManager().setClusterStoppingState(adv);
        } else if (state.equals(states[PEERSTOPPING])) {
            manager.getClusterViewManager().setPeerStoppingState(adv);
        }  else if (state.equals(states[READY])) {
            manager.getClusterViewManager().setPeerReadyState(adv);
        }
    }

    public String getState(final ID peerID) {
        HealthMessage.Entry entry;
        final String state;
        entry = cache.get((PeerID) peerID);
        if (entry != null) {
            state = entry.state;
        }
        else {
            if(((PeerID)peerID).equals(localPeerID)){
                if(!started){
                    state = states[STARTING];
                }
                else {
                    state = states[ALIVE];
                }
            } else {
                if(manager.getClusterViewManager().containsKey(peerID)){
                    state = states[STARTING];//we assume that the peer is in starting state hence its state is not yet known in this peer
                }
                else {
                    state = states[DEAD];
                }
            }
        }
        return state;
    }

    public void reportJoinedAndReadyState() {
        //if I am the master send out a new view with ready event but wait for master discovery to be over.Also send out a health message
        //with state set to READY. Recipients of health message would only update their health state cache.
        //Master Node discovery completion and assignment is necessary for the ready state of a peer to be sent out as an event with a view.
        //if I am not the master, report my state to the group as READY. MasterNode will send out a
        //JOINED_AND_READY_EVENT view to the group on receipt of this health state message.

        if(masterNode.isDiscoveryInProgress()){
            synchronized (masterNode.discoveryLock){
                try {
                    masterNode.discoveryLock.wait();
                    LOG.log(Level.FINEST, "reportJoinedAndReadyState() waiting for masternode discovery to finish...");
                } catch (InterruptedException e) {
                    LOG.log(Level.FINEST, "MasterNode's DiscoveryLock Thread is interrupted "+e);
                }
            }
        }
        if(masterNode.isMaster() && masterNode.isMasterAssigned()){
            LOG.log(Level.FINEST,  "Sending Ready Event View for "+ manager.getSystemAdvertisement().getName());
            ClusterViewEvent cvEvent = masterNode.sendReadyEventView(manager.getSystemAdvertisement());
            LOG.log(Level.FINEST, MessageFormat.format("Notifying Local listeners about " +
                    "Joined and Ready Event View for peer :{0}", manager.getSystemAdvertisement().getName()));
            manager.getClusterViewManager().notifyListeners(cvEvent);
        }
        LOG.log(Level.FINEST, "Calling reportMyState() with READY...");
        reportMyState(READY, null);
    }

    /**
     * Detects suspected failures and then delegates final decision to
     * FailureVerifier
     *
     * @author : Shreedhar Ganapathy
     */
    private class InDoubtPeerDetector implements Runnable {

        void start() {
            failureDetectorThread = new Thread(this, "InDoubtPeerDetector Thread");
            LOG.log(Level.FINE, "Starting InDoubtPeerDetector Thread");
            failureDetectorThread.start();
            FailureVerifier fverifier = new FailureVerifier();
            final Thread fvThread = new Thread(fverifier, "FailureVerifier Thread");
            LOG.log(Level.FINE, "Starting FailureVerifier Thread");
            fvThread.start();
        }

        void stop() {
            final Thread tmpThread = failureDetectorThread;
            failureDetectorThread = null;
            if (tmpThread != null) {
                tmpThread.interrupt();
            }
            synchronized (verifierLock) {
                verifierLock.notify();
            }
        }

        public void run() {
            while (!stop) {
                synchronized (cacheLock) {
                    try {
                        //System.out.println("InDoubtPeerDetector failureDetectorThread waiting for :"+timeout);
                        //wait for specified timeout or until woken up
                        cacheLock.wait(timeout);
                        //LOG.log(Level.FINEST, "Analyzing cache for health...");
                        //get the copy of the states cache
                        if (!manager.isStopping()) {
                            processCacheUpdate();
                        }
                    } catch (InterruptedException ex) {
                        LOG.log(Level.FINEST, "InDoubtPeerDetector Thread stopping as it is now interrupted :"+ex.getLocalizedMessage());
                        break;
                    }
                    catch(Throwable all){
                        LOG.warning("Uncaught Throwable in failureDetectorThread " + Thread.currentThread().getName() + ":" + all);
                    }
                }
            }
        }

        /**
         * computes the number of heart beats missed based on an entry's timestamp
         *
         * @param entry the Health entry
         * @return the number heart beats missed
         */
        int computeMissedBeat(HealthMessage.Entry entry) {
            return (int) ((System.currentTimeMillis() - entry.timestamp) / timeout);
        }

        private void processCacheUpdate() {
            final Map<PeerID, HealthMessage.Entry> cacheCopy = getCacheCopy();
            //for each peer id
            for (HealthMessage.Entry entry : cacheCopy.values()) {
                if (entry.state.equals(states[ALIVE])) {
                    //if there is a record, then get the number of
                    //retries performed in an earlier iteration
                    try {
                        determineInDoubtPeers(entry);
                    } catch (NumberFormatException nfe) {
                        LOG.log(Level.WARNING, "Exception occurred during time stamp conversion : " + nfe.getLocalizedMessage());
                    }
                }
            }
        }

        private void determineInDoubtPeers(final HealthMessage.Entry entry) {

            //if current time exceeds the last state update timestamp from this peer id, by more than the
            //the specified max timeout
            if (!stop) {
                if (computeMissedBeat(entry) >= maxMissedBeats && !isConnected(entry.id)) {
                    LOG.log(Level.FINEST, "timeDiff > maxTime");
                    if (canProcessInDoubt(entry)) {
                        LOG.log(Level.FINER, "Designating InDoubtState");
                        designateInDoubtState(entry);
                        //delegate verification to Failure Verifier
                        LOG.log(Level.FINER, "Notifying FailureVerifier for "+entry.adv.getName());
                        synchronized (verifierLock) {
                            verifierLock.notify();
                            LOG.log(Level.FINER, "Done Notifying FailureVerifier for "+entry.adv.getName());
                        }
                    }
                } else {
                    //dont suspect self
                    if (!entry.id.equals(localPeerID)) {
                        if (canProcessInDoubt(entry)) {
                            LOG.log(Level.FINE, MessageFormat.format("For instance = {0}; last recorded heart-beat = {1}ms ago, heart-beat # {2} out of a max of {3}",
                                    entry.adv.getName(), (System.currentTimeMillis() - entry.timestamp), computeMissedBeat(entry), maxMissedBeats));
                        }
                    }
                }
            }
        }
        private boolean canProcessInDoubt(final HealthMessage.Entry entry) {
            boolean canProcessIndoubt = false;
            if (masterNode.getMasterNodeID().equals(entry.id)) {
                canProcessIndoubt = true;
            } else if (masterNode.isMaster()) {
                canProcessIndoubt = true;
            }
            return canProcessIndoubt;
        }

        private void designateInDoubtState(final HealthMessage.Entry entry) {

            entry.state = states[INDOUBT];
            cache.put(entry.id, entry);
            if (masterNode.isMaster()) {
                //do this only when masternode is not suspect.
                // When masternode is suspect, all members update their states
                // anyways so no need to report
                //Send group message announcing InDoubt State
                LOG.log(Level.FINE, "Sending INDOUBT state message about node ID: " + entry.id + " to the cluster...");
                reportOtherPeerState(INDOUBT, entry.adv);
            }
            LOG.log(Level.FINEST, "Notifying Local Listeners of designated indoubt state for "+entry.adv.getName());
            notifyLocalListeners(entry.state, entry.adv);
        }
    }

    private class FailureVerifier implements Runnable {
        private final long buffer = 500;

        public void run() {
            try {
                synchronized (verifierLock) {
                    while (!stop) {
                        LOG.log(Level.FINER, "FV: Entering verifierLock Wait....");
                        verifierLock.wait();
                        LOG.log(Level.FINER, "FV: Woken up from verifierLock Wait by a notify ....");
                        if (!stop) {
                            LOG.log(Level.FINER, "FV: Calling verify() ....");
                            verify();
                            LOG.log(Level.FINER, "FV: Done verifying ....");
                        }
                    }
                }
            } catch (InterruptedException ex) {
                LOG.log(Level.FINEST, MessageFormat.format("failure Verifier Thread stopping as it is now interrupted: {0}", ex.getLocalizedMessage()));
            }
        }

        void verify() throws InterruptedException {
            //wait for the specified timeout for verification
            Thread.sleep(verifyTimeout + buffer);
            HealthMessage.Entry entry;
            for (HealthMessage.Entry entry1 : getCacheCopy().values()) {
                entry = entry1;
                LOG.log(Level.FINER, "FV: Verifying state of "+entry.adv.getName()+" state = "+entry.state);
                if(entry.state.equals(states[INDOUBT]) && !isConnected(entry.id)){
                    LOG.log(Level.FINER, "FV: Assigning and reporting failure ....");
                    assignAndReportFailure(entry);
                }
            }
        }
    }

    private void assignAndReportFailure(final HealthMessage.Entry entry) {
        if (entry != null) {
            entry.state = states[DEAD];
            cache.put(entry.id, entry);
            if (masterNode.isMaster()) {
                LOG.log(Level.FINE, MessageFormat.format("Reporting Failed Node {0}", entry.id.toString()));
                reportOtherPeerState(DEAD, entry.adv);
            }
            final boolean masterFailed = (masterNode.getMasterNodeID()).equals(entry.id);
            if (masterNode.isMaster() && masterNode.isMasterAssigned()) {
                LOG.log(Level.FINE, MessageFormat.format("Removing System Advertisement :{0} for name {1}", entry.id.toString(), entry.adv.getName()));
                removeMasterAdv(entry, DEAD);
                LOG.log(Level.FINE, MessageFormat.format("Announcing Failure Event of {0} for name {1}...", entry.id, entry.adv.getName()));
                final ClusterViewEvent cvEvent = new ClusterViewEvent(ClusterViewEvents.FAILURE_EVENT, entry.adv);
                masterNode.viewChanged(cvEvent);
            } else if (masterFailed) {
                //remove the failed node
                LOG.log(Level.FINE, MessageFormat.format("Master Failed. Removing System Advertisement :{0} for master named {1}", entry.id.toString(), entry.adv.getName()));
                removeMasterAdv(entry, DEAD);
                //manager.getClusterViewManager().remove(entry.id);
                masterNode.resetMaster();
                masterNode.appointMasterNode();
            }
        }
    }

    private void removeMasterAdv(HealthMessage.Entry entry, short state) {
        manager.getClusterViewManager().remove(entry.adv);
        if (entry.adv != null) {
            switch (state) {
                case DEAD:
                    LOG.log(Level.FINER, "FV: Notifying local listeners of Failure of "+entry.adv.getName());
                    manager.getClusterViewManager().notifyListeners(
                            new ClusterViewEvent(ClusterViewEvents.FAILURE_EVENT, entry.adv));
                    break;
                case PEERSTOPPING:
                    LOG.log(Level.FINER, "FV: Notifying local listeners of Shutdown of "+entry.adv.getName());
                    manager.getClusterViewManager().notifyListeners(
                            new ClusterViewEvent(ClusterViewEvents.PEER_STOP_EVENT, entry.adv));
                    break;
                case CLUSTERSTOPPING:
		    LOG.log(Level.FINER, "FV: Notifying local listeners of Cluster_Stopping of "+entry.adv.getName());
                    manager.getClusterViewManager().notifyListeners(
                            new ClusterViewEvent(ClusterViewEvents.CLUSTER_STOP_EVENT, entry.adv));
                    break;
                default:
                    LOG.log(Level.FINEST, MessageFormat.format("Invalid State for removing adv from view {0}", state));
            }
        } else {
            LOG.log(Level.WARNING, states[state] + " peer: " + entry.id + " does not exist in local ClusterView");
        }
    }

    /**
     * Determines whether a connection to a specific node exists, or one can be created
     *
     * @param pid Node ID
     * @return true, if a connection already exists, or a new was sucessfully created
     */
    public boolean isConnected(PeerID pid) {
        //if System property for InetAddress.isReachable() is set, then check for the following:
        //if InetAddress.isReachable() is true, then check for isConnected()
        //if InetAddress.isReachable() is false, then simply return false
        return  masterNode.getRouteControl().isConnected(pid);
    }
/*
private void shutdown() {
}
private class ShutdownHook extends Thread {
    public void run() {
        shutdown();
    }
}
*/

}
