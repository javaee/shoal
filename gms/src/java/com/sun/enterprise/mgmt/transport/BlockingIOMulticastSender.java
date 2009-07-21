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

package com.sun.enterprise.mgmt.transport;

import com.sun.enterprise.ee.cms.impl.base.PeerID;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.net.Inet6Address;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;

/**
 * This class is a default {@link MulticastMessageSender}'s implementation and extends {@link AbstractMulticastMessageSender}
 *
 * This uses <code>MulticastSocket</code> which is based on Blocking I/O
 *
 * @author Bongjae Chang
 */
public class BlockingIOMulticastSender extends AbstractMulticastMessageSender implements Runnable {

    private static final Logger LOG = GMSLogDomain.getLogger( GMSLogDomain.GMS_LOGGER );

    private final InetSocketAddress localSocketAddress;
    private final InetAddress multicastAddress;
    private final int multicastPort;
    private final InetSocketAddress multicastSocketAddress;
    private final Executor executor;
    private final NetworkManager networkManager;

    private NetworkInterface anInterface;
    private int multicastPacketSize;
    private MulticastSocket multicastSocket;
    private Thread multicastThread;

    private volatile boolean running;
    private CountDownLatch endGate = new CountDownLatch( 1 );
    private long shutdownTimeout = 30 * 1000; // ms

    private static final String DEFAULT_MULTICAST_ADDRESS = "230.30.1.1";
    private static final int DEFAULT_MULTICAST_PACKET_SIZE = 16384;

