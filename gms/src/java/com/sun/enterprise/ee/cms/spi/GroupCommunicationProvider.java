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

package com.sun.enterprise.ee.cms.spi;

 import com.sun.enterprise.ee.cms.core.GMSException;
 import com.sun.enterprise.ee.cms.core.MemberNotInViewException;
 import com.sun.enterprise.ee.cms.core.GMSConstants;

 import java.io.Serializable;
 import java.util.List;
 import java.util.Map;

/**
 * Provides a plugging interface for integrating group communication
 * providers(GCP). Acts as a bridge between GCP packages and GMS. Implementation
 * of this API allows GMS objects to delegate interaction with the underlying
 * GCP through commonly executed calls. GCPs should have a notion of grouping
 * members and allow for messaging. GCPs should be capable of notifying group
 * events. GCP should provide interfaces for programmatic configuration of their
 * group communication and membership protocols. 
 *
 * @author Shreedhar Ganapathy
 *         Date: Jun 26, 2006
 * @version $Revision$
 */
public interface GroupCommunicationProvider {
    /**
     * Initializes the Group Communication Service Provider with the requisite
     * values of group identity, member(self) identity, and a Map containing
     * recognized and valid configuration properties that can be set/overriden
     * by the employing application. The valid property keys must be specified
     * in a datastructure that is available to the implementation and to GMS.
     *
     * @param memberName   member name
     * @param groupName    name of group
     * @param identityMap      - additional member identity params specified
     *                         through key-value pairs.
     * @param configProperties - properties that the employing applications
     *                         likes to configure in the underlying GCP.
     */
    void initializeGroupCommunicationProvider (
            String memberName,
            String groupName,
            Map<String, String> identityMap,
            Map configProperties );

    /**
     * Joins the group using semantics specified by the underlying GCP system
     */
    void join ();

    /**
     * Sends an announcement to the group that a cluster wide shutdown is
     * impending
     * @param gmsMessage an object that encapsulates the application's Message  
     */
    void announceClusterShutdown(GMSMessage gmsMessage);

    /**
     * Leaves the group as a result of a planned administrative action to
     * shutdown.
     * @param isClusterShutdown - true if we are leaving as part of a cluster wide shutdown
     */
    void leave (boolean isClusterShutdown);

    /**
     * Sends a message using the underlying group communication
     * providers'(GCP's) APIs. Requires the users' message to be wrapped into a
     * GMSMessage object. 
     *
     * @param targetMemberIdentityToken The member token string that identifies
     *                      the target member to which this message is addressed.
     *                      The implementation is expected to provide a mapping
     *                      the member token to the GCP's addressing semantics.
     *                      If null, the entire group would receive this message.
     * @param message       a Serializable object that wraps the user specified
     *                      message in order to allow remote GMS instances to
     *                      unpack this message appropriately.
     * @param synchronous   setting true here will call the underlying GCP's api
     *                      that corresponds to a synchronous message, if
     *                      available.
     * @throws com.sun.enterprise.ee.cms.core.GMSException wraps the underlying exception 
     */
    void sendMessage (String targetMemberIdentityToken, Serializable message,
                      boolean synchronous )
            throws GMSException, MemberNotInViewException;

    /**
     * Sends a message to the entire group using the underlying group communication
     * provider's APIs. The Serializable object here is a GMSMessage Object.
     * @param message a Serializable object that wraps the users specified
     * message in order to allow remote GMS instances to unpack this message
     * appropriately
     * @throws GMSException Underlying exception is wrapped in a GMSException
     */
    void sendMessage (Serializable message) throws GMSException, MemberNotInViewException;

    /**
     * returns a list of members that are currently alive in the group.
     * The list should contain the member identity token that GMS understands as
     * member identities.
     *
     * @return list of current live members
     */
    List<String> getMembers ();

    /**
     * Returns true if this peer is the leader of the group
     * @return boolean true if group leader, false if not.
     */
    boolean isGroupLeader();


