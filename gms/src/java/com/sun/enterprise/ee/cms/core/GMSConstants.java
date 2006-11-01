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
package com.sun.enterprise.ee.cms.core;

/**
 * Herein we specify client facing constants that may be applicable to specific
 * GMS notifications, api calls or key descriptions.
 * @author Shreedhar Ganapathy
 *         Date: Aug 15, 2005
 * @version $Revision$
 */
public class GMSConstants {

    public static final String DEFAULT_GROUP_COMMUNICATION_PROVIDER="Jxta";
    public static final String GROUP_COMMUNICATION_PROVIDER =
        System.getProperty ("SHOAL_GROUP_COMMUNICATION_PROVIDER",
                DEFAULT_GROUP_COMMUNICATION_PROVIDER);
    public static enum shutdownType { INSTANCE_SHUTDOWN, GROUP_SHUTDOWN }
    public static enum shutdownState { INITIATED, COMPLETED }
}
