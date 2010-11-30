/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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

package com.sun.enterprise.mgmt.transport.grizzly;

import static com.sun.enterprise.mgmt.ConfigConstants.*;
import static com.sun.enterprise.mgmt.transport.grizzly.GrizzlyConfigConstants.*;

import com.sun.enterprise.ee.cms.core.GMSConstants;
import com.sun.enterprise.ee.cms.impl.base.GMSThreadFactory;
import com.sun.enterprise.ee.cms.impl.common.GMSContext;
import com.sun.enterprise.ee.cms.impl.common.GMSContextFactory;
import com.sun.enterprise.ee.cms.impl.common.GMSMonitor;
import com.sun.enterprise.mgmt.transport.AbstractNetworkManager;
import com.sun.enterprise.mgmt.transport.BlockingIOMulticastSender;
import com.sun.enterprise.mgmt.transport.Message;
import com.sun.enterprise.mgmt.transport.MessageEvent;
import com.sun.enterprise.mgmt.transport.MessageImpl;
import com.sun.enterprise.mgmt.transport.MessageSender;
import com.sun.enterprise.mgmt.transport.MulticastMessageSender;
import com.sun.enterprise.mgmt.transport.NetworkUtility;
import com.sun.enterprise.mgmt.transport.VirtualMulticastSender;

import com.sun.grizzly.*;
import com.sun.grizzly.connectioncache.spi.transport.ContactInfo;
import com.sun.grizzly.util.ThreadPoolConfig;
import com.sun.grizzly.util.GrizzlyExecutorService;
import com.sun.grizzly.util.SelectorFactory;
import com.sun.grizzly.connectioncache.client.CacheableConnectorHandlerPool;
import com.sun.enterprise.ee.cms.impl.base.PeerID;
import com.sun.enterprise.ee.cms.impl.base.Utility;
import com.sun.grizzly.connectioncache.spi.transport.ConnectionFinder;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * @author Bongjae Chang
 */
public class GrizzlyNetworkManager extends AbstractNetworkManager {

    // too many protocol warnings/severe when trying to communicate to a stopped/killed member of cluster.
    // only logger to shoal logger when necessary to debug grizzly transport within shoal.  don't leave this way.
    private static final Logger LOG = GrizzlyUtil.getLogger();

    private final Controller controller = new Controller();
    private final ConcurrentHashMap<String, PeerID<GrizzlyPeerID>> peerIDMap = new ConcurrentHashMap<String, PeerID<GrizzlyPeerID>>();
    private final Map<SelectionKey, String> selectionKeyMap = new ConcurrentHashMap<SelectionKey, String>();

    private volatile boolean running;
    private MessageSender tcpSender;
    private MessageSender udpSender;
    private MulticastMessageSender multicastSender;
    private int multicastTimeToLive;


    private String instanceName;
    private String groupName;
    private String host;
    private int tcpPort;
    private int tcpStartPort;
    private int tcpEndPort;
    private int multicastPort;
    private String multicastAddress;
    private String networkInterfaceName;
    private long failTcpTimeout; // ms
    private int maxPoolSize;
    private int corePoolSize;
    private long keepAliveTime; // ms
    private int poolQueueSize;
    private int highWaterMark;
    private int numberToReclaim;
    private int maxParallelSendConnections;
    private long startTimeout; // ms
    private long sendWriteTimeout; // ms
    private int multicastPacketSize;
    private int writeSelectorPoolSize;
    private String virtualUriList;
    private GrizzlyExecutorService execService;
    private ExecutorService multicastSenderThreadPool = null;
    private TCPSelectorHandler tcpSelectorHandler = null;
    final private String DEFAULT_MULTICAST_ADDRESS = "230.30.1.1";

    private final ConcurrentHashMap<PeerID, CountDownLatch> pingMessageLockMap = new ConcurrentHashMap<PeerID, CountDownLatch>();

    public static final String MESSAGE_SELECTION_KEY_TAG = "selectionKey";

    public GrizzlyNetworkManager() {
    }

    private boolean validMulticastAddress(String multicastAddr) {
        InetAddress validateMulticastAddress = null;
        try {
            validateMulticastAddress = InetAddress.getByName(multicastAddress);
        } catch (UnknownHostException e) { }

        return validateMulticastAddress != null && validateMulticastAddress.isMulticastAddress();
    }

