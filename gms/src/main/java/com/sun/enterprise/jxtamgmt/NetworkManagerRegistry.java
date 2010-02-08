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

package com.sun.enterprise.jxtamgmt;

import java.util.HashMap;
import java.util.Iterator;
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

    /**
     * Returns all registered domain names
     * @return an interator of domain names
     */
    public static Iterator<String> getGroups() {
        return registry.keySet().iterator();
    }
}