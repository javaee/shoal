/*
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 * Use is subject to license terms.
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
package com.sun.enterprise.ee.cms.impl.jxta;

import com.sun.enterprise.ee.cms.core.DistributedStateCache;
import com.sun.enterprise.ee.cms.core.GMSCacheable;
import com.sun.enterprise.ee.cms.core.GMSException;
import com.sun.enterprise.ee.cms.impl.common.DSCMessage;
import com.sun.enterprise.ee.cms.impl.common.GMSContextFactory;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import static java.util.logging.Level.FINER;
import java.util.logging.Logger;


/**
 * Messaging based implementation of a shared distributed state cache(DSC).
 * Every write entry made such as adding a new entry or removing an existing
 * entry is disseminated to all group members through a message. Read-only
 * operation i.e getFromCache() is a local call. During startup of a member, a
 * singleton instance of this class is created. This instance is available to
 * GMS client components in this member through
 * GroupHandle.getDistributedStateCache() so that the client components can
 * read or write to this cache.
 * <p>When an entry is added or removed, this implementation uses underlying
 * GroupCommunicationProvider(GCP) to sends a message to the all members of the
 * group. The recipients in turn call corresponding method for adding or
 * removing the entry to/from their copy of the this DistributedStateCache
 * implementation.
 * <p> When new member joins the group, the join notification is received
 * on every member. When this happens, and if this member is a leader of the
 * group, then it uses the GCP's messaging api to sends a message to the new
 * member to pass on the current state of this DSC. The remote member updates
 * its DSC with this current state while returning its own state to this member
 * through another message. This member updates its own DSC with this new entry
 * resulting in all members getting this updated state.  This brings the
 * group-wide DSC into a synchronized state.
 * <p>This initial sync-up is a heavy weight operation particularly during
 * startup of the whole group concurrently as new members are joining the group
 * rapidly.
 * To prevent data loss during such times, each sync activity will require a
 * blocking call to ensure that rapid group view changes during group startup
 * will not result in data loss.
 *
 * @author Shreedhar Ganapathy
 *         Date: June 20, 2006
 * @version $Revision$
 */
public class DistributedStateCacheImpl implements DistributedStateCache {
    private final ConcurrentHashMap<GMSCacheable, Object> cache =
            new ConcurrentHashMap<GMSCacheable, Object>();
    private GMSContext ctx = null;
    //singleton in each process
    private static DistributedStateCacheImpl instance;
    private final Logger logger =
            GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    private boolean firstSyncDone = false;
    private static final Map<String, DistributedStateCacheImpl> ctxCache =
            new HashMap<String, DistributedStateCacheImpl>();
    private static final byte[] BYTE = new byte[]{};
    private final String groupName;
    private ReentrantLock cacheLock = new ReentrantLock();

    //private constructor for single instantiation
    private DistributedStateCacheImpl(final String groupName) {
        this.groupName = groupName;
    }

    //return the only instance we want to return
    static DistributedStateCache getInstance(final String groupName) {
        if (ctxCache.get(groupName) == null) {
            instance = new DistributedStateCacheImpl(groupName);
            ctxCache.put(groupName, instance);
        } else {
            instance = ctxCache.get(groupName);
        }
        return instance;
    }

    /*
     * adds entry to local cache and calls remote members to add the entry to
     * their cache
     */
    public void addToCache(
            final String componentName, final String memberTokenId,
            final Serializable key, final Serializable state)
            throws GMSException {
        logger.log(Level.FINER, "Adding to DSC by local Member:" + memberTokenId +
                ",Component:" + componentName + ",key:" + key +
                ",State:" + state);
        final GMSCacheable cKey = createCompositeKey(componentName,
                memberTokenId,
                key);
        addToLocalCache(cKey, state);
        addToRemoteCache(cKey, state);
    }

    public void addToCache(final String componentName,
                           final String memberTokenId,
                           final Serializable key,
                           final byte[] state)
            throws GMSException {
        logger.log(Level.FINER, "Adding to DSC by local Member:" + memberTokenId +
                ",Component:" + componentName + ",key:" + key +
                ",State:" + state);
        final GMSCacheable cKey = createCompositeKey(componentName,
                memberTokenId,
                key);
        addToLocalCache(cKey, state);
        addToRemoteCache(cKey, state);
    }


