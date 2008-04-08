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

package com.sun.enterprise.ee.cms.core;

import java.io.Serializable;
import java.util.Map;

/**
 * Provides API for joining, and leaving the group and to register Action Factories of
 * specific types for specific Group Event Signals.
 * @author Shreedhar Ganapathy
 * Date: June 10, 2006
 * @version $Revision$
 */
public interface GroupManagementService {
    /**
     * Members joining the group should be one of the following
     * types. Core members are ones whose failure is a material event
     * to the group, Spectators are those whose failure is not a
     * material event to other group members. 
     */
    public static enum MemberType {CORE, SPECTATOR}

    /**
     * These are possible recovery states used by GMS's recovery selection
     * and failure fencing functions
     */
    public static enum RECOVERY_STATE { RECOVERY_SERVER_APPOINTED, RECOVERY_IN_PROGRESS }

  
    /**
     * Registers a FailureNotificationActionFactory instance.
     * @param failureNotificationActionFactory  Implementation of this interface produces
     * a FailureNotificationAction instance which consumes the failure notification Signal
     */
    void addActionFactory(
          FailureNotificationActionFactory failureNotificationActionFactory);

    /**
     * Registers a FailureRecoveryActionFactory instance.
     * @param componentName The name of the parent application's component
     * that should be notified of being selected for performing recovery
     * operations. One or more components in the parent application may
     * want to be notified of such selection for their respective recovery
     * operations
     * @param failureRecoveryActionFactory Implementation of this interface produces
     * a FailureRecoveryAction instance which consumes the failure recovery selection
     * notification Signal
     */
    void addActionFactory(String componentName,
                     FailureRecoveryActionFactory failureRecoveryActionFactory);

    /**
     * Registers a JoinNotificationActionFactory instance.
     * @param joinNotificationActionFactory  Implementation of this interface produces
     * a JoinNotificationAction instance which consumes the member join notification
     * signal. 
     */
    void addActionFactory(
            JoinNotificationActionFactory joinNotificationActionFactory);

    /**
     * Registers a JoinedAndReadyNotificationActionFactory instance.
     * @param joinedAndReadyNotificationActionFactory
     * Implementation of this interface produces
     * a JoinedAndReadyNotificationAction instance which consumes the member
     * joined and ready notification signal. 
     */

    void addActionFactory(
            JoinedAndReadyNotificationActionFactory joinedAndReadyNotificationActionFactory);

    /**
     * Registers a PlannedShutdownActionFactory instance.
     * @param plannedShutdownActionFactory   Implementation of this interface produces
     * a PlannedShutdownAction instance which consumes the planned shutdown notification
     * Signal
     */
    void addActionFactory(
            PlannedShutdownActionFactory plannedShutdownActionFactory);

     /**
     * Registers a MessageActionFactory instance for the specified component
     * name.
     * @param messageActionFactory Implementation of this interface produces a
     * MessageAction instance that consumes a MessageSignal.
     * @param componentName Name of the component that would like to consume
     * Messages. One or more components in the parent application would want to
     * be notified when messages arrive addressed to them. This registration
     * allows GMS to deliver messages to specific components.  
     *
     */
     void addActionFactory(MessageActionFactory messageActionFactory,
                           String componentName);

    /**
     * Registers a FailureSuspectedActionFactory Instance.
     * @param failureSuspectedActionFactory   Implementation of this interface produces
     * a Failure Suspected Action instance that would consume the FailureSuspectedSignal
     */
    void addActionFactory (
         FailureSuspectedActionFactory failureSuspectedActionFactory );


    /**
     * Removes a FailureNotificationActionFactory instance
     * @param failureNotificationActionFactory
     */
    void removeActionFactory(
            FailureNotificationActionFactory failureNotificationActionFactory);

    /**
     * Removes a FailureRecoveryActionFactory instance
     * @param componentName
     */
    void removeFailureRecoveryActionFactory(String componentName);

    /**
     * Removes a FailureSuspectedActionFactory instance
     * @param failureSuspectedActionFactory
     */
    void removeFailureSuspectedActionFactory(
            FailureSuspectedActionFactory failureSuspectedActionFactory);

    /**
     * Removes a JoinNotificationActionFactory instance
     * @param joinNotificationActionFactory
     */
    void removeActionFactory(
            JoinNotificationActionFactory joinNotificationActionFactory);

