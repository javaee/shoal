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
 * Reference Implementation of FailureSuspicionAction interface
 * 
 * @author Shreedhar Ganapathy
 *         Date: Sep 21, 2005
 * @version $Revision$
 */
public class FailureSuspectedActionImpl implements FailureSuspectedAction {
    private Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    private CallBack caller;

    public FailureSuspectedActionImpl (final CallBack caller) {
        this.caller=caller;
    }
    /**
     * processes the signal. typically involves getting information
     * from the signal, acquiring the signal and after processing, releasing
     * the signal
     * @param signal
     */
    public void consumeSignal(final Signal signal) {
        //ALWAYS call Acquire before doing any other processing
        try {
            signal.acquire();
            notifyListeners(signal);
            //ALWAYS call Release after completing any other processing.
            signal.release();
        } catch (SignalAcquireException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage());
        }
        catch (SignalReleaseException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage());
        }
    }

    private void notifyListeners(final Signal signal) {
        caller.processNotification(signal);
    }

}
