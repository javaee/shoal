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
import net.jxta.endpoint.StringMessageElement;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Set;

/**
 * Simple test harness for LWRMulticast
 *
 *@author     Mohamed Abdelaziz (hamada)
 */

public class LWRMulticastSenderTest implements PipeMsgListener {

    /**
     *  Gram TAG name
     */
    public final static String GRAMTAG = "GRAM";
    /**
     *  Tutorial message name space
     */
    public final static String NAMESPACE = "TEST";
    private static PeerGroup netPeerGroup = null;
    NetworkManager manager;
    /**
     *  Common propagated pipe id
     */
    private final static String completeLock = "completeLock";
    private static PipeAdvertisement pipeAdv = null;
    private static PipeService pipeService = null;


    /**
     *  Gets the pipeAdvertisement attribute of the PropagatedPipeServer class
     *
     * @return    The pipeAdvertisement value
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
     *  main
     *
     * @param  args  command line args
     */
    public static void main(String args[]) {
        LWRMulticastSenderTest client = new LWRMulticastSenderTest();
        LWRMulticast mcast = null;
        client.manager = new NetworkManager("testGroup", "sender", new HashMap());
        pipeAdv = client.getPipeAdvertisement();
        try {
            client.manager.start();
            netPeerGroup = client.manager.getNetPeerGroup();
            System.out.println("Node ID :"+netPeerGroup.getPeerID().toString());
            mcast = new LWRMulticast(netPeerGroup, pipeAdv, client);
            for (int i = 0; i < 5 ; i++) {
                Message msg = new Message();
                msg.addMessageElement(NAMESPACE, new StringMessageElement(GRAMTAG, "Message"+i, null));
                mcast.send(msg, 1);
                Set ack = mcast.getAckList();
                System.out.println("Received "+ ack.size()+" ack's");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        mcast.close();
        client.manager.stop();
    }
}

