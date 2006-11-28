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

import net.jxta.document.AdvertisementFactory;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.NetPeerGroupFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.rendezvous.RendezvousEvent;
import net.jxta.rendezvous.RendezvousListener;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NetworkManager wraps the JXTA plaform lifecycle into a single object. Using the
 * instance name, it encodes a node Peer ID, and provides utilities to derive Peer ID's
 * from an instance name.  Given an instance name, this allows any node to
 * independently interpolate a Peer ID.
 * TODO:REVISIT FOR REFACTORING AND ADDED REQUIRMENTS.
 * TODO: WHEN SPECIFYING INSTANCENAME IN EACH METHOD, IS IT THE INTENTION THAT THE CONSUMING APP COULD POTENTIALLY PROVIDE DIFFERENT INSTANCE NAMES AT DIFFERENT TIMES DURING A GIVEN LIFETIME OF THE APP? WHAT IMPACT WOULD THERE BE IF WE REMOVE INSTANCENAME FROM THE PARAMETERS OF THESE METHODS AND BASE INSTANCE NAME FROM THE CONSTRUCTOR'S CONSTRUCTION FROM PROPERTIES OBJECT?
 * TODO: Why are most methods public? Should they not be private or package private? Is this exposed to outside callers?
 *
 * @author Mohamed Abdelaziz (hamada)
 *         $Date    December 17, 2005
 */

public class NetworkManager implements RendezvousListener {
    private static final Logger LOG = JxtaUtil.getLogger();
    private static MessageDigest digest;
    private static PeerGroup netPeerGroup;
    private static boolean started = false;
    private static boolean stopped = false;
    private RendezVousService rendezvous;
    private String groupName = "defaultGroup";
    private String instanceName;
    private static final String PREFIX = "SHOAL";
    private static final String connectLock = "connectLock";
    private static final File home = new File(System.getProperty("JXTA_HOME", ".shoal"));

    /**
     * The infrastructure PeerGroup ID.
     */
    public static PeerGroupID INFRAPGID;
    private static PipeID socketID = null;
    private static PipeID pipeID = null;

    /**
     * JxtaSocket Pipe ID seed.
     */
    private static String SOCKETSEED = PREFIX + "socket";
    /**
     * JxtaBiDiPipe Pipe ID seed.
     */
    private static String PIPESEED = PREFIX + "BIDI";
    /**
     * Health Pipe ID seed.
     */
    private static String HEALTHSEED = PREFIX + "HEALTH";
    /**
     * Master Pipe ID seed.
     */
    private static String MASTERSEED = PREFIX + "MASTER";

    private static String SESSIONQUERYSEED = PREFIX + "SESSIONQ";

    private static String APPSERVICESEED = "APPSERVICE";


    /**
     * NetworkManager provides a simple interface to configuring and managing the lifecycle
     * of the JXTA platform.  In addition, it provides logical addressing by means of name
     * encoded PeerID, and communication channels.  This allows the application to utilize simple
     * addressing.  Therefore it is key that these names are chosen carefully to avoid collision.
     *
     * @param groupName    Group Name, a logical group name that this peer is part of.
     * @param instanceName Instance Name, a logical name for this peer.
     * @param properties   a Properties object that would contain every configuration
     *                     element that the employing application wants to specify values for. The
     *                     keys in this object must correspond to the constants specified in the
     *                     JxtaConfigConstants enum.
     */
    public NetworkManager(final String groupName,
                          final String instanceName,
                          final Map properties) {

        this.groupName = groupName;
        this.instanceName = instanceName;
        socketID = getSocketID(instanceName);
        pipeID = getPipeID(instanceName);
        applyPropertiesSettings(properties);
    }

    private void applyPropertiesSettings(final Map properties) {
    }

    /**
     * Returns a SHA1 hash of string.
     *
     * @param expression to hash
     * @return a SHA1 hash of string
     */
    private static byte[] hash(final String expression) {
        byte[] digArray;
        if (expression == null) {
            throw new IllegalArgumentException("Invalid null expression");
        }
        if (digest == null) {
            try {
                digest = MessageDigest.getInstance("SHA1");
            } catch (NoSuchAlgorithmException ex) {
                LOG.log(Level.WARNING, ex.getLocalizedMessage());
            }
        }
        digest.reset();
        try {
            digArray = digest.digest(expression.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException impossible) {
            LOG.log(Level.WARNING, "digestEncoding unsupported:"
                    + impossible.getLocalizedMessage() +
                    ":returning digest with default encoding");
            digArray = digest.digest(expression.getBytes());
        }
        return digArray;
    }

    /**
     * Given a instance name, it returns a name encoded PipeID to for binding JxtaBiDiPipes.
     *
     * @param instanceName instance name
     * @return The pipeID value
     */
    public PipeID getPipeID(String instanceName) {
        String seed = instanceName + PIPESEED;
        return IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, hash(seed.toUpperCase()));
    }

