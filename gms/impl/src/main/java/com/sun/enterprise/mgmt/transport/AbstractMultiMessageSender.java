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

package com.sun.enterprise.mgmt.transport;

import com.sun.enterprise.ee.cms.impl.base.PeerID;

import java.io.IOException;

/**
 * This class implements both a common {@link MulticastMessageSender} and {@link MessageSender} logic simply
 * in order to help the specific transport layer to be implemented easily
 *
 * Mainly, this stores both source's {@link PeerID} and target's {@link PeerID}
 * before sending the message to the peer or broadcasting the message to all members
 *
 * @author Bongjae Chang
 */
public abstract class AbstractMultiMessageSender implements MulticastMessageSender, MessageSender {

    /**
     * Represents local {@link PeerID}.
     * This value should be assigned in real {@link MessageSender}'s implementation correspoinding to the specific transport layer
     */
    protected PeerID localPeerID;

    /**
     * {@inheritDoc}
     */
    public boolean broadcast( final Message message ) throws IOException {
        if( message == null )
            throw new IOException( "message is null" );
        if( localPeerID != null )
            message.addMessageElement( Message.SOURCE_PEER_ID_TAG, localPeerID );
        return doBroadcast( message );
    }

    /**
     * {@inheritDoc}
     */
    public boolean send( final PeerID peerID, final Message message ) throws IOException {
        if( peerID == null )
            throw new IOException( "peer ID can not be null" );
        if( message == null )
            throw new IOException( "message is null" );
        if( localPeerID != null )
            message.addMessageElement( Message.SOURCE_PEER_ID_TAG, localPeerID );
        if( peerID != null )
            message.addMessageElement( Message.TARGET_PEER_ID_TAG, peerID );
        return doSend( peerID, message );
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws IOException {
    }

    /**
     * {@inheritDoc}
     */
    public void stop() throws IOException {
    }

    /**
     * Broadcasts or Multicasts the given {@link Message} to all members
     *
     * @param message a message which is sent to all members
     * @return true if the message is sent to all members successfully, otherwise false
     * @throws IOException if I/O error occurs or given parameters are not valid
     */
    protected abstract boolean doBroadcast( final Message message ) throws IOException;

    /**
     * Sends the given {@link Message} to the destination
     *
     * @param peerID the destination {@link PeerID}. <code>null</code> is not allowed
     * @param message a message which is sent to the peer
     * @return true if the message is sent to the destination successfully, otherwise false
     * @throws IOException if I/O error occurs or given parameters are not valid
     */
    protected abstract boolean doSend( final PeerID peerID, final Message message ) throws IOException;
}
