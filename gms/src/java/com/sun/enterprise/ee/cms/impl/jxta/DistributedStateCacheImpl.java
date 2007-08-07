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

package com.sun.enterprise.ee.cms.impl.jxta;

import com.sun.enterprise.ee.cms.core.DistributedStateCache;
import com.sun.enterprise.ee.cms.core.GMSCacheable;
import com.sun.enterprise.ee.cms.core.GMSException;
import com.sun.enterprise.ee.cms.impl.common.DSCMessage;
import com.sun.enterprise.ee.cms.impl.common.GMSContextFactory;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import com.sun.enterprise.ee.cms.spi.MemberStates;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Logger logger =
            GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    private boolean firstSyncDone = false;
    private static final Map<String, DistributedStateCacheImpl> ctxCache =
            new HashMap<String, DistributedStateCacheImpl>();
    private final String groupName;

    //private constructor for single instantiation
    private DistributedStateCacheImpl(final String groupName) {
        this.groupName = groupName;
    }

    //return the only instance we want to return
    static DistributedStateCache getInstance(final String groupName) {
        DistributedStateCacheImpl instance;
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
        logger.log(Level.FINEST, "Adding cKey="+cKey.toString()+" state="+state.toString());
        cache.put(cKey, state);
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
        copy = new ConcurrentHashMap<GMSCacheable, Object>(cache);
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
        cache.remove(cKey);
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
            return cache.get(cKey);
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
        logger.finer("componentName = " + componentName + " memberToken = " + memberToken);
        if (componentName == null || memberToken == null) {
            return retval;
        }
        for (GMSCacheable c : cache.keySet()) {
            logger.finer("c.getComponentName() = " + c.getComponentName() +
                    "c.getMemberTokenId() = " + c.getMemberTokenId());
            if (componentName.equals(c.getComponentName())) {
                if (memberToken.equals(c.getMemberTokenId())) {
                    retval.put((Serializable) c.getKey(),
                            (Serializable) cache.get(c));
                }
            }
        }

        if(!retval.isEmpty()){
            return retval;
        }
        else{
            if(!memberToken.equals(getGMSContext().getServerIdentityToken())){               
                MemberStates state = getGMSContext().getGroupCommunicationProvider().getMemberState(memberToken);
                if(state.equals(MemberStates.ALIVE)) {
                    logger.finer("state is alive");
                    ConcurrentHashMap<GMSCacheable, Object>
                    temp = new ConcurrentHashMap<GMSCacheable, Object>(cache);
                    DSCMessage msg = new DSCMessage(temp,
                            DSCMessage.OPERATION.ADDALLLOCAL.toString(), true);
                    try{
                        sendMessage(memberToken, msg);
                        Thread.sleep(3000);
                        logger.finer("going to putAll into retVal");
                        retval.putAll( getFromCacheForPattern(componentName, memberToken));
                    } catch (GMSException e) {
                        logger.log(Level.WARNING, "GMSException during DistributedStateCache Sync...."+e) ;
                    } catch (InterruptedException e) {
                        logger.finer("InterruptedException occurred...ignoring");
                        //ignore
                    }
                } else logger.finer("state is not ALIVE");
            } else logger.finer("memberToken = " + memberToken + 
                    " getGMSContext().getServerIdentityToken() = " + getGMSContext().getServerIdentityToken());
        }  
        if(retval.isEmpty()){
           logger.info("retVal is empty"); 
        }
        
        return retval;
    }

    public Map<GMSCacheable, Object> getFromCache(final Object key) {
        final Map<GMSCacheable, Object> retval =
                new Hashtable<GMSCacheable, Object>();
        for (GMSCacheable c : cache.keySet()) {
            if (key.equals(c.getComponentName())
                    ||
                    key.equals(c.getMemberTokenId())
                    ||
                    key.equals(c.getKey())) {
                retval.put(c, cache.get(c));
            }
        }
        return retval;
    }

    public boolean contains(final Object key) {
        boolean retval = false;
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
        return retval;
    }

    public boolean contains(final String componentName, final Object key) {
        boolean retval = false;
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
        return retval;
    }

    /*
     * adds all entries from a collection to the cache. This is used
     * to sync states with the group when this instance is joining an existing
     * group.
     * @param map - containing a GMSCacheable as key and an Object as value.
     */
    void addAllToLocalCache(final Map<GMSCacheable, Object> map) {
        if (map!= null && map.size() > 0) {
            cache.putAll(map);
        }
        firstSyncDone = true;
        logger.log(FINER, "done adding all to Distributed State Cache");
    }

    void addAllToRemoteCache()
            throws GMSException {
        ConcurrentHashMap<GMSCacheable, Object> temp;
        temp = new ConcurrentHashMap<GMSCacheable, Object>(cache);
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
        for (GMSCacheable key : cache.keySet()) {
            temp.put(key, cache.get(key));
        }
        return temp;
    }

    void syncCache(final String memberToken,
                   final boolean isCoordinator) throws GMSException {
        final ConcurrentHashMap<GMSCacheable, Object> temp;
        temp = new ConcurrentHashMap<GMSCacheable, Object>(cache);

        final DSCMessage msg = new DSCMessage(temp,
                DSCMessage.OPERATION.ADDALLLOCAL.toString(),
                isCoordinator);
        if(!memberToken.equals(getGMSContext().getServerIdentityToken())){
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
        keys = cache.keySet();
        
        for (GMSCacheable comp : keys) {
            if (comp.equals(cKey)) {
                cKey = comp;
                break;
            }
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
        cache.clear();
    }

    public void removeAllForMember(final String memberToken) {
        final Set<GMSCacheable> keys = getFromCache(memberToken).keySet();
        for (final GMSCacheable key : keys) {
            removeFromLocalCache(key);
        }
    }
}
