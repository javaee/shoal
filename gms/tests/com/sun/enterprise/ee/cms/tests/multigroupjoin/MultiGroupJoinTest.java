/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.ee.cms.tests.multigroupjoin;

/**
 * Created by IntelliJ IDEA.
 * User: sheetal
 * Date: Jan 14, 2008
 * Time: 2:56:41 PM
 * This test is for checking if a server instance can join 2 different groups within the same VM
 * This test can be run in 2 different terminals to see if the 2 server instances that are started in 2 different
 * VMs can join the 2 groups and send/receive messages from each other.
 */

import com.sun.enterprise.ee.cms.core.*;
import com.sun.enterprise.ee.cms.impl.client.*;
import com.sun.enterprise.ee.cms.impl.base.Utility;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MultiGroupJoinTest implements CallBack {
    final static Logger logger = Logger.getLogger("MultiGroupJoinTest");
    final Object waitLock = new Object();
    final String serverName;
    public MultiGroupJoinTest(String serverName) {
        this.serverName = serverName;
    }

    public static void main(String[] args){
        Utility.setLogger(logger);
        Utility.setupLogHandler();
        MultiGroupJoinTest multiGroupJoin = new MultiGroupJoinTest(System.getProperty("INSTANCEID"));
        try {
            multiGroupJoin.runSimpleSample();
        } catch (GMSException e) {
            logger.log(Level.SEVERE, "Exception occured while joining group:" + e);
        }
    }

    /**
     * Runs this sample
     * @throws GMSException
     */
    private void runSimpleSample() throws GMSException {
        logger.log(Level.INFO, "Starting MultiGroupJoinTest....");

        //final String serverName = "server"+System.currentTimeMillis();
        final String group1 = "Group1";
        final String group2 = "Group2";

        //initialize Group Management Service
        GroupManagementService gms1 = initializeGMS(serverName, group1);
        GroupManagementService gms2 = initializeGMS(serverName, group2);

        //register for Group Events
        registerForGroupEvents(gms1);
        registerForGroupEvents(gms2);
        //join group
        joinGMSGroup(group1, gms1);
        joinGMSGroup(group2, gms2);
        try {
            //send some messages
            sendMessages(gms1, serverName, group1);
            sendMessages(gms2, serverName, group2);
            waitForShutdown();

        } catch (InterruptedException e) {
            logger.log(Level.WARNING, e.getMessage());
        }
        //leave the group gracefully
        leaveGroupAndShutdown(serverName, gms1);
        leaveGroupAndShutdown(serverName, gms2);
        System.exit(0);

    }

    private GroupManagementService initializeGMS(String serverName, String groupName) {
        logger.log(Level.INFO, "Initializing Shoal for member: "+serverName+" group:"+groupName);
        return (GroupManagementService) GMSFactory.startGMSModule(serverName,
                groupName, GroupManagementService.MemberType.CORE, null);
    }

    private void registerForGroupEvents(GroupManagementService gms) {
        logger.log(Level.INFO, "Registering for group event notifications");
        gms.addActionFactory(new JoinNotificationActionFactoryImpl(this));
        gms.addActionFactory(new FailureSuspectedActionFactoryImpl(this));
        gms.addActionFactory(new FailureNotificationActionFactoryImpl(this));
        gms.addActionFactory(new PlannedShutdownActionFactoryImpl(this));
        gms.addActionFactory(new JoinedAndReadyNotificationActionFactoryImpl(this));
        gms.addActionFactory(new MessageActionFactoryImpl(this),"SimpleSampleComponent");
    }

    private void joinGMSGroup(String groupName, GroupManagementService gms) throws GMSException {
        logger.log(Level.INFO, "Joining Group "+groupName);
        gms.join();
    }

    private void sendMessages(GroupManagementService gms, String serverName, String groupName) throws InterruptedException, GMSException {
        logger.log(Level.INFO, "wait 5 secs to send 10 messages");
        synchronized(waitLock){
            waitLock.wait(10000);
        }
        GroupHandle gh = gms.getGroupHandle();

        logger.log(Level.INFO, "Sending messages...");
        for(int i = 0; i<=10; i++ ){
            gh.sendMessage("SimpleSampleComponent",
                    MessageFormat.format("Message {0}from server {1} to group {2}", i, serverName, groupName).getBytes());
            logger.info("Message " + i + " sent from " + serverName + " to Group " + groupName);
        }
    }

    private void waitForShutdown() throws InterruptedException {
        logger.log(Level.INFO, "wait 10 secs to shutdown");
        synchronized(waitLock){
            waitLock.wait(20000);
        }
    }

    private void leaveGroupAndShutdown(String serverName, GroupManagementService gms) {
        logger.log(Level.INFO, "Shutting down gms " + gms + "for server " + serverName);
        gms.shutdown(GMSConstants.shutdownType.INSTANCE_SHUTDOWN);
        //System.exit(0);
    }

    public void processNotification(Signal signal) {
        logger.log(Level.INFO, "Received Notification of type : "+signal.getClass().getName());
        try {
            signal.acquire();
            logger.log(Level.INFO,"Source Member: "+signal.getMemberToken() + " group : " + signal.getGroupName());
            if(signal instanceof MessageSignal){
                logger.log(Level.INFO,"Message: "+new String(((MessageSignal)signal).getMessage()));
            }
            signal.release();
        } catch (SignalAcquireException e) {
            logger.log(Level.WARNING, "Exception occured while acquiring signal"+e);
        } catch (SignalReleaseException e) {
            logger.log(Level.WARNING, "Exception occured while releasing signal"+e);
        }

    }
}