    private void configure( final Map properties ) {
        Logger shoalLogger = getLogger();
        GrizzlyUtil.setLogger(LOG);
        host = Utility.getStringProperty( BIND_INTERFACE_ADDRESS.toString(), null, properties );
        tcpStartPort = Utility.getIntProperty( TCPSTARTPORT.toString(), 9090, properties );
        tcpEndPort = Utility.getIntProperty( TCPENDPORT.toString(), 9200, properties );

        // allow grizzly to select port from port range. Grizzly will keep hold of port,
        // preventing other gms clients running at same time from picking same port.
        // tcpPort = NetworkUtility.getAvailableTCPPort( host, tcpStartPort, tcpEndPort );

        multicastPort = Utility.getIntProperty( MULTICASTPORT.toString(), 9090, properties );
        multicastAddress = Utility.getStringProperty( MULTICASTADDRESS.toString(), DEFAULT_MULTICAST_ADDRESS, properties );
        if (!validMulticastAddress(multicastAddress)) {
            shoalLogger.log(Level.SEVERE, "grizzlynetmgr.invalidmcastaddr",
                    new Object[]{multicastAddress, DEFAULT_MULTICAST_ADDRESS});
            multicastAddress = DEFAULT_MULTICAST_ADDRESS;
        }
        if (host != null) {
            try {
                InetAddress inetAddr = InetAddress.getByName(host);
                NetworkInterface ni;
                ni = NetworkInterface.getByInetAddress(inetAddr);
                if (ni != null) {
                    networkInterfaceName = ni.getName();
                }
            } catch (SocketException ex) {
                shoalLogger.log(Level.WARNING, "grizzlynetmgr.invalidbindaddr", new Object[]{ex.getLocalizedMessage()});
            } catch (UnknownHostException ex) {
                shoalLogger.log(Level.WARNING, "grizzlynetmgr.invalidbindaddr", new Object[]{ex.getLocalizedMessage()});
            }
        }
        failTcpTimeout = Utility.getLongProperty( FAILURE_DETECTION_TCP_RETRANSMIT_TIMEOUT.toString(), 10 * 1000, properties );
        maxPoolSize = Utility.getIntProperty( MAX_POOLSIZE.toString(), 50, properties );
        corePoolSize = Utility.getIntProperty( CORE_POOLSIZE.toString(), 20, properties );
        keepAliveTime = Utility.getLongProperty( KEEP_ALIVE_TIME.toString(), 60 * 1000, properties );
        poolQueueSize = Utility.getIntProperty( POOL_QUEUE_SIZE.toString(), 1024 * 4, properties );
        highWaterMark = Utility.getIntProperty( HIGH_WATER_MARK.toString(), 1024, properties );
        numberToReclaim = Utility.getIntProperty( NUMBER_TO_RECLAIM.toString(), 10, properties );
        maxParallelSendConnections = Utility.getIntProperty( MAX_PARALLEL.toString(), 15, properties );
        startTimeout = Utility.getLongProperty( START_TIMEOUT.toString(), 15 * 1000, properties );
        sendWriteTimeout = Utility.getLongProperty( WRITE_TIMEOUT.toString(), 10 * 1000, properties );
        multicastPacketSize = Utility.getIntProperty( MULTICAST_PACKET_SIZE.toString(), 64 * 1024, properties );
        multicastTimeToLive = Utility.getIntProperty(MULTICAST_TIME_TO_LIVE.toString(),
                                      GMSConstants.DEFAULT_MULTICAST_TIME_TO_LIVE, properties);
        writeSelectorPoolSize = Utility.getIntProperty( MAX_WRITE_SELECTOR_POOL_SIZE.toString(), 30, properties );
        virtualUriList = Utility.getStringProperty( VIRTUAL_MULTICAST_URI_LIST.toString(), null, properties );
        if (shoalLogger.isLoggable(Level.CONFIG)) {
            String multicastTTLresults = multicastTimeToLive == GMSConstants.DEFAULT_MULTICAST_TIME_TO_LIVE ?
                    " default" : Integer.toString(multicastTimeToLive);
            StringBuffer buf = new StringBuffer(256);
            buf.append("\nGrizzlyNetworkManager Configuration\n");
            buf.append("BIND_INTERFACE_ADDRESS:").append(host).append("  NetworkInterfaceName:").append(networkInterfaceName).append('\n');
            buf.append("TCPSTARTPORT..TCPENDPORT:").append(tcpStartPort).append("..").append(tcpEndPort).append('\n');
            buf.append("MULTICAST_ADDRESS:MULTICAST_PORT:").append(multicastAddress).append(':').append(multicastPort)
                     .append(" MULTICAST_PACKET_SIZE:").append(multicastPacketSize)
                     .append(" MULTICAST_TIME_TO_LIVE:").append(multicastTTLresults).append('\n');
            buf.append("FAILURE_DETECT_TCP_RETRANSMIT_TIMEOUT(ms):").append(failTcpTimeout).append('\n');
            buf.append("ThreadPool CORE_POOLSIZE:").append(corePoolSize).
                    append(" MAX_POOLSIZE:").append(maxPoolSize).
                    append(" POOL_QUEUE_SIZE:").append(poolQueueSize).
                    append(" KEEP_ALIVE_TIME(ms):").append(keepAliveTime).append('\n');
            buf.append("HIGH_WATER_MARK:").append(highWaterMark).append(" NUMBER_TO_RECLAIM:").append(numberToReclaim)
                .append(" MAX_PARALLEL:").append(maxParallelSendConnections).append('\n');
            buf.append("START_TIMEOUT(ms):").append(startTimeout).append(" WRITE_TIMEOUT(ms):").append(sendWriteTimeout).append('\n');
            buf.append("MAX_WRITE_SELECTOR_POOL_SIZE:").append(writeSelectorPoolSize).append('\n');
            buf.append("VIRTUAL_MULTICAST_URI_LIST:").append(virtualUriList).append('\n');
            shoalLogger.log(Level.CONFIG, buf.toString());
        }
    }

