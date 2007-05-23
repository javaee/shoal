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

import static com.sun.enterprise.jxtamgmt.JxtaUtil.getObjectFromByteArray;
import net.jxta.document.*;
import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.*;
import net.jxta.protocol.PipeAdvertisement;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ClusterManager is the entry point for using the JxtaClusterManagement module
 * which provides group communications and membership semantics on top of JXTA.
 */
public class ClusterManager implements PipeMsgListener {
    private static final Logger LOG = JxtaUtil.getLogger(ClusterManager.class.getName());
    private MasterNode masterNode = null;
    private ClusterViewManager clusterViewManager = null;
    private HealthMonitor healthMonitor = null;
    private NetworkManager netManager = null;
    private String groupName = null;
    private String instanceName = null;
    private boolean started = false;
    private boolean stopped = true;
    private boolean loopbackMessages = false;
    private final String closeLock = new String("closeLock");
    private SystemAdvertisement systemAdv = null;

    private static final String NODEADV = "NAD";
    private transient Map<String, String> identityMap;
    private PipeAdvertisement pipeAdv;
    private PipeService pipeService;
    private MessageElement sysAdvElement = null;
    private InputPipe inputPipe;
    private OutputPipe outputPipe;
    private static final String NAMESPACE = "CLUSTER_MANAGER";
    private PeerID myID;
    private static final String APPMESSAGE = "APPMESSAGE";
    private List<ClusterMessageListener> cmListeners;
    private boolean stopping = false;
    private transient Map<ID, OutputPipe> pipeCache = new Hashtable<ID, OutputPipe>();

    /**
     * The ClusterManager is created using the instanceName,
     * and a Properties object that contains a set of parameters that the
     * employing application would like to set for its purposes, namely,
     * configuration parameters such as failure detection timeout, retries,
     * address and port on which to communicate with the group, group and
     * instance IDs,etc. The set of allowed constants to be used as keys in the
     * Properties object are specified in JxtaConfigConstants enum.
     *
     * @param groupName        Name of Group to which this process/peer seeks
     *                         membership
     * @param instanceName     A token identifying this instance/process
     * @param identityMap      Additional identity tokens can be specified through
     *                         this Map object. These become a part of the
     *                         SystemAdvertisement allowing the peer/system to
     *                         be identified by the application layer based on their
     *                         own notion of identity
     * @param props            a Properties object containing parameters that are
     *                         allowed to be specified by employing application
     *                         //TODO: specify that INFRA IDs and address/port are
     *                         composite keys in essence but addresses/ports could
     *                         be shared across ids with a performance penalty.
     *                         //TODO: provide an API to send messages, synchronously or asynchronously
     * @param viewListeners    listeners interested in group view change events
     * @param messageListeners listeners interested in receiving messages.
     */
    public ClusterManager(final String groupName,
                          final String instanceName,
                          final Map<String, String> identityMap,
                          final Map props,
                          final List<ClusterViewEventListener> viewListeners,
                          final List<ClusterMessageListener> messageListeners) {
        this.groupName = groupName;
        this.instanceName = instanceName;
        this.loopbackMessages = isLoopBackEnabled(props);
        //TODO: ability to specify additional rendezvous and also bootstrap a default rendezvous
        //TODO: revisit and document auto composition of transports
        this.netManager = new NetworkManager(groupName, instanceName, props);
        this.identityMap = identityMap;
        try {
            netManager.start();
        } catch (PeerGroupException pge) {
            LOG.log(Level.SEVERE, pge.getLocalizedMessage());
        } catch (IOException ioe) {
            LOG.log(Level.WARNING, ioe.getLocalizedMessage());
        }
        NetworkManagerRegistry.add(groupName, netManager);
        systemAdv = createSystemAdv(netManager.getNetPeerGroup(), instanceName,
                identityMap);
        LOG.log(Level.FINER, "Instance ID :" + getSystemAdvertisement().getID());
        this.clusterViewManager =
                new ClusterViewManager(getSystemAdvertisement(), this,
                        viewListeners);
        this.masterNode = new MasterNode(this,
                getDiscoveryTimeout(props),
                1);

        this.healthMonitor = new HealthMonitor(this,
                getFailureDetectionTimeout(props),
                getFailureDetectionRetries(props),
                getVerifyFailureTimeout(props));

        pipeService = netManager.getNetPeerGroup().getPipeService();
        myID = netManager.getNetPeerGroup().getPeerID();
        try {
            // create the pipe advertisement, to be used in creating the pipe
            pipeAdv = createPipeAdv();
            //create output
            outputPipe = pipeService.createOutputPipe(pipeAdv, 100);
        } catch (IOException io) {
            LOG.log(Level.FINE, "Failed to create master outputPipe", io);
        }
        cmListeners = messageListeners;
        sysAdvElement = new TextDocumentMessageElement(NODEADV,
                (XMLDocument) getSystemAdvertisement()
                        .getDocument(MimeMediaType.XMLUTF8), null);
    }

