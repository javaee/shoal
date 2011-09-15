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

import net.jxta.pipe.PipeID;
import net.jxta.peer.PeerID;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.peergroup.PeerGroup;

/**
 * Provides a facade over the NetworkManager in order to facilitate apps that require direct access to JXTA artifacts
 * such as pipe advertisements, etc.
 */
public class JxtaNetworkManagerProxy {

    private JxtaNetworkManager manager = null;

    /**
     * Given a network manager instance return a NetworkProxy instance.
     *
     * @param manager the network manager instance
     */
    JxtaNetworkManagerProxy(JxtaNetworkManager manager) {
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
    public PeerID getJxtaPeerID(final String instanceName) {
        return manager.getJxtaPeerID(instanceName);

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
