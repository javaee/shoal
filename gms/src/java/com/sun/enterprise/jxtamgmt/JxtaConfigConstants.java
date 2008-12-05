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

/**
 * Specifies constants that are allowed to be used as keys for configuration
 * elements that are sought to be set or retrieved for/from Jxta platform
 * configuration
 *
 * @author Shreedhar Ganapathy
 *         Date: Jun 22, 2006
 * @version $Revision$
 */
public enum JxtaConfigConstants {
    PRINCIPAL,
    PASSWORD,
    JXTAHOME,
    TCPSTARTPORT,
    TCPENDPORT,
    MULTICASTADDRESS,
    MULTICASTPORT,
    HTTPADDRESS,
    HTTPPORT,
    FAILURE_DETECTION_TIMEOUT,
    FAILURE_DETECTION_RETRIES,
    FAILURE_VERIFICATION_TIMEOUT,
    DISCOVERY_TIMEOUT,
    // specifies if this node is a rendezvous seed peer
    IS_BOOTSTRAPPING_NODE,
    //comma separated list of tcp/http rendezvous seed uri endpoints
    VIRTUAL_MULTICAST_URI_LIST,
    LOOPBACK,
    BIND_INTERFACE_ADDRESS, //used for specifying which interface to use for group communication
                           // This is the address which Shoal should bind to for communication.
    FAILURE_DETECTION_TCP_RETRANSMIT_TIMEOUT, //admin can specify the timeout after which the HealthMonitor.isConnected() thread can
                            //quit checking if the peer's machine is up or not.
    FAILURE_DETECTION_TCP_RETRANSMIT_PORT,   //port where a socket can be created to see if the instance's machine is up or down
    MULTICAST_POOLSIZE,    // how many simultaneous multicast messages can be processed before multicast messages start getting dropped.
    TCP_MAX_POOLSIZE,      // max threads for tcp processing.   See max parameter for ThreadPoolExecutor constructor.
    TCP_CORE_POOLSIZE,     // core threads for tcp processing.  See core parameter for ThreadPoolExecutor constructor.
    TCP_BLOCKING_QUEUESIZE  // queue for pending incoming tcp requests (out of CORE threads).  When full, new threads created till MAX.
}