    private boolean isLoopBackEnabled(final Map props) {
        boolean loopback = false;
        if (props != null && !props.isEmpty()) {
            Object lp = props.get(JxtaConfigConstants.LOOPBACK.toString());
            if (lp != null) {
                loopback = Boolean.parseBoolean((String) lp);
            }
        }
        return loopback;
    }

    private long getDiscoveryTimeout(Map props) {
        long discTimeout = 5000;
        if (props != null && !props.isEmpty()) {
            Object dt = props.get(JxtaConfigConstants.DISCOVERY_TIMEOUT.toString());
            if (dt != null) {
                discTimeout = Long.parseLong((String) dt);
            }
        }
        return discTimeout;
    }

    private long getFailureDetectionTimeout(Map props) {
        long failTimeout = 3000;
        if (props != null && !props.isEmpty()) {
            Object ft = props.get(JxtaConfigConstants.FAILURE_DETECTION_TIMEOUT.toString());
            if (ft != null) {
                failTimeout = Long.parseLong((String) ft);
            }
        }
        return failTimeout;
    }

    private int getFailureDetectionRetries(Map props) {
        int failRetry = 3;

        if (props != null && !props.isEmpty()) {
            Object fr = props.get(JxtaConfigConstants.FAILURE_DETECTION_RETRIES.toString());
            if (fr != null) {
                failRetry = Integer.parseInt((String) fr);
            }
        }
        return failRetry;
    }

    private long getVerifyFailureTimeout(Map props) {
        long verifyTimeout = 2000;
        if (props != null && !props.isEmpty()) {
            Object vt = props.get(JxtaConfigConstants.FAILURE_VERIFICATION_TIMEOUT.toString());
            if (vt != null) {
                verifyTimeout = Long.parseLong((String) vt);
            }
        }
        return verifyTimeout;
    }

    public void addClusteMessageListener(final ClusterMessageListener listener) {
        cmListeners.add(listener);
    }

    public void removeClusterViewEventListener(
            final ClusterMessageListener listener) {
        cmListeners.remove(listener);
    }

    /**
     * @param argv none defined
     */
    public static void main(final String[] argv) {
        JxtaUtil.setupLogHandler();
        LOG.setLevel(Level.FINEST);
        final String name = System.getProperty("INAME", "instanceName");
        final String groupName = System.getProperty("GNAME", "groupName");
        LOG.log(Level.FINER, "Instance Name :" + name);
        final Map props = getPropsForTest();
        final Map<String, String> idMap = getIdMap();
        final List<ClusterViewEventListener> vListeners =
                new ArrayList<ClusterViewEventListener>();
        final List<ClusterMessageListener> mListeners =
                new ArrayList<ClusterMessageListener>();
        vListeners.add(
                new ClusterViewEventListener() {
                    public void clusterViewEvent(
                            final ClusterViewEvent event,
                            final ClusterView view) {
                        LOG.log(Level.INFO, "event.message", new Object[] {event.getEvent().toString()});
                        LOG.log(Level.INFO, "peer.involved", new Object[] {event.getAdvertisement().toString()});
                        LOG.log(Level.INFO, "view.message",new Object[] {view.getPeerNamesInView().toString()});
                    }
                });
        mListeners.add(
                new ClusterMessageListener() {
                    public void handleClusterMessage(
                            final SystemAdvertisement id, final Object message) {
                        LOG.log(Level.INFO, id.getName());
                        LOG.log(Level.INFO, message.toString());
                    }
                }
        );
        final ClusterManager manager = new ClusterManager(groupName,
                name,
                idMap,
                props,
                vListeners,
                mListeners);
        manager.start();
        manager.waitForClose();
    }

    //TODO: NOT YET IMPLEMENTED
    private static Map<String, String> getIdMap() {
        return new HashMap<String, String>();
    }

    //TODO: NOT YET IMPLEMENTED
    private static Map getPropsForTest() {
        return new HashMap();
    }

