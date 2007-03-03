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
import java.util.*;
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
    private long timeout = 60 * 60 * 1000L;
    private long verifyTimeout = 10 * 1000L;
    private long masterDelta = 1000L;
    private int maxRetries = 3;
    private List<PeerID> indoubtPeerList;
    private final String indoubtListLock = new String("IndoubtListLock");
    private final String threadLock = new String("threadLock");
    private final Map<PeerID, HealthMessage.Entry> cache = new HashMap<PeerID, HealthMessage.Entry>();
    private InputPipe inputPipe = null;
    private MasterNode masterNode = null;
    private ClusterManager manager = null;
    private PeerID myID;
    private OutputPipe outputPipe = null;
    private PipeAdvertisement pipeAdv = null;
    private PipeService pipeService;
    private boolean started = false;
    private boolean stop = false;

    private Thread thread = null;
    private Thread fdThread = null;

    private InDoubtPeerDetector inDoubtPeerDetector;
    private String[] states = {"starting",
                               "started",
                               "alive",
                               "clusterstopping",
                               "peerstopping",
                               "stopped",
                               "dead",
                               "indoubt",
                               "unknown"};
    private static final short ALIVE = 2;
    private static final short CLUSTERSTOPPING = 3;
    private static final short PEERSTOPPING = 4;
    private static final short STOPPED = 5;
    private static final short DEAD = 6;
    private static final short INDOUBT = 7;
    private static final short UNK = 8;
    private static final String HEALTHM = "HM";
    private static final String NAMESPACE = "HEALTH";
    private static final String cacheLock = "cacheLock";
    private static final String verifierLock = "verifierLock";

    private Message aliveMsg = null;
    //private ShutdownHook shutdownHook;

    /**
     * Constructor for the HealthMonitor object
     *
     * @param manager       the ClusterManager
     * @param maxRetries    Maximum retries before failure
     * @param verifyTimeout timeout in milliseconds that the health monitor
     *                      waits before finalizing that the in doubt peer is dead.
     * @param timeout       in milliseconds that the health monitor waits before
     *                      retrying an indoubt peer's availability.
     */
    public HealthMonitor(final ClusterManager manager, final long timeout,
                         final int maxRetries, final long verifyTimeout) {
        this.timeout = timeout;
        this.maxRetries = maxRetries;
        this.verifyTimeout = verifyTimeout;
        this.manager = manager;
        this.masterNode = manager.getMasterNode();
        this.myID = manager.getNetPeerGroup().getPeerID();
        this.pipeService = manager.getNetPeerGroup().getPipeService();
        this.indoubtPeerList = new Vector<PeerID>();
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
        return createMessage(state, HEALTHM, manager.getSystemAdvertisement());
    }

    private Message createMessage(final short state, final String tag,
                                  final SystemAdvertisement adv) {
        final Message msg = new Message();
        final HealthMessage hm = new HealthMessage();
        hm.setSrcID(myID);
        final HealthMessage.Entry entry = new HealthMessage.Entry(adv, states[state]);
        hm.add(entry);
        //System.out.println(hm.toString());
        msg.addMessageElement(NAMESPACE, new TextDocumentMessageElement(tag,
                (XMLDocument) hm.getDocument(MimeMediaType.XMLUTF8), null));
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
                            process(getHealthMessage(msgElement));
                        }
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                LOG.log(Level.WARNING, "HealthMonitor:Caught IOException:"
                        + ex.getLocalizedMessage());
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
    private void process(final HealthMessage hm) {//TODO:SG: REFACTORING CANDIDATE
        //System.out.println("Processing Health Message..");
        //LOG.log(Level.FINEST, "Processing Health Message..");
        //discard loopback messages
        if (!hm.getSrcID().equals(myID)) {
            for (HealthMessage.Entry entry : hm.getEntries()) {
                synchronized (cache) {
                    cache.put(entry.id, entry);
                }
                if(!manager.getClusterViewManager().containsKey(entry.id)){
                    try {
                        masterNode.probeNode(entry.id);
                    } catch (IOException e) {
                        //ignored
                    }
                }
                if(entry.state.equals(states[PEERSTOPPING]) ||
                         entry.state.equals(states[CLUSTERSTOPPING])){
                    handleStopEvent(entry.adv, entry.state);
                }
                if (entry.state.equals(states[INDOUBT]) ||
                        entry.state.equals(states[DEAD])) {
                    if (entry.id.equals(myID)) {
                        reportMyState(ALIVE, hm.getSrcID());
                    } else {
                        if (entry.state.equals(states[INDOUBT])) {
                            synchronized (indoubtListLock) {
                                indoubtPeerList.add(entry.id);
                            }
                            notifyLocalListeners(entry.state, entry.adv);
                            LOG.log(Level.FINE,
                                    new StringBuffer().append("Peer ")
                                            .append(entry.id.toString())
                                            .append(" is suspected failed. Its state is ")
                                            .append(entry.state).toString());
                        }
                        if (entry.state.equals(states[DEAD])) {
                            synchronized (indoubtListLock) {
                                indoubtPeerList.remove(entry.id);
                            }
                            LOG.log(Level.FINE,
                                    new StringBuffer().append("Peer ")
                                            .append(entry.id.toString())
                                            .append(" has failed. Its state is ")
                                            .append(entry.state).toString());
                        }
                    }
                } else {
                    synchronized (indoubtListLock) {
                        if (indoubtPeerList.contains(entry.id)) {
                            indoubtPeerList.remove(entry.id);
//TODO: send an Add Event here (or a NoLongerInDoubt event) as clients need to know that the peer is not suspected anymore
                        }
                    }
                }
            }
        }
    }

    private void handleStopEvent(final SystemAdvertisement stoppingPeerAdv, final String state) {
        LOG.log(Level.FINEST, MessageFormat.format("Handling Stop Event for peer :{0}",
                stoppingPeerAdv.getName()));
        short stateByte = PEERSTOPPING;
        if(state.equals(states[CLUSTERSTOPPING])){
            stateByte = CLUSTERSTOPPING;
        }
        if(stoppingPeerAdv.getID().equals(masterNode.getMasterNodeID())){
            //if masternode is resigning, remove master node from view and start discovery
            LOG.log(Level.FINER, MessageFormat.format("Removing master node {0} " +
                    "from view as it has stopped.", stoppingPeerAdv.getName()));
            removeMasterAdv(stoppingPeerAdv.getID(), stateByte);
            masterNode.resetMaster();
            masterNode.appointMasterNode();
        }
        else if(masterNode.isMaster() && masterNode.isMasterAssigned()){
            removeMasterAdv(stoppingPeerAdv.getID(), stateByte);
            LOG.log(Level.FINE, "Announcing Peer Stop Event of " +
                    stoppingPeerAdv.getName() + " to group ...");
            final ClusterViewEvent cvEvent ;
            if(state.equals(states[CLUSTERSTOPPING])){
                cvEvent = new ClusterViewEvent(
                    ClusterViewEvents.CLUSTER_STOP_EVENT, stoppingPeerAdv);
            }
            else {
                cvEvent = new ClusterViewEvent(
                    ClusterViewEvents.PEER_STOP_EVENT, stoppingPeerAdv);
            }
            masterNode.viewChanged(cvEvent);
        }
    }

    private Map<PeerID, HealthMessage.Entry> getCacheCopy() {
        final Map<PeerID, HealthMessage.Entry> h;
        synchronized (cache) {
            h = new HashMap<PeerID, HealthMessage.Entry>(cache);
        }
        return h;
    }

    /**
     * Clears the cache
     */
    public void clearCache() {
        LOG.log(Level.FINEST, "Clearing cache");
        synchronized (cache) {
            cache.clear();
        }
    }

    /**
     * Reports on the wire the specified state
     *
     * @param state specified state can be
     *              ALIVE|SLEEP|HIBERNATING|SHUTDOWN|DEAD
     * @param id    destination node ID, if null broadcast to group
     */
    public void reportMyState(final short state, final PeerID id) {
        if (state == ALIVE) {
            send(id, getAliveMessage());
        } else {
            send(id, createHealthMessage(state));
        }
    }

    void reportOtherPeerState(final short state, final SystemAdvertisement adv) {
        final Message msg = createMessage(state, HEALTHM, adv);
        LOG.log(Level.FINEST,
                MessageFormat.format("Reporting {0} healthstate as {1}",
                        adv.getName(), state));
        send(null, msg);
    }

    /**
     * Main processing method for the HealthMonitor object
     */
    public void run() {
        long actualto = timeout;
        try {
            reportMyState(ALIVE, null);
            //System.out.println("Running HealthMonitor Thread at interval :"+actualto);
            while (!stop) {
                synchronized (threadLock) {
                    threadLock.wait(actualto);
                }
                if (actualto < timeout) {
                    //reset timeout back in case we lose master designation
                    actualto = timeout;
                    //System.out.println("Resetting actualto :"+actualto+"  to timeout :"+timeout);
                }
                reportMyState(ALIVE, null);
//TODO : The following seems to be unneccesary. Can we remove this?
                if (masterNode.isMaster()) {
                    actualto = timeout - masterDelta;
                    synchronized (threadLock) {
                        threadLock.wait(masterDelta);
                    }
                }
//END TODO
                if (stop) {
                    // if asked to stop, exit
                    reportMyState(STOPPED, null);
                    break;
                }
            }
        } catch (Throwable all) {
            LOG.log(Level.WARNING, "Uncaught Throwable in thread :" +
                    Thread.currentThread().getName(), all);
        } finally {
            thread = null;
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
                final OutputPipe output = pipeService.createOutputPipe(pipeAdv,
                        Collections.singleton(peerid), 1);
                output.send(msg);
                output.close();
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

                this.thread = new Thread(this, "HealthMonitor");
                thread.start();
                inDoubtPeerDetector = new InDoubtPeerDetector();
                inDoubtPeerDetector.start();
                started = true;
            } catch (IOException ioe) {
                LOG.log(Level.WARNING, "Failed to create health monitoring pipe advertisement", ioe);
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
        LOG.log(Level.FINE,
                "Announcing stop event to group with clusterShutdown set to " +
                        isClusterShutdown);
        if (isClusterShutdown) {
            reportMyState(CLUSTERSTOPPING, null);
        } else {
            reportMyState(PEERSTOPPING, null);
        }
    }

    /**
     * Stops this service
     */
    void stop() {
        LOG.log(Level.FINE, "Stopping HealthMonitor");
        stop = true;
        started = false;
        inDoubtPeerDetector.stop();
    }

    private static HealthMessage getHealthMessage(final MessageElement msgElement)
            throws IOException {
        final HealthMessage hm;
        hm = new HealthMessage(getStructuredDocument(msgElement));
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
        }
    }

    public String getState(final ID id) {
        HealthMessage.Entry entry;
        synchronized (cache) {
           entry = cache.get(id);
        }
        if (entry != null) {
            return entry.state;
        } else {
            return states[DEAD];
        }
    }

    /**
     * Detects suspected failures and then delegates final decision to
     * FailureVerifier
     *
     * @author : Shreedhar Ganapathy
     */
    private class InDoubtPeerDetector implements Runnable {
        private static final long buffer = 500;
        private final long maxTime = timeout + buffer;
        private final Map<ID, Integer> stateTable = new HashMap<ID, Integer>();

        void start() {
            final Thread fdThread = new Thread(this, "InDoubtPeerDetector Thread");
            LOG.log(Level.FINE, "Starting InDoubtPeerDetector Thread");
            fdThread.start();
            FailureVerifier fverifier = new FailureVerifier();
            final Thread fvThread = new Thread(fverifier, "FailureVerifier Thread");
            LOG.log(Level.FINE, "Starting FailureVerifier Thread");
            fvThread.start();
        }

        void stop() {
            final Thread tmpThread = fdThread;
            fdThread = null;
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
                        //System.out.println("InDoubtPeerDetector thread waiting for :"+timeout);
                        //wait for specified timeout or until woken up
                        cacheLock.wait(timeout);
                        //LOG.log(Level.FINEST, "Analyzing cache for health...");
                        //get the copy of the states cache
                        if (!stop) {
                            processCacheUpdate();
                        }
                    } catch (InterruptedException ex) {
                        LOG.log(Level.FINEST, ex.getLocalizedMessage());
                    }
                }
            }
        }

        private void processCacheUpdate() {
            int retries = 0;
            long timeDiff;
            final Map<PeerID, HealthMessage.Entry> cacheCopy = getCacheCopy();
            //for each peer id
            for (HealthMessage.Entry entry : cacheCopy.values()) {
                if (entry.state.equals(states[ALIVE])) {
                    //if there is a record, then get the number of
                    //retries performed in an earlier iteration
                    if (stateTable.containsKey(entry.id)) {
                        retries = stateTable.get(entry.id);
                    }
                    try {
                        timeDiff = System.currentTimeMillis() -
                                Long.parseLong(entry.timestamp);
                        determineInDoubtPeers(timeDiff, retries, entry);
                    } catch (NumberFormatException nfe) {
                        LOG.log(Level.WARNING,
                                new StringBuffer()
                                        .append("Exception occurred during time stamp conversion: ")
                                        .append(nfe.getLocalizedMessage()).toString());
                    }
                }
            }
        }

        private void determineInDoubtPeers(final long timeDiff,
                                           int retries,
                                           final HealthMessage.Entry entry) {

            //if current time exceeds the last state update
            //timestamp from this peer id, by more than the
            //the specified max timeout
            if (timeDiff > maxTime && !stop) {
                LOG.log(Level.FINEST, "timeDiff > maxTime");
                if (retries >= maxRetries) {
                    if (canProcessInDoubt(entry)) {
                        LOG.log(Level.FINEST, "Designating InDoubtState");
                        designateInDoubtState(entry);
                        stateTable.remove(entry.id); //important for your sanity!
                        retries = 0; //important for your sanity!
                        //delegate verification to Failure Verifier
                        LOG.log(Level.FINEST, "Notifying FailureVerifier");
                        synchronized (verifierLock) {
                            verifierLock.notify();
                        }
                    }
                } else {
                    //dont suspect self
                    if (!entry.id.equals(myID)) {
                        if (canProcessInDoubt(entry)) {
                            // if max retries have not been reached, then
                            //place the record in the stateTable
                            stateTable.put(entry.id, ++retries);
                            LOG.log(Level.FINE, "For PID = " + entry.id +
                                    "; Time Diff = " + timeDiff + "; maxTime = "
                                    + maxTime);
                            LOG.log(Level.FINE, "For PID = " + entry.id + "; retry # "
                                    + retries + " of " + maxRetries);
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
            synchronized (indoubtListLock) {
                indoubtPeerList.add(entry.id);
            }

            entry.state = states[INDOUBT];
            synchronized(cache) {
                cache.put(entry.id, entry);
            }
            if (masterNode.isMaster()) {
                //do this only when masternode is not suspect.
                // When masternode is suspect, all members update their states
                // anyways so no need to report
                //Send group message announcing InDoubt State
                LOG.log(Level.FINE, "Sending INDOUBT state message of Peer " + entry.id +" to group...");
                reportOtherPeerState(INDOUBT, entry.adv);
            }
            notifyLocalListeners(entry.state, entry.adv);
        }
    }

    private class FailureVerifier implements Runnable {
        private long buffer = 500;

        public void run() {
            while (!stop) {
                try {
                    synchronized (verifierLock) {
                        verifierLock.wait();
                        if (!stop) {
                            verify();
                        }
                    }
                } catch (InterruptedException ex) {
                    LOG.log(Level.FINEST,
                            new StringBuffer()
                                    .append("Thread interrupted:")
                                    .append(ex.getLocalizedMessage()).toString());
                }
            }
        }

        //TODO: Add a isPeerLive() public method that the verify method could call and other callers could use to determine liveness of a member actively.
        //TODO: Send direct message to the in doubt peer for verification.
        void verify() throws InterruptedException {
            //wait for the specified timeout for verification
            Thread.sleep(verifyTimeout + buffer);
            HealthMessage.Entry entry;
            synchronized (indoubtListLock) {
                final ListIterator<PeerID> iter = indoubtPeerList.listIterator();
                while (iter.hasNext()) {
                    final PeerID pid = iter.next();
                    synchronized(cache) {
                        entry = cache.get(pid);
                    }
                    if (entry.state.equals(states[ALIVE])) {
                        reportLiveStateToLocalListeners(pid);
                    } else {
                        iter.remove();
                        assignAndReportFailure(pid, entry.adv);
                    }
                }
            }
        }

        private void reportLiveStateToLocalListeners(final PeerID pid) {
            //TODO: NOt YET IMPLEMENTED
        }
    }

    private void assignAndReportFailure(final PeerID failedPid,
                                        final SystemAdvertisement adv) {
        synchronized (cache) {
            final HealthMessage.Entry entry = cache.get(failedPid);
            if (entry != null) {
                entry.state = states[DEAD];
                cache.put(entry.id, entry);
                if (masterNode.isMaster()) {
                    LOG.log(Level.FINE,
                            new StringBuffer()
                                    .append("Reporting Failed Node ")
                                    .append(entry.id.toString()).toString());
                    reportOtherPeerState(DEAD, adv);
                }
                final ID masterId = masterNode.getMasterNodeID();
                if (masterNode.isMaster() || masterId.equals(entry.id)) {
                    LOG.log(Level.FINE,
                            new StringBuffer()
                                    .append("Removing System Advertisement :")
                                    .append(entry.id.toString()).toString());
                    removeMasterAdv(entry.id, DEAD);
                }
                if (masterNode.isMaster()) {
                    LOG.log(Level.FINE, "Announcing Failure Event of " + entry.id + " ...");
                    final ClusterViewEvent cvEvent = new ClusterViewEvent(
                            ClusterViewEvents.FAILURE_EVENT, adv);
                    masterNode.viewChanged(cvEvent);
                } else if (masterId.equals(entry.id)) {
                    //remove the failed node
                    //manager.getClusterViewManager().remove(entry.id);
                    masterNode.appointMasterNode();
                }
            }
        }
    }

    private void removeMasterAdv(ID id, short state) {
        SystemAdvertisement ad = manager.getClusterViewManager().remove(id);
        if(ad != null){
            switch (state) {
                case DEAD :
                    manager.getClusterViewManager().notifyListeners(
                        new ClusterViewEvent(ClusterViewEvents.FAILURE_EVENT,ad));
                    break;
                case PEERSTOPPING :
                    manager.getClusterViewManager().notifyListeners(
                        new ClusterViewEvent(ClusterViewEvents.PEER_STOP_EVENT, ad));
                    break;
                default :
                    LOG.log(Level.FINEST,
                            MessageFormat.format("Invalid State for removing adv from view{0}", state));

            }
        }
        else {
            LOG.log(Level.WARNING, states[state]+" peer: " +id+" does not exist in my ClusterView");
        }
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
