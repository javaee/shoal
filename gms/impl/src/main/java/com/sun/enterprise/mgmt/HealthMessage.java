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

import com.sun.enterprise.ee.cms.impl.base.CustomTagNames;
import com.sun.enterprise.ee.cms.impl.base.SystemAdvertisement;
import com.sun.enterprise.ee.cms.impl.base.PeerID;
import com.sun.enterprise.ee.cms.impl.base.Utility;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class contains health states of members
 *
 * {@link com.sun.enterprise.mgmt.HealthMonitor} uses this messages to check the member's health.
 */
public class HealthMessage implements Serializable {

    static final long serialVersionUID = -5452866103975155397L;

    private static final Logger LOG = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);

    private List<Entry> entries = new ArrayList<Entry>();
    private PeerID srcID;

    /**
     * Default Constructor
     */
    public HealthMessage() {
    }

    /**
     * gets the entries list
     *
     * @return List The List containing Entries
     */
    public List<Entry> getEntries() {
        return entries;
    }

    /**
     * gets the src id
     *
     * @return Peerid The sender's peer id
     */
    public PeerID getSrcID() {
        return srcID;
    }

    public void add(final Entry entry) {
        if (!entries.contains(entry)) {
            entries.add(entry);
        }
    }

    public void remove(final Entry entry) {
        if (entries.contains(entry)) {
            entries.remove(entries.indexOf(entry));
        }
    }

    /**
     * sets the unique id
     *
     * @param id The id
     */
    public void setSrcID(final PeerID id) {
        this.srcID = (id == null ? null : id);
    }

    /**
     * returns the document string representation of this object
     *
     * @return String representation of the of this message type
     */
    public String toString() {
        return HealthMessage.class.getSimpleName() + "[" + srcID + ":" + entries + "]";
    }

    /**
     * Entries class
     */
    public static final class Entry implements Serializable, Cloneable{

        static final long serialVersionUID = 7485962183100651020L;
        /**
         * Entry ID entry id
         */
        final PeerID id;
        /**
         * Entry adv SystemAdvertisement
         */
        final SystemAdvertisement adv;
        /**
         * Entry state
         */
        String state;

        /**
         * Entry timestamp
         */
        long timestamp;

        /**
         * * Entry sequence ID
         */
        final long seqID;
        transient long srcStartTime = 0;

        /**
         * Creates a Entry with id and state
         *
         * @param adv   SystemAdvertisement
         * @param state state value
         * @param seqID health message sequence ID
         */
        public Entry(final SystemAdvertisement adv, final String state, long seqID) {
            this.state = state;
            this.adv = adv;
            this.id = (PeerID) adv.getID();
            this.timestamp =System.currentTimeMillis();
            this.seqID = seqID;
        }

        public Entry(final Entry previousEntry, final String newState) {
            this(previousEntry.adv,  newState, previousEntry.seqID + 1);
        }

        // copy ctor
        public Entry(final Entry entry) {
            this(entry.adv, entry.state, entry.seqID);
        }

        public long getSeqID() {
            return seqID;
        }

        // todo: change calling methods to check for NO_SUCH_TIME instead of -1
        public long getSrcStartTime() {
            srcStartTime = Utility.getStartTime(adv);
            return (srcStartTime == Utility.NO_SUCH_TIME ? -1 : srcStartTime);
        }

        /**
         * Since MasterNode reports on other peers that they are DEAD or INDOUBT, be sure not to compare sequence ids between
         * a peer and a MasterNode health message report on that peer.
         *
         * @param other
         * @return true if this HM.entry and other are from same member.
         */
        public boolean isFromSameMember(HealthMessage.Entry other) {
            return (other != null && id.equals(other.id));
        }

        /**
         * Detect when one hm is from a failed member and the new hm is from the restart of that member.
         * @param other
         * @return true if same instantiation of member sent this health message.
         */
        public boolean isFromSameMemberStartup(HealthMessage.Entry other) {
            return (other != null && id.equals(other.id) && getSrcStartTime() == other.getSrcStartTime());
        }

        public boolean isState(int theState) {
            return state.equals(HealthMonitor.states[theState]);
        }

        /**
         * {@inheritDoc}
         */
        public boolean equals(final Object obj) {
            return obj instanceof Entry && this == obj || obj != null && id.equals(((HealthMessage.Entry)obj).id);
        }

        /**
         * {@inheritDoc}
         */
        public int hashCode() {
            return id.hashCode() * 45191;
        }

        /**
         * {@inheritDoc}
         */
        public String toString() {
            return "HealthMessage.Entry: Id = " + id.toString() + "; State = " + state + "; LastTimeStamp = " + timestamp + "; Sequence ID = " + seqID;
        }

        public Object clone() throws CloneNotSupportedException {
            return (HealthMessage.Entry)super.clone();
        }
    }
}

