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
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.util.concurrent.Executor;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;

/**
 * This class extends {@link BlockingIOMulticastSender}
 * for supporting the circumstance that cluster members are located beyond one subnet
 * or multicast traffic is disabled
 *
 * <code>virtualPeerIdList</code> should contain <code>PeerID</code>s of cluster members which are located beyond one subnet.
 * So, this {@link MulticastMessageSender} will broadcast a message
 * to endpoints which <code>virtualPeerIdList</code> includes as well as one subnet on TCP protocol.
 *
 * @author Bongjae Chang
 */
public class VirtualMulticastSender extends BlockingIOMulticastSender {

    private static final Logger LOG = GMSLogDomain.getLogger( GMSLogDomain.GMS_LOGGER );

    final List<PeerID> virtualPeerIdList = new ArrayList<PeerID>();
    final NetworkManager networkManager;

    public VirtualMulticastSender( String host,
                                   String multicastAddress,
                                   int multicastPort,
                                   String networkInterfaceName,
                                   int multicastPacketSize,
                                   PeerID localPeerID,
                                   Executor executor,
                                   NetworkManager networkManager,
                                   int multicastTimeToLive,
                                   List<PeerID> virtualPeerIdList ) throws IOException {
        super( host, multicastAddress,
               multicastPort,
               networkInterfaceName,
               multicastPacketSize,
               localPeerID,
               executor,
               multicastTimeToLive,
               networkManager );
        this.networkManager = networkManager;
        if( virtualPeerIdList != null && !virtualPeerIdList.isEmpty() )
            this.virtualPeerIdList.addAll( virtualPeerIdList );
    }

    @Override
    public synchronized void stop() throws IOException {
        super.stop();
        virtualPeerIdList.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean doBroadcast( final Message message ) throws IOException {
        boolean result = true;
        if( !super.doBroadcast( message ) )
            result = false;
        // send the message to virtual server on TCP
        MessageSender tcpSender = networkManager.getMessageSender( ShoalMessageSender.TCP_TRANSPORT );
        for( PeerID peerID : virtualPeerIdList ) {
            try {
                if( !tcpSender.send( peerID, message ) )
                    result = false;
            } catch( IOException ie ) {
                if( LOG.isLoggable( Level.FINEST ) )
                    LOG.log( Level.FINEST, "failed to send a message to a virtual multicast endpoint(" + peerID + ")", ie );
            }
        }
        return result;
    }
}