    /**
     * Given a instance name, it returns a name encoded PipeID to for binding JxtaSockets.
     *
     * @param instanceName instance name value
     * @return The scoket PipeID value
     */
    public PipeID getSocketID(final String instanceName) {
        final String seed = instanceName + SOCKETSEED;
        return IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, hash(seed.toUpperCase()));
    }

    /**
     * Given a instance name, it returns a name encoded PeerID to for binding to specific instance.
     *
     * @param instanceName instance name value
     * @return The peerID value
     */
    public PeerID getPeerID(final String instanceName) {
        return IDFactory.newPeerID(getInfraPeerGroupID(),
                hash(PREFIX + instanceName.toUpperCase()));
    }
    /**
     * Given a instance name, it returns a name encoded PeerID to for binding to specific instance.
     *
     * @param groupName instance name value
     * @return The peerID value
     */
    public PeerGroupID getPeerGroupID(final String groupName) {
        return IDFactory.newPeerGroupID(groupName, hash(PREFIX + groupName.toUpperCase()));
    }
    /**
     * Returns the HealthMonitor PipeID, used for health monitoring purposes.
     *
     * @return The HealthPipe Pipe ID
     */
    public PipeID getHealthPipeID() {
        return IDFactory.newPipeID(getInfraPeerGroupID(),
                hash(HEALTHSEED.toUpperCase()));
    }

    /**
     * Returns the MasterNode protocol PipeID. used for dynamic organization of nodes.
     *
     * @return The MasterPipe PipeID
     */
    public PipeID getMasterPipeID() {
        return IDFactory.newPipeID(getInfraPeerGroupID(),
                hash(MASTERSEED.toUpperCase()));
    }

    /**
     * Returns the SessionQeuryPipe ID. Used to query for a session replication
     *
     * @return The SessionQuery PipeID
     */
    public PipeID getSessionQueryPipeID() {
        return IDFactory.newPipeID(getInfraPeerGroupID(),
                hash(SESSIONQUERYSEED.toUpperCase()));
    }

    /**
     * Returns the Pipe ID that will be used for application layer to
     * send and receive messages.
     *
     * @return The Application Service Pipe ID
     */
    public PipeID getAppServicePipeID() {
        return IDFactory.newPipeID(getInfraPeerGroupID(),
                hash(APPSERVICESEED.toUpperCase()));
    }

    /**
     * Simple utility to create a pipe advertisement
     *
     * @param instanceName pipe name
     * @return PipeAdvertisement of type Unicast, and of name instanceName
     */
    private PipeAdvertisement getTemplatePipeAdvertisement(
            final String instanceName) {
        final PipeAdvertisement advertisement = (PipeAdvertisement)
                AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        advertisement.setType(PipeService.UnicastType);
        advertisement.setName(instanceName);
        return advertisement;
    }

    /**
     * Creates a JxtaSocket pipe advertisement with a SHA1 encoded instance name pipe ID.
     *
     * @param instanceName instance name
     * @return a JxtaSocket Pipe Advertisement
     */
    public PipeAdvertisement getSocketAdvertisement(final String instanceName) {
        final PipeAdvertisement advertisement = getTemplatePipeAdvertisement(instanceName);
        advertisement.setPipeID(getSocketID(instanceName));
        return advertisement;
    }

    /**
     * Creates a JxtaBiDiPipe pipe advertisement with a SHA1 encoded instance name pipe ID.
     *
     * @param instanceName instance name
     * @return PipeAdvertisement a JxtaBiDiPipe Pipe Advertisement
     */
    public PipeAdvertisement getPipeAdvertisement(final String instanceName) {
        final PipeAdvertisement advertisement = getTemplatePipeAdvertisement(instanceName);
        advertisement.setPipeID(getPipeID(instanceName));
        return advertisement;
    }

    /**
     * Contructs and returns the Infrastructure PeerGroupID for the ClusterManager.
     * This ensures scoping and isolation of ClusterManager deployments.
     *
     * @return The infraPeerGroupID PeerGroupID
     */
    public PeerGroupID getInfraPeerGroupID() {
        return getPeerGroupID(groupName);
    }

    /**
     * Creates and starts the JXTA NetPeerGroup using a platform configuration
     * template. This class also registers a listener for rendezvous events
     *
     * @throws PeerGroupException Description of the Exception
     * @throws IOException        Description of the Exception
     */
    public synchronized void start() throws PeerGroupException, IOException {
        if (started) {
            return;
        }

        final File userHome = new File(home, instanceName);
        clearCache(userHome);
        //LOG.log(Level.DEBUG,"Starting instance :"+instanceName);
        //ConfigurationFactory.setPeerID(IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID));
        // Configure the peer name
        final NetworkConfigurator config = new NetworkConfigurator();
        config.setHome(userHome);
        config.setPeerID(getPeerID(instanceName));
        config.setName(instanceName);
        //config.setPrincipal(instanceName);
        config.setDescription("Created by Jxta Cluster Management NetworkManager");
        config.setTcpStartPort(9700);
        config.setTcpEndPort(9999);
        config.setInfrastructureID(getInfraPeerGroupID());
        config.setInfrastructureName(groupName);
        config.setInfrastructureDescription(groupName + " Infrastructure Group Name");
        NetPeerGroupFactory factory = new NetPeerGroupFactory(config.getPlatformConfig(), userHome.toURI());
        netPeerGroup = factory.getInterface();
        //hamada: this should only be uncommented for debugging purposes only
        //netPeerGroup.startApp(null);
        rendezvous = netPeerGroup.getRendezVousService();
        rendezvous.addListener(this);
        started = true;
    }

    /**
     * Removes the JXTA CacheManager persistent store.
     *
     * @param rootDir cache directory
     */
    private static void clearCache(final File rootDir) {
        try {
            if (rootDir.exists()) {
                // remove it along with it's content
                File[] list = rootDir.listFiles();
                for (File aList : list) {
                    if (aList.isDirectory()) {
                        clearCache(aList);
                    } else {
                        aList.delete();
                    }
                }
            }
            rootDir.delete();
        } catch (Throwable t) {
            LOG.log(Level.WARNING, "Unable to clear " + rootDir.toString(), t);
        }
    }

    /**
     * Stops the NetworkManager and the JXTA platform.
     */
    public synchronized void stop() {
        if (stopped && !started) {
            return;
        }
        rendezvous.removeListener(this);
        netPeerGroup.stopApp();
        netPeerGroup.unref();
        netPeerGroup = null;
        final File userHome = new File(home, instanceName);
        clearCache(userHome);
        stopped = true;
    }

    /**
     * Returns the netPeerGroup instance for this Cluster.
     *
     * @return The netPeerGroup value
     */
    public PeerGroup getNetPeerGroup() {
        return netPeerGroup;
    }

    /**
     * Blocks until a connection to rendezvous node occurs. Returns immediately
     * if a connection is already established.  This is useful to ensure the widest
     * network exposure.
     *
     * @param timeout timeout in milliseconds
     */
    public void waitForRendezvousConncection(final long timeout) {
        if (!rendezvous.isConnectedToRendezVous()) {
            //System.out.println("Waiting for Rendezvous Connection");
            try {
                if (!rendezvous.isConnectedToRendezVous()) {
                    synchronized (connectLock) {
                        connectLock.wait(timeout);
                    }
                }
            } catch (InterruptedException e) {
                LOG.log(Level.WARNING, e.getLocalizedMessage());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void rendezvousEvent(final RendezvousEvent event) {
        if (event.getType() == RendezvousEvent.RDVCONNECT ||
                event.getType() == RendezvousEvent.RDVRECONNECT ||
                event.getType() == RendezvousEvent.BECAMERDV) {
            synchronized (connectLock) {
                connectLock.notify();
            }
        }
    }

    /**
     * Gets this instance's PeerID
     *
     * @return The peerID value
     */
    public PeerID getPeerID() {
        if (stopped && !started) {
            return null;
        }
        return netPeerGroup.getPeerID();
    }

    /**
     * Determines whether the <code>NetworkManager</code> has started.
     *
     * @return The running value
     */
    public boolean isStarted() {
        return !stopped && started;
    }

    /**
     * Returns this instance name encoded SocketID.
     *
     * @return The socketID value
     */
    public PipeID getSocketID() {
        return socketID;
    }

    /**
     * Returns this instance name encoded PipeID.
     *
     * @return The pipeID value
     */
    public PipeID getPipeID() {
        return pipeID;
    }

    /**
     * Returns this instance name.
     *
     * @return The instance name value
     */
    public String getInstanceName() {
        return instanceName;
    }

    /**
     * Returns the home directory. An instance home directory is used to persists an instance configuration and cache.
     *
     * @return The instance name value
     */
    public File getHome() {
        return home;
    }
}

