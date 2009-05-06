/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
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

import com.sun.enterprise.ee.cms.core.*;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Routes signals to appropriate destinations
 *
 * @author Shreedhar Ganapathy
 *         Date: Jan 16, 2004
 * @version $Revision$
 */
public class Router {
    private final Vector<FailureNotificationActionFactory>
            failureNotificationAF = new Vector<FailureNotificationActionFactory>();

    private final Hashtable<String, FailureRecoveryActionFactory> failureRecoveryAF =
            new Hashtable<String, FailureRecoveryActionFactory>();

    private final Hashtable<String, MessageActionFactory> messageAF =
            new Hashtable<String, MessageActionFactory>();

    private final Vector<PlannedShutdownActionFactory> plannedShutdownAF =
            new Vector<PlannedShutdownActionFactory>();

    private final Vector<JoinNotificationActionFactory> joinNotificationAF =
            new Vector<JoinNotificationActionFactory>();

    private final Vector<JoinedAndReadyNotificationActionFactory> joinedAndReadyNotificationAF =
            new Vector<JoinedAndReadyNotificationActionFactory>();

    private final Vector<FailureSuspectedActionFactory> failureSuspectedAF =
            new Vector<FailureSuspectedActionFactory>();

    private final BlockingQueue<SignalPacket> queue;
    private final Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    private final ExecutorService actionPool;
    private long startupTime;
    private static final int GROUP_WARMUP_TIME = 30000;
    private static final int BUFSIZE = 100;
    private Thread signalHandlerThread;

    public Router() {
        queue = new ArrayBlockingQueue<SignalPacket>(BUFSIZE);
        final SignalHandler signalHandler = new SignalHandler(queue, this);
        //todo: there's no lifecycle handling here.  it would be good to add it deal with a graceful shutdown
        signalHandlerThread = new Thread(signalHandler, this.getClass().getCanonicalName() + " Thread");
        signalHandlerThread.start();
        actionPool = Executors.newCachedThreadPool();
        startupTime = System.currentTimeMillis();
    }

    /**
     * adds a FailureNotificationActionFactory as a destination.
     * Collects this actionfactory in a Collection of same type.
     *
     * @param failureNotificationActionFactory the FailureNotificationActionFactory
     *
     */
    void addDestination(final FailureNotificationActionFactory failureNotificationActionFactory) {
        failureNotificationAF.add(failureNotificationActionFactory);
    }

    /**
     * adds a FailureRecoveryActionFactory as a destination.
     * Collects this actionfactory in a Collection of same type.
     *
     * @param componentName the component name
     * @param failureRecoveryActionFactory the FailureRecoveryActionFactory
     */
    void addDestination(final String componentName, final FailureRecoveryActionFactory failureRecoveryActionFactory) {
        failureRecoveryAF.put(componentName, failureRecoveryActionFactory);
    }

    /**
     * adds a JoinNotificationActionFactory as a destination.
     * Collects this actionfactory in a Collection of same type.
     *
     * @param joinNotificationActionFactory the JoinNotificationActionFactory
     */
    void addDestination(final JoinNotificationActionFactory joinNotificationActionFactory) {
        joinNotificationAF.add(joinNotificationActionFactory);
    }

     /**
     * adds a JoinedAndReadyNotificationActionFactory as a destination.
     * Collects this actionfactory in a Collection of same type.
     *
     * @param joinedAndReadyNotificationActionFactory the JoinedAndReadyNotificationActionFactory
     */
    void addDestination(final JoinedAndReadyNotificationActionFactory joinedAndReadyNotificationActionFactory) {
        joinedAndReadyNotificationAF.add(joinedAndReadyNotificationActionFactory);
    }

    /**
     * adds a PlannedShutdownActionFactory as a destination.
     * Collects this actionfactory in a Collection of same type.
     *
     * @param plannedShutdownActionFactory the PlannedShutdownActionFactory
     */
    void addDestination(final PlannedShutdownActionFactory plannedShutdownActionFactory) {
        plannedShutdownAF.add(plannedShutdownActionFactory);
    }

