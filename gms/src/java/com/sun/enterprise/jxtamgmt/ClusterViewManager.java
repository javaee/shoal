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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages Cluster Views and notifies cluster view listeners when cluster view
 * changes
 */
public class ClusterViewManager {
    private static final Logger LOG = JxtaUtil.getLogger(ClusterViewManager.class.getName());
    private TreeMap<String, SystemAdvertisement> view = new TreeMap<String, SystemAdvertisement>();
    private SystemAdvertisement advertisement = null;
    private SystemAdvertisement masterAdvertisement = null;
    private List<ClusterViewEventListener> cvListeners =
            new ArrayList<ClusterViewEventListener>();
    private long viewId = 0;
    private long masterViewID = 0;
    private ClusterManager manager;
    private ReentrantLock viewLock = new ReentrantLock();

    /**
     * Constructor for the ClusterViewManager object
     *
     * @param advertisement the advertisement of the instance associated with
     *                      this object
     * @param manager       the cluster manager
     * @param listeners     <code>List</code> of <code>ClusterViewEventListener</code>
     */
    public ClusterViewManager(final SystemAdvertisement advertisement,
                              final ClusterManager manager,
                              final List<ClusterViewEventListener> listeners) {
        this.advertisement = advertisement;
        this.manager = manager;
        cvListeners.addAll(listeners);
    }

    public void start() {
        //Self appointed as master and then discover so that we resolve to the right one
        setMaster(advertisement, true);
    }

    public void addClusterViewEventListener(
            final ClusterViewEventListener listener) {
        cvListeners.add(listener);
    }

    public void removeClusterViewEventListener(
            final ClusterViewEventListener listener) {
        cvListeners.remove(listener);
    }

    /**
     * adds a system advertisement
     *
     * @param advertisement system adverisement to add
     */
    void add(final SystemAdvertisement advertisement) {

        viewLock.lock();
        try {
            if (!view.containsKey(advertisement.getID().toString())) {
                LOG.log(Level.FINER, new StringBuffer().append("Adding ")
                        .append(advertisement.getName())
                        .append("   ")
                        .append(advertisement.getID().toString())
                        .toString());

                view.put(advertisement.getID().toString(), advertisement);
                LOG.log(Level.FINER, MessageFormat.format("Cluster view now contains {0} entries", getViewSize()));
            }
        } finally {
            viewLock.unlock();
        }
    }

    /**
     * Set the master instance
     *
     * @param advertisement Master system adverisement
     * @param notify        if true, notifies registered listeners
     */
    void setMaster(final SystemAdvertisement advertisement, boolean notify) {
        if (!advertisement.equals(masterAdvertisement)) {
            masterAdvertisement = advertisement;
            viewLock.lock();
            try {
                view.put(masterAdvertisement.getID().toString(), masterAdvertisement);
            } finally {
                viewLock.unlock();
            }
            if (notify) {
                notifyListeners(new ClusterViewEvent(
                        ClusterViewEvents.MASTER_CHANGE_EVENT,
                        advertisement));
            }
            if (advertisement.getID().equals(this.advertisement.getID())) {
                LOG.log(Level.FINER, "Setting MasterNode Role");
            } else {
                LOG.log(Level.FINER,
                        new StringBuffer().append("Setting Master Node :")
                                .append(advertisement.getName()).append(' ')
                                .append(advertisement.getID()).toString());
            }

        }
    }

    /**
     * Gets the master advertisement
     *
     * @return SystemAdvertisement Master system adverisement
     */
    public SystemAdvertisement getMaster() {
        return masterAdvertisement;
    }

    /**
     * Retrieves a system advertisement from a the table
     *
     * @param id instance id
     * @return Returns the SystemAdvertisement associated with id
     */
    public SystemAdvertisement get(final ID id) {
        final SystemAdvertisement adv;
        viewLock.lock();
        try {
            adv = view.get(id.toString());
        } finally {
            viewLock.unlock();
        }
        return adv;
    }

    /**
     * removes an entry from the table. This is only called when a
     * failure occurs.
     *
     * @param id Instance ID
     * @return SystemAdvertisement removed  or null if not in view.
     */
    SystemAdvertisement remove(final ID id) {
        SystemAdvertisement advertisement = null;
        if (containsKey(id)) {
            viewLock.lock();
            try {
                advertisement = view.remove(id.toString());
            } finally {
                viewLock.unlock();
            }

            LOG.log(Level.FINER, "Removed " + advertisement.getName() + "   "
                    + advertisement.getID().toString());
        } else {
            LOG.log(Level.FINEST, "Skipping removal of " + id
                    + " Not in view");
        }
        return advertisement;
    }

    public boolean containsKey(final ID id) {
        final boolean contains;
        viewLock.lock();
        try {
            contains = view.containsKey(id.toString());
        } finally {
            viewLock.unlock();
        }

        return contains;
    }

    /**
     * Resets the view
     */
    void reset() {
        LOG.log(Level.FINEST, "Resetting View");
        viewLock.lock();
        try {
            view.clear();

            view.put(advertisement.getID().toString(), advertisement);
        } finally {
            viewLock.unlock();
        }
    }

