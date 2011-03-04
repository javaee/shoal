
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
import com.sun.enterprise.ee.cms.impl.client.*;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import com.sun.enterprise.ee.cms.spi.MemberStates;
import com.sun.enterprise.mgmt.ConfigConstants;
import com.sun.enterprise.mgmt.transport.MessageIOException;
import com.sun.enterprise.mgmt.transport.MessageImpl;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.enterprise.ee.cms.impl.common.GMSMonitor;


public class GroupManagementServiceImplTest extends TestCase {
    final private String instanceName = this.getClass().getSimpleName() + "_instance01";
    final private String groupName = this.getClass().getSimpleName() + "_testGroup";
    private GroupManagementService gms = null;
    private int numJoinsReceived = 0;
    private int numJoinedAndReadyReceived = 0;
    private int numPlannedShutdownReceived = 0;
    private int numGroupLeadership = 0;

    final private AtomicBoolean joinedAndReady = new AtomicBoolean(false);
    final private AtomicBoolean isShutdown = new AtomicBoolean(false);
    private AtomicLong numMsgReceived = new AtomicLong(0);

    public GroupManagementServiceImplTest( String testName ) {
        super( testName );
        //GMSLogDomain.getMcastLogger().setLevel(Level.FINEST);
    }

    public void mySetUp() throws GMSException {
        Properties props = new Properties();
        // do not wait default of 5 seconds to make yourself the master.
        props.put(ServiceProviderConfigurationKeys.DISCOVERY_TIMEOUT.toString(), 5L);
        props.put(ServiceProviderConfigurationKeys.MONITORING.toString(), 2L); // in seconds
        props.put(ServiceProviderConfigurationKeys.LOOPBACK.toString(), "true");
        gms = (GroupManagementService)
                GMSFactory.startGMSModule(instanceName, groupName, GroupManagementService.MemberType.CORE, props);
        assertTrue(gms != null);
        assertTrue(gms.getInstanceName().equals(instanceName));
        assertTrue(gms.getGroupName().equals(groupName));

        numGroupLeadership =0;
        gms.addActionFactory(new GroupLeadershipNotificationActionFactoryImpl(new CallBack() {
             public void processNotification(Signal sig) {
                if (sig instanceof GroupLeadershipNotificationSignal) {
                    GroupLeadershipNotificationSignal groupSig = (GroupLeadershipNotificationSignal)sig;
                }   numGroupLeadership++;
            }
        }));

        numJoinsReceived = 0;
        gms.addActionFactory(new JoinNotificationActionFactoryImpl(new CallBack() {
            public void processNotification(Signal sig) {
                if (sig instanceof JoinNotificationSignal) {
                    JoinNotificationSignal joinSig = (JoinNotificationSignal)sig;
                }   numJoinsReceived++;
            }

        }));
        numMsgReceived = new AtomicLong(0);
        gms.addActionFactory(new MessageActionFactoryImpl(new CallBack() {
            public void processNotification(Signal sig) {
                MessageSignal mSig = (MessageSignal)sig;
                numMsgReceived.incrementAndGet();
            }
        }), "testTargetComponent");
        gms.addActionFactory(new MessageActionFactoryImpl(new CallBack() {
            public void processNotification(Signal sig) {
                throw new NullPointerException("not a true null pointer exception. throwing for testing purposes");
            }
        }), "testExceptionThrowingMessageAction");

        numJoinedAndReadyReceived = 0;
        joinedAndReady.set(false);
        gms.addActionFactory(new JoinedAndReadyNotificationActionFactoryImpl(new CallBack() {
            public void processNotification(Signal sig) {
                if (sig instanceof JoinedAndReadyNotificationSignal) {
                    JoinedAndReadyNotificationSignal joinedReadySig = (JoinedAndReadyNotificationSignal)sig;
                    numJoinedAndReadyReceived++;
                    synchronized(joinedAndReady) {
                        boolean result = joinedAndReady.compareAndSet(false, true);
                        //assertTrue(result);
                        joinedAndReady.notify();
                    }
                }
            }

        }));
        numPlannedShutdownReceived = 0;
        isShutdown.set(false);
        gms.addActionFactory(new PlannedShutdownActionFactoryImpl(new CallBack() {
            public void processNotification(Signal sig) {
                if (sig instanceof PlannedShutdownSignal) {
                    PlannedShutdownSignal joinedReadySig = (PlannedShutdownSignal)sig;
                    numPlannedShutdownReceived++;
                    synchronized(isShutdown) {
                        boolean result = isShutdown.compareAndSet(false, true);
                        assertTrue(result);
                        isShutdown.notify();
                    }
                }
            }

        }));
    }

