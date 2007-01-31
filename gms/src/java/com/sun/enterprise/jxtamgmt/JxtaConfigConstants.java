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
    DISCOVERY_TIMEOUT
}
