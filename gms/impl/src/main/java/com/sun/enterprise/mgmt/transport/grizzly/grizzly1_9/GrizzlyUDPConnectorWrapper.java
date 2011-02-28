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

package com.sun.enterprise.mgmt.transport.grizzly.grizzly1_9;

import com.sun.enterprise.mgmt.transport.MessageIOException;
import com.sun.grizzly.ConnectorHandler;
import com.sun.grizzly.Controller;
import com.sun.grizzly.util.OutputWriter;
import com.sun.enterprise.ee.cms.impl.base.PeerID;
import com.sun.enterprise.mgmt.transport.Message;
import com.sun.enterprise.mgmt.transport.NetworkUtility;
import com.sun.enterprise.mgmt.transport.AbstractMultiMessageSender;
import com.sun.enterprise.mgmt.transport.grizzly.GrizzlyPeerID;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.InetAddress;
import java.nio.channels.DatagramChannel;
import java.util.List;

/**
 * @author Bongjae Chang
 */
public class GrizzlyUDPConnectorWrapper extends AbstractMultiMessageSender {

    private final Controller controller;
    private final long writeTimeout; // ms
    private final InetSocketAddress localSocketAddress;
    private final InetSocketAddress multicastSocketAddress;

    private static final String DEFAULT_MULTICAST_ADDRESS = "230.30.1.1";

    public GrizzlyUDPConnectorWrapper( Controller controller,
                                       long writeTimeout,
                                       String host,
                                       int port,
                                       String multicastAddress,
                                       PeerID<GrizzlyPeerID> localPeerID ) {
        this.controller = controller;
        this.writeTimeout = writeTimeout;
        this.localSocketAddress = host == null ? new InetSocketAddress(port) : new InetSocketAddress( host, port );
        this.multicastSocketAddress = new InetSocketAddress( multicastAddress == null ? DEFAULT_MULTICAST_ADDRESS : multicastAddress, port );
        this.localPeerID = localPeerID;
    }

    protected boolean doSend( final PeerID peerID, final Message message ) throws IOException {
        if( peerID == null )
            throw new IOException( "peer ID can not be null" );
        Serializable uniqueID = peerID.getUniqueID();
        SocketAddress remoteSocketAddress;
        if( uniqueID instanceof GrizzlyPeerID ) {
            GrizzlyPeerID grizzlyPeerID = (GrizzlyPeerID)uniqueID;
            remoteSocketAddress = new InetSocketAddress( grizzlyPeerID.getHost(), grizzlyPeerID.getMulticastPort() );
        } else {
            throw new IOException( "peer ID must be GrizzlyPeerID type" );
        }
        try {
            return send( remoteSocketAddress, null, message );
        } catch( IOException ie ) {
            // once retry
            return send( remoteSocketAddress, null, message );
        }
    }

    protected boolean doBroadcast( final Message message ) throws IOException {
        if( multicastSocketAddress == null )
            throw new IOException( "multicast address can not be null" );
        try {
            return send( multicastSocketAddress, localSocketAddress, message );
        } catch (MessageIOException mioe) {
            throw mioe;
        } catch( IOException ie ) {
            // once retry
            return send( multicastSocketAddress, localSocketAddress, message );
        }
    }

    private boolean send( SocketAddress remoteAddress, SocketAddress localAddress, Message message ) throws IOException {
        if( controller == null )
            throw new IOException( "grizzly controller must be initialized" );
        if( remoteAddress == null )
            throw new IOException( "remote address can not be null" );
        if( message == null )
            throw new IOException( "message can not be null" );
        ConnectorHandler connectorHandler = null;
        try {
            connectorHandler = controller.acquireConnectorHandler( Controller.Protocol.UDP );
            connectorHandler.connect( remoteAddress, localAddress );
            OutputWriter.flushChannel( (DatagramChannel)connectorHandler.getUnderlyingChannel(),
                                       remoteAddress,
                                       message.getPlainByteBuffer(),
                                       writeTimeout );
        } finally {
            if( connectorHandler != null ) {
                try {
                    connectorHandler.close();
                } catch( IOException e ) {}
                controller.releaseConnectorHandler( connectorHandler );
            }
        }
        return true;
    }
}
