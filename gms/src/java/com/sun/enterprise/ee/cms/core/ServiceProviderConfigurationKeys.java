
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

package com.sun.enterprise.ee.cms.core;

/**
 * Provides the keys that correspond to properties within group
 * communication providers that can be configured. Note that this is not
 * exhaustive enough to cover all possible configurations in different
 * group communication libraries.
 *
 * @author Shreedhar Ganapathy
 */
public enum ServiceProviderConfigurationKeys {
    MULTICASTADDRESS,
    MULTICASTPORT,
    FAILURE_DETECTION_TIMEOUT,
    FAILURE_DETECTION_RETRIES,
    FAILURE_VERIFICATION_TIMEOUT,
    DISCOVERY_TIMEOUT,
    LOOPBACK,
    /**
     * set true if this node will be an initial host for other members to use for discovery
     */
    IS_BOOTSTRAPPING_NODE,
   /**
    *  a comma separated list of initial tcp/http addresses that is known to all joining members when not using Multicast over UDP.
    * above 2 properties are used for cross-subnet support
    */
    VIRTUAL_MULTICAST_URI_LIST,
    /**
     * used for specifying which interface to use for group communication
     * This is the address which Shoal should bind to for communication.
    */
    BIND_INTERFACE_ADDRESS,
    /**
     * The default TCP timeout is 10 minutes
     * Let's take the case of 2 machines A and B hosting instances A and B. machine B goes down due to
     * the power outage.
     * instance A on machine A won't know that instance B on machine B is down until 10 minutes have passed.
     * This can cause severe issues. States of the instances won't be correctly known to other instances.
     * For this purpose, admin can specify the timeout after which the HealthMonitor.isConnected() thread can
     * quit checking if the peer's machine is up or not.
     * By default it is set to 30 seconds.
     */
    FAILURE_DETECTION_TCP_RETRANSMIT_TIMEOUT,
    /**
     *  port where a socket can be created to see if the instance's machine is up or down
     * admin will need make sure that the same port number is available on all the machines that make up the 
     * cluster, for this purpose.
     */
    FAILURE_DETECTION_TCP_RETRANSMIT_PORT
}