    @SuppressWarnings( "unchecked" )
    public synchronized void initialize( final String groupName, final String instanceName, final Map properties ) throws IOException {
        super.initialize(groupName, instanceName, properties);
        this.instanceName = instanceName;
        this.groupName = groupName;
        configure( properties );
        GMSContext ctx = GMSContextFactory.getGMSContext(groupName);
        if (ctx != null)  {
            GMSMonitor monitor = ctx.getGMSMonitor();
            if (monitor != null) {
                monitor.setSendWriteTimeout(this.sendWriteTimeout);
            }
        }

        // moved setting of localPeerId.

        InetAddress localInetAddress = null;
        if( host != null ) {
            localInetAddress = InetAddress.getByName( host );
        }
        ThreadPoolConfig threadPoolConfig = new ThreadPoolConfig("GMS-GrizzlyControllerThreadPool-Group-" + groupName,
                corePoolSize,
                maxPoolSize,
                new ArrayBlockingQueue<Runnable>( poolQueueSize ),
                poolQueueSize,
                keepAliveTime,
                TimeUnit.MILLISECONDS,
                null,
                java.lang.Thread.NORM_PRIORITY, //priority = 5
                null);

        execService = GrizzlyExecutorService.createInstance(threadPoolConfig);
        controller.setThreadPool( execService );

        final CacheableConnectorHandlerPool cacheableHandlerPool =
                new CacheableConnectorHandlerPool( controller, highWaterMark,
                numberToReclaim, maxParallelSendConnections,
                new ConnectionFinder<ConnectorHandler>() {

            @Override
            public ConnectorHandler find(ContactInfo<ConnectorHandler> cinfo,
                    Collection<ConnectorHandler> idleConnections,
                    Collection<ConnectorHandler> busyConnections)
                    throws IOException {

                if (!idleConnections.isEmpty()) {
                    return null;
                }

                return cinfo.createConnection();
            }
        });
        
        controller.setConnectorHandlerPool( cacheableHandlerPool );

        tcpSelectorHandler = new ReusableTCPSelectorHandler();
        tcpSelectorHandler.setPortRange(new PortRange(this.tcpStartPort, this.tcpEndPort));
        tcpSelectorHandler.setSelectionKeyHandler( new GrizzlyCacheableSelectionKeyHandler( highWaterMark, numberToReclaim, this ) );
        tcpSelectorHandler.setInet( localInetAddress );

        controller.addSelectorHandler( tcpSelectorHandler );

        MulticastSelectorHandler multicastSelectorHandler = new MulticastSelectorHandler();
        multicastSelectorHandler.setPort( multicastPort );
        multicastSelectorHandler.setSelectionKeyHandler( new GrizzlyCacheableSelectionKeyHandler( highWaterMark, numberToReclaim, this ) );
        if( GrizzlyUtil.isSupportNIOMulticast() ) {
            multicastSelectorHandler.setMulticastAddress( multicastAddress );
            multicastSelectorHandler.setNetworkInterface( networkInterfaceName );
            multicastSelectorHandler.setInet( localInetAddress );
            controller.addSelectorHandler( multicastSelectorHandler );                      
        }

        ProtocolChainInstanceHandler pciHandler = new DefaultProtocolChainInstanceHandler() {
            @Override
            public ProtocolChain poll() {
                ProtocolChain protocolChain = protocolChains.poll();
                if( protocolChain == null ) {
                    protocolChain = new DefaultProtocolChain();
                    protocolChain.addFilter( GrizzlyMessageProtocolParser.createParserProtocolFilter( null ) );
                    protocolChain.addFilter( new GrizzlyMessageDispatcherFilter( GrizzlyNetworkManager.this ) );
                }
                return protocolChain;
            }
        };
        controller.setProtocolChainInstanceHandler( pciHandler );
        SelectorFactory.setMaxSelectors( writeSelectorPoolSize );
    }