    /**
     * Removes a JoinedAndReadyNotificationActionFactory instance
     * @param joinedAndReadyNotificationActionFactory
     */
    void removeActionFactory(
            JoinedAndReadyNotificationActionFactory joinedAndReadyNotificationActionFactory);

    /**
     * Removes a PlannedShutdownActionFactory instance
     * @param plannedShutdownActionFactory
     */
    void removeActionFactory(
            PlannedShutdownActionFactory plannedShutdownActionFactory);

    /**
     * Removes a MessageActionFactory instance belonging to the
     * specified component
     * @param componentName
     */
    void removeMessageActionFactory(String componentName);

    /**
     * Returns an implementation of GroupHandle
     * @return com.sun.enterprise.ee.cms.core.GroupHandle
     */
    GroupHandle getGroupHandle();

    /**
     * Invokes the underlying group communication library's group creation and joining operations.
     * @throws GMSException wraps any underlying exception that causes join to not occur
     */
    void join() throws GMSException;
    
    /**
     * Sends a shutdown command to the GMS indicating that the parent thread
     * is about to be shutdown as part of a planned shutdown operation for the
     * given shutdown type. The given shutdown type is specified by GMSConstants
     *

     */
    void shutdown(GMSConstants.shutdownType shutdownType);
    /**
     * Enables the client to update the Member Details shared datastructure
     * The implementation of this api updates an existing datastructure that is
     * keyed to MEMBER_DETAILS in the DistributedStateCache which stores other
     * shared information. The dedicated Member Details datastructure allows for
     * caching configuration type information about a member in the shared
     * cache so that on occurence of join, failure or shutdown details related
     * to the particular member would be readily available. There is nothing
     * preventing other state information from being stored here but this is
     * intended as a lightweight mechanism in terms of messaging overhead.
     * 
     * @param memberToken - identifier token of this member
     * @param key  - Serializable object that uniquely identifies this cachable
     * state
     * @param value - Serializable object that is to be stored in the shared
     * cache
     * @throws GMSException
     */
    void updateMemberDetails(String memberToken, Serializable key,
                              Serializable value) throws GMSException;

    /**
     * returns the details pertaining to the given member.
     * returns a Map containing key-value pairs constituting data pertaining to
     * the member's details

     * @param memberToken
     * @return Map  <Serializable, Serializable>
     */
    Map<Serializable, Serializable> getMemberDetails(String memberToken);

    /**
     *
     * returns the member details pertaining to the given key. This is particularly
     * useful when the details pertain to all members and not just one member
     * and such details are keyed by a common key.
     * Through this route, details of all members could be obtained.
     * returns a Map containing key-value pairs constituting data pertaining to
     * the member's details for the given key.

     * @param key
     * @return Map  <Serializable, Serializable>
     */
    Map<Serializable, Serializable> getAllMemberDetails(Serializable key);

    /**
     * This method can be used by parent application to notify all group members
     * that the parent application is "initiating" or has "completed" shutdown of
     * this group.
     * @param groupName
     * @param shutdownState from GMSConstants.shutdownState - one of Initiated
     * or Completed
     */
    void announceGroupShutdown ( String groupName,
                                 GMSConstants.shutdownState shutdownState);
    /**
     *  This API is provided for the parent application to report to the group
     * its joined and ready state to begin processing its operations.
     * The group member that this parent application represents is now ready to
     * process its operations at the time of this announcement to the group.
     * GMS clients in all other group members that are interested in knowing
     * when another member is ready to start processing operations, can subscribe
     * to the event JoinedAndReadyEvent and be given this JoinedAndReadyNotificationSignal.
     * Currently this API can only be used by cluster members which are of the type CORE and
     * not the SPECTATOR members. The reason being is that Shoal makes the assumption that
     * only the CORE members will act as servers for serving the client requests and not
     * the SPECTATOR members
     * @param groupName name of the group
     */
     void reportJoinedAndReadyState(String groupName);

       /**
     * <p>This API allows applications to query GMS to see if the group is shutting down.
     * This helps with any pre-shutdown processing that may be required to be done on the
     * application's side.</p>
     * @param groupName
     * @return boolean
     */
     boolean isGroupBeingShutdown(String groupName);

}
