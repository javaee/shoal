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
package com.sun.enterprise.ee.cms.spi;

import com.sun.enterprise.ee.cms.core.GMSException;

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
     * @param memberName
     * @param groupName
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
     * Sends an annouuncement to the group that a cluster wide shutdown is
     * impending
     */
    void announceClusterShutdown();

    /**
     * Leaves the group as a result of a planned administrative action to
     * shutdown.
     */
    void leave ();

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
     * @throws com.sun.enterprise.ee.cms.core.GMSException
     */
    void sendMessage (String targetMemberIdentityToken, Serializable message,
                      boolean synchronous )
            throws GMSException;

    /**
     * Sends a message to the entire group using the underlying group communication
     * provider's APIs. The Serializable object here is a GMSMessage Object.
     * @param message a Serializable object that wraps the users specified
     * message in order to allow remote GMS instances to unpack this message
     * appropriately
     * @throws GMSException Underlying exception is wrapped in a GMSException
     */
    void sendMessage (Serializable message) throws GMSException;

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
     * Returns the member state as defined in the Enum MemberStates
     * @return MemberStates
     */
    MemberStates getMemberState(String memberIdentityToken);

    /**
     * Returns the Group Leader as defined by the underlying Group Communication
     * Provider. 
     * @return
     */
    String getGroupLeader();
}
