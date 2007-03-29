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
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.MessageTransport;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.id.ID;
import net.jxta.impl.endpoint.router.EndpointRouter;
import net.jxta.impl.endpoint.router.RouteControl;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.protocol.RouteAdvertisement;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The LWRMulticast class is useful for sending and receiving
 * JXTA multicast messages. A LWRMulticast is a (UDP) DatagramSocket,
 * with additional capabilities for joining "groups" of other multicast hosts
 * on the internet.
 * A multicast group is specified within the context of PeerGroup and a propagate
 * pipe advertisement.
 * One would join a multicast group by first creating a MulticastSocket
 * with the desired peer group and pipe advertisement.
 */
public class LWRMulticast implements PipeMsgListener {
    private final static Logger LOG = Logger.getLogger(LWRMulticast.class.getName());
    /**
     * Name space for this utility
     */
    public final static String NAMESPACE = "JXTAMCAST";
    /**
     * Ack message element name
     */
    public final static String ACKTAG = "ACK";
    /**
     * seq number message element name
     */
    public final static String SEQTAG = "SEQ";
    /**
     * source node id message element name
     */
    public final static String SRCIDTAG = "SRCID";
    private transient PipeAdvertisement pipeAdv;
    private transient PipeService pipeSvc;
    private transient InputPipe in;
    private transient PeerGroup group;
    private transient OutputPipe outputPipe;
    private transient boolean closed = false;
    private transient boolean bound = false;

    private  transient long padding = 250;
    private transient long timeout = 5000 + padding;
    private transient MessageElement srcElement = null;
    private transient AtomicLong sequence = new AtomicLong();
    private final static String ackLock = new String("ackLock");
    private transient int threshold = 0;
    private transient Set<PeerID> ackSet = new HashSet<PeerID>();
    private transient Set<PeerID> ackList = new HashSet<PeerID>();
    private transient Map<PeerID, OutputPipe> pipeCache = new Hashtable<PeerID, OutputPipe>();
    private RouteControl routeControl = null;
    private MessageElement routeAdvElement = null;
    private static final String ROUTEADV = "ROUTE";

    /**
     * The application message listener
     */
    protected transient PipeMsgListener msgListener;

    /**
     * Create a multicast channel bind it to a specific pipe within specified
     * peer group
     *
     * @param pipeAd      PipeAdvertisement
     * @param group       peer group handle
     * @param msgListener the application listener
     * @throws IOException if an io error occurs
     */
    public LWRMulticast(PeerGroup group,
                        PipeAdvertisement pipeAd,
                        PipeMsgListener msgListener) throws IOException {
        super();
        joinGroup(group, pipeAd, msgListener);
        MessageTransport endpointRouter = (group.getEndpointService()).getMessageTransport("jxta");
        if (endpointRouter != null) {
            routeControl = (RouteControl) endpointRouter.transportControl(EndpointRouter.GET_ROUTE_CONTROL, null);
            RouteAdvertisement route = routeControl.getMyLocalRoute();
            if (route != null) {
                routeAdvElement = new TextDocumentMessageElement(ROUTEADV,
                        (XMLDocument) route.getDocument(MimeMediaType.XMLUTF8), null);
            }
        }

    }

    /**
     * joins MutlicastSocket to specified pipe within the context of group
     *
     * @param group       group context
     * @param pipeAd      PipeAdvertisement
     * @param msgListener The application message listener
     * @throws IOException if an io error occurs
     */
    public void joinGroup(PeerGroup group,
                          PipeAdvertisement pipeAd,
                          PipeMsgListener msgListener) throws IOException {
        if (pipeAd.getType() != null && !pipeAd.getType().equals(PipeService.PropagateType)) {
            throw new IOException("Only propagate pipe advertisements are supported");
        }
        if (pipeAd.getPipeID() == null) {
            throw new IOException("Invalid pipe advertisement");
        }
        if (msgListener == null) {
            throw new IllegalArgumentException("msgListener can not be null");
        }
        this.msgListener = msgListener;
        this.group = group;
        this.pipeAdv = pipeAd;
        this.pipeSvc = group.getPipeService();
        this.in = pipeSvc.createInputPipe(pipeAd, this);
        outputPipe = pipeSvc.createOutputPipe(pipeAd, 1);
        String id = group.getPeerID().toString();
        srcElement = new StringMessageElement(SRCIDTAG, id, null);
        LOG.log(Level.FINEST, "Statring LWRMulticast on pipe id :" + pipeAdv.getID());
        bound = true;
    }

    /**
     * Returns the binding state of the LWRMulticast.
     *
     * @return true if the LWRMulticast successfully bound to an address
     */
    public boolean isBound() {
        return bound;
    }

