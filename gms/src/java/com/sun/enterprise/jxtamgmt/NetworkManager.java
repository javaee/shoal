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
import net.jxta.document.XMLDocument;
import net.jxta.document.MimeMediaType;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.logging.Logging;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.NetPeerGroupFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.peergroup.WorldPeerGroupFactory;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.ConfigParams;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.rendezvous.RendezvousEvent;
import net.jxta.rendezvous.RendezvousListener;
import net.jxta.impl.peergroup.StdPeerGroupParamAdv;
import net.jxta.impl.endpoint.mcast.McastTransport;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URI;

import static com.sun.enterprise.jxtamgmt.JxtaConfigConstants.*;

/**
 * NetworkManager wraps the JXTA plaform lifecycle into a single object. Using the
 * instance name, it encodes a node Peer ID, and provides utilities to derive Peer ID's
 * from an instance name.  Given an instance name, this allows any node to
 * independently interpolate a Peer ID.
 *
 * TODO:REVISIT FOR REFACTORING AND ADDED REQUIRMENTS.
 * TODO: WHEN SPECIFYING INSTANCENAME IN EACH METHOD, IS IT THE INTENTION THAT THE CONSUMING APP COULD POTENTIALLY
 * TODO: PROVIDE DIFFERENT INSTANCE NAMES AT DIFFERENT TIMES DURING A GIVEN LIFETIME OF THE APP? WHAT IMPACT WOULD THERE
 * TODO:BE IF WE REMOVE INSTANCENAME FROM THE PARAMETERS OF THESE METHODS AND BASE INSTANCE NAME FROM THE CONSTRUCTOR'S
 * TODO: CONSTRUCTION FROM PROPERTIES OBJECT?
 */
public class NetworkManager implements RendezvousListener {

    private static final Logger LOG = JxtaUtil.getLogger();
    private static MessageDigest digest;
    private PeerGroup netPeerGroup;
    private boolean started = false;
    private boolean stopped = false;
    private RendezVousService rendezvous;
    private String groupName = "defaultGroup";
    private String instanceName;
    private static final String PREFIX = "SHOAL";
    private static final String networkConnectLock = "networkConnectLock";
    private static final File home = new File(System.getProperty("JXTA_HOME", ".shoal"));
    private final PipeID socketID;
    private final PipeID pipeID;
    private static WorldPeerGroupFactory wpgf;

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
    private String mcastAddress;
    private int mcastPort = 0;
    private List<String> rendezvousSeedURIs = new ArrayList<String>();
    private boolean isRendezvousSeed = false;
    private String tcpAddress;

    /**
     * NetworkManager provides a simple interface to configuring and managing the lifecycle
     * of the JXTA platform.  In addition, it provides logical addressing by means of name
     * encoded PeerID, and communication channels.  This allows the application to utilize simple
     * addressing.  Therefore it is key that these names are chosen carefully to avoid collision.
     *
     * @param groupName    Group Name, a logical group name that this peer is part of.
     * @param name Instance Name, a logical name for this peer.
     * @param properties   a Properties object that would contain every configuration
     *                     element that the employing application wants to specify values for. The
     *                     keys in this object must correspond to the constants specified in the
     *                     JxtaConfigConstants enum.
     */
    NetworkManager(final String groupName,
                   final String name,
                   final Map properties) {
        System.setProperty(Logging.JXTA_LOGGING_PROPERTY, Level.OFF.toString());
        this.groupName = groupName;
        this.instanceName = name;

        socketID = getSocketID(instanceName);
        pipeID = getPipeID(instanceName);
        if (properties != null && !properties.isEmpty()) {
            final String ma = (String) properties.get(MULTICASTADDRESS.toString());
            if (ma != null) {
                mcastAddress = ma;
            }
            final Object mp = properties.get(MULTICASTPORT.toString());
            if (mp != null) {
                if (mp instanceof String) {
                    mcastPort = Integer.parseInt((String) mp);
                } else if (mp instanceof Integer) {
                    mcastPort = (Integer) mp;
                }
            }
            final Object virtualMulticastURIList = properties.get(VIRTUAL_MULTICAST_URI_LIST.toString());
            if (virtualMulticastURIList != null) {
                //if this object has multiple addresses that are comma separated
                if (((String) virtualMulticastURIList).indexOf(",") > 0) {
                    String addresses[] = ((String) virtualMulticastURIList).split(",");
                    if (addresses.length > 0) {
                        rendezvousSeedURIs = Arrays.asList(addresses);
                    }
                } else {
                    //this object has only one address in it, so add it to the list
                    rendezvousSeedURIs.add(((String) virtualMulticastURIList));
                }
            }
            Object isVirtualMulticastNode = properties.get(IS_BOOTSTRAPPING_NODE.toString());
            if (isVirtualMulticastNode != null) {
                isRendezvousSeed = Boolean.parseBoolean((String) isVirtualMulticastNode);
                LOG.fine("isRendezvousSeed is set to " + isRendezvousSeed);
            }

            tcpAddress = (String)properties.get(BIND_INTERFACE_ADDRESS.toString());
        }
        try {
            initWPGF(home.toURI(), instanceName);
        } catch (PeerGroupException e) {
            LOG.log(Level.SEVERE, e.getLocalizedMessage());
        }


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
    PipeID getPipeID(String instanceName) {
        String seed = instanceName + PIPESEED;
        return IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, hash(seed.toUpperCase()));
    }

