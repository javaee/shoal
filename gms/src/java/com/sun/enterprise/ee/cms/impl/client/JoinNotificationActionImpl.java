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
package com.sun.enterprise.ee.cms.impl.client;

import com.sun.enterprise.ee.cms.core.*;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reference Implementation of JoinNotificationAction
 * @author Shreedhar Ganapathy
 *         Date: Mar 15, 2005
 * @version $Revision$
 */
public class JoinNotificationActionImpl implements JoinNotificationAction {
    private final CallBack callBack;
    private Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    public JoinNotificationActionImpl(final CallBack callBack) {
        this.callBack = callBack;
    }

    /**
     * Implementations of consumeSignal should strive to return control
     * promptly back to the thread that has delivered the Signal.
     */
    public void consumeSignal(final Signal s) throws ActionException {
        try {
            s.acquire();
            callBack.processNotification(s);
        } catch (SignalAcquireException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage());
        }

        try {
            s.release();
        } catch (SignalReleaseException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage());
        }
    }
}
