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

package com.sun.enterprise.shoal.multithreadmessagesendertest;

import com.sun.enterprise.ee.cms.core.*;
import com.sun.enterprise.ee.cms.impl.client.MessageActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.JoinNotificationActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.base.Utility;
import com.sun.enterprise.ee.cms.impl.common.JoinNotificationSignalImpl;
import com.sun.enterprise.ee.cms.impl.common.MessageSignalImpl;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import com.sun.enterprise.ee.cms.impl.common.GMSContext;
import com.sun.enterprise.ee.cms.impl.common.GMSContextFactory;

import java.util.Properties;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple test for sending messages using multiple threads
 *
 * @author leehui
 */

public class MultiThreadMessageSender implements CallBack{

	private GroupManagementService gms;
	private String memberToken;
	private String destMemberToken;
	private int sendingThreadNum;
	private String msg = "hello, world";
    private AtomicInteger masterMsgId = new AtomicInteger(-1);
    private Thread[] threads;
	
	public MultiThreadMessageSender(String memberToken,String destMemberToken,int sendingThreadNum){
		
		this.memberToken = memberToken;
		this.destMemberToken = destMemberToken;
		this.sendingThreadNum = sendingThreadNum;
        this.threads = new Thread[sendingThreadNum];
				
	}
	
	public void start(){
		initGMS();		
		startSenderThread();
	}

