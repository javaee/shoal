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

package com.sun.enterprise.mgmt.transport;

import com.sun.enterprise.ee.cms.impl.base.PeerID;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

/**
 * @author Bongjae Chang
 * @date 2009. 6. 3
 */
public abstract class AbstractNetworkManager implements NetworkManager {

    private static final Logger LOG = GMSLogDomain.getLogger( GMSLogDomain.GMS_LOGGER );

    protected PeerID localPeerID;

    private final List<MessageListener> messageListeners = new CopyOnWriteArrayList<MessageListener>();

    public void start() throws IOException {
    }

    public void stop() throws IOException {
        messageListeners.clear();
    }

    public void addMessageListener( final MessageListener messageListener ) {
        if( messageListener != null )
            messageListeners.add( messageListener );
    }

    public void removeMessageListener( final MessageListener messageListener ) {
        if( messageListener != null )
            messageListeners.remove( messageListener );
    }

    public void receiveMessage( Message message, Map piggyback ) {
        PeerID sourcePeerID = null;
        PeerID targetPeerID = null;
        if( message != null ) {
            Object element = message.getMessageElement( Message.SOURCE_PEER_ID_TAG );
            if( element instanceof PeerID )
                sourcePeerID = (PeerID)element;
            element = message.getMessageElement( Message.TARGET_PEER_ID_TAG );
            if( element instanceof PeerID )
                targetPeerID = (PeerID)element;
        }
        MessageEvent messageEvent = new MessageEvent( this, message, sourcePeerID, targetPeerID );
        try {
            beforeDispatchingMessage( messageEvent, piggyback );
        } catch( Throwable t ) {
            if( LOG.isLoggable( Level.WARNING ) )
                LOG.log( Level.WARNING, "failed to execute beforeDispatchingMessage()", t );
        }
        for( MessageListener listener : messageListeners ) {
            if( message.getType() == listener.getType() ) {
                try {
                    listener.receiveMessageEvent( messageEvent );
                } catch( Throwable t ) {
                    if( LOG.isLoggable( Level.WARNING ) )
                        LOG.log( Level.WARNING, "failed to receive a message: type = " + message.getType(), t );
                }
            }
        }
        try {
            afterDispatchingMessage( messageEvent, piggyback );
        } catch( Throwable t ) {
            if( LOG.isLoggable( Level.WARNING ) )
                LOG.log( Level.WARNING, "failed to execute afterDispatchingMessage()", t );
        }
    }

    public PeerID getLocalPeerID() {
        return localPeerID;
    }

    protected abstract void beforeDispatchingMessage( MessageEvent messageEvent, Map piggyback );

    protected abstract void afterDispatchingMessage( MessageEvent messageEvent, Map piggyback );
}
