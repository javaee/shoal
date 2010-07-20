/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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
import com.sun.enterprise.ee.cms.impl.client.FailureNotificationActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.JoinedAndReadyNotificationActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.PlannedShutdownActionFactoryImpl;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import com.sun.enterprise.ee.cms.spi.MemberStates;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AliveAndReadyViewWindow {
    protected static final Logger LOG = Logger.getLogger(GMSLogDomain.GMS_LOGGER + ".ready");

    static final long MIN_VIEW_DURATION = 1000;  // 1 second
    private long MAX_CLUSTER_STARTTIME_DURATION_MS = 10000; // todo: revisit this constant of 10 seconds for cluster startup.

    static final int MAX_ALIVE_AND_READY_VIEWS = 5;
    private final List<AliveAndReadyView> aliveAndReadyView = new LinkedList<AliveAndReadyView>();
    private long viewId = 1;

    private JoinedAndReadyNotificationActionFactoryImpl joinedAndReadyActionFactory = null;
    private FailureNotificationActionFactoryImpl failureActionFactory = null;
    private PlannedShutdownActionFactoryImpl plannedShutdownFactory = null;

    final private JoinedAndReadyCallBack jrcallback;
    final private LeaveCallBack leaveCallback;

    private long simulatedStartClusterTime;
    private AtomicBoolean isSimulatedStartCluster = new AtomicBoolean(false);
    private final long startTime;
    private final String currentInstanceName;

    // set to Level.INFO to aid debugging.
    static private final Level TRACE_LEVEL = Level.FINE;

    public AliveAndReadyViewWindow(GMSContext ctx) {
        Router router = ctx.getRouter();
        currentInstanceName = ctx.getServerIdentityToken();

        jrcallback = new JoinedAndReadyCallBack(ctx.getGroupHandle(), aliveAndReadyView);
        joinedAndReadyActionFactory = new JoinedAndReadyNotificationActionFactoryImpl(jrcallback);

        leaveCallback = new LeaveCallBack(ctx.getGroupHandle(), aliveAndReadyView);
        failureActionFactory = new FailureNotificationActionFactoryImpl(leaveCallback);
        plannedShutdownFactory = new PlannedShutdownActionFactoryImpl(leaveCallback);

        router.addSystemDestination(joinedAndReadyActionFactory);
        router.addSystemDestination(failureActionFactory);
        router.addSystemDestination(plannedShutdownFactory);
        startTime = System.currentTimeMillis();
    }

    // junit testing only - only scope to package access
    AliveAndReadyViewWindow() {
        jrcallback = new JoinedAndReadyCallBack(null, aliveAndReadyView);
        leaveCallback = new LeaveCallBack(null, aliveAndReadyView);
        startTime = System.currentTimeMillis();
        currentInstanceName = null;
    }

    public void setStartClusterMaxDuration(long durationInMs) {
        MAX_CLUSTER_STARTTIME_DURATION_MS = durationInMs;
    }

    // junit testing only - only scope to package access
    void junitProcessNotification(Signal signal) {
        if (signal instanceof JoinedAndReadyNotificationSignal) {
            jrcallback.processNotification(signal);
        } else if (signal instanceof PlannedShutdownSignal || signal instanceof FailureNotificationSignal) {
            leaveCallback.processNotification(signal);
        }
    }


    public AliveAndReadyView getPreviousView() {
        AliveAndReadyView result = null;
        synchronized (aliveAndReadyView) {
            int size = aliveAndReadyView.size();
            if (size >= 2) {
                result = aliveAndReadyView.get(size - 2);
                Signal signal = result.getSignal();

                // edge case handling.  Checking for start-cluster situation. No previous view during start-cluster, just current view.
                if (signal != null && signal instanceof JoinedAndReadyNotificationSignal) {
                    JoinedAndReadyNotificationSignal jrsignal = (JoinedAndReadyNotificationSignal)signal;
                    if (jrsignal.getEventSubType() == GMSConstants.startupType.GROUP_STARTUP ) {
                         // return current view when previous join and ready had a short duration and looks like it was part of startup.
                        if (LOG.isLoggable(TRACE_LEVEL)) {
                            LOG.log(TRACE_LEVEL, "getPreviousAliveAndReadyView: returning current view during cluster startup. JoinedAndReadyNotificationSignal indicates " + jrsignal.getEventSubType());
                        }
                        result = aliveAndReadyView.get(size - 1);
                    } else if (isSimulatedStartCluster.get() ) {
                        long duration = result.getSignalTime() - simulatedStartClusterTime;
                        if (duration  < MAX_CLUSTER_STARTTIME_DURATION_MS) {

                            // return current view when previous join and ready had a short duration and looks like it was part of startup.
                            if (LOG.isLoggable(TRACE_LEVEL)) {
                                LOG.log(TRACE_LEVEL, "getPreviousAliveAndReadyView: returning current view since detected cluster startup due to JoinedAndReadyNotification occurring within " + duration + " ms of initial startup");
                            }
                            result = aliveAndReadyView.get(size - 1);
                        }
                    }
                }
            } else if (size == 1) {
                result = aliveAndReadyView.get(0);
            }
        }
        // return current view when previous join and ready had a short duration and looks like it was part of startup.
        if (LOG.isLoggable(TRACE_LEVEL)) {
            LOG.log(TRACE_LEVEL, "getPreviousAliveAndReadyView: returning " + result);
        }

        return result;
    }

    public AliveAndReadyView getCurrentView() {
        AliveAndReadyView result = null;
        synchronized (aliveAndReadyView) {
            int length = aliveAndReadyView.size();
            if (length > 0) {
                result = aliveAndReadyView.get(length - 1);
            }
        }
        return result;
    }

    private abstract class CommonCallBack implements CallBack {
        final protected List<AliveAndReadyView> aliveAndReadyView;
        final protected GroupHandle gh;

        public CommonCallBack(GroupHandle gh, List<AliveAndReadyView> aliveAndReadyViews) {
            this.aliveAndReadyView = aliveAndReadyViews;
            this.gh = gh;
        }

        public void add(Signal signal, SortedSet<String> members) {
            // complete the current view with signal indicating transition that makes this the previous view.
            AliveAndReadyViewImpl current = (AliveAndReadyViewImpl)getCurrentView();
            if (current != null) {
                current.setSignal(signal);
            }
            
            // create a new current view
            AliveAndReadyView arview = new AliveAndReadyViewImpl(members, viewId++);
            aliveAndReadyView.add(arview);
            if (aliveAndReadyView.size() > MAX_ALIVE_AND_READY_VIEWS) {
                aliveAndReadyView.remove(0);
            }
        }
    }

    private class LeaveCallBack extends CommonCallBack {

        public LeaveCallBack(GroupHandle gh, List<AliveAndReadyView> aliveAndReadyView) {
            super(gh, aliveAndReadyView);
        }

        public void processNotification(Signal signal) {
            if (signal instanceof PlannedShutdownSignal ||
                signal instanceof FailureNotificationSignal) {
                synchronized (aliveAndReadyView) {

                    // only consider CORE members.
                    AliveAndReadyView current = getCurrentView();
                    if (current != null && current.getMembers().contains(signal.getMemberToken())) {
                        SortedSet<String> currentMembers = new TreeSet<String>(current.getMembers());
                        boolean result = currentMembers.remove(signal.getMemberToken());
                        assert (result);
                        add(signal, currentMembers);
                    }
                }
            }
        }
    }

    private boolean isMySignal(Signal sig) {
        return currentInstanceName != null && currentInstanceName.equals(sig.getMemberToken());
    }

    private boolean isStartupPeriod() {
        return (System.currentTimeMillis() - startTime) < 10000;
    }

    private class JoinedAndReadyCallBack extends CommonCallBack {

        public JoinedAndReadyCallBack(GroupHandle gh, List<AliveAndReadyView> aliveAndReadyView) {
            super(gh, aliveAndReadyView);
        }

        public void processNotification(Signal signal) {
            if (signal instanceof JoinedAndReadyNotificationSignal) {
                final JoinedAndReadyNotificationSignal jrns = (JoinedAndReadyNotificationSignal) signal;
                final RejoinSubevent rejoin = jrns.getRejoinSubevent();
                if (jrns.getCurrentCoreMembers().contains(signal.getMemberToken())) {
                    synchronized (aliveAndReadyView) {

                        if (aliveAndReadyView.size() == 0) {
                            if (LOG.isLoggable(TRACE_LEVEL)) {
                                LOG.log(TRACE_LEVEL, "first alive and ready view: initializing");
                            }

                            // perform first time startup check for all existing CORE members to find ones that
                            // are already ALIVEANDREADY.
                            SortedSet<String> aliveAndReadyMembers = new TreeSet<String>();
                            for (String member : jrns.getCurrentCoreMembers()) {
                                if (member.compareTo(signal.getMemberToken()) == 0) {

                                    // receiving this member's alive and ready signal, no need to check its state.
                                    aliveAndReadyMembers.add(member);
                                } else if (gh != null) {
                                    MemberStates states = gh.getMemberState(member, 10000, 0);
                                    switch (states) {
                                        case ALIVEANDREADY:
                                        case READY:
                                            aliveAndReadyMembers.add(member);
                                            if (LOG.isLoggable(TRACE_LEVEL)) {
                                                LOG.log(TRACE_LEVEL, "member added " + member + " with  a heartbeat state of " + states.toString());
                                            }
                                            break;

                                        case UNKNOWN:

                                            // member in the UNKNOWN state have either failed during start up,
                                            // just have not reached the ready state, may have a multicast broadcast issue.
                                            if (LOG.isLoggable(TRACE_LEVEL)) {
                                                LOG.log(TRACE_LEVEL, "aliveAndReadyView initialization: member " + member + " has an UNKNOWN member state from 10 seconds of heartbeat state");
                                            }
                                            // MemberStates stateQuery = gh.getMemberState(member, 10000, 2000);
                                            // if (stateQuery == MemberStates.ALIVEANDREADY ||  stateQuery == MemberStates.READY) {
                                            //    aliveAndReadyMembers.add(member);
                                            // } else if (stateQuery == MemberStates.UNKNOWN) {
                                            //    LOG.warning("aliveAndReadyView initialization: member " + member + " still unknown state after a direct query on state");
                                            // }
                                            break;
                                        
                                        case ALIVE:
                                        default:
                                            if (LOG.isLoggable(TRACE_LEVEL)) {
                                                LOG.log(TRACE_LEVEL, "member " + member + " not added with a heartbeat state of " + states.toString());
                                            }
                                            break;
                                    }
                                }
                            }
                            if (aliveAndReadyMembers.size() > 0) {

                                // disabled simulated cluster.

//                                if (jrns.getEventSubType() == GMSConstants.startupType.INSTANCE_STARTUP) {
//                                    isSimulatedStartCluster.compareAndSet(false, true);
//                                    simulatedStartClusterTime = System.currentTimeMillis();
//                                }
                                add(signal, aliveAndReadyMembers);
                            }
                        } else {

                            AliveAndReadyView current = aliveAndReadyView.get(aliveAndReadyView.size() - 1);
                            SortedSet<String> currentMembers = new TreeSet<String>(current.getMembers());

                            // only do this check in first 10 seconds of up time.
                            // also do for instance's own joined and ready.
                            // compensate for possible missed JoinedAndReady at start up.
                            if (gh != null && (isStartupPeriod() || isMySignal(signal))) {
                                // check for missed JoinedAndReady
                                for (String member : jrns.getCurrentCoreMembers()) {
                                    if (currentMembers.contains(member)) {
                                        continue;
                                    }

                                    if (member.compareTo(signal.getMemberToken()) == 0) {
                                        // handle below.
                                        continue;
                                    }

                                    MemberStates states = gh.getMemberState(member, 10000, 0);
                                    switch (states) {
                                        case READY:
                                        case ALIVEANDREADY:
                                            currentMembers.add(member);
                                            // tmp debug
                                            if (LOG.isLoggable(Level.INFO)) {
                                                LOG.log(Level.INFO, "CORRECTION: member added " + member + " with  a heartbeat state of " + states.toString());
                                            }
                                            break;
                                        default:
                                    }
                                }
                            }

                            if (rejoin == null) {
                                // handle joined and ready with no REJOIN subevent
                                boolean result = currentMembers.add(signal.getMemberToken());
                                assert (result);
                            } // else REJOIN means that GMS missed detecting a FAILURE and restarted instance has
                              // already JOINED again.  previous and current view are same when signal is REJOIN.
                            add(signal, currentMembers);
                        }
                    } // end synchronized aliveAndReadyView
                }// else ignore non-CORE member joinedandreadynotification signal.
            }
        }
    }
}
