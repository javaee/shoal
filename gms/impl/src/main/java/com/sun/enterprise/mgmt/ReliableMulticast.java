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
package com.sun.enterprise.mgmt;

import com.sun.enterprise.ee.cms.impl.base.PeerID;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import com.sun.enterprise.mgmt.transport.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ReliableMulticast {
    private static final Logger logger = GMSLogDomain.getMcastLogger();
    private static final Logger monitorLogger = GMSLogDomain.getMonitorLogger();

    private final long DEFAULT_EXPIRE_DURATION_MS;
    private final long DEFAULT_EXPIRE_REAPING_FREQUENCY;

    private MulticastMessageSender sender = null;
    private final Timer time;
    private final ConcurrentHashMap<Long, ReliableBroadcast> sendHistory = new ConcurrentHashMap<Long, ReliableBroadcast>();
    private ClusterManager manager = null;


    private static class ReliableBroadcast {
        final private Message msg;
        final private long    startTime;
        final private long    expirationTime_ms;
        private       int     resends;

        public ReliableBroadcast(Message msg, long expireDuration_ms) {
            this.msg = msg;
            this.startTime = System.currentTimeMillis();
            this.expirationTime_ms = startTime + expireDuration_ms;
            this.resends = 0;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime_ms;
        }
    }


    // added for junit testing verification of expiration.
    public int sendHistorySize() {
        return sendHistory.size();
    }

    void add(Message msg, long expireDuration_ms) {
        long seqId = MasterNode.getMasterViewSequenceID(msg);
        if (seqId != -1 ) {
            ReliableBroadcast rb = new ReliableBroadcast(msg, expireDuration_ms);
            sendHistory.put(seqId, rb);
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("ReliableBroadcast.add msg[" + clusterViewEventMsgToString(msg) + "]");
            }
        }
    }

    static private String clusterViewEventMsgToString(Message msg) {
        StringBuffer sb = new StringBuffer(40);
        try {
        long seqId =  MasterNode.getMasterViewSequenceID(msg);
        Object element = msg.getMessageElement(MasterNode.VIEW_CHANGE_EVENT);
        ClusterViewEvents type = null;
        String cveType = null;
        String memberName = null;
        PeerID peerId = null;
        if (element != null && element instanceof ClusterViewEvent) {
            ClusterViewEvent cve = (ClusterViewEvent)element;
            type = cve.getEvent();
            memberName = cve.getAdvertisement().getName();
            peerId = cve.getAdvertisement().getID();
            cveType = type.toString();
            sb.append("broadcast seq id:").append(seqId).append(" viewChangeEvent:").append(cveType).
               append(" member:").append(memberName).append(" peerId:" + peerId);
        }
        } catch (Error e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public void processExpired() {
        int numExpired = 0;
        Set<ConcurrentHashMap.Entry<Long, ReliableBroadcast>> entrySet = sendHistory.entrySet();
        for (ConcurrentHashMap.Entry<Long, ReliableBroadcast> entry : entrySet) {
            ReliableBroadcast rb = entry.getValue();
            if (rb.isExpired()) {
                numExpired++;
                entrySet.remove(entry);
                if (rb.resends > 0) {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, "expire resent msg with masterViewSeqID=" + entry.getKey() + " resent:" + rb.resends);
                    }
                }
            }
        }

        if (numExpired > 0 && logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "processExpired: expired " + numExpired + " masterViewSeqID messages");
        }
    }

    // TODO:  possible optimization: consider only resending certain ClusterViewEvents.
    //        given the late arrival of the event, its view will almost always be stale. especially add_events.
    public boolean resend(PeerID to, Long seqId) throws IOException {
        boolean result = false;
        ReliableBroadcast rb = sendHistory.get(seqId);
        if (rb != null) {
            Message msg = rb.msg;
            msg.addMessageElement("RESEND", Boolean.TRUE);
            result = manager.getNetworkManager().send(to, rb.msg);
            rb.resends++;
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "mgmt.reliable.mcast.resend",
                                        new Object[]{seqId, to.getInstanceName(),to.getGroupName(), rb.resends,
                                                     clusterViewEventMsgToString(msg)});
            }
        } else if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "mgmt.reliable.mcast.resend.failed",
                                    new Object[]{seqId, to.getInstanceName(),to.getGroupName()});
        }
        return result;
    }


    public ReliableMulticast(ClusterManager manager) {
        DEFAULT_EXPIRE_DURATION_MS = 12 * 1000; // 12 seconds.
        DEFAULT_EXPIRE_REAPING_FREQUENCY = DEFAULT_EXPIRE_DURATION_MS + (DEFAULT_EXPIRE_DURATION_MS / 2);
        this.manager = manager;
        this.sender = manager.getNetworkManager().getMulticastMessageSender();
        TimerTask reaper = new Reaper(this);
        time = new Timer();                                    
        time.schedule(reaper, DEFAULT_EXPIRE_REAPING_FREQUENCY , DEFAULT_EXPIRE_REAPING_FREQUENCY);
    }

    // junit testing.
    public ReliableMulticast(long expire_duration_ms) {
        DEFAULT_EXPIRE_DURATION_MS = expire_duration_ms;
        DEFAULT_EXPIRE_REAPING_FREQUENCY = DEFAULT_EXPIRE_DURATION_MS + (DEFAULT_EXPIRE_DURATION_MS / 2);
        TimerTask reaper = new Reaper(this);
        time = new Timer();
        time.schedule(reaper, DEFAULT_EXPIRE_REAPING_FREQUENCY , DEFAULT_EXPIRE_REAPING_FREQUENCY);
    }

    public void stop() {
        time.cancel();
    }

    public boolean broadcast(Message msg) throws IOException {
        boolean result = false;

        if( sender == null ){
            throw new IOException( "multicast sender is null" );
        }
        result = sender.broadcast(msg);
        if (result) {
            add(msg, DEFAULT_EXPIRE_DURATION_MS);
        }
        return result;
    }

    static class Reaper extends TimerTask {
        final private ReliableMulticast rb;
        public Reaper(ReliableMulticast rb) {
            this.rb = rb;
        }

        public void run() {
            rb.processExpired();
        }
    }
}
