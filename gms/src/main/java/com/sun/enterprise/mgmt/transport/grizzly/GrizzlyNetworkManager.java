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

package com.sun.enterprise.mgmt.transport.grizzly;

import static com.sun.enterprise.mgmt.ConfigConstants.*;
import static com.sun.enterprise.mgmt.transport.grizzly.GrizzlyConfigConstants.*;
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
import com.sun.grizzly.util.DefaultThreadPool;
import com.sun.grizzly.util.SelectorFactory;
import com.sun.grizzly.connectioncache.client.CacheableConnectorHandlerPool;
import com.sun.enterprise.ee.cms.impl.base.PeerID;
import com.sun.enterprise.ee.cms.impl.base.Utility;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SelectionKey;

/**
 * @author Bongjae Chang
 */
public class GrizzlyNetworkManager extends AbstractNetworkManager {

    private static final Logger LOG = GrizzlyUtil.getLogger();

    private final Controller controller = new Controller();
    private final ConcurrentHashMap<String, PeerID<GrizzlyPeerID>> peerIDMap = new ConcurrentHashMap<String, PeerID<GrizzlyPeerID>>();
    private final Map<SelectionKey, String> selectionKeyMap = new ConcurrentHashMap<SelectionKey, String>();

    private volatile boolean running;
    private MessageSender tcpSender;
    private MessageSender udpSender;
    private MulticastMessageSender multicastSender;

    private String host;
    private int tcpPort;
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
    private int maxParallel;
    private long startTimeout; // ms
    private long writeTimeout; // ms
    private int multicastPacketSize;
    private int writeSelectorPoolSize;
    private String virtualUriList;
    private DefaultThreadPool threadPool;
    private ExecutorService multicastSenderThreadPool = null;

    private final ConcurrentHashMap<PeerID, CountDownLatch> pingMessageLockMap = new ConcurrentHashMap<PeerID, CountDownLatch>();

    public static final String MESSAGE_SELECTION_KEY_TAG = "selectionKey";

    public GrizzlyNetworkManager() {
    }

    private void configure( final Map properties ) {
        host = Utility.getStringProperty( BIND_INTERFACE_ADDRESS.toString(), null, properties );
        int tcpStartPort = Utility.getIntProperty( TCPSTARTPORT.toString(), 9090, properties );
        int tcpEndPort = Utility.getIntProperty( TCPENDPORT.toString(), 9120, properties );
        tcpPort = NetworkUtility.getAvailableTCPPort( host, tcpStartPort, tcpEndPort );
        if (LOG.isLoggable(Level.CONFIG)) {
            LOG.config("GrizzlyNetworkManager: TCPSTARTPORT=" + tcpStartPort + " TCPENDPORT=" + tcpEndPort + " tcpport=" + tcpPort);
        }
        multicastPort = Utility.getIntProperty( MULTICASTPORT.toString(), 9090, properties );
        multicastAddress = Utility.getStringProperty( MULTICASTADDRESS.toString(), "230.30.1.1", properties );
        networkInterfaceName = Utility.getStringProperty( BIND_INTERFACE_NAME.toString(), null, properties );
        failTcpTimeout = Utility.getLongProperty( FAILURE_DETECTION_TCP_RETRANSMIT_TIMEOUT.toString(), 10 * 1000, properties );
        maxPoolSize = Utility.getIntProperty( MAX_POOLSIZE.toString(), 50, properties );
        corePoolSize = Utility.getIntProperty( CORE_POOLSIZE.toString(), 20, properties );
        keepAliveTime = Utility.getLongProperty( KEEP_ALIVE_TIME.toString(), 60 * 1000, properties );
        poolQueueSize = Utility.getIntProperty( POOL_QUEUE_SIZE.toString(), 1024 * 4, properties );
        highWaterMark = Utility.getIntProperty( HIGH_WATER_MARK.toString(), 1024, properties );
        numberToReclaim = Utility.getIntProperty( NUMBER_TO_RECLAIM.toString(), 10, properties );
        maxParallel = Utility.getIntProperty( MAX_PARALLEL.toString(), 1, properties );
        startTimeout = Utility.getLongProperty( START_TIMEOUT.toString(), 15 * 1000, properties );
        writeTimeout = Utility.getLongProperty( WRITE_TIMEOUT.toString(), 10 * 1000, properties );
        multicastPacketSize = Utility.getIntProperty( MULTICAST_PACKET_SIZE.toString(), 64 * 1024, properties );
        writeSelectorPoolSize = Utility.getIntProperty( MAX_WRITE_SELECTOR_POOL_SIZE.toString(), 30, properties );
        virtualUriList = Utility.getStringProperty( VIRTUAL_MULTICAST_URI_LIST.toString(), null, properties );
    }

