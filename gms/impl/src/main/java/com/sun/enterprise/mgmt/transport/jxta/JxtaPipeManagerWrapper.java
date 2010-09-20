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

package com.sun.enterprise.mgmt.transport.jxta;

import com.sun.enterprise.mgmt.transport.Message;
import com.sun.enterprise.mgmt.transport.MessageIOException;
import com.sun.enterprise.mgmt.transport.MessageImpl;
import com.sun.enterprise.mgmt.transport.AbstractMultiMessageSender;
import com.sun.enterprise.ee.cms.impl.base.PeerID;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

import net.jxta.protocol.PipeAdvertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.pipe.PipeService;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.endpoint.ByteArrayMessageElement;

/**
 * This class wraps {@link JxtaPipeManager} and extends {@link AbstractMultiMessageSender}
 * which supports both {@link com.sun.enterprise.mgmt.transport.MulticastMessageSender}
 * and {@link com.sun.enterprise.mgmt.transport.MessageSender} transport layer
 *
 * This stores and caches {@link JxtaPipeManager} according to {@link Message}'s type
 * This implements Jxta's PipeMsgListener, receives Jxta's PipeMsgEvent, parses Jxta's message,
 * converts it into {@link Message} and forwards it to {@link com.sun.enterprise.mgmt.transport.NetworkManager}
 *
 * @author Bongjae Chang
 */
public class JxtaPipeManagerWrapper extends AbstractMultiMessageSender implements PipeMsgListener {

    private static final Logger LOG = JxtaUtil.getLogger();

    private static final String NAMESPACE = "JXTA_PIPE_MANAGER";
    private static final String BYTE_MESSAGE = "BYTE_MESSAGE";

    private final JxtaNetworkManager networkManager;
    private final Map<Integer, JxtaPipeManager> pipeManagerMap = new HashMap<Integer, JxtaPipeManager>();

    public JxtaPipeManagerWrapper( JxtaNetworkManager networkManager, PeerID<net.jxta.peer.PeerID> localPeerID ) {
        this.networkManager = networkManager;
        this.localPeerID = localPeerID;
        pipeManagerMap.put( Message.TYPE_CLUSTER_MANAGER_MESSAGE,
                            new JxtaPipeManager( networkManager,
                                                 networkManager.getNetPeerGroup().getPipeService(),
                                                 createPipeAdv( networkManager.getAppServicePipeID() ),
                                                 this ) );
        pipeManagerMap.put( Message.TYPE_MASTER_NODE_MESSAGE,
                            new JxtaPipeManager( networkManager,
                                                 networkManager.getNetPeerGroup().getPipeService(),
                                                 createPipeAdv( networkManager.getMasterPipeID() ),
                                                 this ) );
        pipeManagerMap.put( Message.TYPE_HEALTH_MONITOR_MESSAGE,
                            new JxtaPipeManager( networkManager,
                                                 networkManager.getNetPeerGroup().getPipeService(),
                                                 createPipeAdv( networkManager.getHealthPipeID() ),
                                                 this ) );
    }

