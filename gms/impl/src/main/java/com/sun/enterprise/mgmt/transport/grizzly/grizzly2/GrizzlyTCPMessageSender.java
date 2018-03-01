/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.mgmt.transport.grizzly.grizzly2;

import com.sun.enterprise.mgmt.transport.grizzly.GrizzlyNetworkManager;
import com.sun.enterprise.mgmt.transport.grizzly.GrizzlyPeerID;
import com.sun.enterprise.ee.cms.impl.base.PeerID;
import com.sun.enterprise.mgmt.transport.AbstractMessageSender;
import com.sun.enterprise.mgmt.transport.Message;
import com.sun.enterprise.mgmt.transport.MessageIOException;

import java.io.IOException;
import java.io.Serializable;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.utils.Futures;

/**
 * @author Bongjae Chang
 */
public class GrizzlyTCPMessageSender extends AbstractMessageSender {

    private final static Logger LOG = GrizzlyNetworkManager.getLogger();
    private final TCPNIOTransport tcpNioTransport;

    private final ConnectionCache connectionCache;
    private final long writeTimeoutMillis;

    public GrizzlyTCPMessageSender(final TCPNIOTransport tcpNioTransport,
            final ConnectionCache connectionCache,
            final PeerID<GrizzlyPeerID> localPeerID,
            final long writeTimeoutMillis) {
        this.tcpNioTransport = tcpNioTransport;
        this.localPeerID = localPeerID;
        this.connectionCache = connectionCache;
        this.writeTimeoutMillis = writeTimeoutMillis;
    }

    @Override
    protected boolean doSend(final PeerID peerID, final Message message)
            throws IOException {

        if (peerID == null) {
            throw new IOException("peer ID can not be null");
        }
        Serializable uniqueID = peerID.getUniqueID();
        SocketAddress remoteSocketAddress;
        if (uniqueID instanceof GrizzlyPeerID) {
            GrizzlyPeerID grizzlyPeerID = (GrizzlyPeerID) uniqueID;
            remoteSocketAddress = new InetSocketAddress(grizzlyPeerID.getHost(),
                    grizzlyPeerID.getTcpPort());
        } else {
            throw new IOException("peer ID must be GrizzlyPeerID type");
        }

        return send(null, remoteSocketAddress, message, peerID);
    }

    @SuppressWarnings("unchecked")
    private boolean send(final SocketAddress localAddress,
            final SocketAddress remoteAddress,
            final Message message, final PeerID target) throws IOException {
        
        final int MAX_RESEND_ATTEMPTS = 4;
        if (tcpNioTransport == null) {
            throw new IOException("grizzly controller must be initialized");
        }
        if (remoteAddress == null) {
            throw new IOException("remote address can not be null");
        }
        if (message == null) {
            throw new IOException("message can not be null");
        }

        int attemptNo = 1;
        do {
            final Connection connection;

            try {
                connection = connectionCache.poll(localAddress, remoteAddress);
                if (connection == null) {
                    LOG.log(Level.WARNING, "failed to get a connection from connectionCache in attempt# " +
                                          attemptNo + ". GrizzlyTCPMessageSender.send(localAddr=" + localAddress +
                                          " , remoteAddr=" + remoteAddress + " sendTo=" + target +
                                           " msg=" + message + ". Retrying send", new Exception("stack trace"));
                    // try again.
                    continue;
                }
            } catch (Throwable t) {
                // include local call stack.
                final IOException localIOE = new IOException("failed to connect to " + target.toString(), t);
                 //AbstractNetworkManager.getLogger().log(Level.WARNING, "failed to connect to target " + target.toString(), localIOE);
                throw localIOE;
            }

            try {
                final FutureImpl<WriteResult> syncWriteFuture =
                                Futures.createSafeFuture();
                connection.write(remoteAddress, message, Futures.toCompletionHandler(syncWriteFuture), null);
                syncWriteFuture.get(writeTimeoutMillis, TimeUnit.MILLISECONDS);
                
                connectionCache.offer(connection);
                return true;
            } catch (Exception e) {

                // following exception is getting thrown java.util.concurrent.ExecutionException with MessageIOException
                // as cause when calling synchWriteFuture.get. Unwrap the cause, check it and
                // get this to fail immediately if cause is a MessageIOException.
                Throwable cause = e.getCause();
                if (cause instanceof MessageIOException) {
                    try {
                        connection.close();
                    } catch (Throwable t) {}
                    throw (MessageIOException)cause;
                }

                // TODO: Turn this back to FINE in future. Need to track these for the time being.
                if (LOG.isLoggable(Level.INFO)) {
                    LOG.log(Level.INFO, "exception writing message to connection. Retrying with another connection #" + attemptNo, e);
                }
                connection.close();
            }

            attemptNo++;
        } while (attemptNo <= MAX_RESEND_ATTEMPTS);

        return false;
    }
}