    /**
     * adds a FailureSuspectedActionFactory as a destination.
     * Collects this actionfactory in a Collection of same type.
     *
     * @param failureSuspectedActionFactory the FailureSuspectedActionFactory
     */
    void addDestination(final FailureSuspectedActionFactory failureSuspectedActionFactory) {
        failureSuspectedAF.add(failureSuspectedActionFactory);
    }

    /**
     * adds a MessageActionFactory as a destination for a given component name.
     *
     * @param messageActionFactory the MessageActionFactory
     * @param componentName the component name
     */
    void addDestination(final MessageActionFactory messageActionFactory,final String componentName) {
        messageAF.put(componentName, messageActionFactory);
    }

    /**
     * removes a FailureNotificationActionFactory destination.
     *
     * @param failureNotificationActionFactory the FailureNotificationActionFactory
     *
     */
    void removeDestination(final FailureNotificationActionFactory failureNotificationActionFactory) {
        failureNotificationAF.remove(failureNotificationActionFactory);
    }

    /**
     * removes a JoinNotificationActionFactory destination.
     *
     * @param joinNotificationActionFactory the JoinNotificationActionFactory
     */
    void removeDestination(final JoinNotificationActionFactory joinNotificationActionFactory) {
        joinNotificationAF.remove(joinNotificationActionFactory);
    }

       /**
     * removes a JoinedAndReadyNotificationActionFactory destination.
     *
     * @param joinedAndReadyNotificationActionFactory the JoinedAndReadyNotificationActionFactory
     */
    void removeDestination(final JoinedAndReadyNotificationActionFactory joinedAndReadyNotificationActionFactory) {
        joinedAndReadyNotificationAF.remove(joinedAndReadyNotificationActionFactory);
    }
    /**
     * removes a PlannedShutdownActionFactory destination.
     *
     * @param plannedShutdownActionFactory the PlannedShutdownActionFactory
     */
    void removeDestination(final PlannedShutdownActionFactory plannedShutdownActionFactory) {
        plannedShutdownAF.remove(plannedShutdownActionFactory);
    }

    /**
     * removes a PlannedShutdownActionFactory destination.
     *
     * @param failureSuspectedActionFactory the PlannedShutdownActionFactory
     */
    void removeDestination(final FailureSuspectedActionFactory failureSuspectedActionFactory) {
        failureSuspectedAF.remove(failureSuspectedActionFactory);
    }

    /**
     * removes a MessageActionFactory instance belonging to a specified
     * component
     *
     * @param componentName the component name
     */
    public void removeMessageAFDestination(final String componentName) {
        messageAF.remove(componentName);
    }

    /**
     * removes a FailureRecoveryActionFactory instance belonging to a specified
     * component
     *
     * @param componentName the component name
     */
    public void removeFailureRecoveryAFDestination(final String componentName) {
        messageAF.remove(componentName);
    }