    public BlockingIOMulticastSender( String host,
                                      String multicastAddress,
                                      int multicastPort,
                                      String networkInterfaceName,
                                      int multicastPacketSize,
                                      PeerID localPeerID,
                                      Executor executor,
                                      NetworkManager networkManager ) throws IOException {
        if( host != null ) {
            this.localSocketAddress = new InetSocketAddress( host, multicastPort );
        } else {
            InetAddress firstInetAddress = null;
            InetAddress multicastInetAddress = InetAddress.getByName( multicastAddress );
            // select appropriate IP version type
            if( multicastInetAddress instanceof Inet6Address ) {
                firstInetAddress = NetworkUtility.getFirstInetAddress( true );
            } else {
                firstInetAddress = NetworkUtility.getFirstInetAddress( false );
            }
            if( firstInetAddress != null )
                this.localSocketAddress = new InetSocketAddress( firstInetAddress, multicastPort );
            else
                this.localSocketAddress = null;
        }
        this.multicastPort = multicastPort;
        if( multicastAddress == null )
            multicastAddress = DEFAULT_MULTICAST_ADDRESS;
        this.multicastAddress = InetAddress.getByName( multicastAddress );
        this.multicastSocketAddress = new InetSocketAddress( multicastAddress, multicastPort );
        if( networkInterfaceName != null ) {
            NetworkInterface anInterface = NetworkInterface.getByName( networkInterfaceName );
            if( NetworkUtility.supportsMulticast( anInterface ) )
                this.anInterface = anInterface;
        }
        if( this.anInterface == null )
            this.anInterface = NetworkUtility.getFirstNetworkInterface();
        if( multicastPacketSize < DEFAULT_MULTICAST_PACKET_SIZE )
            this.multicastPacketSize = DEFAULT_MULTICAST_PACKET_SIZE;
        else
            this.multicastPacketSize = multicastPacketSize;
        this.localPeerID = localPeerID;
        this.executor = executor;
        this.networkManager = networkManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void start() throws IOException {
        if( running )
            return;
        super.start();
        if( localSocketAddress != null ) {
            multicastSocket = new MulticastSocket( localSocketAddress.getPort() );
            multicastSocket.setInterface( localSocketAddress.getAddress() );
        } else {
            multicastSocket = new MulticastSocket( multicastPort );
        }
        multicastSocket.setLoopbackMode( false );

        running = true;

        // todo need to use a thread factory?
        multicastThread = new Thread( this, "IP Multicast Listener for " + multicastSocketAddress );
        multicastThread.setDaemon( true );
        multicastThread.start();

        if( anInterface != null )
            multicastSocket.joinGroup( multicastSocketAddress, anInterface );
        else
            multicastSocket.joinGroup( multicastAddress );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void stop() throws IOException {
        if( !running )
            return;
        running = false;
        super.stop();
        if( multicastSocket != null ) {
            try {
                if( anInterface != null )
                    multicastSocket.leaveGroup( multicastSocketAddress, anInterface );
                else
                    multicastSocket.leaveGroup( multicastAddress );
            } catch( IOException e ) {
            }
            multicastSocket.close();
            multicastSocket = null;
        }
        boolean finished = false;
        try {
            finished = endGate.await( shutdownTimeout, TimeUnit.MILLISECONDS );
        } catch( InterruptedException e ) {
        }
        if( !finished && multicastThread != null )
            multicastThread.interrupt();
    }

    public void run() {
        try {
            while( running ) {
                byte[] buffer = new byte[multicastPacketSize];
                DatagramPacket packet = new DatagramPacket( buffer, buffer.length );
                try {
                    multicastSocket.receive( packet );
                    if( !running )
                        return;
                    Runnable processor = new MessageProcessTask( packet );
                    if( executor != null )
                        executor.execute( processor );
                    else
                        processor.run();
                } catch( InterruptedIOException iie ) {
                    Thread.interrupted();
                } catch( IOException e ) {
                    if( !running )
                        break;
                    if( LOG.isLoggable( Level.SEVERE ) )
                        LOG.log( Level.SEVERE, "failure during multicast receive", e );
                    break;
                }
            }
        } catch( Throwable t ) {
            if( LOG.isLoggable( Level.SEVERE ) )
                LOG.log( Level.SEVERE, "Uncaught Throwable in thread :" + Thread.currentThread().getName(), t );
        } finally {
            multicastThread = null;
            endGate.countDown();
        }
    }

    /**
     * {@inheritDoc}
     */
    protected boolean doBroadcast( final Message message ) throws IOException {
        if( !running || multicastSocket == null )
            throw new IOException( "multicast server is not running" );
        if( message == null )
            throw new IOException( "message is null" );
        byte[] messageBytes = message.getPlainBytes();
        int numBytesInPacket = messageBytes.length;
        if( ( numBytesInPacket > multicastPacketSize ) ) {
            if( LOG.isLoggable( Level.WARNING ) )
                LOG.log( Level.WARNING, "Multicast datagram exceeds multicast size" );
        }
        DatagramPacket packet;
        if( localSocketAddress != null ) {
            if( multicastSocketAddress == null )
                throw new IOException( "multicast address can not be null" );
            packet = new DatagramPacket( messageBytes, numBytesInPacket, multicastSocketAddress );
        } else {
            if( multicastAddress == null )
                throw new IOException( "multicast address can not be null" );
            packet = new DatagramPacket( messageBytes, numBytesInPacket, multicastAddress, multicastPort );
        }

        if( multicastSocket != null ) {
            multicastSocket.send( packet );
            return true;
        } else {
            return false;
        }
    }

    private class MessageProcessTask implements Runnable {

        private final DatagramPacket packet;

        public MessageProcessTask( DatagramPacket packet ) {
            this.packet = packet;
        }

        public void run() {
            if( packet == null )
                return;
            try {
                byte[] byteMessage = packet.getData();
                Message message = new MessageImpl();
                try {
                    int messageLen = message.parseHeader( byteMessage, 0 );
                    message.parseMessage( byteMessage, MessageImpl.HEADER_LENGTH, messageLen );
                } catch( IllegalArgumentException iae ) {
                    if( LOG.isLoggable( Level.WARNING ) )
                        LOG.log( Level.WARNING, "damaged multicast discarded", iae );
                    return;
                } catch( MessageIOException mie ) {
                    if( LOG.isLoggable( Level.WARNING ) )
                        LOG.log( Level.WARNING, "damaged multicast discarded", mie );
                    return;
                }
                if( networkManager != null )
                    networkManager.receiveMessage( message, null );
            } catch( Throwable t ) {
                if( LOG.isLoggable( Level.WARNING ) )
                    LOG.log( Level.WARNING, "failed to process a received message", t );
            }
        }
    }
}
