/*
 * Copyright 2004-2005 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.enterprise.ee.cms.core;

import java.util.List;

/**
 * Provides a handle to the interact with the membership group. 
 * While currently limited to sending messages to the group, 
 * in the future the handle could potentially encompass
 * sending of messages to individual and/or a sub-collection of members.
 *
 * @author Shreedhar Ganapathy
 * Date: Jan 12, 2004
 * @version $Revision$
 */
public interface GroupHandle {
    /**
     * Sends a message to all members of the Group.
     * Expects a target component name and a byte array as parameter
     * carrying the payload. Specifying a null component name would
     * result in the message being delivered to all registered
     * components in the target member instance. 
     * @param targetComponentName
     * @param message
     */
    void sendMessage(String targetComponentName, byte[] message) throws GMSException;

    /**
     * Sends a message to a single member of the group
     * Expects a targetServerToken representing the recipient member's
     * id, the target component name in the target recipient member,
     * and a byte array as parameter carrying the payload. Specifying
     * a null component name would result in the message being
     * delivered to all registered components in the target member
     * instance. 
     * @param targetServerToken
     * @param targetComponentName
     * @param message
     */
     void sendMessage(String targetServerToken, String targetComponentName,
                      byte[] message) throws GMSException;

    /**
     * returns a DistributedStateCache object that provides the ability to
     * set and retrieve CachedStates.
     * @see DistributedStateCache
     * @return DistributedStateCache
     */
     DistributedStateCache getDistributedStateCache();

    /**
     * returns a List of strings containing the current core members
     * in the group
     * @return List
     */
    List<String> getCurrentCoreMembers(); 

    /**
     * returns a List of strings containing the current group membership including
     * spectator members.
     * @return List
     */
     List<String> getAllCurrentMembers();

    /**
     * returns a List of strings containing the current core members
     * in the group. Each entry contains a "::" delimiter after the
     * member token id. After the delimited is a string representation of the
     * long value that stands for that member's startup timestamp.
     * @return List
     */
    List<String> getCurrentCoreMembersWithStartTimes();

    /**
     * returns a List of strings containing the current group membership including
     * spectator members. Each entry contains a "::" delimiter after the
     * member token id. After the delimited is a string representation of the
     * long value that stands for that member's startup timestamp.
     * @return List
     */
     List<String> getAllCurrentMembersWithStartTimes();

    //FAILURE FENCING RELATED API
    /**
     * Enables the caller to raise a logical fence on a specified target member
     * token's component.
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
     * <p>Direct calls to this method is meant only for self-recovering clients.
     * For clients that perform recovery as a surrogate for a failed instance,
     * the FailureRecoverySignal's acquire() method should be called. That
     * method has the effect of raising the fence and performing any other state
     * management operation that may be added in future.
     * @param componentName
     * @param failedMemberToken
     * @throws GMSException
     *
     */
    void raiseFence (String componentName,
                     String failedMemberToken ) throws GMSException;

    /**
     * Enables the caller to lower a logical fence that was earlier raised on
     * a target member component. This is typically done when the operation
     * being performed on the target member component has now completed.
     *
     * <p>Direct calls to this method is meant only for self-recovering clients.
     * For clients that perform recovery as a surrogate for a failed instance,
     * the FailureRecoverySignal's release() method should be called. That
     * method has the effect of lowering the fence and performing any other
     * state management operation that may be added in future.
     * 
     * @param componentName
     * @param failedMemberToken
     * @throws GMSException
     */
    void lowerFence (String componentName,
                     String failedMemberToken ) throws GMSException;

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
    boolean isFenced(String componentName, String memberToken);

    /**
     * Checks if a member is alive
     * @param memberToken
     * @return boolean
     */
    boolean isMemberAlive(String memberToken);

    /**
     * Return the leader of the group
     * @return String representing the member identity token of the group leader
     */
    String getGroupLeader();

    /**
     * This is a check to find out if this peer is a group leader.
     * @return true if this peer is the group leader
     */
    boolean isGroupLeader();
}

