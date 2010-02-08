package com.sun.enterprise.mgmt.transport;

import com.sun.enterprise.ee.cms.impl.base.PeerID;

import java.io.IOException;

/**
 * This class implements a common {@link MessageSender} logic simply in order to help the specific transport layer to be implemented easily
 *
 * Mainly, this stores both source's {@link PeerID} and target's {@link PeerID} before sending the message to the peer
 *
 * @author Bongjae Chang
 */
public abstract class AbstractMessageSender implements MessageSender {

    /**
     * Represents local {@link PeerID}.
     * This value should be assigned in real {@link MessageSender}'s implementation correspoinding to the specific transport layer
     */
    protected PeerID localPeerID;

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
     * Sends the given {@link Message} to the destination
     *
     * @param peerID the destination {@link PeerID}. <code>null</code> is not allowed
     * @param message a message which is sent to the peer
     * @return true if the message is sent to the destination successfully, otherwise false
     * @throws IOException if I/O error occurs or given parameters are not valid
     */
    protected abstract boolean doSend( final PeerID peerID, final Message message ) throws IOException;
}
