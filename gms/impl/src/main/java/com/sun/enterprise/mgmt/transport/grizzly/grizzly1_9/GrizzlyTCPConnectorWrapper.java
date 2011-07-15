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

import com.sun.enterprise.mgmt.transport.*;
import com.sun.enterprise.mgmt.transport.grizzly.GrizzlyNetworkManager;
import com.sun.grizzly.ConnectorHandler;
import com.sun.grizzly.Controller;
import com.sun.grizzly.IOEvent;
import com.sun.grizzly.util.OutputWriter;
import com.sun.enterprise.ee.cms.impl.base.PeerID;
import com.sun.enterprise.mgmt.transport.grizzly.GrizzlyPeerID;
import com.sun.grizzly.AbstractConnectorHandler;
import com.sun.grizzly.CallbackHandler;
import com.sun.grizzly.Context;
import com.sun.grizzly.connectioncache.client.CacheableConnectorHandler;

import java.io.IOException;
import java.io.Serializable;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author Bongjae Chang
 */
public class GrizzlyTCPConnectorWrapper extends AbstractMessageSender {

    private final Logger LOG = GrizzlyNetworkManager.getLogger();
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

        return send(remoteSocketAddress, null, message, peerID);
        }

    @SuppressWarnings("unchecked")
    private boolean send( SocketAddress remoteAddress, SocketAddress localAddress, Message message, PeerID target ) throws IOException {
        final int MAX_RESEND_ATTEMPTS = 4;
        if( controller == null )
            throw new IOException( "grizzly controller must be initialized" );
        if( remoteAddress == null )
            throw new IOException( "remote address can not be null" );
        if( message == null )
            throw new IOException( "message can not be null" );
        ConnectorHandler connectorHandler = null;
        try {
            long startGetConnectorHandler = System.currentTimeMillis();
            connectorHandler = controller.acquireConnectorHandler( Controller.Protocol.TCP );
            long durationGetConnectorHandler = System.currentTimeMillis() - startGetConnectorHandler;
            if (durationGetConnectorHandler > 1000) {
                AbstractNetworkManager.getLogger().log(Level.WARNING,
                                                       "grizzlytcpconnectorwrapper.wait.for.getconnector",
                                                       durationGetConnectorHandler);
            }
            int attemptNo = 1;
            do {
                try {
                    connectorHandler.connect(remoteAddress, localAddress, new CloseControlCallbackHandler(connectorHandler));
                } catch (Throwable t) {

                    // close connectorHandler.
                    try {
                        connectorHandler.close();
                    } catch (Throwable tt) {
                        // ignore
                    }

                    // include local call stack.
                    IOException localIOE = new IOException("failed to connect to " + target.toString(), t);
                     //AbstractNetworkManager.getLogger().log(Level.WARNING, "failed to connect to target " + target.toString(), localIOE);
                    throw localIOE;
                }
                try {
                    OutputWriter.flushChannel(connectorHandler.getUnderlyingChannel(), message.getPlainByteBuffer(), writeTimeout);
                    connectorHandler.close();
                    break;
                } catch (MessageIOException mioe) {
                    // thrown when message size is too big.
                    forceClose(connectorHandler);
                    throw mioe;
                } catch (Exception e) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "exception during the flushChannel call. Retrying with another connection #" + attemptNo, e);
                    }

                    forceClose(connectorHandler);
                }

                attemptNo++;
            } while (attemptNo <= MAX_RESEND_ATTEMPTS);
        } finally {
            controller.releaseConnectorHandler(connectorHandler);
        }

        return true;
    }

    private void forceClose(ConnectorHandler connectorHandler) throws IOException {
        if (connectorHandler instanceof CacheableConnectorHandler) {
            ((CacheableConnectorHandler) connectorHandler).forceClose();
                }
            }

    private static final class CloseControlCallbackHandler
            implements CallbackHandler<Context> {

        private final ConnectorHandler connectorHandler;

        public CloseControlCallbackHandler(ConnectorHandler connectorHandler) {
            this.connectorHandler = connectorHandler;
        }

        @Override
        public void onConnect(IOEvent<Context> ioEvent) {
            SelectionKey key = ioEvent.attachment().getSelectionKey();
            if (connectorHandler instanceof AbstractConnectorHandler) {
                ((AbstractConnectorHandler) connectorHandler).setUnderlyingChannel(
                        key.channel());
            }

                try {
                connectorHandler.finishConnect(key);
                ioEvent.attachment().getSelectorHandler().register(key,
                        SelectionKey.OP_READ);
            } catch (IOException ex) {
                Controller.logger().severe(ex.getMessage());
                }
            }

        @Override
        public void onRead(IOEvent<Context> ioEvent) {
            // We don't expect any read, so if any data comes - we suppose it's "close" notification
            final Context context = ioEvent.attachment();
            final SelectionKey selectionKey = context.getSelectionKey();
            // close the channel
            context.getSelectorHandler().addPendingKeyCancel(selectionKey);
        }

        @Override
        public void onWrite(IOEvent<Context> ioe) {
    }

}
}
