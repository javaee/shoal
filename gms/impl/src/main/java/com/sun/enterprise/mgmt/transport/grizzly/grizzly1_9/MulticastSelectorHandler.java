/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.mgmt.transport.grizzly.grizzly1_9;

import com.sun.grizzly.*;
import com.sun.grizzly.util.Copyable;
import com.sun.grizzly.async.UDPAsyncQueueReader;
import com.sun.grizzly.async.UDPAsyncQueueWriter;
import com.sun.enterprise.mgmt.transport.NetworkUtility;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.net.SocketAddress;
import java.net.NetworkInterface;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.BindException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.concurrent.Callable;
import java.lang.reflect.Method;

/**
 * @author Bongjae Chang
 */
public class MulticastSelectorHandler extends ReusableUDPSelectorHandler {

    private volatile InetAddress multicastAddress;
    private volatile NetworkInterface anInterface;

    // todo membership key management
    //private MembershipKey membershipKey;
    private Object membershipKey;

    private final Method joinMethod;

    public MulticastSelectorHandler() {
        try {
            anInterface = NetworkUtility.getFirstNetworkInterface(NetworkUtility.getPreferIpv6Addresses());
        } catch( IOException e ) {
            e.printStackTrace();
        }
        Method method = null;
        try {
            // JDK 1.7
            method = DatagramChannel.class.getMethod( "join", InetAddress.class, NetworkInterface.class );
        } catch( Throwable t ) {
            method = null;
            if( logger.isLoggable( Level.FINER ) ) {
                logger.log( Level.FINER, "this JDK doesn't support DatagramChannel#join()", t );
            }
        }
        joinMethod = method;
    }

    @Override
    public void copyTo( Copyable copy ) {
        super.copyTo( copy );
        MulticastSelectorHandler copyHandler = (MulticastSelectorHandler)copy;
        copyHandler.anInterface = anInterface;
        copyHandler.membershipKey = membershipKey;
    }

    /**
     * Before invoking Selector.select(), make sure the ServerScoketChannel
     * has been created. If true, then register all SelectionKey to the Selector.
     */
    @Override
    public void preSelect( Context ctx ) throws IOException {

        if( asyncQueueReader == null ) {
            asyncQueueReader = new UDPAsyncQueueReader( this );
        }

        if( asyncQueueWriter == null ) {
            asyncQueueWriter = new UDPAsyncQueueWriter( this );
        }

        if( selector == null ) {
            initSelector( ctx );
        } else {
            processPendingOperations( ctx );
        }
    }

    @SuppressWarnings( "unchecked" )
    private void initSelector( Context ctx ) throws IOException {
        try {
            isShutDown.set( false );

            connectorInstanceHandler = new ConnectorInstanceHandler.ConcurrentQueueDelegateCIH( getConnectorInstanceHandlerDelegate() );

            datagramChannel = DatagramChannel.open();
            selector = Selector.open();
            if( role != Role.CLIENT ) {
                datagramSocket = datagramChannel.socket();
                datagramSocket.setReuseAddress( reuseAddress );
                if( inet == null )
                    datagramSocket.bind( new InetSocketAddress( getPort() ) );
                else
                    datagramSocket.bind( new InetSocketAddress( inet, getPort() ) );

                datagramChannel.configureBlocking( false );
                datagramChannel.register( selector, SelectionKey.OP_READ );

                datagramSocket.setSoTimeout( serverTimeout );

                if( multicastAddress != null && joinMethod != null ) {
                    try {
                        membershipKey = joinMethod.invoke( datagramChannel, multicastAddress, anInterface );
                        //membershipKey = datagramChannel.join( multicastAddress, anInterface );
                    } catch( Throwable t ) {
                        if( logger.isLoggable( Level.FINE ) ) {
                            logger.log( Level.FINE, "Exception occured when tried to join datagram channel", t );
                        }
                    }
                }
            }
            ctx.getController().notifyReady();
        } catch( SocketException ex ) {
            throw new BindException( ex.getMessage() + ": " + getPort() );
        }
    }

    public void setMulticastAddress( String multicastAddress ) throws UnknownHostException {
        if( multicastAddress != null )
            this.multicastAddress = InetAddress.getByName( multicastAddress );
    }

    public void setNetworkInterface( String networkInterfaceName ) throws SocketException {
        if( networkInterfaceName != null ) {
            NetworkInterface anInterface = NetworkInterface.getByName( networkInterfaceName );
            if( NetworkUtility.supportsMulticast( anInterface ) )
                this.anInterface = anInterface;
        }
    }

    /**
     * Handle new OP_CONNECT ops.
     */
    @Override
    protected void onConnectOp( Context ctx,
                                TCPSelectorHandler.ConnectChannelOperation connectChannelOp ) throws IOException {
        DatagramChannel newDatagramChannel = (DatagramChannel)connectChannelOp.getChannel();
        SocketAddress remoteAddress = connectChannelOp.getRemoteAddress();
        CallbackHandler callbackHandler = connectChannelOp.getCallbackHandler();

        CallbackHandlerSelectionKeyAttachment attachment = new CallbackHandlerSelectionKeyAttachment( callbackHandler );

        SelectionKey key = newDatagramChannel.register( selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, attachment );
        attachment.associateKey( key );

        try {
            InetAddress remoteInetAddress = InetAddress.getByName( ( (InetSocketAddress)remoteAddress ).getHostName() );
            if( remoteInetAddress.isMulticastAddress() ) {
                if( role == Role.CLIENT && joinMethod != null) {
                    joinMethod.invoke( newDatagramChannel, remoteInetAddress, anInterface );
                    //newDatagramChannel.join( remoteInetAddress, anInterface );
                }
            } else {
                newDatagramChannel.connect( remoteAddress );
            }
        } catch( Throwable t ) {
            if( logger.isLoggable( Level.FINE ) ) {
                logger.log( Level.FINE, "Exception occured when tried to join or connect datagram channel", t );
            }
        }

        onConnectInterest( key, ctx );
    }

    //--------------- ConnectorInstanceHandler -----------------------------
    @Override
    protected Callable<ConnectorHandler> getConnectorInstanceHandlerDelegate() {
        return new Callable<ConnectorHandler>() {
            public ConnectorHandler call() throws Exception {
                return new MulticastConnectorHandler();
            }
        };
    }
}
