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

import com.sun.enterprise.ee.cms.core.*;
import com.sun.enterprise.ee.cms.impl.common.GMSContextFactory;
import com.sun.enterprise.ee.cms.impl.common.ViewWindow;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import com.sun.enterprise.ee.cms.spi.GMSMessage;
import com.sun.enterprise.ee.cms.spi.GroupCommunicationProvider;
import com.sun.enterprise.ee.cms.spi.MemberStates;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of GroupHandle interface.
 *
 * @author Shreedhar Ganapathy
 *         Date: Jan 12, 2004
 * @version $Revision$
 */
public final class GroupHandleImpl implements GroupHandle {
    private String groupName;
    private String serverToken;
    private GMSContext ctx;
    private static final Logger logger =
            GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    private static final String REC_PROGRESS_STATE =
            GroupManagementService
                    .RECOVERY_STATE
                    .RECOVERY_IN_PROGRESS.toString();
    private static final String REC_APPOINTED_STATE =
            GroupManagementService
                    .RECOVERY_STATE
                    .RECOVERY_SERVER_APPOINTED.toString();

    private static final int SYNC_WAIT = 2000;
    private List<String> selfRecoveryList;

    public GroupHandleImpl(
            final String groupName,
            final String serverToken) {
        this.groupName = groupName;
        this.serverToken = serverToken;
        this.selfRecoveryList = new ArrayList<String>();
    }

    private GMSContext getGMSContext() {
        if (ctx == null) {
            ctx = (GMSContext) GMSContextFactory.getGMSContext(groupName);
        }
        return ctx;
    }

    /**
     * Sends a message to all members of the Group. Expects a byte array as
     * parameter carrying the payload.
     *
     * @param componentName Destination component in remote members.
     * @param message       Payload in byte array to be delivered to the destination.
     */
    public void sendMessage(final String componentName, final byte[] message) throws GMSException {
        final GMSMessage gMsg = new GMSMessage(componentName, message, groupName, getGMSContext().getStartTime());
        try {
            getGMSContext().getGroupCommunicationProvider().sendMessage(null, gMsg, true);
        } catch (MemberNotInViewException e) {
            logger.warning("GroupHandleImpl.sendMessage : Could not send message : " + e.getMessage());
        }
    }

    /**
     * Sends a message to a single member of the group
     * Expects a targetServerToken representing the recipient member's
     * id, the target component name in the target recipient member,
     * and a byte array as parameter carrying the payload. Specifying
     * a null component name would result in the message being
     * delivered to all registered components in the target member
     * instance.
     *
     * @param targetServerToken   destination member's identification
     * @param targetComponentName destination member's target component
     * @param message             Payload in byte array to be delivered to the destination.
     */
    public void sendMessage(final String targetServerToken,
                            final String targetComponentName,
                            final byte[] message) throws GMSException {
        final GMSMessage gMsg = new GMSMessage(targetComponentName, message,
                groupName,
                getGMSContext().getStartTime());
        try {
            getGMSContext().getGroupCommunicationProvider()
                .sendMessage(targetServerToken, gMsg, false);
        } catch (MemberNotInViewException e) {
            logger.warning("GroupHandleImpl.sendMessage : Could not send message : " + e.getMessage());
        }

    }

    public void sendMessage(List<String> targetServerTokens, String targetComponentName, byte[] message) throws GMSException {
        final GMSMessage gMsg = new GMSMessage(targetComponentName, message, groupName, getGMSContext().getStartTime());
        if(targetServerTokens.isEmpty()){
            try {
                getGMSContext().getGroupCommunicationProvider().sendMessage(null,gMsg, true  );
            } catch (MemberNotInViewException e) {
                logger.warning("GroupHandleImpl.sendMessage : Could not send message : " + e.getMessage());
            }
        }
        else {
            for(String token : targetServerTokens){
                try {
                    getGMSContext().getGroupCommunicationProvider().sendMessage(token, gMsg, false  );
                } catch (MemberNotInViewException e) {
                    logger.warning("GroupHandleImpl.sendMessage : Could not send message : " + e.getMessage());
                }
            }
        }
    }

    /**
     * returns a DistributedStateCache object that provides the ability to
     * set and retrieve CachedStates.
     *
     * @return DistributedStateCache
     * @see com.sun.enterprise.ee.cms.core.DistributedStateCache
     */
    public DistributedStateCache getDistributedStateCache() {
        return getGMSContext().getDistributedStateCache();
    }

    /**
     * returns a List containing the current core members
     * in the group.
     *
     * @return List a List of member token ids pertaining to core members
     */
    public List<String> getCurrentCoreMembers() {
        final ViewWindow viewWindow = getGMSContext().getViewWindow();
        return viewWindow.getCurrentCoreMembers();
    }

