/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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
import com.sun.enterprise.ee.cms.core.*;
import com.sun.enterprise.ee.cms.impl.client.RejoinSubeventImpl;
import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class AliveAndReadyViewWindowTest extends TestCase {

    private AliveAndReadyViewWindow aliveAndReadyViewWindow;
    private SortedSet<String> clusterMembers;
    private SortedSet<String> coreClusterMembers;
    private List<String> currentCoreMembers;
    private List<String> currentMembers;

    final static private String DAS = "server";
    final static private String GROUP_NAME = "testgroup";
    final static private boolean IS_CORE = true;
    final static private boolean IS_SPECTATOR = false;
    final static private long START_TIME = 1L;

    public AliveAndReadyViewWindowTest( String testName ) {
        super( testName );
    }

    private JoinedAndReadyNotificationSignal createJoinAndReadyNotificationSignal(String memberName,
                                                                                  String groupName,
                                                                                  boolean isCore,
                                                                                  long startTime,
                                                                                  GMSConstants.startupType startupKind,
                                                                                  RejoinSubevent rse)
    {
        currentMembers.add(memberName);
        if (isCore) {
            currentCoreMembers.add(memberName);
        }
        JoinedAndReadyNotificationSignalImpl result =
                new JoinedAndReadyNotificationSignalImpl(memberName, currentCoreMembers, currentMembers,
                                                         groupName, startTime, startupKind, rse);
        return result;
    }

    private PlannedShutdownSignal createPlannedShutdownSignal(String memberName, String groupName,
                                                              boolean isCore, long startTime,
                                                              GMSConstants.shutdownType shutdownKind) {
        PlannedShutdownSignalImpl result =
                new PlannedShutdownSignalImpl(memberName, groupName, startTime, shutdownKind);
        boolean removeResult = currentMembers.remove(memberName);
        assertTrue(removeResult);
        if (isCore) {
            removeResult = currentCoreMembers.remove(memberName);
            assertTrue(removeResult);
        }
        return result; 
    }

    private FailureNotificationSignal createFailureNotificationSignal(String memberName,
                                                                      String groupName,
                                                                      boolean isCore,
                                                                      long startTime) {
        FailureNotificationSignalImpl result = new FailureNotificationSignalImpl(memberName, groupName, startTime);
        boolean removeResult = currentMembers.remove(memberName);
        assertTrue(removeResult);
        if (isCore) {
            removeResult = currentCoreMembers.remove(memberName);
            assertTrue(removeResult);
        }
        return result;
    }



    void mySetup() {
        clusterMembers = new TreeSet<String>();
        coreClusterMembers = new TreeSet<String>();
        currentCoreMembers = new LinkedList<String>();
        currentMembers = new LinkedList<String>();
        aliveAndReadyViewWindow = new AliveAndReadyViewWindow();
    }

    void startCluster(int numberOfInstances) {
        clusterMembers.add(DAS);
        for (int i = 101; i <= 100 + numberOfInstances ; i++) {
            final String member = "instance" + i;
            clusterMembers.add(member);
            coreClusterMembers.add(member);
        }

        JoinedAndReadyNotificationSignal jrSignal;
        jrSignal = this.createJoinAndReadyNotificationSignal(DAS, GROUP_NAME,  IS_SPECTATOR,
                                                                 START_TIME, GMSConstants.startupType.INSTANCE_STARTUP, null);
        aliveAndReadyViewWindow.processNotification(jrSignal);

        for (String memberName: coreClusterMembers) {
            jrSignal = this.createJoinAndReadyNotificationSignal(memberName, GROUP_NAME,  IS_CORE,
                                                                 START_TIME, GMSConstants.startupType.GROUP_STARTUP, null);
            aliveAndReadyViewWindow.processNotification(jrSignal);
        }
        if (coreClusterMembers.size() > 0) {
            System.out.println("Current AliveAndReadyView:" + aliveAndReadyViewWindow.getCurrentView().toString());
            System.out.println("Previous AliveAndReadyView:" + aliveAndReadyViewWindow.getPreviousView().toString());

            assertTrue("startCluster assertion failure coreClusterMembers != getCurrentView()", coreClusterMembers.equals(aliveAndReadyViewWindow.getCurrentView().getMembers()));
            assertTrue("startCluster assertion failure coreClusterMembers == getPrevioussView()", ! coreClusterMembers.equals(aliveAndReadyViewWindow.getPreviousView().getMembers()));
        } else if (coreClusterMembers.size() ==0 ) {
            assertTrue(aliveAndReadyViewWindow.getCurrentView().getMembers().size() == 0);
            assertTrue(aliveAndReadyViewWindow.getPreviousView().getMembers().size() == 0);
            assertTrue(aliveAndReadyViewWindow.getCurrentView().getViewId() !=
                       aliveAndReadyViewWindow.getPreviousView().getViewId());
        }
    }

    // same as startCluster, but does not use startupType.GROUP_STARTUP.  This simulates when one does not use start-cluster, but starts
    // each instance of the cluster up with start-local-instance in a manually manner.
    void simulateStartCluster(int numberOfInstances) {
        clusterMembers.add(DAS);
        for (int i = 101; i <= 100 + numberOfInstances ; i++) {
            final String member = "instance" + i;
            clusterMembers.add(member);
            coreClusterMembers.add(member);
        }

        JoinedAndReadyNotificationSignal jrSignal;
        jrSignal = this.createJoinAndReadyNotificationSignal(DAS, GROUP_NAME,  IS_SPECTATOR,
                                                                 START_TIME, GMSConstants.startupType.INSTANCE_STARTUP, null);
        aliveAndReadyViewWindow.processNotification(jrSignal);

        for (String memberName: coreClusterMembers) {
            jrSignal = this.createJoinAndReadyNotificationSignal(memberName, GROUP_NAME,  IS_CORE,
                                                                 START_TIME, GMSConstants.startupType.INSTANCE_STARTUP, null);
            aliveAndReadyViewWindow.processNotification(jrSignal);
        }
        if (coreClusterMembers.size() > 0) {
            assertTrue("startCluster assertion failure coreClusterMembers != getCurrentView()", coreClusterMembers.equals(aliveAndReadyViewWindow.getCurrentView().getMembers()));
            assertTrue("startCluster assertion failure coreClusterMembers != getPrevioussView()", coreClusterMembers.equals(aliveAndReadyViewWindow.getPreviousView().getMembers()));
        } else if (coreClusterMembers.size() ==0 ) {
            assertTrue(aliveAndReadyViewWindow.getCurrentView() == null);
            assertTrue(aliveAndReadyViewWindow.getPreviousView() == null);
        }
    }

    void stopCluster() {
        PlannedShutdownSignal shutdownSignal;
        for (String memberName : coreClusterMembers) {

            // verify that a stopped or killed instance has been restarted before trying to shutdown.
            if (currentMembers.contains(memberName)) {
                shutdownSignal = createPlannedShutdownSignal(memberName, GROUP_NAME, IS_CORE,  1L, GMSConstants.shutdownType.GROUP_SHUTDOWN);
                aliveAndReadyViewWindow.processNotification(shutdownSignal);
            }
        }

        // effectively shutdown the DAS as SPECTATOR.  DAS is not in coreClusterMembers set.
        for (String memberName : clusterMembers) {

            // verify that a stopped or killed instance has been restarted before trying to shutdown.
            if (currentMembers.contains(memberName)) {
                shutdownSignal = createPlannedShutdownSignal(memberName, GROUP_NAME, IS_SPECTATOR,  1L, GMSConstants.shutdownType.GROUP_SHUTDOWN);
                aliveAndReadyViewWindow.processNotification(shutdownSignal);
            }
        }
        // currently no assertions after group shutdown.  There is no state after shutdown is complete to verify.
    }

    public void testStartClusterStopCluster() throws GMSException {
        mySetup();
        startCluster(10);
        System.out.println("Current AliveAndReadyView:" + aliveAndReadyViewWindow.getCurrentView().toString());
        System.out.println("Previous AliveAndReadyView:" + aliveAndReadyViewWindow.getPreviousView().toString());
        stopCluster();
    }

//    public void testSimulateStartClusterStopCluster() throws GMSException {
//        mySetup();
//        simulateStartCluster(10);
//        stopCluster();
//    }


    public void testStopInstance() throws GMSException {
        mySetup();
        startCluster(10);
        String targetedInstance = clusterMembers.first();

        PlannedShutdownSignal  shutdownSignal = createPlannedShutdownSignal(targetedInstance, GROUP_NAME, IS_CORE,  1L, GMSConstants.shutdownType.INSTANCE_SHUTDOWN);
        aliveAndReadyViewWindow.processNotification(shutdownSignal);
        assertTrue(coreClusterMembers.equals(aliveAndReadyViewWindow.getPreviousView().getMembers()));
        assertTrue(aliveAndReadyViewWindow.getPreviousView().getSignal() instanceof PlannedShutdownSignal);
        assertTrue(aliveAndReadyViewWindow.getPreviousView().getSignal().equals(shutdownSignal));
        TreeSet<String> expectedMembers = new TreeSet<String>(coreClusterMembers);
        boolean result = expectedMembers.remove(targetedInstance);
        assertTrue(result);
        assertTrue(expectedMembers.equals(aliveAndReadyViewWindow.getCurrentView().getMembers()));
        stopCluster();
    }

    public void testStopInstanceStartInstance() throws GMSException {
        mySetup();
        startCluster(10);

        String targetedInstance = coreClusterMembers.first();

        // simulate shutdown of target instance
        PlannedShutdownSignal  shutdownSignal = createPlannedShutdownSignal(targetedInstance, GROUP_NAME, IS_CORE,  1L, GMSConstants.shutdownType.INSTANCE_SHUTDOWN);
        aliveAndReadyViewWindow.processNotification(shutdownSignal);
        assertTrue(coreClusterMembers.equals(aliveAndReadyViewWindow.getPreviousView().getMembers()));
        assertTrue(aliveAndReadyViewWindow.getPreviousView().getSignal() instanceof PlannedShutdownSignal);
        assertTrue(aliveAndReadyViewWindow.getPreviousView().getSignal().equals(shutdownSignal));
        TreeSet<String> expectedMembers = new TreeSet<String>(coreClusterMembers);
        boolean result = expectedMembers.remove(targetedInstance);
        assertTrue(result);
        assertTrue(expectedMembers.equals(aliveAndReadyViewWindow.getCurrentView().getMembers()));
        assertTrue(shutdownSignal.getCurrentView().getViewId() == aliveAndReadyViewWindow.getCurrentView().getViewId());
        assertTrue(shutdownSignal.getPreviousView().getViewId() == aliveAndReadyViewWindow.getPreviousView().getViewId());
        assertTrue(shutdownSignal.getCurrentView().getMembers().equals(aliveAndReadyViewWindow.getCurrentView().getMembers()));
        assertTrue(shutdownSignal.getPreviousView().getMembers().equals(aliveAndReadyViewWindow.getPreviousView().getMembers()));


        // simulate restart of target instance
        JoinedAndReadyNotificationSignal jrSignal;
        jrSignal = this.createJoinAndReadyNotificationSignal(targetedInstance, GROUP_NAME,  IS_CORE,
                                                             START_TIME, GMSConstants.startupType.INSTANCE_STARTUP, null);
        aliveAndReadyViewWindow.processNotification(jrSignal);
        assertTrue(expectedMembers.equals(aliveAndReadyViewWindow.getPreviousView().getMembers()));
        assertTrue(aliveAndReadyViewWindow.getPreviousView().getSignal().equals(jrSignal));
        expectedMembers.add(targetedInstance);
        assertTrue(expectedMembers.equals(aliveAndReadyViewWindow.getCurrentView().getMembers()));
        assertTrue(jrSignal.getCurrentView().getViewId() == aliveAndReadyViewWindow.getCurrentView().getViewId());
        assertTrue(jrSignal.getPreviousView().getViewId() == aliveAndReadyViewWindow.getPreviousView().getViewId());
        assertTrue(jrSignal.getCurrentView().getMembers().equals(aliveAndReadyViewWindow.getCurrentView().getMembers()));
        assertTrue(jrSignal.getPreviousView().getMembers().equals(aliveAndReadyViewWindow.getPreviousView().getMembers()));
        stopCluster();
    }

    public void testKillInstance() throws GMSException {
        mySetup();

        startCluster(10);

        final String killInstance = coreClusterMembers.first();
        FailureNotificationSignal  failureNotificationSignal = createFailureNotificationSignal(killInstance, GROUP_NAME, IS_CORE,  1L);
        aliveAndReadyViewWindow.processNotification(failureNotificationSignal);

        TreeSet<String> expectedMembers = new TreeSet<String>(coreClusterMembers);
        assertTrue(expectedMembers.equals(aliveAndReadyViewWindow.getPreviousView().getMembers()));
        assertTrue(aliveAndReadyViewWindow.getPreviousView().getSignal() instanceof FailureNotificationSignal);
        assertTrue(aliveAndReadyViewWindow.getPreviousView().getSignal().equals(failureNotificationSignal));
        assertTrue(failureNotificationSignal.getCurrentView().getViewId() == aliveAndReadyViewWindow.getCurrentView().getViewId());
        assertTrue(failureNotificationSignal.getPreviousView().getViewId() == aliveAndReadyViewWindow.getPreviousView().getViewId());
        assertTrue(failureNotificationSignal.getCurrentView().getMembers().equals(aliveAndReadyViewWindow.getCurrentView().getMembers()));
        assertTrue(failureNotificationSignal.getPreviousView().getMembers().equals(aliveAndReadyViewWindow.getPreviousView().getMembers()));

        boolean result = expectedMembers.remove(killInstance);
        assertTrue(result);
        assertTrue(expectedMembers.equals(aliveAndReadyViewWindow.getCurrentView().getMembers()));
        System.out.println("Current AliveAndReadyView:" + failureNotificationSignal.getCurrentView().toString());
        System.out.println("Previous AliveAndReadyView:" + failureNotificationSignal.getPreviousView().toString());
        stopCluster();
    }

    // simulate failure followed by a restart of instance that is longer than GMS heartbeat failure detection.
    public void testFailureInstanceRestartInstance() throws GMSException {
        mySetup();
        startCluster(10);

        final String killInstance = coreClusterMembers.first();
        FailureNotificationSignal  failureNotificationSignal = createFailureNotificationSignal(killInstance, GROUP_NAME, IS_CORE,  1L);
        aliveAndReadyViewWindow.processNotification(failureNotificationSignal);

        TreeSet<String> expectedMembers = new TreeSet<String>(coreClusterMembers);
        assertTrue(expectedMembers.equals(aliveAndReadyViewWindow.getPreviousView().getMembers()));
        assertTrue(aliveAndReadyViewWindow.getPreviousView().getSignal() instanceof FailureNotificationSignal);
        assertTrue(aliveAndReadyViewWindow.getPreviousView().getSignal().equals(failureNotificationSignal));

        boolean result = expectedMembers.remove(killInstance);
        assertTrue(result);
        assertTrue(expectedMembers.equals(aliveAndReadyViewWindow.getCurrentView().getMembers()));
        assertTrue(failureNotificationSignal.getCurrentView().getViewId() == aliveAndReadyViewWindow.getCurrentView().getViewId());
        assertTrue(failureNotificationSignal.getPreviousView().getViewId() == aliveAndReadyViewWindow.getPreviousView().getViewId());
        assertTrue(failureNotificationSignal.getCurrentView().getMembers().equals(aliveAndReadyViewWindow.getCurrentView().getMembers()));
        assertTrue(failureNotificationSignal.getPreviousView().getMembers().equals(aliveAndReadyViewWindow.getPreviousView().getMembers()));

        // simulate restart of killed instance
        JoinedAndReadyNotificationSignal jrSignal;
        jrSignal = this.createJoinAndReadyNotificationSignal(killInstance, GROUP_NAME,  IS_CORE,
                                                             START_TIME + 1, GMSConstants.startupType.INSTANCE_STARTUP, null);
        aliveAndReadyViewWindow.processNotification(jrSignal);
        assertTrue(expectedMembers.equals(aliveAndReadyViewWindow.getPreviousView().getMembers()));
        assertTrue(aliveAndReadyViewWindow.getPreviousView().getSignal().equals(jrSignal));
        expectedMembers.add(killInstance);
        assertTrue(expectedMembers.equals(aliveAndReadyViewWindow.getCurrentView().getMembers()));
        assertTrue(jrSignal.getCurrentView().getViewId() == aliveAndReadyViewWindow.getCurrentView().getViewId());
        assertTrue(jrSignal.getPreviousView().getViewId() == aliveAndReadyViewWindow.getPreviousView().getViewId());
        assertTrue(jrSignal.getCurrentView().getMembers().equals(aliveAndReadyViewWindow.getCurrentView().getMembers()));
        assertTrue(jrSignal.getPreviousView().getMembers().equals(aliveAndReadyViewWindow.getPreviousView().getMembers()));
        stopCluster();
    }

    public void testZeroMemberCluster() {
        mySetup();
        startCluster(0);
        stopCluster();
    }


    // confirms that an instance started 10 seconds after cluster started will result in a previous view
    // that differs from current view.
//    public void testStartInstance() throws GMSException {
//           final long JUNIT_TEST_STARTCLUSTER_MAX_DURATION = 1000;
//           mySetup();
//           aliveAndReadyViewWindow.setStartClusterMaxDuration(JUNIT_TEST_STARTCLUSTER_MAX_DURATION);  // for unit testing only, set to 1 second.
//           simulateStartCluster(9); // do not start instance110.  Ensure that this results in current not equaling previous.
//           TreeSet<String> expectedMembers = new TreeSet<String>(coreClusterMembers);
//           String targetedInstance = "instance110";
//
//
//            try {
//                Thread.sleep(JUNIT_TEST_STARTCLUSTER_MAX_DURATION);
//            } catch(InterruptedException e) {}
//
//           // start instance after simulated start cluster completed.
//           JoinedAndReadyNotificationSignal jrSignal;
//           jrSignal = this.createJoinAndReadyNotificationSignal(targetedInstance, GROUP_NAME,  IS_CORE,
//                                                                START_TIME, GMSConstants.startupType.INSTANCE_STARTUP);
//           aliveAndReadyViewWindow.processNotification(jrSignal);
//
//           // assert that the starting of this instance resulted in a new aliveAndReadyView, that it was not incorrectly
//           // folded into start-cluster.
//           assertTrue(expectedMembers.equals(aliveAndReadyViewWindow.getPreviousView().getMembers()));
//           assertTrue(aliveAndReadyViewWindow.getPreviousView().getSignal().equals(jrSignal));
//           expectedMembers.add(targetedInstance);
//           assertTrue(expectedMembers.equals(aliveAndReadyViewWindow.getCurrentView().getMembers()));
//           stopCluster();
//       }




    public void testRejoin() throws GMSException {
        mySetup();
        startCluster(10);
        final String killInstance = coreClusterMembers.first();

        // simulate fast restart of killed instance.
        // GMS fails to detect FAILURE due to instance being restarted quicker than heartbeat failure
        // detection can detect the failure.
        JoinedAndReadyNotificationSignalImpl jrSignal;
        RejoinSubevent rjse = new RejoinSubeventImpl(System.currentTimeMillis() - 4000);
        jrSignal = (JoinedAndReadyNotificationSignalImpl)this.createJoinAndReadyNotificationSignal(killInstance, GROUP_NAME,  IS_CORE,
                                                             START_TIME + 1, GMSConstants.startupType.INSTANCE_STARTUP, rjse);
        JoinedAndReadyNotificationSignal copyCtor = new JoinedAndReadyNotificationSignalImpl(jrSignal);
        assertTrue(copyCtor.getRejoinSubevent().equals(rjse));
        aliveAndReadyViewWindow.processNotification(jrSignal);

        // assert that previous and current view are same when a JoinedAndReady with REJOIN subevent occurs.
        TreeSet<String> expectedMembers = new TreeSet<String>(coreClusterMembers);
        assertTrue(expectedMembers.equals(aliveAndReadyViewWindow.getPreviousView().getMembers()));
        assertTrue(aliveAndReadyViewWindow.getPreviousView().getSignal().equals(jrSignal));
        assertTrue(expectedMembers.equals(aliveAndReadyViewWindow.getCurrentView().getMembers()));
        assertTrue(aliveAndReadyViewWindow.getCurrentView().getMembers().equals(aliveAndReadyViewWindow.getPreviousView().getMembers()));
        assertTrue(jrSignal.getCurrentView().getViewId() == aliveAndReadyViewWindow.getCurrentView().getViewId());
        assertTrue(jrSignal.getPreviousView().getViewId() == aliveAndReadyViewWindow.getPreviousView().getViewId());
        assertTrue(jrSignal.getCurrentView().getMembers().equals(aliveAndReadyViewWindow.getCurrentView().getMembers()));
        assertTrue(jrSignal.getPreviousView().getMembers().equals(aliveAndReadyViewWindow.getPreviousView().getMembers()));
        stopCluster();
    }
}

