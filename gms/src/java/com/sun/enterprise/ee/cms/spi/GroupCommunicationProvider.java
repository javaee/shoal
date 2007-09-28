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
import com.sun.enterprise.jxtamgmt.ClusterManager;

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
     * @param memberIdentityToken identity of member.
     */
    MemberStates getMemberState(String memberIdentityToken);

    /**
     * Returns the Group Leader as defined by the underlying Group Communication
     * Provider. 
     * @return   String
     */
    String getGroupLeader();  
    
    ClusterManager getClusterManager();
    
    /**
     * Let's an instance become a Master by force
     * Used by the DAS to become a master forcefully (if not already)
     * when the cluster is shutting down
     **/

    void assumeGroupLeadership(String groupName);
}