    public void addToLocalCache(
            final String componentName, final String memberTokenId,
            final Serializable key, final Serializable state) {
        final GMSCacheable cKey = createCompositeKey(componentName,
                memberTokenId,
                key);
        addToLocalCache(cKey, state);
    }

    public void addToLocalCache(final String componentName,
                                final String memberTokenId,
                                final Serializable key,
                                final byte[] state) {
        final GMSCacheable cKey = createCompositeKey(componentName,
                memberTokenId,
                key);
        addToLocalCache(cKey, state);
    }

    /*
     * This is called by both addToCache() method and by the
     * RPCInvocationHandler to handle remote sync operations.
     */
    public void addToLocalCache(GMSCacheable cKey,
                                final Object state) {
        cKey = getTrueKey(cKey);
        cacheLock.lock();
        try{
            cache.put(cKey, state);
        }
        finally{
            cacheLock.unlock();
        }
        printDSCContents();
    }

    private void printDSCContents () {
        logger.log(FINER, getGMSContext().getServerIdentityToken()+
                               ":DSC now contains ---------\n"+getDSCContents());
    }

    private GMSContext getGMSContext(){
        if(ctx == null ){
            ctx = ( GMSContext ) GMSContextFactory.getGMSContext( groupName );
        }
        return ctx;
    }

    private String getDSCContents() {
        final StringBuffer buf = new StringBuffer();
        final ConcurrentHashMap<GMSCacheable, Object> copy;
        cacheLock.lock();
        try{
            copy = new ConcurrentHashMap<GMSCacheable, Object>(cache);
        }
        finally{
            cacheLock.unlock();
        }
        for (GMSCacheable c : copy.keySet()) {
            buf.append(c.hashCode()).append(" key=")
                    .append(c.toString())
                    .append(" : value=")
                    .append(copy.get(c))
                    .append("\n");
        }
        return buf.toString();
    }

    private void addToRemoteCache(final GMSCacheable cKey,
                                  final Object state)
            throws GMSException {
        final DSCMessage msg = new DSCMessage(cKey, state,
                DSCMessage.OPERATION.ADD.toString());

        sendMessage(null, msg);
    }

    /*
     * removes an entry from local cache and calls remote members to remove
     * the entry from their cache
     */
    public void removeFromCache(
            final String componentName, final String memberTokenId,
            final Serializable key) throws GMSException {
        final GMSCacheable cKey = createCompositeKey(componentName, memberTokenId,
                key);
        removeFromLocalCache(cKey);
        removeFromRemoteCache(cKey);
    }

    void removeFromLocalCache(GMSCacheable cKey) {
        cKey = getTrueKey(cKey);
        cacheLock.lock();
        try{
            cache.remove(cKey);
        }
        finally{
            cacheLock.unlock();
        }
    }

    private void removeFromRemoteCache(GMSCacheable cKey)
            throws GMSException {
        cKey = getTrueKey(cKey);
        final DSCMessage msg = new DSCMessage(cKey, null,
                DSCMessage.OPERATION.REMOVE.toString());
        sendMessage(null, msg);
    }

    /*
     * retrieves an entry from the local cache for the given parameters
     */
    public Object getFromCache(
            final String componentName, final String memberTokenId,
            final Serializable key)
            throws GMSException {
        if (key != null && componentName != null && memberTokenId != null) {
            GMSCacheable cKey = createCompositeKey(componentName, memberTokenId,
                    key);
            cKey = getTrueKey(cKey);
            final Object ret;
            cacheLock.lock();
            try{
                ret = cache.get(cKey);
            }
            finally{
                cacheLock.unlock();
            }
            return ret;
        } else {  //TODO: Localize
            throw new GMSException(
                    new StringBuffer().append(
                            "DistributedStateCache: ")
                            .append("componentName, memberTokenId and key ")
                            .append("are required parameters and cannot be null")
                            .toString());
        }
    }

    public Map<GMSCacheable, Object> getAllCache() {
        return new ConcurrentHashMap<GMSCacheable, Object>(cache);
    }