    private final CountDownLatch controllerGate = new CountDownLatch( 1 );
    private boolean controllerGateIsReady = false;
    private Throwable controllerGateStartupException = null;


    @Override
    @SuppressWarnings( "unchecked" )
    public synchronized void start() throws IOException {
        if( running )
            return;
        super.start();

        ControllerStateListener controllerStateListener = new ControllerStateListener() {

            public void onStarted() {
            }

            public void onReady() {
                if( LOG.isLoggable( Level.FINER ) )
                    LOG.log( Level.FINER, "GrizzlyNetworkManager is ready" );
                controllerGateIsReady = true;
                controllerGate.countDown();
            }

            public void onStopped() {
                controllerGate.countDown();
            }

            @Override
            public void onException(Throwable e) {
                if (controllerGate.getCount() > 0) {
                    getLogger().log(Level.SEVERE, "Exception during " +
                            "starting the controller", e);
                    controllerGate.countDown();
                    controllerGateStartupException = e;
                } else {
                    getLogger().log(Level.SEVERE, "Exception during " +
                            "controller processing", e);
                }
            }
        };
        controller.addStateListener( controllerStateListener );
        new Thread( controller ).start();
        long controllerStartTime = System.currentTimeMillis();
        try {
            controllerGate.await( startTimeout, TimeUnit.MILLISECONDS );
        } catch( InterruptedException e ) {
            e.printStackTrace();
        }
        long durationInMillis = System.currentTimeMillis() - controllerStartTime;

        // do not continue if controller did not start.
        if (!controller.isStarted() || !controllerGateIsReady) {
            if (controllerGateStartupException != null) {
                throw new IllegalStateException("Grizzly Controller was not started and ready after " + durationInMillis + " ms",
                        controllerGateStartupException);
            } else {
                throw new IllegalStateException("Grizzly Controller was not started and ready after " + durationInMillis + " ms");

            }
        } else if (controllerGateIsReady) {
            // todo: make this FINE in future.
            getLogger().config("Grizzly controller listening on " + tcpSelectorHandler.getInet() + ":" + tcpSelectorHandler.getPort() + ". Controller started in " + durationInMillis + " ms");
        }

        tcpPort = tcpSelectorHandler.getPort();

        if (localPeerID == null) {
            String uniqueHost = host;
            if (uniqueHost == null) {
                // prefer IPv4
                InetAddress firstInetAddress = NetworkUtility.getFirstInetAddress(false);
                if (firstInetAddress == null)
                    firstInetAddress = NetworkUtility.getFirstInetAddress(true);
                if (firstInetAddress == null)
                    throw new IOException("can not find a first InetAddress");
                uniqueHost = firstInetAddress.getHostAddress();
            }
            if (uniqueHost == null)
                throw new IOException("can not find an unique host");
            localPeerID = new PeerID<GrizzlyPeerID>(new GrizzlyPeerID(uniqueHost, tcpPort, multicastAddress, multicastPort), groupName, instanceName);
            peerIDMap.put(instanceName, localPeerID);
            if (LOG.isLoggable(Level.FINE))
                LOG.log(Level.FINE, "local peer id = " + localPeerID);
        }
        tcpSender = new GrizzlyTCPConnectorWrapper( controller, sendWriteTimeout, host, tcpPort, localPeerID );
        GrizzlyUDPConnectorWrapper udpConnectorWrapper = new GrizzlyUDPConnectorWrapper( controller,
                                                                                         sendWriteTimeout,
                                                                                         host,
                                                                                         multicastPort,
                                                                                         multicastAddress,
                                                                                         localPeerID );
        udpSender = udpConnectorWrapper;
        List<PeerID> virtualPeerIdList = getVirtualPeerIDList( virtualUriList );
        if( virtualPeerIdList != null && !virtualPeerIdList.isEmpty() ) {
            final boolean FAIRNESS = true;
            ThreadFactory tf = new GMSThreadFactory("GMS-mcastSenderThreadPool-thread");

            multicastSenderThreadPool = new ThreadPoolExecutor( 10, 10, 60 * 1000, TimeUnit.MILLISECONDS,
                                                                new ArrayBlockingQueue<Runnable>( 1024, FAIRNESS ), tf);
            multicastSender = new VirtualMulticastSender( host,
                                                          multicastAddress,
                                                          multicastPort,
                                                          networkInterfaceName,
                                                          multicastPacketSize,
                                                          localPeerID,
                                                          multicastSenderThreadPool,
                                                          this,
                                                          multicastTimeToLive,
                                                          virtualPeerIdList );
        } else {
            if( GrizzlyUtil.isSupportNIOMulticast() ) {
                multicastSender = udpConnectorWrapper;
            } else {
                final boolean FAIRNESS = true;
                ThreadFactory tf = new GMSThreadFactory("GMS-McastMsgProcessor-Group-" + groupName + "-thread");                          
                multicastSenderThreadPool = new ThreadPoolExecutor( 10, 10, 60 * 1000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>( 1024, FAIRNESS ), tf );
                multicastSender = new BlockingIOMulticastSender( host,
                                                                 multicastAddress,
                                                                 multicastPort,
                                                                 networkInterfaceName,
                                                                 multicastPacketSize,
                                                                 localPeerID,
                                                                 multicastSenderThreadPool,
                                                                 multicastTimeToLive,
                                                                 this );
            }
        }
        if( tcpSender != null )
            tcpSender.start();
        if( udpSender != null )
            udpSender.start();
        if( multicastSender != null )
            multicastSender.start();
        addMessageListener( new PingMessageListener() );
        addMessageListener( new PongMessageListener() );
        running = true;
    }

