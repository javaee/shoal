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
import java.util.ArrayList;

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
    ClusterManager manager;

    /**
     * Gets the pipeAdvertisement attribute of the PropagatedPipeServer class
     *
     * @return The pipeAdvertisement value
     */
    public PipeAdvertisement getPipeAdvertisement() {
        PipeID pipeID = manager.getNetworkManager().getPipeID("multicasttest");
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
        server.manager = new ClusterManager("testGroup", "receiver", new HashMap<String, String>(), new HashMap(), new ArrayList<ClusterViewEventListener>(), new ArrayList<ClusterMessageListener>());

        PipeAdvertisement pipeAdv = server.getPipeAdvertisement();
        try {
            server.manager.start();
            PeerGroup netPeerGroup = server.manager.getNetPeerGroup();
            System.out.println("Node ID :" + netPeerGroup.getPeerID().toString());
            mcast1 = new LWRMulticast(server.manager, pipeAdv, server);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        server.waitForever();
        mcast1.close();
        server.manager.stop(false);
    }
}

