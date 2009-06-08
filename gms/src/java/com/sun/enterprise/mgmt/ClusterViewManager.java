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
package com.sun.enterprise.mgmt;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.ee.cms.impl.base.SystemAdvertisement;
import com.sun.enterprise.ee.cms.impl.base.PeerID;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

/**
 * Manages Cluster Views and notifies cluster view listeners when cluster view
 * changes
 */
public class ClusterViewManager {
    private static final Logger LOG = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    private TreeMap<PeerID, SystemAdvertisement> view = new TreeMap<PeerID, SystemAdvertisement>();
    private SystemAdvertisement advertisement = null;
    private SystemAdvertisement masterAdvertisement = null;
    private List<ClusterViewEventListener> cvListeners = new ArrayList<ClusterViewEventListener>();
    private long viewId = 0;
    private AtomicLong masterViewID = new AtomicLong(0);
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
        // avoid notifying listener
        setMaster(advertisement, false);
    }

    public void addClusterViewEventListener(
            final ClusterViewEventListener listener) {
        cvListeners.add(listener);
    }

    public void removeClusterViewEventListener(final ClusterViewEventListener listener) {
        cvListeners.remove(listener);
    }

    /**
     * adds a system advertisement
     *
     * @param advertisement system adverisement to add
     */
    void add(final SystemAdvertisement advertisement) {
        lockLog("add()");
        viewLock.lock();
        try {
            if (!view.containsKey(advertisement.getID())) {
                LOG.log(Level.FINER, new StringBuffer().append("Adding ")
                        .append(advertisement.getName())
                        .append("   ")
                        .append(advertisement.getID().toString())
                        .toString());

                view.put(advertisement.getID(), advertisement);
                LOG.log(Level.FINER, MessageFormat.format("Cluster view now contains {0} entries", getViewSize()));
            } else {
                //if view does contain the same sys adv but the start time is different from what
                //was already in the view
                //then add the new sys adv
                SystemAdvertisement adv = view.get(advertisement.getID());
                if (manager.getMasterNode().confirmInstanceHasRestarted(adv, advertisement)) {
                    if (LOG.isLoggable(Level.FINE))  {
                        LOG.fine("ClusterViewManager .add() : Instance "+ advertisement.getName() + " has restarted. Adding it to the view.");
                    }
                    view.put(advertisement.getID(), advertisement);
                }
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
            lockLog("setMaster()");
            viewLock.lock();
            try {
                view.put(masterAdvertisement.getID(), masterAdvertisement);
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
     * Set the master instance with new view
     *
     * @param newView list of advertisements
     * @param advertisement Master system adverisement
     * @return true if there is master's change, false otherwise
     */
    boolean setMaster( final List<SystemAdvertisement> newView, final SystemAdvertisement advertisement ) {
        if( advertisement.equals( masterAdvertisement ) ) {
            return false;
        }
        lockLog("setMaster()");
        viewLock.lock();
        try {
            if ( newView != null ) {
                addToView( newView );
            }
            setMaster( advertisement, true );
        } finally {
            viewLock.unlock();
        }
        return true;
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
    public SystemAdvertisement get(final PeerID id) {
        final SystemAdvertisement adv;
        lockLog("get()");
        viewLock.lock();
        try {
            adv = view.get(id);
        } finally {
            viewLock.unlock();
        }
        return adv;
    }

    /**
     * removes an entry from the table. This is only called when a
     * failure occurs.
     *
     * @param advertisement Instance advertisement
     * @return SystemAdvertisement removed  or null if not in view.
     */
    SystemAdvertisement remove(final SystemAdvertisement advertisement) {
        SystemAdvertisement removed = null;
        final PeerID id = advertisement.getID();
        lockLog("remove()");
        viewLock.lock();
        try {
            removed = view.remove(id);
        } finally {
            viewLock.unlock();
        }
        if (removed != null ) {
            LOG.log(Level.FINER, "Removed " + removed.getName() + "   " + id);
        } else {
            LOG.log(Level.FINEST, "Skipping removal of " + id + " Not in view");
        }
        return removed;
    }

    public boolean containsKey(final PeerID id) {
        final boolean contains;
        viewLock.lock();
        try {
            contains = view.containsKey(id);
        } finally {
            viewLock.unlock();
        }

        return contains;
    }

    /**
     * Resets the view
     */
    void reset() {
        viewLock.lock();
        try {
            view.clear();
            view.put(advertisement.getID(), advertisement);
        } finally {
            viewLock.unlock();
        }
    }

    /**
     * Returns a sorted localView
     *
     * @return The localView List
     */
    @SuppressWarnings("unchecked")
    public ClusterView getLocalView() {
        final TreeMap<PeerID, SystemAdvertisement> temp;
        long localViewId = 0;
        lockLog("getLocalView()");
        viewLock.lock();
        try {
            temp = (TreeMap<PeerID, SystemAdvertisement>) view.clone();
            localViewId = viewId++;
        } finally {
            viewLock.unlock();
        }
        LOG.log(Level.FINEST, "returning new ClusterView with view size:" + view.size());
        return new ClusterView(temp, localViewId);
    }

    /**
     * Gets the viewSize attribute of the ClusterViewManager object
     *
     * @return The viewSize
     */
    public int getViewSize() {
        int size;
        lockLog("getViewSize()");
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
        lockLog("getMasterCandidate()");
        viewLock.lock();
        try {
            final PeerID id = view.firstKey();
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
        final PeerID id = view.firstKey();
        return advertisement.getID().equals(id);
    }

    /**
     * the index of id this view, or -1 if this view does not contain this
     * element.
     *
     * @param id id
     * @return the index of id this view, or -1 if this view does not
     *         contain this element.
     */
    public int indexOf(final PeerID id) {
        if (id == null) {
            return -1;
        }
        int index = 0;
        PeerID key;
        for (PeerID peerID : view.keySet()) {
            key = peerID;
            if (key.equals(id)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public PeerID getID(final String name) {
        return manager.getID(name);
    }

    /**
     * Adds a list of advertisements to the view
     *
     * @param newView           list of advertisements
     * @param cvEvent           the cluster event
     * @param notifyForcefully  if true, notifies registered listeners forcefully. if false, notifies listeners when view changed.
     */
    void addToView( final List<SystemAdvertisement> newView,
                    final boolean notifyForcefully,
                    final ClusterViewEvent cvEvent ) {
        if( cvEvent == null )
            return;
        boolean changed = addToView( newView );
        if( notifyForcefully || changed )
            notifyListeners( cvEvent );
    }

    /**
     * Adds a list of advertisements to the view
     *
     * @param newView       list of advertisements
     * @return true if there are changes, false otherwise
     */
    private boolean addToView( final List<SystemAdvertisement> newView ) {
        boolean changed = false;
        // We need old view's snapshot for becoming aware of changes before reset()
        // Though reset() also uses viewLock for view.clear(),
        // if reset() is called before addToView() acquires the viewLock, view can be changed in a short time
        // If view was changed in a short time, unexpected result occurred in becoming aware of changes
        // So for safety, if we need to become aware of real changes, snapshooting and view.clear() should be called in addToView()'s viewLock
        //reset();
        lockLog( "addToView()" );
        viewLock.lock();
        // old view's snapshot
        TreeMap oldView = (TreeMap)view.clone();
        LOG.log( Level.FINER, "Resetting View" );
        // we should clear view after old view's snapshot
        view.clear();
        view.put(advertisement.getID(), advertisement);
        try {
            // we don't need put manager.getSystemAdvertisement(). this operation is maybe duplicated.
            if( !newView.contains( manager.getSystemAdvertisement() ) ) {
                view.put( manager.getSystemAdvertisement().getID(),
                          manager.getSystemAdvertisement() );
            }
            for( SystemAdvertisement elem : newView ) {
                LOG.log( Level.FINER,
                         new StringBuffer().append( "Adding " )
                                 .append( elem.getID() ).append( " to view" )
                                 .toString() );
                if( !changed && !oldView.containsKey( elem.getID() ) ) {
                    changed = true;
                }
                // Always add the wire version of the adv
                view.put( elem.getID(), elem );
            }
        } finally {
            viewLock.unlock();
        }
        return changed;
    }

    void notifyListeners(final ClusterViewEvent event) {
        LOG.log(Level.FINER, MessageFormat.format("Notifying the {0} to listeners, peer in event is {1}",
                    event.getEvent().toString(), event.getAdvertisement().getName()));
        for (ClusterViewEventListener elem : cvListeners) {
            // carryel, this method should be thread-safe
            synchronized( elem ) {
                elem.clusterViewEvent(event, getLocalView());
            }
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
        return masterViewID.get();
    }

    public void setMasterViewID(long masterViewID) {
        this.masterViewID.set(masterViewID);
    }
    private void lockLog(String method) {
        LOG.log(Level.FINE, MessageFormat.format("{0} viewLock Hold count :{1}, lock queue count:{2}", method, viewLock.getHoldCount(), viewLock.getQueueLength()));
    }

    public void setPeerReadyState(SystemAdvertisement adv) {
        if (adv == null) {
            throw new IllegalArgumentException("SystemAdvertisment may not be null");
        }
        notifyListeners(new ClusterViewEvent(ClusterViewEvents.JOINED_AND_READY_EVENT, adv));

    }
}