    private List<PeerID> getVirtualPeerIDList( String virtualUriList ) {
        if( virtualUriList == null )
            return null;
        LOG.config( "VIRTUAL_MULTICAST_URI_LIST = " + virtualUriList );
        List<PeerID> virtualPeerIdList = new ArrayList<PeerID>();
        //if this object has multiple addresses that are comma separated
        if( virtualUriList.indexOf( "," ) > 0 ) {
            String addresses[] = virtualUriList.split( "," );
            if( addresses.length > 0 ) {
                List<String> virtualUriStringList = Arrays.asList( addresses );
                for( String uriString : virtualUriStringList ) {
                    try {
                        PeerID peerID = getPeerIDFromURI( uriString );
                        if( peerID != null ) {
                            virtualPeerIdList.add( peerID );
                            LOG.config( "VIRTUAL_MULTICAST_URI = " + uriString + ", Converted PeerID = " + peerID );
                        }
                    } catch( URISyntaxException use ) {
                        if( LOG.isLoggable( Level.CONFIG ) )
                            LOG.log( Level.CONFIG, "failed to parse the virtual multicast uri(" + uriString + ")", use );
                    }
                }
            }
        } else {
            //this object has only one address in it, so add it to the list
            try {
                PeerID peerID = getPeerIDFromURI( virtualUriList );
                if( peerID != null ) {
                    virtualPeerIdList.add( peerID );
                    LOG.config( "VIRTUAL_MULTICAST_URI = " + virtualUriList + ", Converted PeerID = " + peerID );
                }
            } catch( URISyntaxException use ) {
                if( LOG.isLoggable( Level.CONFIG ) )
                    LOG.log( Level.CONFIG, "failed to parse the virtual multicast uri(" + virtualUriList + ")", use );
            }
        }
        return virtualPeerIdList;
    }

