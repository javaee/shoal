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

package com.sun.enterprise.ee.cms.impl.common;

import com.sun.enterprise.ee.cms.core.FailureNotificationSignal;
import com.sun.enterprise.ee.cms.core.FailureRecoverySignal;
import com.sun.enterprise.ee.cms.core.FailureSuspectedSignal;
import com.sun.enterprise.ee.cms.core.GroupLeadershipNotificationSignal;
import com.sun.enterprise.ee.cms.core.JoinNotificationSignal;
import com.sun.enterprise.ee.cms.core.JoinedAndReadyNotificationSignal;
import com.sun.enterprise.ee.cms.core.MessageSignal;
import com.sun.enterprise.ee.cms.core.PlannedShutdownSignal;
import com.sun.enterprise.ee.cms.core.Signal;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * On a separate thread, analyses and handles the Signals delivered to it.
 * Picks up signals from a BlockingQueue and processes them.
 *
 * @author Shreedhar Ganapathy
 *         Date: Jan 22, 2004
 * @version $Revision$
 */
public class SignalHandler implements Runnable {
    private final BlockingQueue<SignalPacket> signalQueue;
    private final Router router;
    private Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    private AtomicBoolean stopped = new AtomicBoolean(false);

    /**
     * Creates a SignalHandler
     *
     * @param packetQueue the packet exchange queue
     * @param router the Router
     */
    public SignalHandler(final BlockingQueue<SignalPacket> packetQueue, final Router router) {
        this.signalQueue = packetQueue;
        this.router = router;
    }

    public void run() {
        try {
            Signal[] signals;
            while (!stopped.get()) {
                SignalPacket signalPacket = null;
                try {
                    signalPacket = signalQueue.take();
                    if (signalPacket != null) {
                        if ((signals = signalPacket.getSignals()) != null) {
                            handleSignals(signals);
                        } else {
                            handleSignal(signalPacket.getSignal());
                        }
                    }
                } catch (InterruptedException e) {
                    stopped.set(true);
                } catch (Throwable e) {
                    logger.log(Level.SEVERE, "sig.handler.unhandled", new Object[]{Thread.currentThread().getName()});
                    logger.log(Level.WARNING, "stack trace", e);
                }
            }
        } finally {
            logger.log(Level.INFO, "sig.handler.thread.terminated", new Object[]{Thread.currentThread().getName()});
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
        if (signal == null) {
            throw new IllegalArgumentException("Signal is null. Cannot analyze.");
        }
        if (logger.isLoggable(Level.FINEST)){
            logger.log(Level.FINEST, "SignalHandler : processing a received signal " + signal.getClass().getName());
        }
        try {
            if (signal instanceof FailureRecoverySignal) {
                router.notifyFailureRecoveryAction((FailureRecoverySignal) signal);
            } else if (signal instanceof FailureNotificationSignal) {
                router.aliveAndReadyView.processNotification(signal);
                router.notifyFailureNotificationAction(
                        (FailureNotificationSignal) signal);
            } else if (signal instanceof MessageSignal) {
                router.notifyMessageAction((MessageSignal) signal);
            } else if (signal instanceof JoinNotificationSignal) {
                router.notifyJoinNotificationAction((JoinNotificationSignal) signal);
            } else if (signal instanceof PlannedShutdownSignal) {
                router.aliveAndReadyView.processNotification(signal);
                router.notifyPlannedShutdownAction((PlannedShutdownSignal) signal);
            } else if (signal instanceof FailureSuspectedSignal) {
                router.notifyFailureSuspectedAction((FailureSuspectedSignal) signal);
            } else if (signal instanceof JoinedAndReadyNotificationSignal) {
                router.aliveAndReadyView.processNotification(signal);
                router.notifyJoinedAndReadyNotificationAction((JoinedAndReadyNotificationSignal) signal);
            } else if (signal instanceof GroupLeadershipNotificationSignal) {
                router.notifyGroupLeadershipNotificationAction((GroupLeadershipNotificationSignal) signal);
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, "sig.handler.ignoring.exception", new Object[]{t.getLocalizedMessage()});
            logger.log(Level.WARNING, t.getLocalizedMessage(), t);
        }
    }

    public void stop(Thread t) {
        stopped.set(true);
        t.interrupt();
    }
}