    /**
     * Closes this LWRMulticast.
     */
    public synchronized void close() {
        if (closed) {
            return;
        }
        bound = false;
        closed = true;
        in.close();
        outputPipe.close();
        in = null;
    }

    /**
     * {@inheritDoc}
     */
    public void pipeMsgEvent(PipeMsgEvent event) {

        Message message = event.getMessage();
        if (message == null) {
            return;
        }

        MessageElement element;
        PeerID id = getSource(message);
        if (id.equals(group.getPeerID())) {
            //loop back
            return;
        }
        element = message.getMessageElement(NAMESPACE, ACKTAG);

        if (element != null) {
            processAck(id, element.toString());
        } else {
            // does the message contain any data
            element = message.getMessageElement(NAMESPACE, SEQTAG);
            if (element != null) {
                ackMessage(id, element);
                try {
                    if (msgListener != null) {
                        LOG.log(Level.FINEST, "Calling message listener");
                        msgListener.pipeMsgEvent(event);
                    }
                } catch (Throwable th) {
                    LOG.log(Level.FINEST, "Exception occurred while calling message listener", th);
                }
            }
        }
        processRoute(message);
    }

    /**
     * process an ack message
     *
     * @param id  source peer ID
     * @param seq message sequence number
     */
    private void processAck(PeerID id, String seq) {
        LOG.log(Level.FINEST, "Processing ack for message sequence " + seq);
        if (!ackSet.contains(id)) {
            ackSet.add(id);
            if (ackSet.size() >= threshold) {
                synchronized (ackLock) {
                    ackLock.notifyAll();
                }
            }
        }
    }

    /**
     * ack a message
     *
     * @param id  source peer ID
     * @param seq message sequence number
     */
    private void ackMessage(PeerID id, MessageElement seq) {
        LOG.log(Level.FINEST, "Ack'ing message Sequence :" + seq.toString());
        Message msg = new Message();
        msg.addMessageElement(NAMESPACE, srcElement);
        msg.addMessageElement(NAMESPACE, new StringMessageElement(ACKTAG, seq.toString(), null));
        try {
            send(id, msg);
        } catch (IOException io) {
            LOG.log(Level.FINEST, "I/O Error occured " + io.toString());
        }
    }

    /**
     * Returns a list of ack's received from nodes identified by  PeerID's
     *
     * @return a List of PeerID's
     */
    public Set<PeerID> getAckList() {
        return ackList;
    }

    /**
     * Gets the Timeout attribute of the LWRMulticast
     *
     * @return The soTimeout value
     */
    public synchronized long getSoTimeout() {
        return timeout;
    }

    /**
     * Sets the Timeout attribute of the LWRMulticast
     * a timeout of 0 blocks forever, by default this channel's
     * timeout is set to 0
     *
     * @param timeout The new soTimeout value
     * @throws IOException if an I/O error occurs
     */
    public synchronized void setSoTimeout(long timeout) throws IOException {
        checkState();
        this.timeout = timeout + padding;
    }

    /**
     * Returns the closed state of the LWRMulticast.
     *
     * @return true if the channel has been closed
     */
    public synchronized boolean isClosed() {
        return closed;
    }

    /**
     * Throws a IOException if closed or not bound
     *
     * @throws IOException if an I/O error occurs
     */
    private void checkState() throws IOException {
        if (isClosed()) {
            throw new IOException("LWRMulticast is closed");
        } else if (!isBound()) {
            throw new IOException("LWRMulticast not bound");
        }
    }