    private PeerID<GrizzlyPeerID> getPeerIDFromURI( String uri ) throws URISyntaxException {
        if( uri == null )
            return null;
        URI virtualUri = new URI( uri );
        return new PeerID<GrizzlyPeerID>( new GrizzlyPeerID( virtualUri.getHost(),
                                                             virtualUri.getPort(),
                                                             multicastAddress,
                                                             multicastPort ),
                                          localPeerID.getGroupName(),
                                          // the instance name is not meaningless in this case
                                          "Unknown" );
    }

    @Override
    public synchronized void stop() throws IOException {
        if( !running )
            return;
        running = false;
        super.stop();
        if( tcpSender != null )
            tcpSender.stop();
        if( udpSender != null )
            udpSender.stop();
        if( multicastSender != null )
            multicastSender.stop();
        if( multicastSenderThreadPool != null ) {
            multicastSenderThreadPool.shutdown();
        }
        peerIDMap.clear();
        selectionKeyMap.clear();
        pingMessageLockMap.clear();
        controller.stop();
        execService.shutdown();
    }

    protected void beforeDispatchingMessage( MessageEvent messageEvent, Map piggyback ) {
        if( messageEvent == null )
            return;
        SelectionKey selectionKey = null;
        if( piggyback != null ) {
            Object value = piggyback.get( MESSAGE_SELECTION_KEY_TAG );
            if( value instanceof SelectionKey )
                selectionKey = (SelectionKey)value;
        }
        addRemotePeer( messageEvent.getSourcePeerID(), selectionKey );
    }

    protected void afterDispatchingMessage( MessageEvent messageEvent, Map piggyback ) {
    }

