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
package com.sun.enterprise.ee.cms.impl.common;

/**
 * Configuration constants used to denote keys for configuration elements. These
 * are used both to populate values for such contants and to retrive them.
 *
 * @author Shreedhar Ganapathy
 *         Date: Jul 13, 2005
 * @version $Revision$
 * TODO: Move this out of here to impl.jxta
 */
public class GMSConfigConstants {
    public static final String MULTICAST_ADDRESS = "UDP::mcast_addr";
    public static final String MULTICAST_PORT = "UDP::mcast_port";
    public static final String FD_TIMEOUT = "FD::timeout";
    public static final String FD_MAX_RETRIES = "FD::max_tries";
    public static final String MERGE_MAX_INTERVAL = "MERGE2::max_interval";
    public static final String MERGE_MIN_INTERVAL = "MERGE2::min_interval";
    public static final String VS_TIMEOUT = "VERIFY_SUSPECT::timeout";
    public static final String PING_TIMEOUT = "PING::timeout";    
}
