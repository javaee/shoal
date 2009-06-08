package com.sun.enterprise.mgmt.transport;

import com.sun.enterprise.ee.cms.impl.base.PeerID;

import java.io.IOException;

/**
 * @author Bongjae Chang
 * @date 2009. 6. 3
 */
public abstract class AbstractMessageSender implements MessageSender {

    protected PeerID localPeerID;

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

    public void start() throws IOException {
    }

    public void stop() throws IOException {
    }

    protected abstract boolean doSend( final PeerID peerID, final Message message ) throws IOException;
}
