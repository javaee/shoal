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
import java.util.ArrayList;

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
    private static ClusterManager manager = null;

    private static String groupName = "testGroup";
    private static String instanceName = "testInstance." + System.nanoTime();

    public static String getGroupName() {
        return groupName;
    }

    public static String getInstanceName() {
        return instanceName;
    }

    public static void setGroupName(String _groupName) {
        if (manager != null)
            groupName = _groupName;
    }

    public static void setInstanceName(String _instanceName) {
        if (manager != null)
            instanceName = _instanceName;
    }

    public static boolean startManager() {
        if (manager != null) {
            return true;
        }

        pipeAdv = getPipeAdvertisement();
        manager = new ClusterManager(groupName, instanceName, new HashMap<String, String>(), new HashMap(), new ArrayList<ClusterViewEventListener>(), new ArrayList<ClusterMessageListener>());
        try {
            manager.start();
            netPeerGroup = manager.getNetworkManager().getNetPeerGroup();
            System.out.println("Network manager started.");
            System.out.println("Node ID :" + getNodeID());
        } catch (Exception e) {
            e.printStackTrace();
            manager = null;
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
        if (manager != null) {
            manager.stop(false);
            manager = null;
        }
    }

    /**
     * Gram TAG name
     */
    public static final String GRAMTAG = "GRAM";

    /**
     * Tutorial message name space
     */
    public static final String NAMESPACE = "TEST";

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
                mcast = new LWRMulticast(manager, pipeAdv, listener);
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
