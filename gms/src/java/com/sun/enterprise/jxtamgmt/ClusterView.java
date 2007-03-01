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

import net.jxta.id.ID;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
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

    /**
     * Constructs the ClusterView object with a given TreeMap containing the
     * system advertisements of members in the group. The membership is sorted
     * based on the PeerIds.
     *
     * @param advertisements the Map of advertisements ordered by Peer ID sort
     * @param viewId         View ID
     */
    ClusterView(final TreeMap<String, SystemAdvertisement> advertisements,
                final long viewId) {
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
        view.put(advertisement.getID().toString(), advertisement);
        this.viewId = 0;
    }

    /**
     * Retrieves a system advertisement from a the table.
     *
     * @param id instance id
     * @return Returns the SystemAdvertisement associated with id
     */
    public synchronized SystemAdvertisement get(final ID id) {
        return view.get(id.toString());
    }

    /**
     * Adds a system advertisement to the view.
     *
     * @param adv adds a system advertisement to the view
     */
    public synchronized void add(final SystemAdvertisement adv) {
        view.put(adv.getID().toString(), adv);
    }

    public synchronized boolean containsKey(final ID id) {
        return view.containsKey(id.toString());
    }

    /**
     * Returns a sorted list containing the System Advertisements of peers in
     * PeerId sorted order
     *
     * @return List - list of system advertisements
     */
    public synchronized List<SystemAdvertisement> getView() {
        return new ArrayList<SystemAdvertisement>(view.values());
    }

    /**
     * Returns the list of peer names in the peer id sorted order from the
     * cluster view
     *
     * @return List
     */
    public synchronized List<String> getPeerNamesInView() {
        return new ArrayList<String>(view.keySet());
    }

    /**
     * Gets the viewSize attribute of the ClusterView object
     *
     * @return The viewSize
     */
    public synchronized int getSize() {
        return view.size();
    }

    /**
     * Returns the top node on the list
     *
     * @return the top node on the list
     */
    public SystemAdvertisement getMasterCandidate() {
        final String id = view.firstKey();
        final SystemAdvertisement adv = view.get(id);
        LOG.log(Level.FINE,
                new StringBuffer().append("Returning Master Candidate Node :")
                        .append(adv.getName()).append(' ').append(adv.getID())
                        .toString());
        return adv;
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
     * the index of id this view, or -1 if this view does not contain this
     * element.
     *
     * @param id id
     * @return the index of id this view, or -1 if this view does not
     *         contain this element.
     */
    public int indexOf(final ID id) {
        int retval = -1;
        if (id != null) {
            final Iterator<String> it = view.keySet().iterator();
            final String idStr = id.toString();
            int index = 0;
            while (it.hasNext()) {
                if (it.next().equals(idStr)) {
                    retval = index;
                }
                index++;
            }
        } else {
            retval = -1;
        }
        return retval;
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
        view.clear();
    }
}

