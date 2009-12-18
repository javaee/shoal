/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.enterprise.shoal.messagesenderreceiver;

import com.sun.enterprise.ee.cms.core.*;
import com.sun.enterprise.ee.cms.impl.client.JoinedAndReadyNotificationActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.PlannedShutdownActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.MessageActionFactoryImpl;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class SenderReceiver {

    private GroupManagementService gms = null;
    private static final Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    private final String group = "TestGroup";
    static ConcurrentHashMap<String, ConcurrentHashMap> chm = new ConcurrentHashMap<String, ConcurrentHashMap>();
    static AtomicBoolean completedCheck = new AtomicBoolean(false);
    static boolean msgIdReceived[];
    static String memberID = null;
    static int numberOfInstances = 0;
    static int messageSize = 0;
    static int numberOfMessages = 0;
    static int numOfStopMsgReceived = 0;
    static int numberOfPlannedShutdown = 0;
    static int numberOfJoinAndReady = 0;
    static List<String> waitingToReceiveStopFrom;

    public static void main(String[] args) {

        //for (int z = 0; z < args.length; z++) {
        //    System.out.println(z + "=" + args[z]);
        //}

        if (args[0].equalsIgnoreCase("-h")) {
            usage();
        }

        if (args[0].equalsIgnoreCase("server")) {
            if (args.length != 2) {
                usage();
            }
            memberID = args[0];
            System.out.println("memberID=" + memberID);
            numberOfInstances = Integer.parseInt(args[1]);
            System.out.println("numberOfInstances=" + numberOfInstances);
        } else if (args[0].contains("instance")) {
            if (args.length >= 4) {
                memberID = args[0];
                System.out.println("memberID=" + memberID);
                numberOfInstances = Integer.parseInt(args[1]);
                System.out.println("numberOfInstances=" + numberOfInstances);
                messageSize = Integer.parseInt(args[2]);
                System.out.println("messageSize=" + messageSize);
                numberOfMessages = Integer.parseInt(args[3]);
                System.out.println("numberOfMessages=" + numberOfMessages);
            } else {
                usage();
            }
        } else {
            usage();
        }

        SenderReceiver sender = new SenderReceiver();
        try {
            //sender.test(memberID, numberOfInstances, messageSize, numberOfMessages);
            sender.test();
        } catch (GMSException e) {
            logger.log(Level.SEVERE, "Exception occured while joining group:" + e);
        }
        sender.waitTillDone();

        // only do verification for INSTANCES
        if (!memberID.equalsIgnoreCase("server")) {
            System.out.println("Checking to see if correct number of messages (" + numberOfMessages + ")  were received from each instance");

            //System.out.println("chm.size()=" + chm.size());

            Enumeration e = chm.keys();
            while (e.hasMoreElements()) {
                int droppedMessages = 0;

                String key = (String) e.nextElement();
                ConcurrentHashMap instance_chm = chm.get(key);

                for (int i = 0; i < numberOfMessages; i++) {
                    if (instance_chm.get(i) == null) {
                        droppedMessages++;
                        System.out.println("Never received msg id " + i);
                    }
                }
                System.out.println("================================================================");

                if (droppedMessages == 0) {
                    System.out.println(key + ": PASS.  No dropped messages");
                } else {
                    System.out.println(key + ": FAILED. Confirmed (" + droppedMessages + ") messages were dropped from: " + key);
                }

            }
        }
        System.out.println("================================================================");
        logger.log(Level.INFO, "Testing Complete");

    }

    public static void usage() {

        System.out.println(" For server:");
        System.out.println("    <memberid(server)> <number_of_instances>");
        System.out.println(" For instances:");
        System.out.println("    <memberid(instancexxx)> <number_of_instances> <messagesize> <number_of_messages>");
        System.exit(0);
    }
    //private void test(String memberID, int numberOfInstances, int messageSize, int numberOfMessages) throws GMSException {

    private void test() throws GMSException {

        System.out.println("Testing Started");
        List<String> members;


        //initialize Group Management Service and register for Group Events

        logger.log(Level.INFO, "Registering for group event notifications");
        if (memberID.equalsIgnoreCase("server")) {
            gms = initializeGMS(memberID, group, GroupManagementService.MemberType.SPECTATOR);
            gms.addActionFactory(new JoinedAndReadyNotificationActionFactoryImpl(new JoinAndReadyNotificationCallBack(memberID)));
            gms.addActionFactory(new PlannedShutdownActionFactoryImpl(new PlannedShutdownCallBack(memberID)));

        } else {
            gms = initializeGMS(memberID, group, GroupManagementService.MemberType.CORE);
            gms.addActionFactory(new JoinedAndReadyNotificationActionFactoryImpl(new JoinAndReadyNotificationCallBack(memberID)));
            gms.addActionFactory(new MessageActionFactoryImpl(new MessageCallBack(memberID, numberOfMessages)), "TestComponent");
        }


        //join group
        logger.log(Level.INFO, "Joining Group " + group);
        gms.join();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
        }


        gms.reportJoinedAndReadyState(group);

        if (memberID.equalsIgnoreCase("server")) {
            logger.log(Level.INFO, ("==================================================="));
            logger.log(Level.INFO, ("Waiting for JOINEDANDREADY from all CORE members"));
            while (true) {
                try {
                    Thread.sleep(5000); // 5 seconds
                } catch (InterruptedException e) {
                }
                logger.log(Level.INFO, ("==================================================="));
                logger.log(Level.INFO, ("Number of JOINEDANDREADY received from all CORE members is " + numberOfJoinAndReady));
                if (numberOfJoinAndReady == numberOfInstances) {
                    logger.log(Level.INFO, ("==================================================="));
                    logger.log(Level.INFO, ("All CORE members have sent JOINEDANDREADY (" + numberOfJoinAndReady + "," + numberOfInstances + ")"));
                    logger.log(Level.INFO, ("==================================================="));
                    break;
                }

            }
        } else {

            logger.log(Level.INFO, ("Waiting for all members to joined the group:" + group));

            while (true) {

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }

                System.out.println("numberOfJoinAndReady=" + numberOfJoinAndReady);
                System.out.println("numberOfInstances=" + (numberOfInstances));

                if (numberOfJoinAndReady == (numberOfInstances)) {
                    logger.log(Level.INFO, ("==================================================="));
                    logger.log(Level.INFO, ("All members have joined the group:" + group));
                    logger.log(Level.INFO, ("==================================================="));
                    //System.out.println(".");
                    members = gms.getGroupHandle().getCurrentCoreMembers();
                    members.remove(memberID);
                    waitingToReceiveStopFrom = new ArrayList<String>();
                    for (int i = 0; i < members.size(); i++) {
                        waitingToReceiveStopFrom.add(members.get(i));
                    }
                    logger.log(Level.INFO, ("waitingToReceiveStopFrom=[" + waitingToReceiveStopFrom.toString() + "]"));
                    break;
                }

            }

            logger.log(Level.INFO, ("Sending Messages to the following members [" + members.toString() + "]"));

            logger.log(Level.INFO, ("Sending messages"));
            for (int i = 0; i < numberOfMessages; i++) {
                for (int j = 0; j < members.size(); j++) {
                    if (!members.get(j).equalsIgnoreCase(memberID)) {
                        //String msg = "TO:" + members.get(j) + ", FROM:" + memberID + ", MSGID:" + i;
                        //logger.log(Level.INFO, ("Sending Message:" + msg + ", to " + members.get(j)));

                        StringBuffer sb = new StringBuffer(messageSize);
                        sb.append("TO:").append(members.get(j)).append(", FROM:").append(memberID).append(", MSGID:").append(Integer.toString(i)).append(" ");
                        int startIndex = sb.toString().length();
                        for (int k = startIndex; k < messageSize; k++) {
                            sb.append("X");
                        }
                        String msg = sb.toString();
                        logger.log(Level.INFO, ("Sending Message:" + msg.substring(0, 30) + "... msg.length()=" + msg.length() + " , to " + members.get(j)));

                        gms.getGroupHandle().sendMessage(members.get(j), "TestComponent", msg.getBytes());

                    }
                }
            }
            for (int j = 0; j < members.size(); j++) {
                if (!members.get(j).equalsIgnoreCase(memberID)) {
                    String stopMsg = "TO:" + members.get(j) + ", FROM:" + memberID + ", STOP";
                    System.out.println("Sending STOP message to " + members.get(j) + "!!!!!!!!!!!!!!!");
                    gms.getGroupHandle().sendMessage(members.get(j), "TestComponent", stopMsg.getBytes());
                }
            }
        }

    }

    public void waitTillDone() {
        List<String> members;
        if (memberID.equalsIgnoreCase("server")) {
            logger.log(Level.INFO, ("==================================================="));
            logger.log(Level.INFO, ("Waiting for all CORE members to send PLANNEDSHUTDOWN"));
            while (true) {

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }

                logger.log(Level.INFO, ("==================================================="));
                logger.log(Level.INFO, ("Number of PLANNEDSHUTDOWN received from all CORE members is " + numberOfPlannedShutdown));
                if (numberOfPlannedShutdown == numberOfInstances) {
                    logger.log(Level.INFO, ("==================================================="));
                    logger.log(Level.INFO, ("Have received PLANNEDSHUTDOWN from all CORE members (" + numberOfPlannedShutdown + ")"));
                    logger.log(Level.INFO, ("==================================================="));
                    break;
                }

            }
        } else {
            // instance
            while (!completedCheck.get() && (gms.getGroupHandle().getCurrentCoreMembers().size() > 1)) {
                int waitTime = 10000; // 10 seconds

                System.out.println("Waiting " + (waitTime / 1000) + " seconds inorder to complete processing of expected incoming messages...");
                System.out.println("members=" + gms.getGroupHandle().getCurrentCoreMembers().toString());


                synchronized (completedCheck) {
                    try {
                        completedCheck.wait(waitTime);
                    } catch (InterruptedException ie) {
                    }
                }

            }
            System.out.println("Completed processing of incoming messages");
            try {
                Thread.sleep(1000);
            } catch (Throwable t) {
            }
        }
        gms.shutdown(GMSConstants.shutdownType.INSTANCE_SHUTDOWN);

    }

    private GroupManagementService initializeGMS(String memberID, String groupName, GroupManagementService.MemberType mType) {
        logger.log(Level.INFO, "Initializing Shoal for member: " + memberID + " group:" + groupName);
        return (GroupManagementService) GMSFactory.startGMSModule(memberID,
                groupName,
                mType,
                //GroupManagementService.MemberType.CORE,
                //null ); // Now if properties is null, NPE occurred.
                new Properties());

    }

    private void leaveGroupAndShutdown(String memberID, GroupManagementService gms) {
        logger.log(Level.INFO, "Shutting down gms " + gms + "for server " + memberID);
        gms.shutdown(GMSConstants.shutdownType.INSTANCE_SHUTDOWN);
    }

    private class JoinAndReadyNotificationCallBack implements CallBack {

        private String memberID;

        public JoinAndReadyNotificationCallBack(String memberID) {
            this.memberID = memberID;
        }

        public void processNotification(Signal notification) {
            if (!(notification instanceof JoinedAndReadyNotificationSignal)) {
                logger.log(Level.SEVERE, "received unknown notification type:" + notification);
            } else {
                if (!notification.getMemberToken().equals("server")) {
                    numberOfJoinAndReady++;
                    logger.log(Level.INFO, "numberOfJoinAndReady received so far is" + numberOfJoinAndReady);

                }
            }
            logger.log(Level.INFO, "***JoinNotification received: ServerName = " + memberID + ", Signal.getMemberToken() = " + notification.getMemberToken());
        }
    }

    private class PlannedShutdownCallBack implements CallBack {

        private String memberID;

        public PlannedShutdownCallBack(String memberID) {
            this.memberID = memberID;
        }

        public void processNotification(Signal notification) {
            if (!(notification instanceof PlannedShutdownSignal)) {
                logger.log(Level.SEVERE, "received unknown notification type:" + notification);
            } else {
                if (!notification.getMemberToken().equals("server")) {
                    numberOfPlannedShutdown++;
                    logger.log(Level.INFO, "numberOfPlannedShutdown received so far is" + numberOfPlannedShutdown);

                }
            }

            logger.log(Level.INFO, "***PlannedShutdownNotification received: ServerName = " + memberID + ", Signal.getMemberToken() = " + notification.getMemberToken());
        }
    }

    private class MessageCallBack implements CallBack {

        private String memberID;
        private int numberOfMsgs;

        public MessageCallBack(String memberID, int numberOfMsgs) {
            this.memberID = memberID;
            this.numberOfMsgs = numberOfMsgs;
        }

        public void processNotification(Signal notification) {


            if (!(notification instanceof MessageSignal)) {
                logger.log(Level.SEVERE, memberID + " received unknown notification type:" + notification);
            }
            //logger.log(Level.INFO, "***Message received: ServerName = " + memberID + ", Signal.getMemberToken() = " + notification.getMemberToken());
            try {
                notification.acquire();
                MessageSignal messageSignal = (MessageSignal) notification;
                final String msgString = new String(messageSignal.getMessage());
                //System.out.println(memberID + " Received msg: " + msgString);

                String shortMsg = msgString;
                if (msgString.length() > 56) {
                    shortMsg = shortMsg.substring(0, 55) + "...";
                }
                System.out.println(memberID + " Received msg: " + shortMsg);


                int msgIdIdx = msgString.indexOf(" MSGID:");
                int msgFromIdx = msgString.indexOf("FROM:");
                int msgNextCommaIdx = msgString.indexOf(", ", msgFromIdx + 5);
                String msgFrom = msgString.substring(msgFromIdx + 5, msgNextCommaIdx);
                //System.out.println("msgIdIdx=" + msgIdIdx + ", msgFromIdx" + msgFromIdx);

                if (msgIdIdx != -1 && msgFromIdx != -1) {
                    //String msgId = msgString.substring(msgIdIdx + 7);
                    String msgId = msgString.substring(msgIdIdx + 7, msgString.indexOf("XX") - 1);

                    int msgIdInt = Integer.valueOf(msgId);


                    // if the INSTANCE does not exist in the map, create it.
                    ConcurrentHashMap instance_chm = chm.get(msgFrom);
                    if (instance_chm == null) {
                        instance_chm = new ConcurrentHashMap();
                    }

                    instance_chm.put(msgIdInt, "");

                    //System.out.println(msgFrom + ":instance_chm.size()=" + instance_chm.size());

                    chm.put(msgFrom, instance_chm);
                    //System.out.println("chm.size()=" + chm.size());

                } else {
                    System.out.println("comparing message |" + msgString + "| to see if it is a stop command");
                    if (msgString.contains("STOP")) {
                        System.out.println("Received STOP message from " + msgFrom + " !!!!!!!!");
                        numOfStopMsgReceived++;
                        waitingToReceiveStopFrom.remove(msgFrom);
                        System.out.println("Total number of STOP messages received so far is: " + numOfStopMsgReceived);
                        if (waitingToReceiveStopFrom.size() > 1) {
                            System.out.println("Waiting to receive STOP from: " + waitingToReceiveStopFrom.toString());
                        }

                    }
                }
                if ((numOfStopMsgReceived == numberOfInstances - 1)) {
                    completedCheck.set(true);
                    synchronized (completedCheck) {
                        completedCheck.notify();
                    }
                }
            } catch (SignalAcquireException e) {
                e.printStackTrace();
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                try {
                    notification.release();
                } catch (Exception e) {
                }

            }

        }
    }
}