    public void testGMSMessageSend() throws GMSException {
        Logger log = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
        log.setLevel(Level.FINEST);
        System.out.println("beginTestGMSMessageSend");
        mySetUp();
        gms.join();
        GMSContext ctx = GMSContextFactory.getGMSContext(groupName);
        GMSMonitor monitor = ctx.getGMSMonitor();

         try {
            Thread.sleep(3000);
        } catch(InterruptedException ie) {}
        for (int i =0; i < 10; i++) {
            gms.getGroupHandle().sendMessage(instanceName, "NotRegisteredtestTargetComponent", "hello".getBytes());
            gms.getGroupHandle().sendMessage(instanceName, "NotRegistered2testTargetComponent", "goodbye".getBytes());          
        }
        for (int i =0; i < 20; i++) {
            gms.getGroupHandle().sendMessage(instanceName, "testTargetComponent", "hello".getBytes());
            gms.getGroupHandle().sendMessage(instanceName, "testTargetComponent", "goodbye".getBytes());
        }
        gms.getGroupHandle().sendMessage(instanceName, "testExceptionThrowingMessageAction", "hello".getBytes());
        try {
            gms.getGroupHandle().sendMessage("NonExistentInstanceName", "testTargetComponent", "hello".getBytes());
            assertTrue("must throw MemberNotInViewException sending to non-existent instance", false);
        } catch (MemberNotInViewException me) {
            System.out.println("Passed. Threw expected exception sending to non-existent instance." + me.getLocalizedMessage());
        }
        try {
            Thread.sleep(1000);
        } catch(InterruptedException ie) {}

        assertTrue("expected to receive 40 messages, only received " + numMsgReceived.get(), numMsgReceived.get() == 40);
        assertTrue(monitor.getGMSMessageMonitorStats("NotRegisteredtestTargetComponent").getNumMsgsNoListener() == 10);
        assertTrue(monitor.getGMSMessageMonitorStats("NotRegistered2testTargetComponent").getNumMsgsNoListener() == 10);            
        monitor.report();

        System.out.println("Test sending too big of a message");
        byte[] tooBigPayload = new byte[MessageImpl.getMaxMessageLength()];
        byte filler = 'e';
        Arrays.fill(tooBigPayload, filler);
        try {
            gms.getGroupHandle().sendMessage(instanceName, "testTargetComponent", tooBigPayload);
            assertTrue("Failed.  Should have thrown GMSException caused by MessageIOException sending too big a message payload", false);
        } catch (GMSException ge) {
            //log.log(Level.WARNING, "following exception thrown attempting to write too big of a message", ge);
            Throwable causeException = ge.getCause();
            String causeExceptionName = causeException == null ? "<noCauseException>" : causeException.getClass().getName();
            String causeMsg = causeException == null ? "" : causeException.getMessage();
            log.log(Level.INFO, "handled expected exception GMSException caused by " + causeExceptionName + ":" + causeMsg);
            assertTrue("Passed sending too big a message test. Handled expected exception.", causeException instanceof MessageIOException);
        } catch (Throwable t) {
            log.log(Level.WARNING, "unexpected exception sending too big of a message", t);
            assertTrue("Failed", false);
        }
        monitor.report();

        gms.shutdown(GMSConstants.shutdownType.INSTANCE_SHUTDOWN);
        log.setLevel(Level.INFO);                                                
    }

    public void testMultipleJoinsLeaves() throws GMSException {
        MemberStates memberState;

        mySetUp();
        memberState = gms.getGroupHandle().getMemberState(instanceName, 10000, 0);
        System.out.println("memberState after setup is " + memberState.name());
        assertTrue(memberState == MemberStates.UNKNOWN);
        assertTrue(gms.getGroupHandle().isMemberAlive(instanceName) == true);
        gms.join();

        memberState = gms.getGroupHandle().getMemberState(instanceName, 10000, 0);
        System.out.println("memberState after join is " + memberState.name());
        assertTrue(memberState == MemberStates.STARTING || memberState == MemberStates.ALIVE);
        assertTrue(gms.getGroupHandle().isMemberAlive(instanceName) == true);
        long startTime = System.currentTimeMillis();
        gms.reportJoinedAndReadyState();
        synchronized(joinedAndReady) {
            if (!joinedAndReady.get()) {
                try {
                    joinedAndReady.wait(4000);
                } catch (InterruptedException ie) {}
            }
            assertTrue(joinedAndReady.get());
        }
        long readynotifyduration = System.currentTimeMillis() - startTime;
        System.out.println("joinedAndReady = " + joinedAndReady.get() + " wait duration(ms)=" + readynotifyduration);
        memberState = gms.getGroupHandle().getMemberState(instanceName, 10000, 0);
        assertTrue("expected READY, observed "+ memberState.toString(),
                   memberState == MemberStates.READY || memberState == MemberStates.ALIVEANDREADY);

        // The Master never receives its own join.
        //assertTrue(numJoinsReceived==1);

        assertTrue(numJoinedAndReadyReceived==1);
        assertTrue(numPlannedShutdownReceived==0);

        gms.join();  // duplicate join should just be ignored.
        //assertTrue(numJoinsReceived==1);
        assertTrue(numJoinedAndReadyReceived==1);
        assertTrue(numPlannedShutdownReceived==0);

        // test multiple shutdown calls.
        gms.shutdown(GMSConstants.shutdownType.INSTANCE_SHUTDOWN);
        gms.shutdown(GMSConstants.shutdownType.INSTANCE_SHUTDOWN);

        // currently, an instance does not receive its shutdown notification.
        // In order to guarantee that master received shutdown notification, that may need to change in future.
        // for now, just simulate that planned shutdown was called for this instance by setting isShutdown to true.
        isShutdown.set(true);
        // remove above line in future if instance does receive its planned shutdown notification in future.

        synchronized(isShutdown) {
            if (!isShutdown.get()) {
                try {
                    isShutdown.wait();
                } catch (InterruptedException ie) {}
            }
            assertTrue(isShutdown.get());
        }
        memberState = gms.getGroupHandle().getMemberState(instanceName, 10000, 0);
        assertTrue(memberState == MemberStates.UNKNOWN);
        System.out.println("after shutdown memberState=" + memberState.name());
    }

    public void testRestartMember() throws GMSException {
        testMultipleJoinsLeaves();
        testMultipleJoinsLeaves();
    }

}