    public Map<Serializable, Serializable> getFromCacheForPattern(
            final String componentName,
            final String memberToken) {
        final Map<Serializable, Serializable> retval =
                new Hashtable<Serializable, Serializable>();

        if (componentName == null || memberToken == null) {
            return retval;
        }
        cacheLock.lock();
        try{
            for (GMSCacheable c : cache.keySet()) {
                if (componentName.equals(c.getComponentName())) {
                    if (memberToken.equals(c.getMemberTokenId())) {
                        retval.put((Serializable) c.getKey(),
                                (Serializable) cache.get(c));
                    }
                }
            }
        }
        finally{
            cacheLock.unlock();
        }

        if(!retval.isEmpty()){
            return retval;
        }
        else{
            if(!memberToken.equals(ctx.getServerIdentityToken())){
                cacheLock.lock();
                ConcurrentHashMap<GMSCacheable, Object> temp;
                try {
                    temp = new ConcurrentHashMap<GMSCacheable, Object>(cache);
                }
                finally {
                    cacheLock.unlock();
                }
                DSCMessage msg = new DSCMessage(temp,
                        DSCMessage.OPERATION.ADDALLLOCAL.toString(), true);
                try{
                    sendMessage(memberToken, msg);
                    Thread.sleep(3000);
                    retval.putAll( getFromCacheForPattern(componentName, memberToken));
                } catch (GMSException e) {
                    logger.log(Level.WARNING, "GMSException during DistributedStateCache Sync...."+e) ;
                } catch (InterruptedException e) {
                    //ignore
                }
            }
        }
        return retval;
    }

    public Map<GMSCacheable, Object> getFromCache(final Object key) {
        final Map<GMSCacheable, Object> retval =
                new Hashtable<GMSCacheable, Object>();
        cacheLock.lock();
        try{
            for (GMSCacheable c : cache.keySet()) {
                if (key.equals(c.getComponentName())
                        ||
                        key.equals(c.getMemberTokenId())
                        ||
                        key.equals(c.getKey())) {
                    retval.put(c, cache.get(c));
                }
            }
        }
        finally{
            cacheLock.unlock();
        }
        return retval;
    }

    public boolean contains(final Object key) {
        boolean retval = false;
        cacheLock.lock();
        try {
            for (GMSCacheable c : cache.keySet()) {
                logger.log(FINER,
                        new StringBuffer()
                                .append("key=")
                                .append(key)
                                .append(" underlying key=")
                                .append(c.getKey())
                                .toString());
                if (key.equals(c.getKey())) {
                    retval = true;
                }
            }
        }
        finally {
            cacheLock.unlock();
        }
        return retval;
    }

    public boolean contains(final String componentName, final Object key) {
        boolean retval = false;
        cacheLock.lock();
        try{
            for (GMSCacheable c : cache.keySet()) {
                logger.log(FINER,
                        new StringBuffer()
                                .append("comp=")
                                .append(componentName)
                                .append(" underlying comp=")
                                .append(c.getComponentName())
                                .toString());
                logger.log(FINER,
                        new StringBuffer()
                                .append("key=")
                                .append(key)
                                .append(" underlying key=")
                                .append(c.getKey())
                                .toString());
                if (key.equals(c.getKey())
                        &&
                        componentName.equals(c.getComponentName())) {
                    retval = true;
                }
            }
        }
        finally{
            cacheLock.unlock();
        }
        return retval;
    }

    /*
     * adds all entries from a collection to the cache. This is used
     * to sync states with the group when this instance is joining an existing
     * group.
     * @param map - containing a GMSCacheable as key and an Object as value.
     */
    void addAllToLocalCache(final Map<GMSCacheable, Object> map) {
        cacheLock.lock();
        try{
            if (map.size() > 0) {
                cache.putAll(map);
            }
            firstSyncDone = true;
        }
        finally{
            cacheLock.unlock();
        }
        logger.log(FINER, "done adding all to Distributed State Cache");
    }

    void addAllToRemoteCache()
            throws GMSException {
        ConcurrentHashMap<GMSCacheable, Object> temp;
        cacheLock.lock();
        try{
            temp = new ConcurrentHashMap<GMSCacheable, Object>(cache);
        }finally{
            cacheLock.unlock();
        }
        final DSCMessage msg = new DSCMessage(temp,
                DSCMessage.OPERATION.ADDALLREMOTE.toString(),
                false);
        sendMessage(null, msg);

    }

