/*
 *  Copyright 2006 Sun Microsystems, Inc.  All rights reserved.
 *  Use is subject to license terms.
 */
/*
* The contents of this file are subject to the terms
* of the Common Development and Distribution License
* (the License).  You may not use this file except in
* compliance with the License.
*
* You can obtain a copy of the license at
* https://shoal.dev.java.net/public/CDDLv1.0.html
*
* See the License for the specific language governing
* permissions and limitations under the License.
*
* When distributing Covered Code, include this CDDL
* Header Notice in each file and include the License file
* at
* If applicable, add the following below the CDDL Header,
* with the fields enclosed by brackets [] replaced by
* you own identifying information:
* "Portions Copyrighted [year] [name of copyright owner]"
*
* Copyright 2006 Sun Microsystems, Inc. All rights reserved.
*/
package com.sun.enterprise.jxtamgmt;

import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;

import java.util.HashMap;

/**
 * Simple test harness for LWRMulticast
 *
 * @author Mohamed Abdelaziz (hamada)
 */

public class LWRMulticastRecTest implements PipeMsgListener {

    /**
     * Pong TAG name
     */
    public final static String GRAMTAG = "GRAM";
    /**
     * Tutorial message name space
     */
    public final static String NAMESPACE = "TEST";
    /**
     * Common propagated pipe id
     */
    private final static String completeLock = "completeLock";
    NetworkManager manager;

    /**
     * Gets the pipeAdvertisement attribute of the PropagatedPipeServer class
     *
     * @return The pipeAdvertisement value
     */
    public PipeAdvertisement getPipeAdvertisement() {
        PipeID pipeID = manager.getPipeID("multicasttest");
        PipeAdvertisement advertisement = (PipeAdvertisement)
                AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        advertisement.setPipeID(pipeID);
        advertisement.setType(PipeService.PropagateType);
        advertisement.setName("LWRMulticastTest");
        return advertisement;
    }

    /**
     * {@inheritDoc}
     */
    public void pipeMsgEvent(PipeMsgEvent event) {

        Message message = event.getMessage();
        if (message == null) {
            return;
        }
        MessageElement gel = message.getMessageElement(NAMESPACE, GRAMTAG);

        if (gel == null) {
            return;
        }
        System.out.println("Received a gram :" + gel.toString());
    }

    /**
     * Keep running, avoids existing
     */
    private void waitForever() {
        try {
            System.out.println("Waiting for Messages.");
            synchronized (completeLock) {
                completeLock.wait();
            }
            System.out.println("Done.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * main
     *
     * @param args command line args
     */
    public static void main(String args[]) {
        LWRMulticastRecTest server = new LWRMulticastRecTest();
        LWRMulticast mcast1 = null;
        server.manager = new NetworkManager("testGroup", "receiver", new HashMap());
        PipeAdvertisement pipeAdv = server.getPipeAdvertisement();
        try {
            server.manager.start();
            PeerGroup netPeerGroup = server.manager.getNetPeerGroup();
            System.out.println("Node ID :" + netPeerGroup.getPeerID().toString());
            mcast1 = new LWRMulticast(netPeerGroup, pipeAdv, server);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        server.waitForever();
        mcast1.close();
        server.manager.stop();
    }
}

