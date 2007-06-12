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

import net.jxta.id.ID;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ClusterView is a snapshot of the current membership of the group. The
 * ClusterView is created anew each time a membership change occurs in the group.
 * ClusterView is managed by ClusterViewManager.
 */
public class ClusterView {
    private static final Logger LOG = JxtaUtil.getLogger(ClusterView.class.getName());

    private final TreeMap<String, SystemAdvertisement> view;
    private final long viewId;
    private ReentrantLock viewLock = new ReentrantLock(true);

    /**
     * Constructs the ClusterView object with a given TreeMap containing the
     * system advertisements of members in the group. The membership is sorted
     * based on the PeerIds.
     *
     * @param advertisements the Map of advertisements ordered by Peer ID sort
     * @param viewId         View ID
     */
    ClusterView(final TreeMap<String, SystemAdvertisement> advertisements, final long viewId) {
        view = new TreeMap<String, SystemAdvertisement>(advertisements);
        this.viewId = viewId;
    }

    /**
     * Constructs the ClusterView object with a given TreeMap containing the
     * system advertisements of members in the group. The membership is sorted
     * based on the PeerIds.
     *
     * @param advertisement this nodes system advertisement
     */
    ClusterView(SystemAdvertisement advertisement) {
        view = new TreeMap<String, SystemAdvertisement>();
        viewLock.lock();
        try {
            view.put(advertisement.getID().toString(), advertisement);
        } finally {
            viewLock.unlock();
        }
        this.viewId = 0;
    }

    /**
     * Retrieves a system advertisement from a the table.
     *
     * @param id instance id
     * @return Returns the SystemAdvertisement associated with id
     */
    public SystemAdvertisement get(final ID id) {
        return view.get(id.toString());
    }

    /**
     * Adds a system advertisement to the view.
     *
     * @param adv adds a system advertisement to the view
     */
    public void add(final SystemAdvertisement adv) {
        viewLock.lock();
        try {
            view.put(adv.getID().toString(), adv);
        } finally {
            viewLock.unlock();
        }
    }

    public boolean containsKey(final ID id) {
        boolean hasKey = false;
        viewLock.lock();
        try {
            hasKey = view.containsKey(id.toString());
        } finally {
            viewLock.unlock();
        }
        return hasKey;
    }

    /**
     * Returns a sorted list containing the System Advertisements of peers in
     * PeerId sorted order
     *
     * @return List - list of system advertisements
     */
    public List<SystemAdvertisement> getView() {
        List<SystemAdvertisement> viewCopy;
        viewLock.lock();
        try {
            viewCopy = new ArrayList<SystemAdvertisement>(view.values());
        } finally {
            viewLock.unlock();
        }
        return viewCopy;
    }

    /**
     * Returns the list of peer names in the peer id sorted order from the
     * cluster view
     *
     * @return List
     */
    public List<String> getPeerNamesInView() {
        List<String> peerNamesList;
        viewLock.lock();
        try {
            peerNamesList = new ArrayList<String>(view.keySet());
        } finally {
            viewLock.unlock();
        }
        return peerNamesList;
    }

    /**
     * Gets the viewSize attribute of the ClusterView object
     *
     * @return The viewSize
     */
    public int getSize() {
        int size = 0;
        viewLock.lock();
        try {
            size = view.size();
        } finally {
            viewLock.unlock();
        }
        return size;
    }

    /**
     * Returns the top node on the list
     *
     * @return the top node on the list
     */
    public SystemAdvertisement getMasterCandidate() {
        viewLock.lock();
        try {
            final String id = view.firstKey();
            final SystemAdvertisement adv = view.get(id);
            LOG.log(Level.FINE,
                    new StringBuffer().append("Returning Master Candidate Node :")
                            .append(adv.getName()).append(' ').append(adv.getID())
                            .toString());
            return adv;
        } finally {
            viewLock.unlock();
        }
    }

    /**
     * Determines whether this node is at the top of the list
     *
     * @param advertisement the advertisement to test
     * @return true if this node is a the top of the list, false otherwise
     */
    public boolean isFirst(SystemAdvertisement advertisement) {
        final String id = view.firstKey();
        return advertisement.getID().toString().equals(id);
    }

    /**
     * Returns the cluster view ID
     *
     * @return the view id
     */
    public long getClusterViewId() {
        return viewId;
    }

    /**
     * Removes all of the elements from this set (optional operation).
     */
    public void clear() {
        viewLock.lock();
        try {
            view.clear();
        } finally {
            viewLock.unlock();
        }
    }
}

