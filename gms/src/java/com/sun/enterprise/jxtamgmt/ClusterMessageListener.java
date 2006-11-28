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
 * @author Shreedhar Ganapathy
 *         Date: Jul 10, 2006
 * @version $Revision$
 */
public interface ClusterMessageListener {
    /**
     * Called when a message is received from another member.
     *
     * @param system source system advertisement
     * @param message the message
     */
    void handleClusterMessage(SystemAdvertisement system, Object message);
}
