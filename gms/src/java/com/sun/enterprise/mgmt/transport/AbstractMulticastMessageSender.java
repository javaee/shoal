package com.sun.enterprise.mgmt.transport;

import com.sun.enterprise.ee.cms.impl.base.PeerID;

import java.io.IOException;

/**
 * @author Bongjae Chang
 * @date 2009. 6. 3
 */
public abstract class AbstractMulticastMessageSender implements MulticastMessageSender {

    protected PeerID localPeerID;

    public boolean broadcast( final Message message ) throws IOException {
        if( message == null )
            throw new IOException( "message is null" );
        if( localPeerID != null )
            message.addMessageElement( Message.SOURCE_PEER_ID_TAG, localPeerID );
        return doBroadcast( message );
    }

    public void start() throws IOException {
    }

    public void stop() throws IOException {
    }

    protected abstract boolean doBroadcast( final Message message ) throws IOException;
}