    /**
     * returns the source peer id of a message
     *
     * @param msg message
     * @return The source value
     */
    public static long getSequenceID(Message msg) {
        MessageElement sel = msg.getMessageElement(NAMESPACE, SEQTAG);
        if (sel != null) {
            return Long.parseLong(sel.toString());
        }
        return -1;
    }
        /**
     * returns the source peer id of a message
     *
     * @param msg message
     * @return The source value
     */
    public static PeerID getSource(Message msg) {
        String addrStr = null;
        PeerID id = null;
        MessageElement sel = msg.getMessageElement(NAMESPACE, SRCIDTAG);
        if (sel != null) {
            try {
                addrStr = new String(sel.getBytes(false), 0, (int) sel.getByteLength(), "UTF8");
            } catch (UnsupportedEncodingException uee) {
                LOG.log(Level.FINEST, "Encoding Error occured " + uee.toString());
            }
        }
        if (addrStr != null) {
            id = (PeerID) ID.create(URI.create(addrStr));
        }
        return id;
    }
    /**
     * Send a message to the predefined set of nodes, and expect a minimum of specified acks.
     * <p/>
     * This method blocks until ack's upto to the specified threshold
     * have been received or the timeout has been reached.
     * A call to getAckList() returns a list of ack source peer ID's
     *
     * @param msg       the message to send
     * @param threshold the minimun of ack expected, 0 indicates none are expected
     * @throws IOException if an i/o error occurs, or SocketTimeoutException
     *                     if the threshold is not met within timeout
     */
    public void send(Message msg, int threshold) throws IOException {
        if (threshold < 0) {
            throw new IllegalArgumentException("Invalid threshold " + threshold + " must be >= 0");
        }
        if (routeAdvElement != null && routeControl != null && sequence.intValue() < 2) {
            msg.addMessageElement(NAMESPACE, routeAdvElement);
        }

        this.threshold = threshold;
        ackList.clear();
        msg.addMessageElement(NAMESPACE, srcElement);
        long seq = sequence.getAndIncrement();
        msg.addMessageElement(NAMESPACE, new StringMessageElement(SEQTAG, Long.toString(seq), null));

        LOG.log(Level.FINEST, "Sending message sequence #: " + seq + " Threshold :" + threshold);
        send((PeerID) null, msg);
        if (threshold == 0) {
            return;
        }
        synchronized (ackLock) {
            try {
                ackLock.wait(timeout);
                if (ackSet.size() >= threshold) {
                    ackList = new HashSet<PeerID>(ackSet);
                    ackSet.clear();
                    return;
                }
            } catch (InterruptedException ie) {
                LOG.log(Level.FINEST, "Interrupted " + ie.toString());
            }
            ackList = new HashSet<PeerID>(ackSet);
            ackSet.clear();

            if (ackList.size() < threshold) {
                throw new SocketTimeoutException("Failed to receive minimum acknowledments of " + threshold + " received :" + ackList.size());
            }
        }
    }

    /**
     * Send a message.
     *
     * @param pid destination PeerID
     * @param msg the message to send
     * @throws IOException if an i/o error occurs
     */
    public void send(PeerID pid, Message msg) throws IOException {
        checkState();
        OutputPipe op;
        if (routeAdvElement != null && routeControl != null && sequence.intValue() < 2) {
            msg.addMessageElement(NAMESPACE, routeAdvElement);
        }

        LOG.log(Level.FINEST, "Sending a message");
        if (pid != null) {
            if (!pipeCache.containsKey(pid)) {
                // Unicast datagram
                // create a op pipe to the destination peer
                op = pipeSvc.createOutputPipe(pipeAdv, Collections.singleton(pid), 1000);
                pipeCache.put(pid, op);
            } else {
                op = pipeCache.get(pid);
            }
            op.send(msg);
        } else {
            // multicast
            outputPipe.send(msg);
            //wait for ack's
        }
    }

    /**
     * Send a message to a set of peers
     *
     * @param ids destination PeerIDs
     * @param msg the message to send
     * @throws IOException if an i/o error occurs
     */
    public void send(Set<PeerID> ids, Message msg) throws IOException {
        checkState();
        this.threshold = ids.size();
        ackList.clear();
        ackSet.clear();
        if (routeAdvElement != null && routeControl != null && sequence.intValue() < 2) {
            msg.addMessageElement(NAMESPACE, routeAdvElement);
        }

        LOG.log(Level.FINEST, "Sending a message");
        if (!ids.isEmpty()) {
            // Unicast datagram
            // create a op pipe to the destination peer
            OutputPipe op = pipeSvc.createOutputPipe(pipeAdv, ids, 1000);
            op.send(msg);
            op.close();
            synchronized (ackLock) {
                try {
                    ackLock.wait(timeout);
                    if (ackSet.size() >= threshold) {
                        ackList = new HashSet<PeerID>(ackSet);
                        ackSet.clear();
                        return;
                    }
                } catch (InterruptedException ie) {
                    LOG.log(Level.FINEST, "Interrupted " + ie.toString());
                }
                if (ackSet.size() < threshold) {
                    ackList = new HashSet<PeerID>(ackSet);
                    ackSet.clear();
                    throw new SocketTimeoutException("Failed to receive minimum acknowledments of " + threshold + " received :" + ackSet.size());
                }
            }
        }
    }

    private void processRoute(final Message msg) {
        try {
            final MessageElement routeElement = msg.getMessageElement(NAMESPACE, ROUTEADV);
            if (routeElement != null && routeControl != null) {
                XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(
                        routeElement.getMimeType(), routeElement.getStream());
                final RouteAdvertisement route = (RouteAdvertisement)
                        AdvertisementFactory.newAdvertisement(asDoc);
                routeControl.addRoute(route);
            }
        } catch (IOException io) {
            LOG.log(Level.WARNING, io.getLocalizedMessage(), io);
        }
    }
}