    /**
     * Given a instance name, it returns a name encoded PipeID to for binding JxtaSockets.
     *
     * @param instanceName instance name value
     * @return The scoket PipeID value
     */
    PipeID getSocketID(final String instanceName) {
        final String seed = instanceName + SOCKETSEED;
        return IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, hash(seed.toUpperCase()));
    }

    /**
     * Given a instance name, it returns a name encoded PeerID to for binding to specific instance.
     *
     * @param instanceName instance name value
     * @return The peerID value
     */
    PeerID getPeerID(final String instanceName) {
        return IDFactory.newPeerID(getInfraPeerGroupID(), hash(PREFIX + instanceName.toUpperCase()));
    }

    /**
     * Given a instance name, it returns a name encoded PeerID to for binding to specific instance.
     *
     * @param groupName instance name value
     * @return The peerID value
     */
    PeerGroupID getPeerGroupID(final String groupName) {
        if (mcastAddress == null && mcastPort <= 0) {
            return IDFactory.newPeerGroupID(PeerGroupID.defaultNetPeerGroupID,
                    hash(PREFIX + groupName.toUpperCase()));
        } else {
            return IDFactory.newPeerGroupID(PeerGroupID.defaultNetPeerGroupID,
                    hash(PREFIX + groupName.toUpperCase() + mcastAddress + mcastPort));
        }
    }

    /**
     * Returns the HealthMonitor PipeID, used for health monitoring purposes.
     *
     * @return The HealthPipe Pipe ID
     */
    PipeID getHealthPipeID() {
        return IDFactory.newPipeID(getInfraPeerGroupID(), hash(HEALTHSEED.toUpperCase()));
    }

    /**
     * Returns the MasterNode protocol PipeID. used for dynamic organization of nodes.
     *
     * @return The MasterPipe PipeID
     */
    PipeID getMasterPipeID() {
        return IDFactory.newPipeID(getInfraPeerGroupID(), hash(MASTERSEED.toUpperCase()));
    }

    /**
     * Returns the SessionQeuryPipe ID. Used to query for a session replication
     *
     * @return The SessionQuery PipeID
     */
    PipeID getSessionQueryPipeID() {
        return IDFactory.newPipeID(getInfraPeerGroupID(), hash(SESSIONQUERYSEED.toUpperCase()));
    }

    /**
     * Returns the Pipe ID that will be used for application layer to
     * send and receive messages.
     *
     * @return The Application Service Pipe ID
     */
    PipeID getAppServicePipeID() {
        return IDFactory.newPipeID(getInfraPeerGroupID(), hash(APPSERVICESEED.toUpperCase()));
    }

    /**
     * Simple utility to create a pipe advertisement
     *
     * @param instanceName pipe name
     * @return PipeAdvertisement of type Unicast, and of name instanceName
     */
    private PipeAdvertisement getTemplatePipeAdvertisement(final String instanceName) {
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
    PipeAdvertisement getSocketAdvertisement(final String instanceName) {
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
    PipeAdvertisement getPipeAdvertisement(final String instanceName) {
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
    PeerGroupID getInfraPeerGroupID() {
        return getPeerGroupID(groupName);
    }

    /**
     * Creates and starts the JXTA NetPeerGroup using a platform configuration
     * template. This class also registers a listener for rendezvous events
     *
     * @throws IOException        if an io error occurs during creation of the cache directory
     * @throws PeerGroupException if the NetPeerGroup fails to initialize
     */
    synchronized void start() throws PeerGroupException, IOException {
        if (started) {
            return;
        }

        final File userHome = new File(home, instanceName);
        clearCache(userHome);
        startDomain();
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
    synchronized void stop() {
        if (stopped && !started) {
            return;
        }
        try {
            rendezvous.removeListener(this);
            netPeerGroup.stopApp();
            netPeerGroup.unref();
            netPeerGroup = null;
            final File userHome = new File(home, instanceName);
            clearCache(userHome);
        } catch (Throwable th) {
            LOG.log(Level.FINEST, th.getLocalizedMessage());
        }
        stopped = true;
        started = false;
    }

    /**
     * Returns the netPeerGroup instance for this Cluster.
     *
     * @return The netPeerGroup value
     */
    PeerGroup getNetPeerGroup() {
        return netPeerGroup;
    }

    /**
     * Blocks until a connection to rendezvous node occurs. Returns immediately
     * if a connection is already established.  This is useful to ensure the widest
     * network exposure.
     *
     * @param timeout timeout in milliseconds
     * @return connection state
     */
    boolean waitForRendezvousConnection(long timeout) {
        if (0 == timeout) {
            timeout = Long.MAX_VALUE;
        }

        long timeoutAt = System.currentTimeMillis() + timeout;

        if (timeoutAt <= 0) {
            // handle overflow.
            timeoutAt = Long.MAX_VALUE;
        }

        while (started && !stopped && !rendezvous.isConnectedToRendezVous() && !rendezvous.isRendezVous()) {
            LOG.fine("rendezvous.isRendezVous() = " + rendezvous.isRendezVous() +
                    "rendezvous.isConnectedToRendezVous() = " + rendezvous.isConnectedToRendezVous());
            try {
                long waitFor = timeoutAt - System.currentTimeMillis();

                if (waitFor > 0) {
                    synchronized (networkConnectLock) {
                        networkConnectLock.wait(timeout);
                    }
                } else {
                    // all done with waiting.
                    break;
                }
            } catch (InterruptedException e) {
                Thread.interrupted();
                break;
            }
        }
        LOG.fine("outside while loop -> rendezvous.isRendezVous() = " + rendezvous.isRendezVous() +
                "rendezvous.isConnectedToRendezVous() = " + rendezvous.isConnectedToRendezVous());
        return rendezvous.isConnectedToRendezVous() || rendezvous.isRendezVous();
    }

    /**
     * {@inheritDoc}
     */
    public void rendezvousEvent(RendezvousEvent event) {
        if (event.getType() == RendezvousEvent.RDVCONNECT || event.getType() == RendezvousEvent.RDVRECONNECT
                || event.getType() == RendezvousEvent.BECAMERDV) {
            synchronized (networkConnectLock) {
                networkConnectLock.notifyAll();
            }
        }
    }

    /**
     * Gets this instance's PeerID
     *
     * @return The peerID value
     */
    PeerID getPeerID() {
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
    boolean isStarted() {
        return !stopped && started;
    }

    /**
     * Returns this instance name encoded SocketID.
     *
     * @return The socketID value
     */
    PipeID getSocketID() {
        return socketID;
    }

    /**
     * Returns this instance name encoded PipeID.
     *
     * @return The pipeID value
     */
    PipeID getPipeID() {
        return pipeID;
    }

    /**
     * Returns this instance name.
     *
     * @return The instance name value
     */
    String getInstanceName() {
        return instanceName;
    }

    /**
     * Returns the home directory. An instance home directory is used to persists an instance configuration and cache.
     *
     * @return The instance name value
     */
    File getHome() {
        return home;
    }


    /**
     * Configure and start the World Peer Group Factory
     *
     * @param storeHome    The location JXTA will use to store all persistent data.
     * @param instanceName The name of the peer.
     * @throws PeerGroupException Thrown for errors creating the world peer group.
     */
    private void initWPGF(URI storeHome, String instanceName) throws PeerGroupException {
        synchronized (NetworkManager.class) {
            if (null == wpgf) {
                NetworkConfigurator worldGroupConfig = NetworkConfigurator.newAdHocConfiguration(storeHome);

                PeerID peerid = getPeerID(instanceName);
                worldGroupConfig.setName(instanceName);
                worldGroupConfig.setPeerID(peerid);
                // Disable multicast because we will be using a separate multicast in each group.
                worldGroupConfig.setUseMulticast(false);
                ConfigParams config =  worldGroupConfig.getPlatformConfig();
                // Instantiate the world peer group factory.
                wpgf = new WorldPeerGroupFactory(config, storeHome);
            }
        }
    }

    /**
     * Configure and start a separate top-level JXTA domain.
     *
     * @return the net peergroup
     * @throws PeerGroupException Thrown for errors creating the domain.
     */
    private PeerGroup startDomain() throws PeerGroupException {
        final File userHome = new File(home, instanceName);
        clearCache(userHome);
        // Configure the peer name
        final NetworkConfigurator config;
        if (isRendezvousSeed && rendezvousSeedURIs.size() > 0) {
            config = new NetworkConfigurator(NetworkConfigurator.RDV_NODE + NetworkConfigurator.RELAY_NODE, userHome.toURI());
            //TODO: Need to figure out this process's seed addr from the list so that the right port can be bound to
            //For now, we only pick the first seed URI's port and let the other members be non-seeds even if defined in the list.
            String myPort = rendezvousSeedURIs.get(0);
            LOG.fine("myPort is " + myPort);
            myPort = myPort.substring(myPort.lastIndexOf(":") + 1, myPort.length());
            LOG.fine("myPort is " + myPort);
            //TODO: Add a check for port availability and consequent actions
            config.setTcpPort(Integer.parseInt(myPort));
        } else {
            config = new NetworkConfigurator(NetworkConfigurator.EDGE_NODE, userHome.toURI());
            config.setTcpStartPort(9701);
            config.setTcpEndPort(9999);
        }

        config.setPeerID(getPeerID(instanceName));
        config.setName(instanceName);
        //config.setPrincipal(instanceName);
        config.setDescription("Created by Jxta Cluster Management NetworkManager");
        config.setInfrastructureID(getInfraPeerGroupID());
        config.setInfrastructureName(groupName);

        LOG.fine("Rendezvous seed?:" + isRendezvousSeed);
        if (!rendezvousSeedURIs.isEmpty()) {
            LOG.fine("Setting Rendezvous seeding uri's to network configurator:" + rendezvousSeedURIs);
            config.setRendezvousSeeds(new HashSet<String>(rendezvousSeedURIs));
            //limit it to configured rendezvous at this point
            config.setUseOnlyRendezvousSeeds(true);
        }

        config.setUseMulticast(true);
        config.setMulticastSize(64 * 1024);
        config.setInfrastructureDescriptionStr(groupName + " Infrastructure Group Name");
        if (mcastAddress != null) {
            config.setMulticastAddress(mcastAddress);
        }
        if (mcastPort > 0) {
            config.setMulticastPort(mcastPort);
        }
        LOG.fine("node config adv = " + config.getPlatformConfig().toString());

        //if a machine has multiple network interfaces,
        //specify which interface the group communication should start on
        if (tcpAddress != null && !tcpAddress.equals("")) {
            config.setTcpInterfaceAddress(tcpAddress);
        }

        PeerGroup worldPG = wpgf.getInterface();

        ModuleImplAdvertisement npgImplAdv;
        try {
            npgImplAdv = worldPG.getAllPurposePeerGroupImplAdvertisement();
            npgImplAdv.setModuleSpecID(PeerGroup.allPurposePeerGroupSpecID);
            StdPeerGroupParamAdv params = new StdPeerGroupParamAdv(npgImplAdv.getParam());
            params.addProto(McastTransport.MCAST_TRANSPORT_CLASSID, McastTransport.MCAST_TRANSPORT_SPECID);
            npgImplAdv.setParam((XMLDocument) params.getDocument(MimeMediaType.XMLUTF8));
        } catch (Exception failed) {
            throw new PeerGroupException("Could not construct domain ModuleImplAdvertisement", failed);
        }

        ConfigParams cfg =  config.getPlatformConfig();
        // Configure the domain
        NetPeerGroupFactory factory = new NetPeerGroupFactory(worldPG, cfg, npgImplAdv);
        netPeerGroup = factory.getInterface();
        rendezvous = netPeerGroup.getRendezVousService();
        rendezvous.addListener(this);
        stopped = false;
        started = true;
        if (!rendezvousSeedURIs.isEmpty()) {
            waitForRendezvousConnection(30000);
        }
        LOG.fine("Connected to the bootstrapping node?: " + (rendezvous.isConnectedToRendezVous() || rendezvous.isRendezVous()));
        return netPeerGroup;
    }
}