    /**
     * Returns a sorted localView
     *
     * @return The localView List
     */
    public ClusterView getLocalView() {
        final TreeMap<String, SystemAdvertisement> temp;
        viewLock.lock();
        try {
            temp = (TreeMap<String, SystemAdvertisement>) view.clone();
        } finally {
            viewLock.unlock();
        }
        LOG.log(Level.FINEST, "returning new ClusterView with view size:" + view.size());
        return new ClusterView(temp, viewId++);
    }

    /**
     * Gets the viewSize attribute of the ClusterViewManager object
     *
     * @return The viewSize
     */
    public int getViewSize() {
        int size;

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
    SystemAdvertisement getMasterCandidate() {
        final SystemAdvertisement adv;
        final String id = view.firstKey();
        viewLock.lock();
        try {
            adv = view.get(id);
        } finally {
            viewLock.unlock();
        }
        LOG.log(Level.FINER,
                new StringBuffer().append("Returning Master Candidate Node :")
                        .append(adv.getName()).append(' ').append(adv.getID())
                        .toString());
        return adv;
    }

    /**
     * Determines whether this node is the Master
     *
     * @return true if this node is the master node
     */
    public boolean isMaster() {
        return masterAdvertisement != null && masterAdvertisement.getID().equals(advertisement.getID());
    }

    /**
     * Determines whether this node is at the top of the list
     *
     * @return true if this node is a the top of the list, false otherwise
     */
    public boolean isFirst() {
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
        if (id == null) {
            return -1;
        }
        final String idStr = id.toString();
        int index = 0;
        String key;
        for (String s : view.keySet()) {
            key = s;
            if (key.equals(idStr)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public ID getID(final String name) {
        return manager.getID(name);
    }

    /**
     * Adds a list of advertisements to the view
     *
     * @param newView       list of advertisements
     * @param cvEvent       the cluster event
     * @param authoritative whether the view is authoritative or not
     */
    void addToView(final List<SystemAdvertisement> newView,
                   final boolean authoritative,
                   final ClusterViewEvent cvEvent) {
        //TODO: need to review the use cases of the callers of method
        if (cvEvent == null) {
            return;
        }
        //if this is a new authoritative view, just replace the old one with the new one.
        if (authoritative) {
            LOG.log(Level.FINER, "Resetting View");
            reset();
        }

        if (authoritative) {
            boolean changed = false;
            viewLock.lock();
            try {
                if (!newView.contains(manager.getSystemAdvertisement())) {
                    view.put(manager.getSystemAdvertisement().getID().toString(),
                            manager.getSystemAdvertisement());
                }
                for (SystemAdvertisement elem : newView) {
                    LOG.log(Level.FINER,
                            new StringBuffer().append("Adding ")
                                    .append(elem.getID()).append(" to view")
                                    .toString());
                    if (!view.containsKey(elem.getID().toString())) {
                        changed = true;
                    }
                    // Always add the wire version of the adv
                    view.put(elem.getID().toString(), elem);
                }
            } finally {
                viewLock.unlock();
            }
            if (changed) {
                //only if there are changes that we notify
                notifyListeners(cvEvent);
            }
        }
    }

    void notifyListeners(final ClusterViewEvent event) {
        LOG.log(Level.FINER, MessageFormat.format("Notifying the {0} to listeners, peer in event is {1}",
                    event.getEvent().toString(), event.getAdvertisement().getName()));
        for (ClusterViewEventListener elem : cvListeners) {
            elem.clusterViewEvent(event, getLocalView());
        }
    }

    public void setInDoubtPeerState(final SystemAdvertisement adv) {
        if (adv == null) {
            throw new IllegalArgumentException("SystemAdvertisment may not be null");
        }
        notifyListeners(new ClusterViewEvent(ClusterViewEvents.IN_DOUBT_EVENT, adv));
    }

    public void setPeerStoppingState(final SystemAdvertisement adv) {
        if (adv == null) {
            throw new IllegalArgumentException("SystemAdvertisment may not be null");
        }
        notifyListeners(new ClusterViewEvent(ClusterViewEvents.PEER_STOP_EVENT, adv));
    }

    public void setClusterStoppingState(final SystemAdvertisement adv) {
        if (adv == null) {
            throw new IllegalArgumentException("SystemAdvertisment may not be null");
        }
        notifyListeners(new ClusterViewEvent(ClusterViewEvents.CLUSTER_STOP_EVENT, adv));
    }

    public void setPeerNoLongerInDoubtState(final SystemAdvertisement adv) {
        if (adv == null) {
            throw new IllegalArgumentException("SystemAdvertisment may not be null");
        }
        notifyListeners(new ClusterViewEvent(ClusterViewEvents.NO_LONGER_INDOUBT_EVENT, adv));
    }

    public long getMasterViewID() {
        return masterViewID;
    }

    public void setMasterViewID(long masterViewID) {
        this.masterViewID = masterViewID;
    }
}