    private PipeAdvertisement createPipeAdv( PipeID pipeID ) {
        final PipeAdvertisement pipeAdv;
        // create the pipe advertisement, to be used in creating the pipe
        pipeAdv = (PipeAdvertisement)AdvertisementFactory.newAdvertisement( PipeAdvertisement.getAdvertisementType() );
        pipeAdv.setPipeID( pipeID );
        pipeAdv.setType( PipeService.PropagateType );
        return pipeAdv;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void start() throws IOException {
        super.start();
        for( JxtaPipeManager pipeManager : pipeManagerMap.values() ) {
            try {
                pipeManager.start();
            } catch( Throwable t ) {
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void stop() throws IOException {
        super.stop();
        for( JxtaPipeManager pipeManager : pipeManagerMap.values() ) {
            try {
                pipeManager.stop();
            } catch( Throwable t ) {
            }
        }
        pipeManagerMap.clear();
    }

    /**
     * {@inheritDoc}
     */
    protected boolean doBroadcast( final Message message ) throws IOException {
        if( message == null )
            throw new IOException( "message is null" );
        int type = message.getType();
        JxtaPipeManager pipeManager = pipeManagerMap.get( type );
        if( pipeManager == null )
            pipeManager = pipeManagerMap.get( Message.TYPE_HEALTH_MONITOR_MESSAGE );
        if( pipeManager == null )
            throw new IOException( "couldn't find a pipe manager: type = " + type );
        if( type == Message.TYPE_MASTER_NODE_MESSAGE || type == Message.TYPE_HEALTH_MONITOR_MESSAGE )
            networkManager.addRoute( message );
        return pipeManager.broadcast( createMessage( message ) );
    }

    /**
     * {@inheritDoc}
     */
    protected boolean doSend( final PeerID peerID, final Message message ) throws IOException {
        if( peerID == null )
            throw new IOException( "peer ID can not be null" );
        if( message == null )
            throw new IOException( "message is null" );
        int type = message.getType();
        JxtaPipeManager pipeManager = pipeManagerMap.get( type );
        if( pipeManager == null )
            pipeManager = pipeManagerMap.get( Message.TYPE_HEALTH_MONITOR_MESSAGE );
        if( pipeManager == null )
            throw new IOException( "couldn't find a pipe manager: type = " + type );
        if( type == Message.TYPE_MASTER_NODE_MESSAGE || type == Message.TYPE_HEALTH_MONITOR_MESSAGE )
            networkManager.addRoute( message );
        Serializable uniqueID = peerID.getUniqueID();
        if( !( uniqueID instanceof net.jxta.peer.PeerID ) )
            throw new IOException( "peer ID must be net.jxta.peer.PeerID type" );
        net.jxta.peer.PeerID jxtaPeerID = (net.jxta.peer.PeerID)uniqueID;
        return pipeManager.send( jxtaPeerID, createMessage( message ) );
    }

    private net.jxta.endpoint.Message createMessage( Message message ) throws IOException {
        net.jxta.endpoint.Message jxtaMessage = new net.jxta.endpoint.Message();
        if( message != null ) {
            byte[] byteMessage;
            try {
                byteMessage = message.getPlainBytes();
            } catch( MessageIOException mie ) {
                throw mie;
            }
            net.jxta.endpoint.MessageElement jxtaMessageElement = new ByteArrayMessageElement( BYTE_MESSAGE,
                                                                                               MimeMediaType.AOS,
                                                                                               byteMessage,
                                                                                               null );
            jxtaMessage.addMessageElement( NAMESPACE, jxtaMessageElement );
        }
        return jxtaMessage;
    }

    public void pipeMsgEvent( PipeMsgEvent event ) {
        net.jxta.endpoint.Message jxtaMessage = event.getMessage();
        if( jxtaMessage == null )
            return;
        net.jxta.endpoint.MessageElement jxtaMessageElement = jxtaMessage.getMessageElement( NAMESPACE, BYTE_MESSAGE );
        byte[] byteMessage = jxtaMessageElement.getBytes( false );
        Message message = new MessageImpl();
        int messageLen = message.parseHeader( byteMessage, 0 );
        try {
            message.parseMessage( byteMessage, MessageImpl.HEADER_LENGTH, messageLen );
        } catch( MessageIOException mie ) {
            if( LOG.isLoggable( Level.WARNING ) )
                LOG.log( Level.WARNING, "failed to parse a message: " + message, mie );
            mie.printStackTrace();
            return;
        }
        networkManager.receiveMessage( message, null );
    }

    /**
     * in the event of a failure or planned shutdown, remove the
     * pipe from the pipeCache
     *
     * @param peerid peerID
     */
    public void removePipeFromCache( net.jxta.peer.PeerID peerid ) {
        for( JxtaPipeManager pipeManager : pipeManagerMap.values() ) {
            try {
                pipeManager.removePipeFromCache( peerid );
            } catch( Throwable t ) {
            }
        }
    }

    public void clearPipeCache() {
        for( JxtaPipeManager pipeManager : pipeManagerMap.values() ) {
            try {
                pipeManager.clearPipeCache();
            } catch( Throwable t ) {
            }
        }
    }
}
