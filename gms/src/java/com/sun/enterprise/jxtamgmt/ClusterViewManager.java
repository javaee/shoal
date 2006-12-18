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

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO: Stop should send out stop event to all members followed by notifications
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
    private Map<String, ID> nameTable = new HashMap<String, ID>();
    private static final String viewLock = new String("vl");

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

        synchronized (viewLock) {
            if (!view.containsKey(advertisement.getID().toString())) {
                LOG.log(Level.FINER, new StringBuffer().append("Adding ")
                        .append(advertisement.getName())
                        .append("   ")
                        .append(advertisement.getID().toString())
                        .toString());

                view.put(advertisement.getID().toString(), advertisement);
                LOG.log(Level.FINER, "Cluster view now contains " + getViewSize()
                        + " entries");
            }
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
            synchronized (viewLock) {
                view.put(masterAdvertisement.getID().toString(),
                        masterAdvertisement);
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
        synchronized (viewLock) {
            adv = view.get(id.toString());
        }
        return adv;
    }

    /**
     * removes an entry from the table. This is only called when a
     * failure occurs.
     *
     * @param id Instance ID
     */
    void remove(final ID id) {
        if (containsKey(id)) {
            synchronized (viewLock) {
                final SystemAdvertisement advertisement =
                        view.remove(id.toString());

                LOG.log(Level.FINER, "Removed " + advertisement.getName() + "   "
                        + advertisement.getID().toString());

                notifyListeners(
                        new ClusterViewEvent(ClusterViewEvents.FAILURE_EVENT,
                                advertisement));
            }
        } else {
            LOG.log(Level.FINEST, "Skipping removal of " + advertisement.getName()
                    + "   " + advertisement.getID().toString()
                    + " Not in view");
        }
    }

    public boolean containsKey(final ID id) {
        final boolean contains;
        synchronized (viewLock) {
            contains = view.containsKey(id.toString());
        }
        return contains;
    }

    /**
     * Resets the view
     */
    void reset() {
        LOG.log(Level.FINEST, "Resetting View");
        synchronized (viewLock) {
            view.clear();
            view.put(advertisement.getID().toString(), advertisement);
        }
    }

    /**
     * Returns a sorted localView
     *
     * @return The localView List
     */
    public ClusterView getLocalView() {
        final TreeMap<String, SystemAdvertisement> temp;
        synchronized (viewLock) {
            temp = (TreeMap<String, SystemAdvertisement>) view.clone();
            LOG.log(Level.FINEST, "returning new ClusterView with view size:" +
                    view.size());
        }
        return new ClusterView(temp, viewId++);
    }

    /**
     * Gets the viewSize attribute of the ClusterViewManager object
     *
     * @return The viewSize
     */
    public int getViewSize() {
        synchronized (viewLock) {
            return view.size();
        }
    }

    /**
     * Returns the top node on the list
     *
     * @return the top node on the list
     */
    SystemAdvertisement getMasterCandidate() {
        final SystemAdvertisement adv;
        final String id = view.firstKey();
        synchronized (viewLock) {
            adv = view.get(id);
            LOG.log(Level.FINER,
                    new StringBuffer().append("Returning Master Candidate Node :")
                            .append(adv.getName()).append(' ').append(adv.getID())
                            .toString());
        }
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
        final Iterator<String> it = view.keySet().iterator();
        final String idStr = id.toString();
        int index = 0;
        String key;
        while (it.hasNext()) {
            key = it.next();
            if (key.equals(idStr)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public ID getID(final String name) {
        ID id = nameTable.get(name);
        if (id == null) {
            synchronized (viewLock) {
                for (final SystemAdvertisement adv : view.values()) {
                    if (adv.getName().equals(name)) {
                        id = adv.getID();
                        nameTable.put(name, id);
                        break;
                    }
                }
            }
        }
        return id;
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
            synchronized (viewLock) {
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
            }
            if (changed) {
                //only if there are changes that we notify
                notifyListeners(cvEvent);
            }
        }
    }

    void notifyListeners(final ClusterViewEvent event) {
        LOG.log(Level.FINER, "Notifying the " + event.getEvent().toString() +
                " to listeners, peer in event is " +
                event.getAdvertisement().getName());
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

