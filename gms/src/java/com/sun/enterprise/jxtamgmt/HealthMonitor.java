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

import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.id.ID;
import net.jxta.peer.PeerID;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.protocol.RouteAdvertisement;
import net.jxta.impl.pipe.BlockingWireOutputPipe;
import net.jxta.endpoint.StringMessageElement;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final Object threadLock = new Object();
    private final Object indoubtthreadLock = new Object();
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
    private static final String NODEADV = "NAD";

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
            "ready",
            "aliveandready"};
    // "alive_in_isolation"};
    private static final short STARTING = 0;
    private static final short ALIVE = 2;
    private static final short CLUSTERSTOPPING = 3;
    private static final short PEERSTOPPING = 4;
    private static final short STOPPED = 5;
    private static final short DEAD = 6;
    private static final short INDOUBT = 7;
    private static final short UNKNOWN = 8;
    private static final short READY = 9;
    private static final short ALIVEANDREADY = 10;
    //private static final short ALIVE_IN_ISOLATION = 10;
    private static final String HEALTHM = "HM";
    private static final String NAMESPACE = "HEALTH";
    private final Object cacheLock = new Object();
    private final Object verifierLock = new Object();

    private static final String MEMBER_STATE_QUERY = "MEMBERSTATEQUERY";
    private static final String MEMBER_STATE_RESPONSE = "MEMBERSTATERESPONSE";
    private boolean readyStateComplete = false;

    private Message aliveMsg = null;
    private Message aliveAndReadyMsg = null;

    private transient Map<ID, OutputPipe> pipeCache = new Hashtable<ID, OutputPipe>();

    //counter for keeping track of the seq ids of the health messages
    AtomicLong hmSeqID = new AtomicLong();

    //use LWRMulticast to send messages for getting the member state
    LWRMulticast mcast = null;
    int lwrTimeout = 6000;
    private final Object memberStateLock = new Object();
    private String memberState;
    private ReentrantLock sendStopLock = new ReentrantLock(true);

    private final String hwFailureDetectionthreadLock = new String("hwFailureDetectionthreadLock");
    private static final String CONNECTION_REFUSED = "Connection refused";
    private long failureDetectionTCPTimeout;
    private int failureDetectionTCPPort;
    private final ThreadPoolExecutor isConnectedPool;
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
                         final int maxMissedBeats, final long verifyTimeout,
                         final long failureDetectionTCPTimeout,
                         final int failureDetectionTCPPort) {
        this.timeout = timeout;
        this.maxMissedBeats = maxMissedBeats;
        this.verifyTimeout = verifyTimeout;
        this.manager = manager;
        this.masterNode = manager.getMasterNode();
        this.localPeerID = manager.getNetPeerGroup().getPeerID();
        this.pipeService = manager.getNetPeerGroup().getPipeService();
        this.failureDetectionTCPTimeout = failureDetectionTCPTimeout;
        this.failureDetectionTCPPort = failureDetectionTCPPort;
        isConnectedPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        if (LOG.isLoggable(Level.CONFIG)) {
            LOG.config("HealthMonitor: heartBeatTimeout(ms)=" + timeout + 
                       " maxMissedBeats=" + maxMissedBeats + 
                       " failureDetectionTCPTimeout(ms)=" + failureDetectionTCPTimeout + 
                       " failureDetectionTCPPort=" + failureDetectionTCPPort);
        }

        try {
            mcast = new LWRMulticast(manager,
                    createPipeAdv(),
                    this);
            mcast.setSoTimeout(lwrTimeout);
        } catch (IOException e) {
            LOG.warning("Cound not instantiate LWRMulticast : " + e.getMessage());
        }

        //this.shutdownHook = new ShutdownHook();
        //Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    void fine(String msg, Object[] obj) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, msg, obj);
        }
    }

    void fine(String msg) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, msg);
        }
    }


    /**
     * in the event of a failure or planned shutdown, remove the
     * pipe from the pipeCache
     */
    public void removePipeFromCache(ID token) {
        pipeCache.remove(token);
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
        fine("createMessage() => putting into cache " + entry.adv.getName() + " state is " + entry.state);
        synchronized (cacheLock) {
            cache.put(entry.id, entry);
        }
        //fine(" after putting into cache " + cache + " , contents are :-");
        //print(cache);
        return msg;
    }

    private Message getAliveMessage() {
        if (aliveMsg == null) {
            aliveMsg = createHealthMessage(ALIVE);
        }
        return aliveMsg;
    }

    private Message getAliveAndReadyMessage() {
        if (aliveAndReadyMsg == null) {
            aliveAndReadyMsg = createHealthMessage(ALIVEANDREADY);
        }
        return aliveAndReadyMsg;
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
                        } else if (msgElement != null && msgElement.getElementName().equals(MEMBER_STATE_QUERY)) {
                            processMemberStateQuery(msg);
                        } else if (msgElement != null && msgElement.getElementName().equals(MEMBER_STATE_RESPONSE)) {
                            processMemberStateResponse(msg);
                        }
                    }
                }
            } catch (IOException ex) {
                if (LOG.isLoggable(Level.FINE)) {
                    ex.printStackTrace();
                }
                LOG.log(Level.WARNING, "HealthMonitor:Caught IOException : " + ex.getLocalizedMessage());
            } catch (Throwable e) {
                if (LOG.isLoggable(Level.FINE)) {
                    e.printStackTrace();
                }
                LOG.log(Level.WARNING, e.getLocalizedMessage());
            }
        }
    }

    private void processMemberStateQuery(Message msg) {
        boolean foundNodeAdv = false;
        LOG.fine(" received a MemberStateQuery...");
        try {
            //SystemAdvertisement adv = masterNode.processNodeAdvertisement(msg);
            Message.ElementIterator iter = msg.getMessageElements();
            MessageElement msgElement = null;
            while (iter.hasNext()) {
                msgElement = iter.next();
                if (msgElement.getElementName().equals(NODEADV)) {
                    foundNodeAdv = true;
                    break;
                }
            }
            if (foundNodeAdv == true) {
                final StructuredDocument asDoc;
                asDoc = StructuredDocumentFactory.newStructuredDocument(msgElement.getMimeType(), msgElement.getStream());
                final SystemAdvertisement adv = new SystemAdvertisement(asDoc);
                if (!adv.getID().equals(localPeerID)) {
                    LOG.log(Level.FINER, "Received a System advertisment Name :" + adv.getName());
                }
                if (adv != null) {
                    ID sender = adv.getID();       //sender of this query
                    String state = getStateFromCache(localPeerID);
                    Message response = createMemberStateResponse(state);
                    LOG.fine(" sending via LWR response to " + sender.toString() + " with state " + state + " for " + localPeerID);
                    mcast.send((PeerID) sender, response);    //send the response back to the query sender
                }
            } else {
                LOG.warning("Don't know where this query came from. SysAdv is null");
            }
        } catch (IOException e) {
            if (LOG.isLoggable(Level.FINE)) {
                e.printStackTrace();
            }
            LOG.warning("Could not send the message via LWRMulticast : " + e.getMessage());
        }
    }

    private void processMemberStateResponse(Message msg) {
        LOG.fine("received a MemberStateResponse...");

        memberState = msg.getMessageElement(NAMESPACE, MEMBER_STATE_RESPONSE).toString();

        LOG.fine(" member state in processMemberStateResponse() is " + memberState);

        synchronized (memberStateLock) {
            memberStateLock.notify();
        }

    }

    /**
     * creates a node query message specially for querying the member state
     *
     * @return msg Message
     */
    private Message createMemberStateQuery() {
        Message msg = new Message();
        msg.addMessageElement(NAMESPACE, new TextDocumentMessageElement(NODEADV,
                (XMLDocument) manager.getSystemAdvertisement()
                        .getDocument(MimeMediaType.XMLUTF8), null));
        msg.addMessageElement(NAMESPACE, new StringMessageElement(MEMBER_STATE_QUERY, "member state query", null));
        LOG.log(Level.FINE, "Created a Member State Query Message ");
        return msg;
    }

    /**
     * creates a node response message specially for sending the member state
     *
     * @return msg Message
     */
    private Message createMemberStateResponse(String myState) {
        Message msg = new Message();
        msg.addMessageElement(NAMESPACE, new StringMessageElement(MEMBER_STATE_RESPONSE, myState, null));
        msg.addMessageElement(NAMESPACE, new TextDocumentMessageElement(NODEADV,
                (XMLDocument) manager.getSystemAdvertisement()
                        .getDocument(MimeMediaType.XMLUTF8), null));
        LOG.log(Level.FINE, "Created a Member State Response Message with " + myState);
        return msg;
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

                LOG.log(Level.FINEST, "Processing Health Message " + entry.getSeqID() + " for entry " + entry.adv.getName() + 
                         " state=" + entry.state);
                synchronized (cacheLock) {
                    HealthMessage.Entry cachedEntry = cache.get(entry.id);
                    if (cachedEntry != null) {
                        if (LOG.isLoggable(Level.FINEST)) {
                        LOG.log(Level.FINEST, "cachedEntry id=" + cachedEntry.id + " entry name=" + cachedEntry.adv.getName() + 
                                " state=" + cachedEntry.state + " seqId=" + cachedEntry.getSeqID());
                        }
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
                                if (!cachedEntry.state.equals(states[STOPPED])) {
                                    cache.put(entry.id, entry);
                                }
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
                    cache.put(entry.id, entry);
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Put into cache " + entry.adv.getName() + " state = " + entry.state + " peerid = " + entry.id +
                                " seq id=" + entry.getSeqID());
                    }
                }
                //fine("after putting into cache " + cache + " , contents are :-");
                //print(cache);
                if (!manager.getClusterViewManager().containsKey(entry.id) &&
                        (!entry.state.equals(states[CLUSTERSTOPPING]) &&
                                !entry.state.equals(states[PEERSTOPPING]) &&
                                !entry.state.equals(states[STOPPED]) &&
                                !entry.state.equals(states[DEAD]))) {
                    try {
                        masterNode.probeNode(entry);
                    } catch (IOException e) {
                        if (LOG.isLoggable(Level.FINE)) {
                            e.printStackTrace();
                        }
                        LOG.warning("IOException occured while sending probeNode() Message in HealthMonitor:" + e.getLocalizedMessage());
                    }
                }
                if (entry.state.equals(states[READY])) {
                    handleReadyEvent(entry);
                }
                if (entry.state.equals(states[PEERSTOPPING]) || entry.state.equals(states[CLUSTERSTOPPING])) {
                    handleStopEvent(entry);
                }
                if (entry.state.equals(states[INDOUBT]) || entry.state.equals(states[DEAD])) {
                    if (entry.id.equals(localPeerID)) {
                        if (readyStateComplete) {
                            reportMyState(ALIVEANDREADY, hm.getSrcID());
                        } else {
                            reportMyState(ALIVE, hm.getSrcID());
                        }
                    } else {
                        if (entry.state.equals(states[INDOUBT])) {
                            LOG.log(Level.FINE, "Peer " + entry.id.toString() + " is suspected failed. Its state is " + entry.state);
                            notifyLocalListeners(entry.state, entry.adv);
                        }
                        if (entry.state.equals(states[DEAD])) {
                            LOG.log(Level.FINE, "Peer " + entry.id.toString() + " has failed. Its state is " + entry.state);
                            cleanAllCaches(entry);
                        }
                    }
                }
            }
        }
    }

    private void handleReadyEvent(final HealthMessage.Entry entry) {
        synchronized (cacheLock) {
            cache.put(entry.id, entry);
        }
        //fine(" after putting into cache " + cache + " , contents are :-");
        //print(cache);
        if (entry.id.equals(masterNode.getMasterNodeID())) {
            //if this is a ready state sent out by master, take no action here as master would send out a view to the group.
            return;
        }
        //if this is a ready state sent by a non-master, and if I am the assigned master, send out a new view and notify local listeners.
        if (masterNode.isMaster() && masterNode.isMasterAssigned()) {
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
            manager.getClusterViewManager().remove(entry.adv);
            LOG.log(Level.FINE, "Announcing Peer Stop Event of " + entry.adv.getName() + " to group ...");
            final ClusterViewEvent cvEvent;
            if (stateByte == CLUSTERSTOPPING) {
                cvEvent = new ClusterViewEvent(ClusterViewEvents.CLUSTER_STOP_EVENT, entry.adv);
            } else {
                cvEvent = new ClusterViewEvent(ClusterViewEvents.PEER_STOP_EVENT, entry.adv);
            }
            masterNode.viewChanged(cvEvent);
        }
        cleanAllCaches(entry);
    }

    private void cleanAllCaches(HealthMessage.Entry entry) {
        LOG.fine("HealthMonitor.cleanAllCaches : removing pipes and route from cache..." + entry.id);
        removePipeFromCache(entry.id);
        manager.removePipeFromCache(entry.id);
        manager.removeRouteFromCache(entry.id);
        masterNode.removePipeFromCache(entry.id);
    }

    private Map<PeerID, HealthMessage.Entry> getCacheCopy() {
        ConcurrentHashMap<PeerID, HealthMessage.Entry> clone = new ConcurrentHashMap<PeerID, HealthMessage.Entry>();
        //fine(" cache object in getCacheCopy() = " + cache);
        //fine(" getCacheCopy => printing main cache contents (size = " + cache.size() + ") ...");
        //print(cache);
        synchronized (cacheLock) {
            for (Map.Entry<PeerID, HealthMessage.Entry> entry : cache.entrySet()) {
                try {
                    clone.put(entry.getKey(), (HealthMessage.Entry) entry.getValue().clone());
                } catch (CloneNotSupportedException e) {
                    LOG.fine("Exception occurred : " + e);
                }
            }
        }
        //fine(" getCacheCopy => printing clone cache contents (size = " + clone.size() + ")...");
        //print(clone);
        return clone;
    }

    private void print(ConcurrentHashMap<PeerID, HealthMessage.Entry> c) {

        for (Iterator i = c.values().iterator(); i.hasNext();) {
            HealthMessage.Entry e = (HealthMessage.Entry) i.next();
            fine("cache contents => " + e.adv.getName() +
                    " state => " + e.state);
        }

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
            if (state == ALIVEANDREADY) {
                send(id, getAliveAndReadyMessage());
            } else
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
                    if (readyStateComplete) {
                        reportMyState(ALIVEANDREADY, null);
                    } else {
                        reportMyState(ALIVE, null);
                    }
                }
            } catch (InterruptedException e) {
                stop = true;
                LOG.log(Level.FINEST, "Shoal Health Monitor Thread Stopping as the thread is now interrupted...:" + e.getLocalizedMessage());
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
        sendStopLock.lock();
        try {
                //the message contains only one messageElement and
            //  the  healthMessage contains only 1 entry
            final Message.ElementIterator iter = msg.getMessageElements();
            while (iter.hasNext()) {
                MessageElement msgElement = iter.next();
                if (msgElement != null && msgElement.getElementName().equals(HEALTHM)) {
                    HealthMessage hm = getHealthMessage(msgElement);
                    for (HealthMessage.Entry entry : hm.getEntries()) {

                        //if stop() has been called before the sendStopLock was acquired
                        //then don't send any alive/aliveready/ready messages
                        if (stop &&
                                (!entry.state.equals(states[CLUSTERSTOPPING]) &&
                                        !entry.state.equals(states[PEERSTOPPING]) &&
                                        !entry.state.equals(states[STOPPED]))) {
                            LOG.fine("HealthMonitor.send()=> not sending the message since HealthMonitor is trying to stop. state = " + entry.state);
                            return;
                        }
                    }
                }
            }
            if (peerid != null) {
                // Unicast datagram
                // create a op pipe to the destination peer
                LOG.log(Level.FINE, "Unicasting Message to :" + peerid.toString());
                OutputPipe output = null;
                if (!pipeCache.containsKey(peerid)) {
                    RouteAdvertisement route = manager.getCachedRoute((PeerID) peerid);
                    if (route != null) {
                        output = new BlockingWireOutputPipe(manager.getNetPeerGroup(), pipeAdv, (PeerID) peerid, route);
                    }
                    if (output == null) {
                        // Unicast datagram
                        // create a op pipe to the destination peer
                        output = pipeService.createOutputPipe(pipeAdv, Collections.singleton(peerid), 1);
                    }
                    pipeCache.put(peerid, output);
                } else {
                    output = pipeCache.get(peerid);
                    if (output == null || output.isClosed()) {
                        output = pipeService.createOutputPipe(pipeAdv, Collections.singleton(peerid), 1);
                        pipeCache.put(peerid, output);
                    }
                }
                output.send(msg);
            } else {
                final boolean result = outputPipe.send(msg);
                if (!result) {
                    LOG.log(Level.WARNING,"HealthMonitor.send returned false");
                }
            }
        } catch (IOException io) {
            LOG.log(Level.WARNING, "Failed to send message", io);
        } finally {
            sendStopLock.unlock();
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
     *
     * @param isClusterShutdown true if the cluster is shutting down
     */
    void stop(boolean isClusterShutdown) {
        sendStopLock.lock();
        try {
            stop = true;
            started = false;
        } finally {
            sendStopLock.unlock();
        }

        announceStop(isClusterShutdown);
        reportMyState(STOPPED, null);
        //acquire lock again since dont want send() to send messages while stop()
        //is clearing the caches
        sendStopLock.lock();
        try {
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
            manager.clearAllCaches();
            masterNode.clearPipeCache();
        } finally {
            sendStopLock.unlock();
        }
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
        } else if (state.equals(states[ALIVEANDREADY])) {
            manager.getClusterViewManager().setPeerNoLongerInDoubtState(adv);
        } else if (state.equals(states[CLUSTERSTOPPING])) {
            manager.getClusterViewManager().setClusterStoppingState(adv);
        } else if (state.equals(states[PEERSTOPPING])) {
            manager.getClusterViewManager().setPeerStoppingState(adv);
        } else if (state.equals(states[READY])) {
            manager.getClusterViewManager().setPeerReadyState(adv);
        }  /*else if (state.equals(states[ALIVE_IN_ISOLATION])) {
            manager.getClusterViewManager().setInIsolationState(adv);
        } */
    }

    public String getMemberStateFromHeartBeat(ID peerID, long threshold) {

        HealthMessage.Entry entry;
        synchronized (cacheLock) {
            entry  = cache.get((PeerID) peerID);
        }
        if (entry != null) {
            //calculate if the timestamp of the entry and the current time gives a delta that is <= threshold
            if (System.currentTimeMillis() - entry.timestamp <= threshold) {
                return entry.state;
            } else {
                return states[UNKNOWN]; //this means that either the heartbeat from that instance has'nt come in yet so the state in the cache could be stale
                //so rather than give a stale state, give it as UNKNOWN
            }
        } else {
            LOG.warning("There is no entry in the cache for the given peerID. " +
                    "Please check the name of the instance whose state is requested. Returning UNKNOWN state");
            return states[UNKNOWN];
        }
    }


    public String getMemberState(final ID peerID) {

        //don't get the cached member state for that instance
        //instead send a query to that instance to get the most up-to-date state
        LOG.fine("inside getMemberState for " + peerID.toString());
        Message msg = createMemberStateQuery();
        //send it via LWRMulticast
        try {
            mcast.send((PeerID) peerID, msg);
            LOG.fine("send message in getMemberState via LWR...");
        } catch (IOException e) {
            LOG.warning("Could not send the LWR Multicast message to get the member state of " + peerID.toString() + " IOException : " + e.getMessage());
        }

        synchronized (memberStateLock) {
            try {
                memberStateLock.wait(timeout);
            } catch (InterruptedException e) {
                LOG.warning("wait() was interrupted : " + e.getMessage());
            }
        }
        if (memberState != null) {
            String state = memberState;
            memberState = null;  //nullify this string before returning so that the next time this method is accessed
            // memberState is null before it is assigned a value in processMemberStateResponse
            LOG.fine("inside getMemberState got state via lwr " + state);
            return state;
        } else {    //if timeout happened even before notify() was called
            String state = getStateFromCache(peerID);
            LOG.fine("inside getMemberState got state after timeout " + state);
            return state;
        }
    }

    String getStateFromCache(final ID peerID) {
        HealthMessage.Entry entry;
        final String state;
        entry = cache.get((PeerID) peerID);
        if (entry != null) {
            state = entry.state;
        } else {
            if (((PeerID) peerID).equals(localPeerID)) {
                if (!started) {
                    state = states[STARTING];
                } else if (readyStateComplete) {
                    state = states[ALIVEANDREADY];
                } else {
                    state = states[ALIVE];
                }
            } else {
                entry = cache.get((PeerID) peerID);
                if (entry != null) {
                    state = entry.state;
                } else {
                    if (manager.getClusterViewManager().containsKey(peerID)) {
                        state = states[STARTING];//we assume that the peer is in starting state hence its state is not yet known in this peer
                    } else {
                        state = states[UNKNOWN];
                    }
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

        if (masterNode.isDiscoveryInProgress()) {
            synchronized (masterNode.discoveryLock) {
                try {
                    masterNode.discoveryLock.wait();
                    LOG.log(Level.FINEST, "reportJoinedAndReadyState() waiting for masternode discovery to finish...");
                } catch (InterruptedException e) {
                    LOG.log(Level.FINEST, "MasterNode's DiscoveryLock Thread is interrupted " + e);
                }
            }
        }
        if (masterNode.isMaster() && masterNode.isMasterAssigned()) {
            LOG.log(Level.FINEST, "Sending Ready Event View for " + manager.getSystemAdvertisement().getName());
            ClusterViewEvent cvEvent = masterNode.sendReadyEventView(manager.getSystemAdvertisement());
            LOG.log(Level.FINEST, MessageFormat.format("Notifying Local listeners about " +
                    "Joined and Ready Event View for peer :{0}", manager.getSystemAdvertisement().getName()));
            manager.getClusterViewManager().notifyListeners(cvEvent);
        }
        LOG.log(Level.FINEST, "Calling reportMyState() with READY...");
        reportMyState(READY, null);
        readyStateComplete = true;
    }

    /**
     * Detects suspected failures and then delegates final decision to
     * FailureVerifier
     *
     * @author : Shreedhar Ganapathy
     */
    private class InDoubtPeerDetector implements Runnable {
        private Thread fvThread = null;

        void start() {
            failureDetectorThread = new Thread(this, "InDoubtPeerDetector Thread");
            LOG.log(Level.FINE, "Starting InDoubtPeerDetector Thread");
            failureDetectorThread.start();
            FailureVerifier fverifier = new FailureVerifier();
            fvThread = new Thread(fverifier, "FailureVerifier Thread");
            LOG.log(Level.FINE, "Starting FailureVerifier Thread");
            fvThread.start();
        }

        void stop() {
            final Thread tmpThread = failureDetectorThread;
            failureDetectorThread = null;
            if (tmpThread != null) {
                tmpThread.interrupt();
            }

            // Interrupt failure verifier rather than notify.
            // Getting verifierLock deadlocked with FV.assignAndReportFailure trying to acquire stopSendLock.
            final Thread tmpFVThread = fvThread;
            fvThread = null;
            if (tmpFVThread != null) {
                tmpFVThread.interrupt();
            }

            // Commented out since resulted in deadlock with FailureVerifier thread already holding verifierLock and trying to
            // acquire sendStopLock to send a message.
//            synchronized (verifierLock) {
//                verifierLock.notify();<
//            }
        }

        public void run() {
            while (!stop) {
                try {
                    //System.out.println("InDoubtPeerDetector failureDetectorThread waiting for :"+timeout);
                    //wait for specified timeout or until woken up
                    synchronized (indoubtthreadLock) {
                        indoubtthreadLock.wait(timeout);
                    }
                    //LOG.log(Level.FINEST, "Analyzing cache for health...");
                    //get the copy of the states cache
                    if (!manager.isStopping()) {
                        processCacheUpdate();
                    }
                } catch (InterruptedException ex) {
                    LOG.log(Level.FINEST, "InDoubtPeerDetector Thread stopping as it is now interrupted :" + ex.getLocalizedMessage());
                    break;
                }
                catch (Throwable all) {
                    LOG.log(Level.FINE, "Uncaught Throwable in failureDetectorThread " + Thread.currentThread().getName() + ":" + all);
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
                //don't check for isConnected with your own self
                if (!entry.id.equals(manager.getSystemAdvertisement().getID())) {
                    LOG.fine("processCacheUpdate : " + entry.adv.getName() + " 's state is " + entry.state);
                    if (entry.state.equals(states[ALIVE]) ||
                            (entry.state.equals(states[ALIVEANDREADY]))) {
                        //if there is a record, then get the number of
                        //retries performed in an earlier iteration
                        try {
                            determineInDoubtPeers(entry);
                        } catch (NumberFormatException nfe) {
                            if (LOG.isLoggable(Level.FINE)) {
                                nfe.printStackTrace();
                            }
                            LOG.log(Level.WARNING, "Exception occurred during time stamp conversion : " + nfe.getLocalizedMessage());
                        }
                    }
                }
            }
        }

        private void determineInDoubtPeers(final HealthMessage.Entry entry) {

            //if current time exceeds the last state update timestamp from this peer id, by more than the
            //the specified max timeout
            if (!stop) {
                //dont suspect self
                if (!entry.id.equals(localPeerID)) {
                    if (canProcessInDoubt(entry)) {
                        LOG.log(Level.FINE, MessageFormat.format("For instance = {0}; last recorded heart-beat = {1}ms ago, heart-beat # {2} out of a max of {3}",
                                entry.adv.getName(), (System.currentTimeMillis() - entry.timestamp), computeMissedBeat(entry), maxMissedBeats));
                    }
                }
                if (computeMissedBeat(entry) >= maxMissedBeats && !isConnected(entry)) {
                    LOG.log(Level.FINEST, "timeDiff > maxTime");
                    if (canProcessInDoubt(entry)) {
                        LOG.log(Level.FINER, "Designating InDoubtState");
                        designateInDoubtState(entry);
                        //delegate verification to Failure Verifier
                        LOG.log(Level.FINER, "Notifying FailureVerifier for " + entry.adv.getName());
                        synchronized (verifierLock) {
                            verifierLock.notify();
                            LOG.log(Level.FINER, "Done Notifying FailureVerifier for " + entry.adv.getName());
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
            fine(" in designateInDoubtState, going to set the state of " + entry.adv.getName() + " to indoubt");
            synchronized (cacheLock) {
                entry.state = states[INDOUBT];
                cache.put(entry.id, entry);
            }
            //fine(" after putting into cache " + cache + " , contents are :-");
            //print(cache);
            if (masterNode.isMaster()) {
                //do this only when masternode is not suspect.
                // When masternode is suspect, all members update their states
                // anyways so no need to report
                //Send group message announcing InDoubt State
                fine("Sending INDOUBT state message about node ID: " + entry.id + " to the cluster...");
                reportOtherPeerState(INDOUBT, entry.adv);
            }
            LOG.log(Level.FINEST, "Notifying Local Listeners of designated indoubt state for " + entry.adv.getName());
            notifyLocalListeners(entry.state, entry.adv);
        }
    }

    private class FailureVerifier implements Runnable {
        private final long buffer = 500;

        public void run() {
            try {
                synchronized (verifierLock) {
                    while (!stop) {
                        LOG.log(Level.FINE, "FV: Entering verifierLock Wait....");
                        verifierLock.wait();
                        LOG.log(Level.FINE, "FV: Woken up from verifierLock Wait by a notify ....");
                        if (!stop) {
                            LOG.log(Level.FINE, "FV: Calling verify() ....");
                            verify();
                            LOG.log(Level.FINE, "FV: Done verifying ....");
                        }
                    }
                }
            } catch (InterruptedException ex) {
                LOG.log(Level.FINE, MessageFormat.format("failure Verifier Thread stopping as it is now interrupted: {0}", ex.getLocalizedMessage()));
                print(cache);
            }
        }

        void verify() throws InterruptedException {
            //wait for the specified timeout for verification
            Thread.sleep(verifyTimeout + buffer);
            HealthMessage.Entry entry;
            for (HealthMessage.Entry entry1 : getCacheCopy().values()) {
                entry = entry1;
                LOG.log(Level.FINE, "FV: Verifying state of "+entry.adv.getName()+" state = "+entry.state);
                if(entry.state.equals(states[INDOUBT]) && !isConnected(entry)){
                    LOG.log(Level.FINE, "FV: Assigning and reporting failure ....");
                    assignAndReportFailure(entry);
                }
            }
        }
    }

    private void assignAndReportFailure(final HealthMessage.Entry entry) {
        if (entry != null) {
            fine(" assigAndReportFailure => going to put into cache " + entry.adv.getName() +
                    " state is " + entry.state);
            synchronized (cacheLock) {
                entry.state = states[DEAD];
                cache.put(entry.id, entry);
            }
            //fine(" after putting into cache " + cache + " , contents are :-");
            //print(cache);
            if (masterNode.isMaster()) {
                LOG.log(Level.FINE, MessageFormat.format("Reporting Failed Node {0}", entry.id.toString()));
                reportOtherPeerState(DEAD, entry.adv);
            }
            final boolean masterFailed = (masterNode.getMasterNodeID()).equals(entry.id);
            if (masterNode.isMaster() && masterNode.isMasterAssigned()) {
                LOG.log(Level.FINE, MessageFormat.format("Removing System Advertisement :{0} for name {1}", entry.id.toString(), entry.adv.getName()));
                manager.getClusterViewManager().remove(entry.adv);
                LOG.log(Level.FINE, MessageFormat.format("Announcing Failure Event of {0} for name {1}...", entry.id, entry.adv.getName()));
                final ClusterViewEvent cvEvent = new ClusterViewEvent(ClusterViewEvents.FAILURE_EVENT, entry.adv);
                masterNode.viewChanged(cvEvent);
            } else if (masterFailed) {
                //remove the failed node
                LOG.log(Level.FINE, MessageFormat.format("Master Failed. Removing System Advertisement :{0} for master named {1}", entry.id.toString(), entry.adv.getName()));
                manager.getClusterViewManager().remove(entry.adv);
                masterNode.resetMaster();
                masterNode.appointMasterNode();
                if (masterNode.isMaster() && masterNode.isMasterAssigned()) {
                    LOG.log(Level.FINE, MessageFormat.format("Announcing Failure Event of {0} for name {1}...", entry.id, entry.adv.getName()));
                    final ClusterViewEvent cvEvent = new ClusterViewEvent(ClusterViewEvents.FAILURE_EVENT, entry.adv);
                    masterNode.viewChanged(cvEvent);
                }
            }
            cleanAllCaches(entry);
        }
    }

    private void removeMasterAdv(HealthMessage.Entry entry, short state) {
        manager.getClusterViewManager().remove(entry.adv);
        if (entry.adv != null) {
            switch (state) {
                case DEAD:
                    LOG.log(Level.FINER, "FV: Notifying local listeners of Failure of " + entry.adv.getName());
                    manager.getClusterViewManager().notifyListeners(
                            new ClusterViewEvent(ClusterViewEvents.FAILURE_EVENT, entry.adv));
                    break;
                case PEERSTOPPING:
                    LOG.log(Level.FINER, "FV: Notifying local listeners of Shutdown of " + entry.adv.getName());
                    manager.getClusterViewManager().notifyListeners(
                            new ClusterViewEvent(ClusterViewEvents.PEER_STOP_EVENT, entry.adv));
                    break;
                case CLUSTERSTOPPING:
                    LOG.log(Level.FINER, "FV: Notifying local listeners of Cluster_Stopping of " + entry.adv.getName());
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
     * This method is for designating myself in network isolation
     * if my network interface is not up.
     * It does not matter if I am the master or not.
     * @param entry
     */
    /* private void designateInIsolationState(final HealthMessage.Entry entry) {

       entry.state = states[ALIVE_IN_ISOLATION];
       cache.put(entry.id, entry);
       LOG.log(Level.FINE, "Sending ALIVE_IN_ISOLATION state message about node ID: " + entry.id + " to the cluster...");
       reportMyState(ALIVE_IN_ISOLATION, entry.id);
       LOG.log(Level.FINE, "Notifying Local Listeners of designated ALIVE_IN_ISOLATION state for " + entry.adv.getName());
       notifyLocalListeners(entry.state, entry.adv);
   } */

    public boolean isConnected(HealthMessage.Entry entry) {
        //TODO : put only during the first heartbeat that comes in for that entry
        //TODO : cleaup needed if entry goes down
        //TODO : map  host to uris. and entries to host

        List<URI> list = entry.adv.getURIs();
        List<CheckConnectionToPeerMachine> connections = new ArrayList<CheckConnectionToPeerMachine>(list.size());
        AtomicInteger outstandingConnections = new AtomicInteger(list.size());
        
        for (URI uri : list) {
            fine("Checking for machine status for network interface : " + uri.toString());
            CheckConnectionToPeerMachine connectToPeer = new CheckConnectionToPeerMachine(entry, uri.getHost(), outstandingConnections);
            connections.add(connectToPeer);
            connectToPeer.setFuture(isConnectedPool.submit(connectToPeer));
        }

        try {
            fine("failureDetectionTCPTimeout = " + failureDetectionTCPTimeout);
            // wait until one network interface is confirmed to be up or all network interface addresses are confirmed to be down.
            synchronized (hwFailureDetectionthreadLock) {
                hwFailureDetectionthreadLock.wait(failureDetectionTCPTimeout);
            }
        } catch (InterruptedException e) {
            fine("InterruptedException occurred..." + e.getMessage(), new Object[]{e});
        }

        // check if at least one network interface is up and isConnectable.
        // also be sure to cancel any outstanding future tasks.  (Could be hung trying to create a socket to a failed network interface)
        boolean result = false;
        boolean canceled = false;
        for (CheckConnectionToPeerMachine connection : connections) {
            try {
                Future future = connection.getFuture();
                //if the task has completed after waiting for the
                //specified timeout
                if (connection.completedComputation()) {
                    if (!result && connection.isConnectionUp()) {
                        boolean isConnected = masterNode.getRouteControl().isConnected(entry.id, manager.getCachedRoute(entry.id));
                        fine("routeControl.isConnected() for " + entry.adv.getName() + " is => " + isConnected + " using " +
                              connection.address);
                        if (isConnected) {
                            result = isConnected;
                        }
                    }
                } else if (! future.isDone()) {
                    
                    //interrupt the thread which is still running the task submitted to it.
                    // chances are that the thread is hung on socket creation to a failed network interface.
                    fine("Going to cancel the future task for address " + connection.address);
                    future.cancel(true);
                    canceled = true;
                    fine("Finished cancelling future task for address " + connection.address);

                    //cancelling the task is as good as getting false
                    //underlying socket that got created will still be alive.
                }
            } catch (Throwable t) {
                // ignore.  must iterate over all connections.
            }
        }
        if (canceled) {
            // be sure to purge since there was one or more cancelled future threads.
            isConnectedPool.purge();
        }
        String machineStatus = result ? "up!" : "down!";
        fine("Peer Machine for " + entry.adv.getName() + " is " + machineStatus);
        return result;
    }

    //This is the Callable Object that gets executed by the ExecutorService
    private class CheckConnectionToPeerMachine implements Callable<Object> {
        HealthMessage.Entry entry;
        String address;
        Boolean connectionIsUp;
        boolean running;
        Future future;
        AtomicInteger outstandingConnectionChecks;

        CheckConnectionToPeerMachine(HealthMessage.Entry entry, String address, AtomicInteger outstanding) {
            this.entry = entry;
            this.address = address;
            this.connectionIsUp = null;   // unknown
            this.running = true;
            this.outstandingConnectionChecks = outstanding;
        }
        
        void setFuture(Future future) {
            this.future = future;
        }
        
        Future getFuture() {
            return future;
        }
        
        boolean isConnectionUp() {
            return connectionIsUp != null && connectionIsUp;
        }
        
        boolean completedComputation() {
            return !running;
        }
        
        public Object call() {
            Socket socket = null;
            try {
                fine("Going to create a socket at " + address + "port = " + failureDetectionTCPPort);
                socket = new Socket(InetAddress.getByName(address), failureDetectionTCPPort);
                fine("Socket created at " + address + "port = " + failureDetectionTCPPort);
                
                //for whatever reason a socket got created, finally block will close it and return true
                //i.e. this instance was able to create a socket on that machine so it is up
                connectionIsUp = Boolean.TRUE;
            } catch (InterruptedIOException intioe) {
                connectionIsUp = null;
            }  catch (IOException e) {
                fine("IOException occurred while trying to connect to peer " + entry.adv.getName() +
                        "'s machine : " + e.getMessage(), new Object[]{e});
                if (e.getMessage().trim().contains(CONNECTION_REFUSED)) {
                    fine("Going to call notify since the peer machine is up");
                    connectionIsUp = Boolean.TRUE;
                } else {
                    connectionIsUp = Boolean.FALSE;
                }
            } finally {
                running = false;
                outstandingConnectionChecks.decrementAndGet();
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        fine("Could not close the socket due to " + e.getMessage());
                    } catch (Throwable t) {}
                }
                
                if (isConnectionUp() || outstandingConnectionChecks.get() <= 0) {
                    
                    // completed computation, notify caller to proceed.
                    // either one network interface has been verified working OR
                    // all network interfaces have been confirmed to not be connectable.
                    if (LOG.isLoggable(Level.FINE)) {
                        fine("stop waiting for isConnected check.  isConnectionUp="  + isConnectionUp() +
                             " outstandingNetworkInterfaceChecks=" + outstandingConnectionChecks.get());
                    }
                    synchronized (hwFailureDetectionthreadLock) {
                        hwFailureDetectionthreadLock.notify();
                    }
                }
                return connectionIsUp;
            }
        }
    }

    /**
     * Determines whether a connection to a specific node exists, or one can be created
     *
     * @param entry HealthMessage.Entry
     * le@return true, if a connection already exists, or a new was sucessfully created
     */

    /*     public boolean isConnected(HealthMessage.Entry entry) {
     //if System property for InetAddress.isReachable() is set, then check for the following:
     //if InetAddress.isReachable() is true, then check for isConnected()
     //if InetAddress.isReachable() is false, then simply return false

     //check if using JDK 5 or 6. isUp() API available only in 6
     Method method ;
     try {
         Class c = Class.forName("java.net.NetworkInterface");
         method = c.getMethod("isUp", new Class[]{});
     } catch (NoSuchMethodException nsme) {
         //we are using JDK version < 6
         return masterNode.getRouteControl().isConnected(entry.id, manager.getCachedRoute(entry.id));
     } catch (SecurityException s) {
         return masterNode.getRouteControl().isConnected(entry.id, manager.getCachedRoute(entry.id));
     } catch (ClassNotFoundException c) {
         return masterNode.getRouteControl().isConnected(entry.id, manager.getCachedRoute(entry.id));
     }

     try {
         String ipAddr = manager.getSystemAdvertisement().getIP(); //get my IP address
         LOG.fine("ipAddr in isConnected => " + ipAddr);
         NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getByName(ipAddr));
         //if (ni.isUp()) {
         if (((Boolean) method.invoke(ni, new Object[]{})).booleanValue()) {
             LOG.fine("The network interface " + ni.getDisplayName() + " is up");
             return masterNode.getRouteControl().isConnected(entry.id, manager.getCachedRoute(entry.id));
         } else {
             LOG.fine("The network interface " + ni.getDisplayName() + " is NOT up");
             MasterNode.INSTANCE_IN_NETWORK_ISOLATION = true;
             designateInIsolationState((HealthMessage.Entry) cache.get(manager.getSystemAdvertisement().getID())); //put myself in network isolation category
             return false;
         }

     } catch (Exception e) {
         return false;
     }
 }   */

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
