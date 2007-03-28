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

import com.sun.enterprise.jxtamgmt.LWRMulticast;
import com.sun.enterprise.jxtamgmt.NetworkManager;
import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.Message.ElementIterator;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
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
 * A test harness for the LWRMulticast class in SHOAL
 *
 * @author kkg
 */
public class LWRMulticastTest implements PipeMsgListener {

    /**
     * Common propagated pipe id
     */
    public final static String PIPEIDSTR =
            "urn:jxta:uuid-59616261646162614E504720503250336FA944D18E8A4131AA74BB6F4FF85DEF04";

    /**
     * Gets the pipeAdvertisement attribute of the PropagatedPipeServer class
     *
     * @return The pipeAdvertisement value
     */
    public static PipeAdvertisement getPipeAdvertisement() {
        PipeID pipeID = null;

        try {
            pipeID = (PipeID) IDFactory.fromURI(new URI(PIPEIDSTR));
        } catch (URISyntaxException use) {
            use.printStackTrace();
        }

        PipeAdvertisement advertisement = (PipeAdvertisement)
                AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        advertisement.setPipeID(pipeID);
        advertisement.setType(PipeService.PropagateType);
        advertisement.setName("LWRMulticastTest");
        return advertisement;
    }

    private static PeerGroup netPeerGroup = null;
    private static PipeAdvertisement pipeAdv = null;
    private static NetworkManager netManager = null;

    private static String groupName = "testGroup";
    private static String instanceName = "testInstance." + System.nanoTime();

    public static String getGroupName() {
        return groupName;
    }

    public static String getInstanceName() {
        return instanceName;
    }

    public static void setGroupName(String _groupName) {
        if (netManager != null)
            groupName = _groupName;
    }

    public static void setInstanceName(String _instanceName) {
        if (netManager != null)
            instanceName = _instanceName;
    }

    public static boolean startManager() {
        if (netManager != null) {
            return true;
        }

        pipeAdv = getPipeAdvertisement();
        netManager = new NetworkManager(groupName, instanceName, new HashMap());

        try {
            netManager.start();
            netPeerGroup = netManager.getNetPeerGroup();
            System.out.println("Network manager started.");
            System.out.println("Node ID :" + getNodeID());
        } catch (Exception e) {
            e.printStackTrace();
            netManager = null;
            return false;
        }

        return true;
    }

    public static String getNodeID() {
        if (netPeerGroup != null)
            return netPeerGroup.getPeerID().toString();

        return null;
    }

    public static void stopManager() {
        if (netManager != null) {
            netManager.stop();
            netManager = null;
        }
    }

    /**
     * Gram TAG name
     */
    public static String GRAMTAG = "GRAM";

    /**
     * Tutorial message name space
     */
    public static String NAMESPACE = "TEST";

    /**
     * {@inheritDoc}
     */
    public void pipeMsgEvent(PipeMsgEvent event) {

        ElementIterator iter = getMessageElements(event.getMessage());

        while (iter.hasNext()) {
            System.out.println("Received a gram :" + iter.next().toString());
        }
    }

    private LWRMulticast mcast = null;

    /**
     * Creates a new instance of LWRMulticastTest
     */
    public LWRMulticastTest() {
        this(null);
    }

    public LWRMulticastTest(PipeMsgListener listener) {
        if (listener == null) {
            listener = this;
        }

        try {
            if (startManager()) {
                mcast = new LWRMulticast(netPeerGroup, pipeAdv, listener);
                mcast.setSoTimeout(6000);
            } else {
                System.out.println("Failed to start Network Manager");
            }

        } catch (Exception e) {
            mcast = null;
            e.printStackTrace();
        }
    }

    public int sendMessage(String msgString) {
        return sendMessage(msgString, 1);
    }

    public int sendMessage(String msgString, int numReceivers) {
        try {
            Message msg = new Message();
            msg.addMessageElement(NAMESPACE, new StringMessageElement(GRAMTAG, msgString, null));
            mcast.send(msg, numReceivers);

            Set<PeerID> ack = mcast.getAckList();
            return ack.size();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public ElementIterator getMessageElements(Message message) {
        if (message != null) {
            // return message.getMessageElements(NAMESPACE, GRAMTAG);
             return message.getMessageElements(NAMESPACE, LWRMulticast.SEQTAG);
            //return message.getMessageElements();
        }
        return null;
    }

    public void close() {
        mcast.close();
    }
}
