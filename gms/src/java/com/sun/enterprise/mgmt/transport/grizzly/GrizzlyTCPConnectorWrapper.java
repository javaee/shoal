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

import com.sun.grizzly.ConnectorHandler;
import com.sun.grizzly.Controller;
import com.sun.grizzly.util.OutputWriter;
import com.sun.enterprise.ee.cms.impl.base.PeerID;
import com.sun.enterprise.mgmt.transport.Message;
import com.sun.enterprise.mgmt.transport.AbstractMessageSender;

import java.io.IOException;
import java.io.Serializable;
import java.net.SocketAddress;
import java.net.InetSocketAddress;

/**
 * @author Bongjae Chang
 */
public class GrizzlyTCPConnectorWrapper extends AbstractMessageSender {

    private final Controller controller;
    private final long writeTimeout; // ms
    private final InetSocketAddress localSocketAddress; // todo not used

    public GrizzlyTCPConnectorWrapper( Controller controller, long writeTimeout, String host, int port, PeerID<GrizzlyPeerID> localPeerID ) {
        this.controller = controller;
        this.writeTimeout = writeTimeout;
        if( host != null )
            this.localSocketAddress = new InetSocketAddress( host, port );
        else
            this.localSocketAddress = null;
        this.localPeerID = localPeerID;
    }

    protected boolean doSend( final PeerID peerID, final Message message ) throws IOException {
        if( peerID == null )
            throw new IOException( "peer ID can not be null" );
        Serializable uniqueID = peerID.getUniqueID();
        SocketAddress remoteSocketAddress;
        if( uniqueID instanceof GrizzlyPeerID ) {
            GrizzlyPeerID grizzlyPeerID = (GrizzlyPeerID)uniqueID;
            remoteSocketAddress = new InetSocketAddress( grizzlyPeerID.getHost(), grizzlyPeerID.getTcpPort() );
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

    private boolean send( SocketAddress remoteAddress, SocketAddress localAddress, Message message ) throws IOException {
        if( controller == null )
            throw new IOException( "grizzly controller must be initialized" );
        if( remoteAddress == null )
            throw new IOException( "remote address can not be null" );
        if( message == null )
            throw new IOException( "message can not be null" );
        ConnectorHandler connectorHandler = null;
        try {
            connectorHandler = controller.acquireConnectorHandler( Controller.Protocol.TCP );
            connectorHandler.connect( remoteAddress, localAddress );
            OutputWriter.flushChannel( connectorHandler.getUnderlyingChannel(), message.getPlainByteBuffer(), writeTimeout );
            //connectorHandler.writeToAsyncQueue( message.getPlainByteBuffer());
        } finally {
            if( connectorHandler != null ) {
                try {
                    connectorHandler.close();
                } catch( IOException e ) {
                    e.printStackTrace();
                }
                controller.releaseConnectorHandler( connectorHandler );
            }
        }
        return true;
    }
}
