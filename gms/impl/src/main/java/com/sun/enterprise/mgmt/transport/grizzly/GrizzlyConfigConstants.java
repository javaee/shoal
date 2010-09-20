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

/**
 * Specifies constants that are allowed to be used as keys for configuration
 * elements that are sought to be set or retrieved for/from Grizzly platform
 * configuration
 *
 * @author Bongjae Chang
 */
public enum GrizzlyConfigConstants {
    TCPSTARTPORT,
    TCPENDPORT,
    BIND_INTERFACE_NAME,

    // thread pool
    MAX_POOLSIZE, // max threads for tcp and multicast processing. See max parameter for ThreadPoolExecutor constructor.
    CORE_POOLSIZE, // core threads for tcp and multicast processing. See core parameter for ThreadPoolExecutor constructor.
    KEEP_ALIVE_TIME, // ms
    POOL_QUEUE_SIZE,

    // pool management
    HIGH_WATER_MARK, // maximum number of active outbound connections Controller will handle
    NUMBER_TO_RECLAIM, // number of LRU connections, which will be reclaimed in case highWaterMark limit will be reached
    MAX_PARALLEL, // maximum number of active outbound connections to single destination (usually <host>:<port>)

    START_TIMEOUT, // ms
    WRITE_TIMEOUT, // ms

    MAX_WRITE_SELECTOR_POOL_SIZE,

    // comma separated list of tcp uri endpoints
    // ex) tcp://192.168.0.3:9090,tcp://61.77.153.2:9090
    VIRTUAL_MULTICAST_URI_LIST,
    MULTICAST_TIME_TO_LIVE
}
