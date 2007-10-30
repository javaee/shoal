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

import com.sun.enterprise.ee.cms.impl.common.GroupManagementServiceImpl;

import java.util.*;

/**
 * <p>This is the entry point to GMS for the parent application that is
 * initiating GMS module and by client components in the parent app that need to
 * interact with GMS for group events or send or receive messages.</p>
 * <p>GMSFactory is the interface for starting GMS module through the
 * startGMSModule() api which returns a GroupManagementService instance, and for
 * retrieving the said GroupManagementService instance by any client components.</p>
 *
 * <p>The GroupManagementService instance provides APIs for registering clients
 * who wish to be notified of Group Events and Message Events, and in addition
 * provides a reference to GroupHandle, and and api for announcing the impending
 * shutdown of this parent process.</p>
 *
 * <p>Example for parent lifecycle module to start GMS:<br>
 * <code>final Runnable gms = GMSFactory.startGMSModule(serverName, groupName,
                                            memberType, properties);<br>
 * final Thread gservice = new Thread(gms, "GMSThread");<br>
   gservice.start();<br>
 * </code></p>
 *
 * <p>Example for parent lifecycle module to shutdown GMS:<br>
 * <code>
 * gms.shutdown(GMSConstants.shutdownType.INSTANCE_SHUTDOWN); <br>
 * or<br>
 * gms.shutdown(GMSConstants.shutdownType.GROUP_SHUTDOWN);<br>
 * </code></p>
 * <p>Registration Example for clients that want to consume group events and
 * message events:<br>
 * <code>
 * GroupManagementService gms = GMSFactory.getGMSModule(groupName);<br>
 * gms.addActionFactory(myfailureNotificationActionFactoryImpl); <br>
 * </code></p>
  * @author Shreedhar Ganapathy
  * @version $Revision$
  */
 
public class GMSFactory {
    private static Hashtable<String, Runnable> groups =
            new Hashtable<String, Runnable>();
    private static Map<String, Boolean> gmsEnabledMap =
            new HashMap<String, Boolean>();
    private static String memberToken;

    private GMSFactory () {
    }

    /**
     * starts and returns the GMS module object as a Runnable that
     * could be started in a separate thread. This method is only used to
     * create an instance of GroupManagementService and sets up the configuration
     * properties in appropriate delegate objects.
     * To actually create a  group or join an existing group, one has to either
     * pass in the GroupManagementService Object in a new Thread and start it or
     * call the GroupManagementService.join() method.
     *
     * The startGMSModule method is expected to be called  by the parent
     * module's lifecycle management code to initiate GMS module. Invocation of
     * this method assumes that GMS is enabled in the parent application's
     * configuration.
     *
     * Calls to GMSFactory.getGMSModule() made before any code calls this method
     * will result in GMSNotEnabledException to be thrown. 
     *
     * @param serverToken  a logical name or identity given the member that
     * is repeatable over lifetimes of the server
     * @param groupName   name of the group
     * @param memberType  The member type corresponds to the MemberType
     * specified in GroupManagementService.MemberType
     * @param properties  Key-Value pairs of entries that are intended to
     * configure the underlying group communication provider's protocols such as
     * address, failure detection timeouts and retries, etc. Allowable keys
     * are specified in GMSConfigConstants
     * @return java.lang.Runnable
     */
    public static Runnable startGMSModule(final String serverToken,
                            final String groupName,
                            final GroupManagementService.MemberType memberType,
                            final Properties properties)
    {
        Runnable gms;
        //if this method is called, GMS is enabled. It is assumed that
        //calling code made checks in configurations about the enablement
        //The recommended way for calling code for this purpose is to call the
        // setGMSEnabledState() method in this class(see below).
        gmsEnabledMap.put(groupName, Boolean.TRUE);
        try { //sanity check: if this group instance is
            // already created return that instance
            gms = (Runnable) getGMSModule(groupName);
        } catch (GMSException e) {
            gms = new GroupManagementServiceImpl(serverToken, groupName, memberType, properties) ;
            memberToken = serverToken;
            groups.put(getCompositeKey(groupName), gms );
        }
        return gms;
    }

    /**
     * This returns an instance of the GroupManagementService for a given
     * non-null group name.
     * @param groupName groupName
     * @return GroupManagementService
     * @throws GMSException  - if the groupName is null
     *
     * @throws GMSNotEnabledException  - If GMS is not enabled
     * @throws GMSNotInitializedException - If GMS is not initialized
     */
    public static GroupManagementService getGMSModule(final String groupName)
            throws  GMSNotEnabledException, GMSException,
                   GMSNotInitializedException {
        if(groupName == null){
            throw new GMSException("Group Name was not specified and cannot be null");
        }
        final String key = getCompositeKey(groupName);
        if(groups.containsKey(key))
            return (GroupManagementService)groups.get(key);
        else if(!isGMSEnabled(groupName)){
            throw new GMSNotEnabledException(
                            new StringBuffer()
                            .append( "Group Management Service is not ")
                            .append("enabled for group")
                            .append( groupName ).toString());
        }
        else {
            throw new GMSNotInitializedException(
                            new StringBuffer()
                            .append( "Group Management Service is not ")
                            .append("initialized for group ")
                            .append( groupName ).toString());
        }

    }
    /**
     * This is to be used only in the case where this process is a member of
     * one and only one group and the group name is unknown to the caller.
     * @return GroupManagementService
     * @throws GMSException - wraps a throwable GMSNotInitializedException if
     * there are no GMS instances found.
     */
    public static GroupManagementService getGMSModule() throws GMSException {
        GroupManagementService gms;
        final Collection instances = getAllGMSInstancesForMember();
        if(instances.size() == 0){
            throw new GMSNotInitializedException(
                            new StringBuffer()
                            .append( "Group Management Service is not ")
                            .append("initialized for any group ").toString());
        }
        gms = (GroupManagementService)instances.toArray()[0];
        return gms;
    }

    /**
     * For the case where there are multiple groups in which this process has
     * become a member.
     * @return Collection
     */
    public static Collection getAllGMSInstancesForMember(){
        return groups.values();
    }

    private static String getCompositeKey(final String groupName) {
        return memberToken+"::"+groupName;
    }
    /**
     * returns true if GMS is enabled for the specified group.
     * @param groupName  Name of the group
     * @return true if GMS is enabled
     */
    public static boolean isGMSEnabled (final String groupName) {
        final Boolean val = gmsEnabledMap.get( groupName );
        return !(val == null || val.equals(Boolean.FALSE));
    }

    /**
     * enables an initialization code in the Application layer to set
     * GMS to be enabled or not based on the application's configuration
     * @param groupName  Name of the group
     * @param value a Boolean value
     */
    public static void setGMSEnabledState(final String groupName,
                                          final Boolean value){
        gmsEnabledMap.put(groupName, value);
    }

    /**
     * removes the GMS instance that is cached from a prior initialization. This
     * is typically called only when GMS module is being shutdown by a lifecycle
     * action.
     * @param groupName Name of the Group
     */
    public static void removeGMSModule(final String groupName){
        if(groupName != null){
            final String key  = getCompositeKey(groupName);
            if(groups.containsKey(key)){
                groups.remove(key);
            }
        }
    }
}