    @SuppressWarnings( "unchecked" )
    private void addRemotePeer( PeerID peerID, SelectionKey selectionKey ) {
        if( peerID == null )
            return;
        if( peerID.equals( localPeerID ) )
            return; // lookback
        String instanceName = peerID.getInstanceName();
        if( instanceName != null && peerID.getUniqueID() instanceof GrizzlyPeerID ) {
//            PeerID<GrizzlyPeerID> previous = peerIDMap.get(instanceName);
//            if (previous != null) {
//                if (previous.getUniqueID().getTcpPort() != ((GrizzlyPeerID) peerID.getUniqueID()).tcpPort) {
//                    LOG.log(Level.WARNING, "addRemotePeer(selectionKey): assertion failure: no mapping should have existed for member:"
//                            + instanceName + " existingID=" + previous + " adding peerid=" + peerID, new Exception("stack trace"));
//                }
//            }
            PeerID<GrizzlyPeerID> previous = peerIDMap.put( instanceName, peerID );
            if (previous == null) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("addRemotePeer: " + instanceName + " peerId:" + peerID);
                }
            }
            if( selectionKey != null )
                selectionKeyMap.put( selectionKey, instanceName );
        }
    }

    @SuppressWarnings( "unchecked" )
    public void addRemotePeer( PeerID peerID ) {
        if( peerID == null )
            return;
        if( peerID.equals( localPeerID ) )
            return; // lookback
        String instanceName = peerID.getInstanceName();
        if( instanceName != null && peerID.getUniqueID() instanceof GrizzlyPeerID ) {
//            PeerID<GrizzlyPeerID> previous = peerIDMap.get(instanceName);
//            if (previous != null) {
//                if (previous.getUniqueID().getTcpPort() != ((GrizzlyPeerID) peerID.getUniqueID()).tcpPort) {
//                    LOG.log(Level.WARNING, "addRemotePeer: assertion failure: no mapping should have existed for member:"
//                            + instanceName + " existingID=" + previous + " adding peerid=" + peerID, new Exception("stack trace"));
//                }
//            }
            PeerID<GrizzlyPeerID> previous = peerIDMap.put( instanceName, peerID );
            if (previous == null) {
                Level debugLevel = Level.FINEST;
                if (LOG.isLoggable(debugLevel)) {
                    LOG.log(debugLevel, "addRemotePeer: " + instanceName + " peerId:" + peerID, new Exception("stack trace"));
                }
            }
        }
    }

    public void removeRemotePeer(String instanceName) {
        for (Map.Entry<SelectionKey, String> entry : selectionKeyMap.entrySet()) {
            if (entry.getValue().equals(instanceName)) {
                if (getLogger().isLoggable(Level.FINE)) {
                    getLogger().log(Level.FINE, "remove selection key for instance name: " + entry.getValue() + " selectionKey:" + entry.getKey());
                }
                tcpSelectorHandler.getSelectionKeyHandler().cancel(entry.getKey());
                selectionKeyMap.remove(entry.getKey());
            }
        }
    }

    public void removeRemotePeer( SelectionKey selectionKey ) {
        if(selectionKey == null) {
            return;
        }
        selectionKeyMap.remove(selectionKey);

        // Bug Fix. DO NOT REMOVE member name to peerid mapping when selection key is being removed.
        // THIS HAPPENS TOO FREQUENTLY.  Only remove this mapping when member fails or planned shutdown.\
        // This method was getting called by GrizzlyCacheableSelectionKeyHandler.cancel(SelectionKey).

        // use following line instead of remove call above if uncommenting the rest
//        String instanceName = selectionKeyMap.remove( selectionKey );
//      if( instanceName != null ) {
//          Level level = Level.FINEST;
//          if (LOG.isLoggable(level)) {
//              LOG.log(level, "removeRemotePeer selectionKey=" + selectionKey + " instanceName=" + instanceName,
//                      new Exception("stack trace"));
//          }
//          peerIDMap.remove( instanceName );
//      }
    }

    public boolean send( final PeerID peerID, final Message message ) throws IOException {
        if( !running )
            throw new IOException( "network manager is not running" );
        MessageSender sender = tcpSender;
        if( sender == null )
            throw new IOException( "message sender is not initialized" );
        return sender.send( peerID, message );
    }

    public boolean broadcast( final Message message ) throws IOException {
        if( !running )
            throw new IOException( "network manager is not running" );
        MulticastMessageSender sender = multicastSender;
        if( sender == null )
            throw new IOException( "multicast message sender is not initialized" );
        return sender.broadcast( message );
    }

    public PeerID getPeerID( final String instanceName ) {
        PeerID peerID = null;
        if( instanceName != null )
            peerID = peerIDMap.get( instanceName );
        if( peerID == null ) {
            peerID = PeerID.NULL_PEER_ID;
            if (this.instanceName.equals(instanceName)) {
                LOG.log(Level.FINE, "grizzly.netmgr.localPeerId.null", new Object[]{instanceName});
                LOG.log(Level.FINE, "stack trace", new Exception("stack trace"));
            }
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "getPeerID(" + instanceName + ")" + " returning null peerIDMap=" + peerIDMap);
            }
        }
        return peerID;
    }

    public void removePeerID( final PeerID peerID ) {
        if( peerID == null )
            return;
        String instanceName = peerID.getInstanceName();
        if( instanceName == null )
            return;
        Level debugLevel = Level.FINE;
        if (LOG.isLoggable(debugLevel)) {
            LOG.log(debugLevel, "removePeerID peerid=" + peerID, new Exception("stack trace"));
        }
        peerIDMap.remove( instanceName );
        removeRemotePeer( instanceName );
    }

    public boolean isConnected( final PeerID peerID ) {
        boolean isConnected = false;
        if( peerID != null ) {
            try {
                send( peerID, new MessageImpl( Message.TYPE_PING_MESSAGE ) );
                CountDownLatch latch = new CountDownLatch( 1 );
                CountDownLatch oldLatch = pingMessageLockMap.putIfAbsent( peerID, latch );
                if( oldLatch != null )
                    latch = oldLatch;
                try {
                    isConnected = latch.await( failTcpTimeout, TimeUnit.MILLISECONDS );
                } catch( InterruptedException e ) {
                }
            } catch( Throwable ie ) {
                if( LOG.isLoggable( Level.FINE ) )
                    LOG.log( Level.FINE, "isConnected( " + peerID + " ) = " + isConnected, ie );
                return isConnected;
            } finally {
                pingMessageLockMap.remove( peerID );
            }
            return isConnected;
        } else {
            return isConnected;
        }
    }

    public CountDownLatch getPingMessageLock( PeerID peerID ) {
        if( peerID != null )
            return pingMessageLockMap.get( peerID );
        else
            return null;
    }

    public MessageSender getMessageSender( int transport ) {
        if( running ) {
            MessageSender sender;
            switch( transport ) {
                case TCP_TRANSPORT:
                    sender = tcpSender;
                    break;
                case UDP_TRANSPORT:
                    sender = udpSender;
                    break;
                default:
                    sender = tcpSender;
                    break;
            }
            return sender;
        } else {
            return null;
        }
    }

    public MulticastMessageSender getMulticastMessageSender() {
        if( running )
            return multicastSender;
        else
            return null;
    }
}