    /**
     * Announces Stop event to all members indicating that this peer
     * will gracefully exit the group. Presently this method is non-blocking.
     * TODO:Make this a blocking call
     *
     * @param isClusterShutdown boolean value indicating whether this
     *                          announcement is in the context of a clusterwide shutdown or a shutdown of
     *                          this peer only.
     */
    public void announceStop(final boolean isClusterShutdown) {
        stopping = true;
        healthMonitor.announceStop(isClusterShutdown);        
    }

    /**
     * Stops the ClusterManager and all it's services
     * @param isClusterShutdown true if this peer is shutting down as part of cluster wide shutdown
     */
    public synchronized void stop(final boolean isClusterShutdown) {
        if (!stopped) {
            announceStop(isClusterShutdown);
            outputPipe.close();
            inputPipe.close();
            pipeCache.clear();
            healthMonitor.stop();
            masterNode.stop();
            netManager.stop();
            NetworkManagerRegistry.remove(groupName);
            stopped = true;
            synchronized (closeLock) {
                closeLock.notify();
            }
        }
    }

    /**
     * Starts the ClusterManager and all it's services
     */
    public synchronized void start() {
        if (!started) {
            masterNode.start();
            healthMonitor.start();
            started = true;
            stopped = false;

            try {
                inputPipe = pipeService.createInputPipe(pipeAdv, this);
            } catch (IOException ioe) {
                LOG.log(Level.SEVERE, "Failed to create service input pipe: " + ioe);
            }
        }
    }

    /**
     * Returns the NetworkManager instance
     *
     * @return The networkManager value
     */
    public NetworkManager getNetworkManager() {
        return netManager;
    }

    /**
     * Returns the MasterNode instance
     *
     * @return The masterNode value
     */
    public MasterNode getMasterNode() {
        return masterNode;
    }

    /**
     * Returns the HealthMonitor instance.
     *
     * @return The healthMonitor value
     */
    public HealthMonitor getHealthMonitor() {
        return healthMonitor;
    }

    /**
     * Returns the ClusterViewManager object.  All modules use this common object which
     * represents a synchronized view of a set of AppServers
     *
     * @return The clusterViewManager object
     */
    public ClusterViewManager getClusterViewManager() {
        return clusterViewManager;
    }

    /**
     * Gets the foundation Peer Group of the ClusterManager
     *
     * @return The netPeerGroup value
     */
    public PeerGroup getNetPeerGroup() {
        return netManager.getNetPeerGroup();
    }

    /**
     * Gets the instance name
     *
     * @return The instance name
     */
    public String getInstanceName() {
        return instanceName;
    }

    public boolean isMaster() {
        return clusterViewManager.isMaster() && masterNode.isMasterAssigned();
    }

    /**
     * Ensures the ClusterManager continues to run.
     */
    private void waitForClose() {
        try {
            LOG.log(Level.FINER, "Waiting for close");
            synchronized (closeLock) {
                closeLock.wait();
            }
            stop(false);
            LOG.log(Level.FINER, "Good Bye");
        } catch (InterruptedException e) {
            LOG.log(Level.WARNING, e.getLocalizedMessage());
        }
    }

    /**
     * Send a message to a specific node or the group. In the case where the id
     * is null the message is sent to the entire group
     *
     * @param peerid the node ID
     * @param msg    the message to send
     * @throws java.io.IOException if an io error occurs
     */
    public void send(final ID peerid, final Serializable msg) throws IOException {
        if (!stopping) {
            final Message message = new Message();
            message.addMessageElement(NAMESPACE, sysAdvElement);
            final ByteArrayMessageElement bame =
                    new ByteArrayMessageElement(APPMESSAGE,
                            MimeMediaType.AOS,
                            JxtaUtil.createByteArrayFromObject(msg),
                            null);
            message.addMessageElement(NAMESPACE, bame);

            if (peerid != null) {
                final OutputPipe output;
                if (!pipeCache.containsKey(peerid)) {
                    // Unicast datagram
                    // create a op pipe to the destination peer
                    output = pipeService.createOutputPipe(pipeAdv, Collections.singleton(peerid), 1);
                    pipeCache.put(peerid, output);
                } else {
                    output = pipeCache.get(peerid);
                }
                output.send(message);
            } else {
                // multicast
                LOG.log(Level.FINER, "Broadcasting Message");
                outputPipe.send(message);
            }
            //JxtaUtil.printMessageStats(message, true);
        }
    }


