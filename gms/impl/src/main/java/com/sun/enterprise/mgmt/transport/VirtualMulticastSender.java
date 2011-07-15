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

package com.sun.enterprise.mgmt.transport;

import com.sun.enterprise.ee.cms.impl.base.PeerID;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
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
public class VirtualMulticastSender extends AbstractMulticastMessageSender {

    private static final Logger LOG = GMSLogDomain.getLogger( GMSLogDomain.GMS_LOGGER );

    final Set<PeerID> virtualPeerIdList = new CopyOnWriteArraySet<PeerID>();
    final NetworkManager networkManager;

    public VirtualMulticastSender(NetworkManager networkManager, List<PeerID> initialPeerIds) throws IOException {
        this.networkManager = networkManager;
        if( initialPeerIds != null && !initialPeerIds.isEmpty() ) {
            this.virtualPeerIdList.addAll(initialPeerIds);
        }
    }

    public Set<PeerID> getVirtualPeerIDSet() {
        return virtualPeerIdList;
    }

    @Override
    public synchronized void start() throws IOException {
        // ADDED EXPLICITLY SO SUPERCLASS start() is not called to start multicast listener.
        // we are not supporting hybrid solution of some multicast and some non-multicast at this time,
        // it is all multicast or all virtual multicast.

    }

    @Override
    public synchronized void stop() throws IOException {
        // did not call super.start(), no need to call super.stop().
        // super.stop();
        virtualPeerIdList.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean doBroadcast( final Message message ) throws IOException {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.entering(this.getClass().getSimpleName(), "doBroadcast", new Object[]{message});
            LOG.finer("VirtualMulticastSender.doBroadcast() virtualPeerIdList = " + virtualPeerIdList);
        }
        boolean result = true;

        // TODO:  Removed combining multicast with TCP virtual multicast.
//        if( !super.doBroadcast( message ) )
//            result = false;
        // send the message to virtual server on TCP
        MessageSender tcpSender = networkManager.getMessageSender( ShoalMessageSender.TCP_TRANSPORT );

        for( PeerID peerID : virtualPeerIdList ) {
            try {
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.log(Level.FINEST, "VirtualMulticastSender.doBroadcast prepare to send msg to peerID " + peerID);
                }
                if( !tcpSender.send( peerID, message ) ) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "VirtualMulticastSender.doBroadcast failed to send msg to peerID " + peerID);
                    }
                    result = false;
                } else {
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.log(Level.FINEST, "VirtualMulticastSender.doBroadcast succeded to send msg to peerID " + peerID);
                    }
                }

            } catch( IOException ie ) {
                if( LOG.isLoggable( Level.INFO ) )
                    LOG.log( Level.INFO, "failed to send message to a virtual multicast endpoint[" + peerID +
                                         "] message=[" + message + "]", ie );
            }
        }
        return result;
    }
}