    /**
     * returns a List containing the current group membership including
     * spectator members.
     *
     * @return List a List of member token ids pertaining to all members
     */
    public List<String> getAllCurrentMembers() {
        final ViewWindow viewWindow = getGMSContext().getViewWindow();
        return viewWindow.getAllCurrentMembers();
    }

    public List<String> getCurrentCoreMembersWithStartTimes() {
        final ViewWindow viewWindow = getGMSContext().getViewWindow();
        return viewWindow.getCurrentCoreMembersWithStartTimes();
    }

    public List<String> getAllCurrentMembersWithStartTimes() {
        final ViewWindow viewWindow = getGMSContext().getViewWindow();
        return viewWindow.getAllCurrentMembersWithStartTimes();
    }

    /**
     * Enables the caller to raise a logical fence on a specified target member
     * token's component.   This API is directly called only when a component
     * is raising a fence itself and not as part of acquiring a signal. If this
     * is part of acquiring a signal, then the call should be to
     * signal.acquire() which encompasses raising a fence and potentially
     * other state updates.
     * <p>Failure Fencing is a group-wide protocol that, on one hand, requires
     * members to update a shared/distributed datastructure if any of their
     * components need to perform operations on another members' corresponding
     * component. On the other hand, the group-wide protocol requires members
     * to observe "Netiquette" during their startup so as to check if any of
     * their components are being operated upon by other group members.
     * Typically this check is performed by the respective components
     * themselves. See the isFenced() method below for this check.
     * When the operation is completed by the remote member component, it
     * removes the entry from the shared datastructure. See the lowerFence()
     * method below.
     * <p>Raising the fence, places an entry into a distributed datastructure
     * that is accessed by other members during their startup
     *
     * @param componentName
     * @param failedMemberToken
     * @throws GMSException
     */
    public void raiseFence(final String componentName,
                           final String failedMemberToken)
            throws GMSException {
        if (!isFenced(componentName, failedMemberToken)) {
            final DistributedStateCache dsc = getGMSContext().
                    getDistributedStateCache();
            dsc.addToCache(componentName,
                    getGMSContext().getServerIdentityToken(),
                    failedMemberToken,
                    setStateAndTime());
            if (fenceForSelfRecovery(failedMemberToken)) {
                saveRaisedFenceState(componentName, failedMemberToken);
            }

            logger.log(Level.FINE, "Fence raised for member "
                    + failedMemberToken + " by member "
                    + getGMSContext().getServerIdentityToken()
                    + " component " + componentName);
        }
    }

    private void saveRaisedFenceState(
            final String componentName, final String failedMemberToken) {
        selfRecoveryList.add(componentName + failedMemberToken);
    }

    private boolean fenceForSelfRecovery(final String failedMemberToken) {
        return failedMemberToken.equals(getGMSContext()
                .getServerIdentityToken());
    }

    /**
     * Enables the caller to lower a logical fence that was earlier raised on
     * a target member component. This is typically done when the operation
     * being performed on the target member component has now completed.
     * This api is directly called only by a component that is lowering a fence
     * directly and not as part of releasing a signal. If the operation is to
     * release a signal, then the appropriate call is to signal.release() which
     * encompasses lowering the fence and other cleanups.
     *
     * @param componentName target member component
     * @param failedMemberToken
     * @throws GMSException
     */
    public void lowerFence(final String componentName,
                           final String failedMemberToken)
            throws GMSException {   //If there is a fence for delegated recovery  or self recovery
        if (isFenced(componentName, failedMemberToken)
                ||
                selfRecoveryList.contains(componentName + failedMemberToken)) {
            final DistributedStateCache dsc = getGMSContext()
                    .getDistributedStateCache();
            dsc.removeFromCache(componentName,
                    getGMSContext().getServerIdentityToken(),
                    failedMemberToken);
            logger.log(Level.FINE, "Fence lowered for member "
                    + failedMemberToken + " by member "
                    + getGMSContext().getServerIdentityToken()
                    + " component " + componentName);
            //this removes any recovery appointments that were made but were
            // not exercised by the client thus leaving an orphan entry in
            // cache.
            removeRecoveryAppointments(dsc.getFromCache(failedMemberToken),
                    failedMemberToken);
            selfRecoveryList.remove(componentName + failedMemberToken);
        }
    }