    /**
     * removes all entries pertaining to a particular component and member
     * token id, except those marked by special keys.
     * <p>A null component name would indicate that all entries pertaining to a
     * member be removed except for the ones specified by the special keys.
     * <p> Null special keys would indicate that all entries pertaining to a
     * member token id be removed.
     * <p> This operation could also be used to sync states of the group when an
     * instance has failed/left the group.
     *
     * @param componentName component name
     * @param memberTokenId member token id
     * @param exempted      - an List of Serializable keys that are exempted from
     *                      being removed from the cache as part of this operation.
     */
    void removeAllFromCache(final String componentName,
                            final String memberTokenId,
                            final List<Serializable> exempted) {
        //TODO: Implement this for clean bookkeeping
    }

    Hashtable<GMSCacheable, Object> getAllEntries() {
        final Hashtable<GMSCacheable, Object> temp =
                new Hashtable<GMSCacheable, Object>();
        cacheLock.lock();
        try{
            for (GMSCacheable key : cache.keySet()) {
                temp.put(key, cache.get(key));
            }
        }
        finally{
            cacheLock.unlock();
        }
        return temp;
    }

    void syncCache(final String memberToken,
                   final boolean isCoordinator) throws GMSException {
        final ConcurrentHashMap<GMSCacheable, Object> temp;
        cacheLock.lock();
        try{
            temp = new ConcurrentHashMap<GMSCacheable, Object>(cache);
        }
        finally{
            cacheLock.unlock();
        }

        final DSCMessage msg = new DSCMessage(temp,
                DSCMessage.OPERATION.ADDALLLOCAL.toString(),
                isCoordinator);
        if(!memberToken.equals(ctx.getServerIdentityToken())){
            logger.log(Level.FINER, "Sending sync message from DistributedStateCache " +

                    "to member "+memberToken);
            sendMessage(memberToken, msg);
        }
        if (isCoordinator) {
            firstSyncDone = true;
        }
    }

    private GMSCacheable getTrueKey(GMSCacheable cKey) {
        final Set<GMSCacheable> keys;
        cacheLock.lock();
        try{
            keys = cache.keySet();
            for (GMSCacheable comp : keys) {
                if (comp.equals(cKey)) {
                    cKey = comp;
                    break;
                }
            }
        }
        finally{
            cacheLock.unlock();
        }
        return cKey;
    }

    private static GMSCacheable createCompositeKey(
            final String componentName, final String memberTokenId,
            final Object key) {
        return new GMSCacheable(componentName, memberTokenId, key);
    }

    private void sendMessage(final String member,
                                          final DSCMessage msg)
            throws GMSException {

        getGMSContext().getGroupCommunicationProvider().sendMessage(member,
                msg, true);
    }

    public boolean isFirstSyncDone() {
        return firstSyncDone;
    }

    /**
     * Empties the DistributedStateCache. This is typically called in a group
     * shutdown context so that the group's stale data is not retained for any
     * later lives of the group.
     */
    public void removeAll() {
        cacheLock.lock();
        try{
            cache.clear();
        }finally{
            cacheLock.unlock();
        }
    }

    public void removeAllForMember(final String memberToken) {
        final Set<GMSCacheable> keys = getFromCache(memberToken).keySet();
        for (final GMSCacheable key : keys) {
            removeFromLocalCache(key);
        }
    }

/*    public byte[] getStateFromRemoteMember (final String componentName,
                                            final String remoteMemberToken,
                                            final Serializable key ) {

        final IpAddress dest;
        final ViewWindow viewWindow = getGMSContext().getViewWindow();
        dest =  ( IpAddress ) viewWindow.getMemberTokenAddress( remoteMemberToken );
        byte[] state = BYTE;
        try {
            final MethodCall method =
                    new MethodCall("getKeyState",
                                   new Object[]{componentName,  key },
                                   new Class[]{String.class, Serializable.class});
            state = ( byte[] ) getGMSContext().getRPCDispatcher()
                                .callRemoteMethod(dest,
                                      method,
                                      GroupRequest.GET_FIRST, 0);
        }
        catch ( TimeoutException e ) {
            logger.log(Level.WARNING, e.getLocalizedMessage());
        }
        catch ( SuspectedException e ) {
            logger.log(Level.WARNING, e.getLocalizedMessage());
        }
        return state;
    }
*/
}
