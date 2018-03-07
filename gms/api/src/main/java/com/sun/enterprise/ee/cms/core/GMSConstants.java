/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package com.sun.enterprise.ee.cms.core;

/**
 * Herein we specify client facing constants that may be applicable to specific
 * GMS notifications, api calls or key descriptions.
 * @author Shreedhar Ganapathy
 *         Date: Aug 15, 2005
 * @version $Revision$
 */
public class GMSConstants {
    public static final String GRIZZLY_GROUP_COMMUNICATION_PROVIDER="grizzly2";
    public static final String JXTA_GROUP_COMMUNICATION_PROVIDER="jxta";
    public static final String DEFAULT_GROUP_COMMUNICATION_PROVIDER=GRIZZLY_GROUP_COMMUNICATION_PROVIDER;
    public static final String GROUP_COMMUNICATION_PROVIDER =
        System.getProperty ("SHOAL_GROUP_COMMUNICATION_PROVIDER",
                DEFAULT_GROUP_COMMUNICATION_PROVIDER);
    public static enum shutdownType { INSTANCE_SHUTDOWN, GROUP_SHUTDOWN }
    public static enum shutdownState { INITIATED, COMPLETED }
    public static enum startupType { INSTANCE_STARTUP, GROUP_STARTUP }
    public static enum groupStartupState { INITIATED, COMPLETED_SUCCESS, COMPLETED_FAILED }
    public static final int DEFAULT_MULTICAST_TIME_TO_LIVE = -1;
    public static final int MINIMUM_MULTICAST_TIME_TO_LIVE = 4;
    public static final String JOIN_CLUSTER_SEED_URI_LIST = "JOIN_CLUSTER_SEED_URI_LIST";
}