    public void waitTillDone() {
        boolean done = false;
        boolean threadDone[] = new boolean[threads.length];

        for (int i=0; i < threadDone.length; i++) {
            threadDone[i] = false;
        }

        while (! done) {
            done = true;
            int i = 0;
            for (Thread t : threads) {
                if (!threadDone[i]) {
                    if (t.isAlive()) {
                        logger.finer("thread " + t.getName() + " still alive");
                        done = false;
                    } else {
                        threadDone[i] = true;
                        System.out.println("thread " + t.getName() + " has completed");
                    }
                }
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {}
        }
        System.out.println("Done waiting for sending threads (number=" + threads.length + ") to complete");
        while (! completedCheck.get()) {
            System.out.println("Waiting to complete processing of expected incoming messages...");
            synchronized(completedCheck) {
                try {
                    completedCheck.wait(10000);
                } catch (InterruptedException ie) {}
            }

        }
        System.out.println("Completed processing of incoming messages");
        try {
            Thread.sleep(1000);
        } catch (Throwable t)  {}
        GMSContext ctx = GMSContextFactory.getGMSContext("DemoGroup");
        ctx.getGMSMonitor().report();
        gms.shutdown(GMSConstants.shutdownType.INSTANCE_SHUTDOWN);
    }

	private void initGMS(){
		try {
            Properties props = new Properties();
            props.put(ServiceProviderConfigurationKeys.INCOMING_MESSAGE_QUEUE_SIZE.toString(), "1500");
            props.put(ServiceProviderConfigurationKeys.MONITORING.toString(), "0");
			gms = (GroupManagementService) GMSFactory.startGMSModule(memberToken,"DemoGroup", GroupManagementService.MemberType.CORE, props);
			gms.addActionFactory(new MessageActionFactoryImpl(this),"SimpleSampleComponent");
            gms.addActionFactory(new JoinNotificationActionFactoryImpl(this));
			gms.join();
			Thread.sleep(5000);
		} catch (GMSException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {

		}
	}
	private void startSenderThread(){

        for(int i=0;i<sendingThreadNum; i++){
            final int i1 = i;
            threads[i] = new Thread(new Runnable(){
                String msg1 = msg + " "+ i1;
                public void run() {
                    int msgId = -1;
                    try {
                        List<String> members;
                        int i = -1;
                        members = gms.getGroupHandle().getAllCurrentMembers();
                        // System.out.println("Thead id: " + i1 + " members: " + members.toString());
                        while(true){
                            i++;
                            msgId = masterMsgId.getAndIncrement();
//                            if (msgId == 87) {
//                                // tmp test, skip one message sent to see if missed on receiving side.
//                                msgId = masterMsgId.getAndIncrement();
//                            }
                            msg1 = msg + " " + " threadid:" + i1 + " msgid:" + msgId + " " + payload.toString();
                            while (true) {
                                members = gms.getGroupHandle().getAllCurrentMembers();
                                try {
							        Thread.sleep(10);
                                    if (msgId >= EXPECTED_NUMBER_OF_MESSAGES) {
//                                        String stopMsg = "stop";
//                                        System.out.println("sending stop message");
//                                        gms.getGroupHandle().sendMessage(destMemberToken, "SimpleSampleComonent", stopMsg.getBytes());
//                                        gms.getGroupHandle().sendMessage(destMemberToken, "SimpleSampleComonent", stopMsg.getBytes());
                                        break;
                                    }
                                    if(members.size()>=2){
                                        gms.getGroupHandle().sendMessage(destMemberToken, "SimpleSampleComponent",msg1.getBytes());
                                        break;
                                    }
                               } catch (InterruptedException e) {
							        e.printStackTrace();
                                    //break;
                                } catch (GMSException e) {
                                    System.out.println("thread " + i + "caught GMSException " + e );
                                    e.printStackTrace();
                                //break;
                                }
                            }
                            if (msgId >= EXPECTED_NUMBER_OF_MESSAGES) {
                                break;
                            }
	                    }
                    } finally {
					    System.out.println("Exiting threadid " + i1);
                    }
                }
			}, "SendingThread_" + i);
            threads[i].start();
		}
	}


	public void processNotification(Signal arg0) {
        if (arg0 instanceof JoinNotificationSignal) {
            JoinNotificationSignal joinSig = (JoinNotificationSignal)arg0;
        } else if (arg0 instanceof MessageSignal) {
            int droppedMessages = 0;
            int localNumMsgReceived = 0;
            boolean localStopFlag = false;
            try {
                MessageSignal messageSignal = (MessageSignal) arg0;
                final String msgString = new String(messageSignal.getMessage());
                String outputStr = msgString;
                if (msgString.length() > 38) {
                    outputStr = msgString.substring(0,37) + "...truncated...";
                }
                int msgIdIdx = msgString.indexOf(" msgid:");
                if (msgIdIdx != -1) {
                    localNumMsgReceived = numMsgIdReceived.getAndIncrement();

                    String msgId = msgString.substring(msgIdIdx + 7, msgString.indexOf('X') - 1);
                    int msgIdInt = Integer.valueOf(msgId);
                    if (msgIdInt < msgIdReceived.length) {
                        msgIdReceived[msgIdInt] = true;
                    }
                    if (localNumMsgReceived >= EXPECTED_NUMBER_OF_MESSAGES) {
                        localStopFlag = true;
                    }
                } else {
                    System.out.println("comparing message:" + msgString + " to see if it is a stop command compareTo(stop)" + msgString.compareTo("stop"));
                    if (msgString.compareTo("stop") == 0) {
                        System.out.println("received stop message");
                        localStopFlag = true;
                    }
                }
                if ((localNumMsgReceived % 2500) == 0) {
                    System.out.println("received msg[" + localNumMsgReceived + "]: length:" + msgString.length() + " payload:" + outputStr);
                }
                if (localStopFlag && !completedCheck.get()) {
                    completedCheck.set(true);
                    System.out.println("Checking to see if first " + EXPECTED_NUMBER_OF_MESSAGES + " messages were received");

                    for (int i = 0; i < msgIdReceived.length; i++) {
                        if (!msgIdReceived[i]) {
                            droppedMessages++;
                            System.out.println("Never received msg id " + i);
                        }
                    }
                    if (droppedMessages == 0) {
                        System.out.println("PASS.  No dropped messages");
                    } else {
                        System.out.println("FAILED. Confirmed " + droppedMessages + " messages were dropped");
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
    final static int NUM_MESSAGES_TO_SEND = 10000;
    static int EXPECTED_NUMBER_OF_MESSAGES;
    static private boolean msgIdReceived[];
    static private AtomicInteger numMsgIdReceived = new AtomicInteger(0);
    static private AtomicBoolean completedCheck = new AtomicBoolean(false);

    private static final Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);



    private static final int PAYLOADSIZE = 80 * 1024;
    private static final StringBuffer payload = new StringBuffer(PAYLOADSIZE);

	/**
	 * main
	 * 
	 * start serveral threads to send message
	 * 
	 * usage: start two instance using following two commands for the test 
	 * 			java MultiThreadMessageSender A B 20
	 * 		  	java MultiThreadMessageSender B A 0
	 * 
	 * @param args command line args
	 */
	public static void main(String[] args) {
		String memberToken = args[0];
		String destMemberToken = args[1];
		int sendingThreadNum = Integer.parseInt(args[2]);
        EXPECTED_NUMBER_OF_MESSAGES = NUM_MESSAGES_TO_SEND * sendingThreadNum;

        msgIdReceived = new boolean[EXPECTED_NUMBER_OF_MESSAGES];

        for (int i =0; i < msgIdReceived.length; i++) {
            msgIdReceived[i] = false;
        }
        Utility.setLogger(logger);
        Utility.setupLogHandler();
        logger.setLevel(Level.CONFIG);
        for (int i = 0; i < PAYLOADSIZE; i++) {
            payload.append('X');
        }        
		MultiThreadMessageSender multiThreadMessageSender = new MultiThreadMessageSender(memberToken,destMemberToken,sendingThreadNum);
		multiThreadMessageSender.start();
        multiThreadMessageSender.waitTillDone();
        logger.info("Test completed.");

	}
}