    @SuppressWarnings( "unchecked" )
    public synchronized void initialize( final String groupName, final String instanceName, final Map properties ) throws IOException {
        configure( properties );
        if( localPeerID == null ) {
            String uniqueHost = host;
            if( uniqueHost == null ) {
                // prefer IPv4
                InetAddress firstInetAddress = NetworkUtility.getFirstInetAddress( false );
                if( firstInetAddress == null )
                    firstInetAddress = NetworkUtility.getFirstInetAddress( true );
                if( firstInetAddress == null )
                    throw new IOException( "can not find a first InetAddress" );
                uniqueHost = firstInetAddress.getHostAddress();
            }
            if( uniqueHost == null )
                throw new IOException( "can not find an unique host" );
            localPeerID = new PeerID<GrizzlyPeerID>( new GrizzlyPeerID( uniqueHost, tcpPort, multicastPort ), groupName, instanceName );
            peerIDMap.put( instanceName, localPeerID );
            if( LOG.isLoggable( Level.FINE ) )
                LOG.log( Level.FINE, "local peer id = " + localPeerID );
        }
        InetAddress localInetAddress = null;
        if( host != null )
            localInetAddress = InetAddress.getByName( host );

        threadPool = new DefaultThreadPool( "",
                                                              corePoolSize,
                                                              maxPoolSize,
                                                              keepAliveTime,
                                                              TimeUnit.MILLISECONDS,
                                                              null,
                                                              new ArrayBlockingQueue<Runnable>( poolQueueSize ) );
        threadPool.setInitialByteBufferSize( MessageImpl.MAX_TOTAL_MESSAGE_LENGTH );
        controller.setThreadPool( threadPool );

        ConnectorHandlerPool cacheableHandlerPool = new CacheableConnectorHandlerPool( controller, highWaterMark, numberToReclaim, maxParallel );
        controller.setConnectorHandlerPool( cacheableHandlerPool );

        TCPSelectorHandler tcpSelectorHandler = new ReusableTCPSelectorHandler();
        tcpSelectorHandler.setPort( tcpPort );
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

    @Override
    @SuppressWarnings( "unchecked" )
    public synchronized void start() throws IOException {
        if( running )
            return;
        super.start();
        final CountDownLatch controllerGate = new CountDownLatch( 1 );
        ControllerStateListener controllerStateListener = new ControllerStateListener() {

            public void onStarted() {
            }

            public void onReady() {
                if( LOG.isLoggable( Level.FINER ) )
                    LOG.log( Level.FINER, "GrizzlyNetworkManager is ready" );
                controllerGate.countDown();
            }

            public void onStopped() {
                controllerGate.countDown();
            }

            public void onException( Throwable throwable ) {
                controllerGate.countDown();
            }
        };
        controller.addStateListener( controllerStateListener );
        new Thread( controller ).start();
        try {
            controllerGate.await( startTimeout, TimeUnit.MILLISECONDS );
        } catch( InterruptedException e ) {
            e.printStackTrace();
        }
        tcpSender = new GrizzlyTCPConnectorWrapper( controller, writeTimeout, host, tcpPort, localPeerID );
        GrizzlyUDPConnectorWrapper udpConnectorWrapper = new GrizzlyUDPConnectorWrapper( controller,
                                                                                         writeTimeout,
                                                                                         host,
                                                                                         multicastPort,
                                                                                         multicastAddress,
                                                                                         localPeerID );
        udpSender = udpConnectorWrapper;
        List<PeerID> virtualPeerIdList = getVirtualPeerIDList( virtualUriList );
        if( virtualPeerIdList != null && !virtualPeerIdList.isEmpty() ) {
            multicastSenderThreadPool = new ThreadPoolExecutor( 10, 10, 60 * 1000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>( 1024 ) );
            multicastSender = new VirtualMulticastSender( host,
                                                          multicastAddress,
                                                          multicastPort,
                                                          networkInterfaceName,
                                                          multicastPacketSize,
                                                          localPeerID,
                                                          multicastSenderThreadPool,
                                                          this,
                                                          virtualPeerIdList );
        } else {
            if( GrizzlyUtil.isSupportNIOMulticast() ) {
                multicastSender = udpConnectorWrapper;
            } else {
                multicastSenderThreadPool = new ThreadPoolExecutor( 10, 10, 60 * 1000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>( 1024 ) );
                multicastSender = new BlockingIOMulticastSender( host,
                                                                 multicastAddress,
                                                                 multicastPort,
                                                                 networkInterfaceName,
                                                                 multicastPacketSize,
                                                                 localPeerID,
                                                                 multicastSenderThreadPool,
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
        threadPool.stop();
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
            PeerID previous = peerIDMap.putIfAbsent( instanceName, peerID );
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
            PeerID previous = peerIDMap.putIfAbsent( instanceName, peerID );
            if (previous == null) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("addRemotePeer: " + instanceName + " peerId:" + peerID);
                }
            }
        }
    }

    public void removeRemotePeer( SelectionKey selectionKey ) {
        if( selectionKey == null )
            return;
        String instanceName = selectionKeyMap.remove( selectionKey );
        if( instanceName != null )
            peerIDMap.remove( instanceName );
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
        if( peerID == null )
            peerID = PeerID.NULL_PEER_ID;
        return peerID;
    }

    public void removePeerID( final PeerID peerID ) {
        if( peerID == null )
            return;
        String instanceName = peerID.getInstanceName();
        if( instanceName == null )
            return;
        peerIDMap.remove( instanceName );
        // todo should we remove selection key?
        // selectionKeyMap.remove( key );
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