    /**
     * Returns a pipe advertisement for Cluster messaging of propagate type
     *
     * @return a pipe advertisement for Cluster messaging
     */
    private PipeAdvertisement createPipeAdv() {
        final PipeAdvertisement pipeAdv;
        // create the pipe advertisement, to be used in creating the pipe
        pipeAdv = (PipeAdvertisement)
                AdvertisementFactory.newAdvertisement(
                        PipeAdvertisement.getAdvertisementType());
        pipeAdv.setPipeID(getNetworkManager().getAppServicePipeID());
        pipeAdv.setType(PipeService.PropagateType);
        return pipeAdv;
    }

    /**
     * {@inheritDoc}
     */
    public void pipeMsgEvent(final PipeMsgEvent event) {
        if (started && !stopping) {
            final Message msg;
            MessageElement msgElement;
            // grab the message from the event
            try {
                msg = event.getMessage();
                if (msg == null) {
                    LOG.log(Level.WARNING, "Received a null message");
                    return;
                }
                //JxtaUtil.printMessageStats(msg, true);
                LOG.log(Level.FINEST, "ClusterManager:Received a AppMessage ");
                msgElement = msg.getMessageElement(NAMESPACE, NODEADV);
                if (msgElement == null) {
                    // no need to go any further
                    LOG.log(Level.WARNING, "Received an unknown message");
                    return;
                }

                final SystemAdvertisement adv;
                final StructuredDocument asDoc = StructuredDocumentFactory.newStructuredDocument(msgElement.getMimeType(), msgElement.getStream());
                adv = new SystemAdvertisement(asDoc);
                final PeerID srcPeerID = (PeerID) adv.getID();
                if (!loopbackMessages) {
                    if (srcPeerID.equals(myID)) {
                        LOG.log(Level.FINEST, "CLUSTERMANAGER:Discarding loopback message");
                        return;
                    }
                }

                msgElement = msg.getMessageElement(NAMESPACE, APPMESSAGE);
                if (msgElement != null) {
                    final Object appMessage = getObjectFromByteArray(msgElement);
                    if (appMessage != null) {
                        LOG.log(Level.FINEST, "ClusterManager: Notifying APPMessage Listeners");
                        notifyMessageListeners(adv, appMessage);
                    }
                }
            } catch (Throwable e) {
                LOG.log(Level.WARNING, e.getLocalizedMessage());
            }
        }
    }

    private void notifyMessageListeners(final SystemAdvertisement senderSystemAdvertisement, final Object appMessage) {
        for (ClusterMessageListener listener : cmListeners) {
            listener.handleClusterMessage(senderSystemAdvertisement, appMessage);
        }
    }

    public SystemAdvertisement getSystemAdvertisementForMember(final ID id) {
        return clusterViewManager.get(id);
    }

    /**
     * Gets the systemAdvertisement attribute of the JXTAPlatform object
     *
     * @return The systemAdvertisement value
     */
    public SystemAdvertisement getSystemAdvertisement() {
        if (systemAdv == null) {
            systemAdv = createSystemAdv(netManager.getNetPeerGroup(), instanceName, identityMap);
        }
        return systemAdv;
    }

    public PeerID getNodeID() {
        return myID;
    }

    /**
     * Given a peergroup and a SystemAdvertisement is returned
     *
     * @param group      peer group, used to obtain peer id
     * @param name       host name
     * @param customTags A Map object. These are additional system identifiers
     *                   that the application layer can provide for its own
     *                   identification.
     * @return SystemAdvertisement object
     */
    private static synchronized SystemAdvertisement createSystemAdv(final PeerGroup group,
                                                                    final String name,
                                                                    final Map<String, String> customTags) {
        final SystemAdvertisement sysAdv = new SystemAdvertisement();
        sysAdv.setID(group.getPeerID());
        sysAdv.setName(name);
        try {
            sysAdv.setIP(InetAddress.getLocalHost().getHostAddress());
        }
        catch (UnknownHostException ignored) {
            LOG.log(Level.WARNING, "Failed to obtain IP address :" + ignored);
        }
        sysAdv.setOSName(System.getProperty("os.name"));
        sysAdv.setOSVersion(System.getProperty("os.version"));
        sysAdv.setOSArch(System.getProperty("os.arch"));
        sysAdv.setHWArch(System.getProperty("HOSTTYPE", System.getProperty("os.arch")));
        sysAdv.setHWVendor(System.getProperty("java.vm.vendor"));
        sysAdv.setCustomTags(customTags);
        return sysAdv;
    }

    public String getNodeState(final ID address) {
        return getHealthMonitor().getState(address);
    }

    /**
     * Returns name encoded ID
     * @param name to name to encode
     * @return name encoded ID
     */
    public ID getID(final String name) {
        return netManager.getPeerID(name);
    }

    boolean isStopping(){
        return stopping;
    }
}

