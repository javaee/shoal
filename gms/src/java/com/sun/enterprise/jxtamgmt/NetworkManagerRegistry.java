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

import java.util.HashMap;
import java.util.Map;

/**
 * This is a registry that holds the network manager instances based on group name.
 *
 * @author Shreedhar Ganapathy
 *         Date: Oct 12, 2006
 *         Time: 10:46:25 AM
 */
public class NetworkManagerRegistry {
    private static final Map<String, NetworkManagerProxy> registry = new HashMap<String, NetworkManagerProxy>();

    private NetworkManagerRegistry() {

    }

    /**
     * Adds a Network Manager Proxy to the registry for the given GroupName and NetworkManager Instance
     *
     * @param groupName - name of the group
     * @param manager   - Network Manager instance for which a proxy is registered
     */
    static void add(final String groupName, final NetworkManager manager) {
        synchronized (registry) {
            registry.put(groupName, new NetworkManagerProxy(manager));
        }
    }

    /**
     * returns a NetworkManagerProxy for the given groupName
     *
     * @param groupName name of the group
     * @return NetworkManagerProxy instance wrapping a network manager corresponding to the group
     */
    static NetworkManagerProxy getNetworkManagerProxy(final String groupName) {
        return registry.get(groupName);
    }

    /**
     * removes the NetworkManagerProxy instance from the registry.
     *
     * @param groupName name of the group
     */
    public static void remove(final String groupName) {
        synchronized (registry) {
            registry.remove(groupName);
        }
    }
}