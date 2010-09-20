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

package com.sun.enterprise.mgmt.transport.grizzly;

import com.sun.enterprise.mgmt.transport.MessageListener;
import com.sun.enterprise.mgmt.transport.MessageEvent;
import com.sun.enterprise.mgmt.transport.Message;
import com.sun.enterprise.mgmt.transport.NetworkManager;
import com.sun.enterprise.mgmt.transport.MessageIOException;
import com.sun.enterprise.mgmt.transport.MessageImpl;
import com.sun.enterprise.ee.cms.impl.base.PeerID;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author Bongjae Chang
 */
public class PingMessageListener implements MessageListener {

    private static final Logger LOG = GrizzlyUtil.getLogger();

    public void receiveMessageEvent( final MessageEvent event ) throws MessageIOException {
        if( event == null )
            return;
        final Message msg = event.getMessage();
        if( msg == null )
            return;
        Object obj = event.getSource();
        if( !( obj instanceof NetworkManager ) )
            return;
        NetworkManager networkManager = (NetworkManager)obj;
        PeerID sourcePeerId = event.getSourcePeerID();
        if( sourcePeerId == null )
            return;
        PeerID targetPeerId = event.getTargetPeerID();
        if( targetPeerId == null )
            return;
        if( networkManager.getLocalPeerID().equals( targetPeerId ) ) {
            // send a pong message
            try {
                networkManager.send( sourcePeerId, new MessageImpl( Message.TYPE_PONG_MESSAGE ));
            } catch( IOException ie ) {
                if( LOG.isLoggable( Level.WARNING ) )
                    LOG.log( Level.WARNING, "failed to send a pong message" , ie );
            }
        }
    }

    public int getType() {
        return Message.TYPE_PING_MESSAGE;
    }
}