    /**
     * Queues signals.  Expects an array of signals which are handed off
     * to working threads that will determine their corresponding actions
     * to call their consumeSignal method.
     *
     * @param signalPacket the signal packet
     */
    public void queueSignals(final SignalPacket signalPacket) {
        try {
            queue.put(signalPacket);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Adds a single signal to the queue.
     *
     * @param signalPacket the signal packet
     */
    public void queueSignal(final SignalPacket signalPacket) {
        try {
            queue.put(signalPacket);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, e.getMessage());
            e.printStackTrace();
        }
    }

    void undocketAllDestinations() {
        synchronized (failureRecoveryAF) {
            failureRecoveryAF.clear();
        }
        synchronized (failureNotificationAF) {
            failureNotificationAF.removeAllElements();
        }
        synchronized (plannedShutdownAF) {
            plannedShutdownAF.removeAllElements();
        }
        synchronized (joinNotificationAF) {
            joinNotificationAF.removeAllElements();
        }
        synchronized (messageAF) {
            messageAF.clear();
        }
        synchronized (failureSuspectedAF) {
            failureSuspectedAF.removeAllElements();
        }
    }

    void notifyFailureNotificationAction(final FailureNotificationSignal signal) {
        FailureNotificationAction a;
        FailureNotificationSignal fns;
        logger.log(Level.INFO, "failurenotificationsignals.send.member", new Object[]{signal.getMemberToken()});
        synchronized (failureNotificationAF) {
            for (FailureNotificationActionFactory fnaf : failureNotificationAF) {
                a = (FailureNotificationAction) fnaf.produceAction();
                fns = new FailureNotificationSignalImpl(signal);
                callAction(a, fns);
            }
        }
    }

    void notifyFailureRecoveryAction(final FailureRecoverySignal signal) {
        final FailureRecoveryAction a;
        final FailureRecoverySignal frs;
        logger.log(Level.INFO, "failurenotificationsignals.send.component",
                new Object[]{signal.getComponentName()});
        synchronized (failureRecoveryAF) {
            final FailureRecoveryActionFactory fraf = failureRecoveryAF.get(signal.getComponentName());
            a = (FailureRecoveryAction) fraf.produceAction();
            frs = new FailureRecoverySignalImpl(signal);
            callAction(a, frs);
        }
    }

    void notifyFailureSuspectedAction(final FailureSuspectedSignal signal) {
        FailureSuspectedAction a;
        FailureSuspectedSignal fss;
        logger.log(Level.INFO, "failuresuspectedsignals.send.member",
                new Object[]{signal.getMemberToken()});
        synchronized (failureSuspectedAF) {
            for (FailureSuspectedActionFactory fsaf : failureSuspectedAF) {
                a = (FailureSuspectedAction) fsaf.produceAction();
                fss = new FailureSuspectedSignalImpl(signal);
                callAction(a, fss);
            }
        }
    }

    void notifyMessageAction(final MessageSignal signal) {

        synchronized (messageAF) {
            MessageActionFactory maf = messageAF.get(signal.getTargetComponent());
            if (maf != null) {
                MessageAction a = (MessageAction) maf.produceAction();
                try {
                    //due to message ordering requirements,
                    //this call is not delegated to a the thread pool
                    a.consumeSignal(signal);
                } catch (ActionException e) {
                    logger.log(Level.WARNING, "action.exception", new Object[]{e.getLocalizedMessage()});
                }
            }
        }
    }

    void notifyJoinNotificationAction(final JoinNotificationSignal signal) {
        JoinNotificationAction a;
        JoinNotificationSignal jns;
        //todo: NEED to be able to predetermine the number of GMS clients 
        //that would register for join notifications.
        if (isJoinNotificationAFRegistered()) {
            logger.log(Level.FINE,
                    MessageFormat.format("Sending JoinNotificationSignals to " +
                            "registered Actions, Member {0}...", signal.getMemberToken()));
            synchronized (joinNotificationAF) {
                for (JoinNotificationActionFactory jnaf : joinNotificationAF) {
                    a = (JoinNotificationAction) jnaf.produceAction();
                    jns = new JoinNotificationSignalImpl(signal);
                    callAction(a, jns);
                }
            }
        } else if (System.currentTimeMillis() - startupTime < GROUP_WARMUP_TIME) {
            // put it back to the queue if it is less than
            // 30 secs since start time. we give 30 secs for join notif
            // registrations to happen until which time, the signals are
            // available in queue. 
            try {
                queue.put(new SignalPacket(signal));
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, e.getMessage());
                e.printStackTrace();
            }
        }
    }

