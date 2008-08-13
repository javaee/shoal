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

package com.sun.enterprise.shoal.multithreadmessagesendertest;

import com.sun.enterprise.ee.cms.core.*;
import com.sun.enterprise.ee.cms.impl.client.MessageActionFactoryImpl;

import java.util.List;

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
	
	public MultiThreadMessageSender(String memberToken,String destMemberToken,int sendingThreadNum){
		
		this.memberToken = memberToken;
		this.destMemberToken = destMemberToken;
		this.sendingThreadNum = sendingThreadNum;
				
	}
	
	public void start(){
		initGMS();		
		startSenderThread();
	}

	private void initGMS(){
		try {
			gms = (GroupManagementService) GMSFactory.startGMSModule(memberToken,"DemoGroup", GroupManagementService.MemberType.CORE, null);
			gms.addActionFactory(new MessageActionFactoryImpl(this),"SimpleSampleComponent");
			gms.join();
			Thread.sleep(5000);
		} catch (GMSException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	private void startSenderThread(){
        for(int i=0;i<sendingThreadNum; i++){
            final int i1 = i;
            new Thread(new Runnable(){
                String msg1 = msg + " "+ i1;
                public void run() {
                    List<String> members;
                    while(true){
                        members = gms.getGroupHandle().getAllCurrentMembers();
                        System.out.println(members.toString());
                        try {
							Thread.sleep(10);
                            if(members.size()>=2){
                                gms.getGroupHandle().sendMessage(destMemberToken, "SimpleSampleComponent",msg1.getBytes());
                            }
                        } catch (InterruptedException e) {
							e.printStackTrace();
                            break;
                        } catch (GMSException e) {							
                            e.printStackTrace();

                            break;                            
                        }
					}
					
				}
				
			}).start();
		}
	}
	
	public void processNotification(Signal arg0) {
		try {
			arg0.acquire();
			MessageSignal messageSignal = (MessageSignal)arg0;
			System.out.println("received msg: " + new String(messageSignal.getMessage()));
			arg0.release();
		} catch (SignalAcquireException e) {
			e.printStackTrace();
		} catch (SignalReleaseException e) {
			e.printStackTrace();
		}
		
	}	
	
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
		
		MultiThreadMessageSender multiThreadMessageSender = new MultiThreadMessageSender(memberToken,destMemberToken,sendingThreadNum);
		multiThreadMessageSender.start();		
	}
}

