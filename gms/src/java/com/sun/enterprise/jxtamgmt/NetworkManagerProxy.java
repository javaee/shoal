/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://shoal.dev.java.net/public/CDDLv1.0.html
 *
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 */
package com.sun.enterprise.jxtamgmt;

import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import net.jxta.protocol.PipeAdvertisement;

/**
 * Provides a facade over the NetworkManager in order to facilitate apps that require direct access to JXTA artifacts
 * such as pipe advertisements, etc.
 */
public class NetworkManagerProxy {

    private NetworkManager manager = null;

    /**
     * Given a network manager instance return a NetworkProxy instance.
     *
     * @param manager the network manager instance
     */
    NetworkManagerProxy(NetworkManager manager) {
        this.manager = manager;
    }

    /**
     * Gets the pipeID attribute of the NetworkManager class.
     *
     * @param instanceName instance name
     * @return The pipeID value
     */
    public PipeID getPipeID(String instanceName) {
        return manager.getPipeID(instanceName);
    }

    /**
     * Gets the socketID attribute of the NetworkManager class.
     *
     * @param instanceName instance name value
     * @return The socketID value
     */
    public PipeID getSocketID(final String instanceName) {
        return manager.getSocketID(instanceName);
    }

    /**
     * Gets the peerID attribute of the NetworkManager class.
     *
     * @param instanceName instance name value
     * @return The peerID value
     */
    public PeerID getPeerID(final String instanceName) {
        return manager.getPeerID(instanceName);

    }

    /**
     * Returns the SessionQeuryPipe ID.
     *
     * @return The SessionQueryPipe Pipe ID
     */
    public PipeID getSessionQueryPipeID() {
        return manager.getSessionQueryPipeID();
    }

    /**
     * Creates a JxtaSocket pipe advertisement with a SHA1 encoded instance name pipe ID.
     *
     * @param instanceName instance name
     * @return a JxtaSocket Pipe Advertisement
     */
    public PipeAdvertisement getSocketAdvertisement(final String instanceName) {
        return manager.getSocketAdvertisement(instanceName);
    }

    /**
     * Creates a JxtaBiDiPipe pipe advertisement with a SHA1 encoded instance name pipe ID.
     *
     * @param instanceName instance name
     * @return PipeAdvertisement a JxtaBiDiPipe Pipe Advertisement
     */
    public PipeAdvertisement getPipeAdvertisement(final String instanceName) {
        return manager.getPipeAdvertisement(instanceName);
    }

    /**
     * Gets the infraPeerGroupID attribute of the NetworkManager class.
     *
     * @return The infraPeerGroupID value
     */
    public PeerGroupID getInfraPeerGroupID() {
        return manager.getInfraPeerGroupID();
    }

    /**
     * Gets the netPeerGroup instance.
     *
     * @return The netPeerGroup value
     */
    public PeerGroup getNetPeerGroup() {
        return manager.getNetPeerGroup();
    }

    /**
     * Gets the running attribute of the NetworkManager class
     *
     * @return The running value
     */
    public boolean isStarted() {
        return manager.isStarted();
    }
}