    private void removeRecoveryAppointments(
            final Map<GMSCacheable, Object> fromCache,
            final String failedMemberToken) throws GMSException {
        final DistributedStateCache dsc = getGMSContext()
                .getDistributedStateCache();

        for (final GMSCacheable cKey : fromCache.keySet()) {
            if (cKey.getKey().equals(failedMemberToken)
                    &&
                    fromCache.get(cKey).toString().startsWith(REC_APPOINTED_STATE)) {
                dsc.removeFromCache(cKey.getComponentName(),
                        cKey.getMemberTokenId(),
                        (Serializable) cKey.getKey());
            }
        }
    }

    /**
     * Provides the status of a member component's fence, if any.
     * <p>This check is <strong>mandatorily</strong> done at the time a member
     * component is in the process of starting(note that at this point we assume
     * that this member failed in its previous lifetime).
     * <p>The boolean value returned would indicate if this member component is
     * being recovered by any other member. The criteria  for returning a
     * boolean "true" is that this componentName-memberToken combo is present
     * as a value for any key in the GMS DistributedStateCache. If a
     * true is returned, for instance, this could mean that the client component
     * should continue its startup without attempting to perform its own
     * recovery operations.</p>
     * <p>The criteria for returning a boolean "false" is that the
     * componentId-memberTokenId combo is not present in the list of values in
     * the DistributedStateCache.If a boolean "false" is returned, this could
     * mean that the client component can continue with its lifecycle startup
     * per its normal startup policies.
     *
     * @param componentName
     * @param memberToken
     * @return boolean
     */
    public boolean isFenced(final String componentName,
                            final String memberToken) {
        boolean retval = false;
        final DistributedStateCache dsc = getDistributedStateCache();
        final Map<GMSCacheable, Object> entries;
        final List<String> members = getAllCurrentMembers();
        int count = 0;
        while (members.size() > 1 && !dsc.isFirstSyncDone()) {
            logger.log(Level.FINE, "Waiting for DSC first Sync");
            try {
                Thread.sleep(SYNC_WAIT);
                count++;
                //this is 
                if (count > 4) {
                    forceDSCSync((DistributedStateCacheImpl) dsc);
                }
            }
            catch (InterruptedException e) {
                logger.log(Level.WARNING, e.getLocalizedMessage());
            }
        }
        entries = dsc.getFromCache(memberToken);
        for (GMSCacheable c : entries.keySet()) {
            if (componentName.equals(c.getComponentName())) {   //if this member is being recovered by someone
                if (memberToken.equals(c.getKey())) {   //if this is an old record of a self recovery then ignore
                    if (!memberToken.equals(c.getMemberTokenId())) {
                        if (((String) entries.get(c))
                                .startsWith(REC_PROGRESS_STATE)) {
                            logger.log(Level.FINER,
                                    c.toString() + " value:" + entries.get(c));
                            logger.log(Level.FINER,
                                    "Returning true for isFenced query");
                            retval = true;
                            break;
                        }
                    }
                }
            }
        }
        return retval;
    }

    private void forceDSCSync(final DistributedStateCacheImpl dsc) {
        try {
            final String token = getGMSContext().getGroupCommunicationProvider()
                    .getGroupLeader();

            logger.log(Level.FINE,
                    "Force Syncing DistributedStateCache with " + token);
            dsc.syncCache(token, true);
        }
        catch (GMSException e) {
            logger.log(Level.WARNING,
                    "Force Syncing of DistributedStateCache failed:"
                            + e.getLocalizedMessage());
        }

    }

    public boolean isMemberAlive(final String memberToken) {
        return memberToken.equals(serverToken) || getAllCurrentMembers().contains(memberToken);
    }

    public String getGroupLeader() {
        return getGMSContext().getGroupCommunicationProvider().getGroupLeader();
    }

    public boolean isGroupLeader() {
        return getGMSContext().getGroupCommunicationProvider().isGroupLeader();
    }

    public List<String> getSuspectList() {
        return getGMSContext().getSuspectList();
    }

    public String toString() {
        return getClass().getName();
    }

    private static String setStateAndTime() {
        return GroupManagementService
                .RECOVERY_STATE
                .RECOVERY_IN_PROGRESS.toString() + "|" +
                System.currentTimeMillis();

    }

    public List<String> getCurrentAliveOrReadyMembers() {
        List<String> members = getCurrentCoreMembers();
        List<String> currentAliveOrReadyMembers = new ArrayList<String>();
        GroupCommunicationProvider gcp = getGMSContext().getGroupCommunicationProvider();

        for (String member : members) {
            MemberStates state = gcp.getMemberState(member);
            if (state == MemberStates.ALIVE || state == MemberStates.READY) {
                currentAliveOrReadyMembers.add(member);
            }
        }
        return currentAliveOrReadyMembers;
    }
}
