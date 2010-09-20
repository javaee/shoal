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

import java.util.Map;
import java.io.IOException;

/**
 * This interface has common APIs for network managements
 *
 * According to a kind of transport layers, this interface will be implemented adequately.
 * Currently, {@link com.sun.enterprise.mgmt.ClusterManager} initializes this with calling {@link NetworkManager#initialize(String, String, java.util.Map)}.
 * After initialization, {@link com.sun.enterprise.mgmt.transport.NetworkManager#start()} ) will be called.
 *
 * @author Bongjae Chang
 */
public interface NetworkManager extends MulticastMessageSender, MessageSender {

    /**
     * Initializes this network manager with given params and properties
     *
     * @param groupName group name
     * @param instanceName instance name
     * @param properties specific properties
     * @throws IOException if an unexpected error occurs
     */
    public void initialize( final String groupName, final String instanceName, final Map properties ) throws IOException;

    /**
     * Starts this network manager
     *
     * This method will be called after {@link com.sun.enterprise.mgmt.transport.NetworkManager#initialize(String, String, java.util.Map)} internally
     *
     * @throws IOException if an I/O error occurs
     */
    public void start() throws IOException;

    /**
     * Stops this network manager
     *
     * For cleaning up remaining values and finishing I/O operation, this method could be used 
     *
     * @throws IOException if an I/O error occurs
     */
    public void stop() throws IOException;

    /**
     * Adds the {@link com.sun.enterprise.mgmt.transport.MessageListener}
     *
     * @param messageListener a message listener which should be registered on this network manager
     */
    public void addMessageListener( final MessageListener messageListener );

    /**
     * Removes the {@link com.sun.enterprise.mgmt.transport.MessageListener}
     *
     * @param messageListener a message listener which should be removed
     */
    public void removeMessageListener( final MessageListener messageListener );

    /**
     * Processes a received {@link Message}
     *
     * In this process, inbound {@link Message} will be wrapped into {@link MessageEvent}
     * and be delivered to registered {@link MessageListener} with corresponding to the message type
     *
     * @param message inbound message
     * @param piggyback piggyback
     */
    public void receiveMessage( Message message, Map piggyback );

    /**
     * Returns local {@link PeerID}
     * @return peer id
     */
    public PeerID getLocalPeerID();

    /**
     * Returns the proper {@link PeerID} corresponding with a given instance name
     *
     * @param instanceName instance name
     * @return peer id
     */
    public PeerID getPeerID( final String instanceName );

    /**
     * Add the <code>peerID</code> to this network manager
     * @param peerID
     */
    public void addRemotePeer( final PeerID peerID );

    /**
     * Removes the <code>peerID</code> from this network manager
     * @param peerID
     */
    public void removePeerID( final PeerID peerID );

    /**
     * Check whether the suspicious peer is alive or not
     *
     * This API is mainly used in {@link com.sun.enterprise.mgmt.HealthMonitor} in order to determine the failure member
     *
     * @param peerID peer id
     * @return true if the peer is still alive, otherwise false
     */
    public boolean isConnected( final PeerID peerID );

    /**
     * Returns a {@link MessageSender} corresponding with transport type
     *
     * @param transport transport type. {@link ShoalMessageSender#TCP_TRANSPORT} or {@link ShoalMessageSender#UDP_TRANSPORT}'s integer value
     * @return a {@link MessageSender}'s instance which this network manager contains
     */
    public MessageSender getMessageSender( int transport );

    /**
     * Returns a {@link MulticastMessageSender}
     * @return a {@link MulticastMessageSender}'s instance which this network manager contains
     */
    public MulticastMessageSender getMulticastMessageSender();
}