    /**
     * Returns the state of the member.
     * The parameters <code>threshold</code> and <code>timeout</code> enable the caller to tune this
     * lookup between accuracy and time it will take to complete the call.  It is lowest cost to just return
     * the local computed concept for a member's state. <code>threshold</code> parameter controls this.
     * If the local state is stale, then the <code>timeout</code> parameter enables one to control how
     * long they are willing to wait for more accurate state information from the member itself.
     * @param member
     * @param threshold allows caller to specify how up-to-date the member state information has to be.
     *  The  larger this value, the better chance that this method just returns the local concept of this member's state.
     * The smaller this value, the better chance that the local state is not fresh enough and the method will find out directly
     * from the instance what its current state is.
     * @param timeout is the time for which the caller instance should wait to get the state from the concerned member
     * via a network call.
     * if timeout and threshold are both 0, then the default values are used
     * if threshold is 0, then a network call is made to get the state of the member
     * if timeout is 0, then the caller instance checks for the state of the member stored with it within the
     * given threshold
     * @return the state of the member
     * Returns UNKNOWN when the local state for the member is considered stale (determined by threshold value)
     * and the network invocation to get the member state times out before getting a reply from the member of what its state is.
     */
 
    MemberStates getMemberState(String member, long threshold, long timeout);

    /**
     * Returns the member state as defined in the Enum MemberStates
     * @return MemberStates
     * @param memberIdentityToken identity of member.
     */
    MemberStates getMemberState(String memberIdentityToken);

    /**
     * Returns the Group Leader as defined by the underlying Group Communication
     * Provider. 
     * @return   String
     */
    String getGroupLeader();

    /**
     * <p>Provides for this instance to become a group leader explicitly.
     * Typically this can be employed by an administrative member to become
     * a group leader prior to shutting down a group of members simultaneously.</p>
     *
     * <p>For underlying Group Communication Providers who don't support the feature
     * of a explicit leader role assumption, the implementation of this method
     * would be a no-op.</p>
     *     
     **/
    void assumeGroupLeadership();

    /**
     * Can be used especially to inform the HealthMonitoring service
     * that the group is shutting down.
     */
    void setGroupStoppingState();

    /**
     *  This API is provided for the parent application to report to the group
     * its joined and ready state to begin processing its operations.
     * The group member that this parent application represents is now ready to
     * process its operations at the time of this announcement to the group.
     * GMS clients in all other group members that are interested in knowing
     * when another member is ready to start processing operations, can subscribe
     * to the event JoinedAndReadyEvent and be notified of this JoinedAndReadyNotificationSignal.
     */

    void reportJoinedAndReadyState();

    /**
     * Allow for enhanced GMS failure detection by external control entities (one example is NodeAgent of Glassfish Application Server.)
     * Only a GMS MemberType of WATCHDOG is allowed to broadcast to all members of a group that this <code>serverToken</code> has failed.
     * @param serverToken     failed member
     * @throws GMSException   if called by a member that is not a WATCHDOG member or if serverToken is not currently running in group.
     */
    void announceWatchdogObservedFailure(String serverToken) throws GMSException;

    /**
     * Invoked indirectly by a controlling parent application that has a static preconfiguration
     * of all members of the group to announce that the parent application is "initiating" and then
     * that it has "completed" startup of all preconfigured members of this group.
     *
     * <P>Group members in parameter <code>members</code> is interpreted differently based on startupState.
     * All preconfigured members of group are passed in <code>members</code> when
     * {@link INITIATED} or {@link COMPLETED_SUCCESS}.
     * When startupState is  {@link COMPLETED_FAILED}, <code>members</code> is a list of the
     * members that failed to start.
     *
     * @param groupName
     * @param startupState  demarcate initiation of groupStartup and completion of group startup
     * @param memberTokens  list of memberTokens.
     */
    void announceGroupStartup(String groupName,
                              GMSConstants.groupStartupState startupState,
                              List<String> memberTokens);

    boolean isDiscoveryInProgress(); 
}
