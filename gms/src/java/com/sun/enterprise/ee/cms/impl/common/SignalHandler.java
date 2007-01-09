/*
 * Copyright 2004-2005 Sun Microsystems, Inc.  All rights reserved.
 * Use is subject to license terms.
 */
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

import com.sun.enterprise.ee.cms.core.*;

/**
 * On a separate thread, analyses and handles the Signals delivered to it.
 * Picks up signals from QueueHelper and processes them.
 *
 * @author Shreedhar Ganapathy
 *         Date: Jan 22, 2004
 * @version $Revision$
 */
public class SignalHandler implements Runnable {
    private final QueueHelper qh;
    private final Router router;

    public SignalHandler(final QueueHelper qh, final Router router) {
        this.qh = qh;
        this.router = router;
    }

    public void run() {
        Signal[] signals;
        while (true) {
            //todo infinite loop
            final SignalPacket signalPacket = qh.take();
            if ((signals = signalPacket.getSignals()) != null) {
                handleSignals(signals);
            } else {
                handleSignal(signalPacket.getSignal());
            }
        }
    }

    private void handleSignal(final Signal signal) {
        analyzeSignal(signal);
    }

    private void handleSignals(final Signal[] signals) {
        for (Signal signal : signals) {
            analyzeSignal(signal);
        }
    }

    private void analyzeSignal(final Signal signal) {
        if (signal instanceof FailureRecoverySignal) {
            router.notifyFailureRecoveryAction((FailureRecoverySignal) signal);
        } else if (signal instanceof FailureNotificationSignal) {
            router.notifyFailureNotificationAction(
                    (FailureNotificationSignal) signal);
        } else if (signal instanceof MessageSignal) {
            router.notifyMessageAction((MessageSignal) signal);
        } else if (signal instanceof JoinNotificationSignal) {
            router.notifyJoinNotificationAction((JoinNotificationSignal) signal);
        } else if (signal instanceof PlannedShutdownSignal) {
            router.notifyPlannedShutdownAction((PlannedShutdownSignal) signal);
        } else if (signal instanceof FailureSuspectedSignal) {
            router.notifyFailureSuspectedAction((FailureSuspectedSignal) signal);
        }
    }
}
