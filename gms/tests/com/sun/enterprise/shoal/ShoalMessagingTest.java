/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package com.sun.enterprise.shoal;

import com.sun.enterprise.ee.cms.core.CallBack;
import com.sun.enterprise.ee.cms.core.GMSConstants;
import com.sun.enterprise.ee.cms.core.GMSException;
import com.sun.enterprise.ee.cms.core.GMSFactory;
import com.sun.enterprise.ee.cms.core.GroupManagementService;
import com.sun.enterprise.ee.cms.core.GroupManagementService.MemberType;
import com.sun.enterprise.ee.cms.core.JoinNotificationSignal;
import com.sun.enterprise.ee.cms.core.MessageSignal;
import com.sun.enterprise.ee.cms.core.ServiceProviderConfigurationKeys;
import com.sun.enterprise.ee.cms.core.Signal;
import com.sun.enterprise.ee.cms.impl.client.JoinNotificationActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.MessageActionFactoryImpl;

import java.util.Properties;

/**
 * @author Rafael Barbosa
 */
public class ShoalMessagingTest implements Runnable, CallBack {
    String serviceName = "service";
    GroupManagementService gms;
    private final Object sendMessagesSignal = new Object();
    private int nbOfMembers = 2;
    int total_msgs_received = 0;
    final String groupName;
    final int num_msgs = 10000;

    public ShoalMessagingTest() {
        Properties props = new Properties();
        props.put(ServiceProviderConfigurationKeys.LOOPBACK.toString(), "true");
        String instanceId = System.getProperty("INSTANCEID");
        groupName = "ShoalMessagingTestGroup";
        gms = (GroupManagementService) GMSFactory.startGMSModule(instanceId, groupName, MemberType.CORE, props);

        try {
            gms.addActionFactory(new MessageActionFactoryImpl(this), serviceName);
            gms.addActionFactory(new JoinNotificationActionFactoryImpl(this));
            gms.join();
            System.err.println("Shoal member:" + instanceId + " joined group:" + groupName);
        } catch (GMSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void run() {
        if (gms.getGroupHandle().getAllCurrentMembers().size() < nbOfMembers) {
            System.err.println("GroupHandle.getAllCurrentMembers()=" + gms.getGroupHandle().getAllCurrentMembers());
            System.err.println("Waiting for all members to join...");
            synchronized (sendMessagesSignal) {
                try {
                    sendMessagesSignal.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        byte[] payload = generatePayload();
        long log_interval = 1000;
        int total_msgs = 0;
        try {
            for (int i = 0; i < num_msgs; i++) {
                gms.getGroupHandle().sendMessage(serviceName, payload);
                total_msgs++;
                if (total_msgs % log_interval == 0) {
                    System.out.println("++ sent " + total_msgs);
                }
            }
        } catch (GMSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        synchronized (sendMessagesSignal) {
            try {
                sendMessagesSignal.wait(30000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // run is complete shutdown the instance
        gms.shutdown(GMSConstants.shutdownType.INSTANCE_SHUTDOWN);
    }

    byte[] generatePayload() {
        byte[] payload = new byte[128];
        for (int i = 0; i < payload.length; i++) {
            if (i % 2 == 0) {
                payload[i] = 1;
            }
        }
        return payload;
    }

    public void processNotification(Signal signal) {
        if (signal instanceof JoinNotificationSignal) {
            JoinNotificationSignal joinSig = (JoinNotificationSignal)signal;
            int size = gms.getGroupHandle().getAllCurrentMembers().size();
            System.err.println("Join Notification: member:" + joinSig.getMemberToken() + " Join current members:" + joinSig.getAllCurrentMembers() +
                               "GroupHandle members:" + size);
            if ( size == nbOfMembers) {
                synchronized (sendMessagesSignal) {
                    sendMessagesSignal.notify();
                }
            }
        } else if (signal instanceof MessageSignal) {
            total_msgs_received++;
            if (total_msgs_received % 1000 == 0) {
                System.out.println("-- received " + total_msgs_received);
            }
            if (total_msgs_received == 2001) {
		        throw new NullPointerException("simulated unchecked exception test");
	        }
            if (total_msgs_received == num_msgs * nbOfMembers) {
                // signal that completed receiving all expected messages so app can continue and shutdown.
                synchronized(sendMessagesSignal) {
                    sendMessagesSignal.notify();
                }
            }
        } else {
            System.err.println(new StringBuffer().append(serviceName)
                    .append(": Notification Received from:")
                    .append(signal.getMemberToken())
                    .append(":[")
                    .append(signal.toString())
                    .append("] has been processed")
                    .toString());
        }
    }

    public static void main(String[] args) {
        Thread t = new Thread(new ShoalMessagingTest());
        t.start();
    }
}