    void notifyJoinedAndReadyNotificationAction(final JoinedAndReadyNotificationSignal signal) {
        JoinedAndReadyNotificationAction a;
        JoinedAndReadyNotificationSignal jns;
        //todo: NEED to be able to predetermine the number of GMS clients
        //that would register for joined and ready notifications.
        if (isJoinedAndReadyNotificationAFRegistered()) {
            logger.log(Level.FINE,
                    MessageFormat.format("Sending JoinedAndReadyNotificationSignals to " +
                            "registered Actions, Member {0}...", signal.getMemberToken()));
            synchronized (joinedAndReadyNotificationAF) {
                for (JoinedAndReadyNotificationActionFactory jnaf : joinedAndReadyNotificationAF) {
                    a = (JoinedAndReadyNotificationAction) jnaf.produceAction();
                    jns = new JoinedAndReadyNotificationSignalImpl(signal);
                    callAction(a, jns);
                }
            }
        }
     }

    void notifyPlannedShutdownAction(final PlannedShutdownSignal signal) {
        PlannedShutdownAction a;
        PlannedShutdownSignal pss;
        logger.log(Level.INFO, "plannedshutdownsignals.send.member",
                new Object[]{signal.getEventSubType(), signal.getMemberToken()});
        synchronized (plannedShutdownAF) {
            for (PlannedShutdownActionFactory psaf : plannedShutdownAF) {
                a = (PlannedShutdownAction) psaf.produceAction();
                pss = new PlannedShutdownSignalImpl(signal);
                callAction(a, pss);
            }
        }
    }

    private void callAction(final Action a, final Signal signal) {
        try {
            final CallableAction task = new CallableAction(a, signal);
            actionPool.submit(task);
        } catch (RejectedExecutionException e) {
            logger.log(Level.WARNING, e.getMessage());
        }
    }

    public boolean isFailureNotificationAFRegistered() {
        boolean retval = true;
        synchronized (failureNotificationAF) {
            if (failureNotificationAF.isEmpty())
                retval = false;
        }
        return retval;
    }

    public boolean isFailureRecoveryAFRegistered() {
        boolean retval = true;
        synchronized (failureRecoveryAF) {
            if (failureRecoveryAF.isEmpty())
                retval = false;
        }
        return retval;
    }

    public boolean isMessageAFRegistered() {
        boolean retval = true;
        synchronized (messageAF) {
            if (messageAF.isEmpty())
                retval = false;
        }
        return retval;
    }

    public boolean isPlannedShutdownAFRegistered() {
        boolean retval = true;
        synchronized (plannedShutdownAF) {
            if (plannedShutdownAF.isEmpty())
                retval = false;
        }
        return retval;
    }

    public boolean isJoinNotificationAFRegistered() {
        boolean retval = true;
        synchronized (joinNotificationAF) {
            if (joinNotificationAF.isEmpty())
                retval = false;
        }
        return retval;
    }

       public boolean isJoinedAndReadyNotificationAFRegistered() {
        boolean retval = true;
        synchronized (joinedAndReadyNotificationAF) {
            if (joinedAndReadyNotificationAF.isEmpty())
                retval = false;
        }
        return retval;
    }

    public boolean isFailureSuspectedAFRegistered() {
        boolean retval = true;
        synchronized (failureSuspectedAF) {
            if (failureSuspectedAF.isEmpty()) {
                retval = false;
            }
        }
        return retval;
    }

    Hashtable<String, FailureRecoveryActionFactory>
    getFailureRecoveryAFRegistrations() {
        return
                new Hashtable<String, FailureRecoveryActionFactory>(failureRecoveryAF);
    }

    public Set<String> getFailureRecoveryComponents() {
        logger.log(Level.FINEST, MessageFormat.format("Router Returning failure " +
                "recovery components={0}", failureRecoveryAF.keySet()));
        return failureRecoveryAF.keySet();
    }

    /**
     * implements Callable. Used for handing off the job of calling the Action's
     * consumeSignal() method to a ThreadPool.
     */
    private static class CallableAction implements Callable<Object> {
        private Action action;
        private Signal signal;

        CallableAction(final Action action, final Signal signal) {
            this.action = action;
            this.signal = signal;
        }

        public Object call() throws ActionException {
            action.consumeSignal(signal);
            return null;
        }
    }
}
