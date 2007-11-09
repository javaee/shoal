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

package com.sun.enterprise.ee.cms.impl.common;
/**
 * Provides API for joining, and leaving the group and to register Action Factories of
 * specific types for specific Group Event Signals.
 * @author Shreedhar Ganapathy
 * Date: June 10, 2006
 * @version $Revision$
 */

import com.sun.enterprise.ee.cms.core.*;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GroupManagementServiceImpl implements GroupManagementService, Runnable{
    private final GMSContext ctx;
    private Router router;

    //Logging related stuff
    private static final Logger logger =  GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    private static final String MEMBER_DETAILS = "MEMBERDETAILS";

    /**
     * Creates a GMSContext instance with the given paramters. GMSContext calls the
     * underlying Group Communication Provider to initialize it with these parameters.
     * @param serverToken    identity token of this member process
     * @param groupName      name of the group
     * @param membertype     Type of member as specified in GroupManagementService.MemberType
     * @param properties     Configuration Properties
     */
    public GroupManagementServiceImpl(final String serverToken, final String groupName,
                                      final GroupManagementService.MemberType membertype,
                                      final Properties properties)
    {
        ctx = GMSContextFactory.produceGMSContext(serverToken,
                                    groupName, membertype,
                                    properties);
        router = ctx.getRouter();
    }

    public void run(){
        startup();
    }

    private void startup() {
        try{
            logger.log(Level.INFO, "gms.joinMessage");
            join();
        } catch (GMSException e) {
            logger.log(Level.FINE,"gms.joinException", e);
        }
    }

    /**
     * Registers a FailureNotificationActionFactory instance.
     * To add MessageActionFactory instance, use the method
     * addActionFactory(MessageActionFactory maf, String componentName);
     *
     * @param failureNotificationActionFactory   implementation of this interface
     *
     */
    public void addActionFactory(final FailureNotificationActionFactory failureNotificationActionFactory) {
        router.addDestination(failureNotificationActionFactory);
    }

    /**
     * Registers a FailureRecoveryActionFactory instance.
     * To add MessageActionFactory instance, use the method
     * addActionFactory(MessageActionFactory maf, String componentName);
     * @param componentName   name of component
     * @param failureRecoveryActionFactory  implmentation of this interface
     */
    public void addActionFactory(final String componentName,
                         final FailureRecoveryActionFactory failureRecoveryActionFactory) {
        router.addDestination(componentName, failureRecoveryActionFactory);
    }

    /**
     * Registers a JoinNotificationActionFactory instance.
     * To add MessageActionFactory instance, use the method
     * addActionFactory(MessageActionFactory maf, String componentName);
     *
     * @param joinNotificationActionFactory   implementation of this interface
     */
    public void addActionFactory(final JoinNotificationActionFactory joinNotificationActionFactory) {
        router.addDestination(joinNotificationActionFactory);
    }

    /**
     * Registers a PlannedShuttdownActionFactory instance.
     * To add MessageActionFactory instance, use the method
     * addActionFactory(MessageActionFactory maf, String componentName);
     *
     * @param plannedShutdownActionFactory implementation of this interface
     */
    public void addActionFactory(final PlannedShutdownActionFactory plannedShutdownActionFactory) {
        router.addDestination(plannedShutdownActionFactory);
    }

    /**
     * Registers a MessageActionFactory instance for the specified component
     * name.
     * @param messageActionFactory implementation of this interface
     * @param componentName name of component to identify target component for message delivery
     */
    public void addActionFactory(final MessageActionFactory messageActionFactory,
                                 final String componentName)
    {
        router.addDestination(messageActionFactory, componentName);
    }

    public void addActionFactory (
            final FailureSuspectedActionFactory failureSuspectedActionFactory ) {
        router.addDestination( failureSuspectedActionFactory );
    }

    /**
     * Removes a FailureNotificationActionFactory instance
     * To remove a MessageActionFactory for a specific component,
     * use the method:
     * removeActionFactory(String componentName);
     *
     * @param failureNotificationActionFactory  implementation of this interface
     *
     */
    public void removeActionFactory(final FailureNotificationActionFactory failureNotificationActionFactory) {
        router.removeDestination(failureNotificationActionFactory);
    }

    /**
     * Removes a FailureRecoveryActionFactory instance
     * To remove a MessageActionFactory for a specific component,
     * use the method:
     * removeActionFactory(String componentName);
     * @param componentName name of component
     */
    public void removeFailureRecoveryActionFactory(final String componentName) {
        router.removeFailureRecoveryAFDestination( componentName );
    }

    public void removeFailureSuspectedActionFactory (
            final FailureSuspectedActionFactory failureSuspectedActionFactory )
    {
        router.removeDestination( failureSuspectedActionFactory );    
    }

    /**
     * Removes a JoinNotificationActionFactory instance
     * To remove a MessageActionFactory for a specific component,
     * use the method:
     * removeActionFactory(String componentName);
     *
     * @param joinNotificationActionFactory implementation of this interface
     */
    public void removeActionFactory(final JoinNotificationActionFactory joinNotificationActionFactory) {
        router.removeDestination(joinNotificationActionFactory);
    }

    /**
     * Removes a PlannedShutdownActionFactory instance
     * To remove a MessageActionFactory for a specific component,
     * use the method:
     * removeActionFactory(String componentName);
     *
     * @param plannedShutdownActionFactory implementation of this interface
     */
    public void removeActionFactory(final PlannedShutdownActionFactory plannedShutdownActionFactory) {
        router.removeDestination(plannedShutdownActionFactory);
    }

    /**
     * Removes a MessageActionFactory instance belonging to the
     * specified component
     * @param componentName    name of component
     */
    public void removeMessageActionFactory(final String componentName)
    {
        router.removeMessageAFDestination( componentName);
    }

    /**
     * Returns an implementation of GroupHandle
     * @return com.sun.enterprise.ee.cms.GroupHandle
     */
    public GroupHandle getGroupHandle() {
        return ctx.getGroupHandle();
    }

    /**
     * Sends a shutdown command to the GMS indicating that the parent thread
     * is about to be shutdown as part of a planned shutdown operation
     */
    public void shutdown(final GMSConstants.shutdownType shutdownType) {
        leave(shutdownType);
    }

    public void updateMemberDetails ( final String memberToken,
                                      final Serializable key,
                                      final Serializable value )
            throws GMSException
    {
        ctx.getDistributedStateCache()
                .addToCache(MEMBER_DETAILS,
                            memberToken,
                            key,
                            value );

    }

    /**
     *
     * returns the details pertaining to the given member. At times, details
     * pertaining to all members may be stored in the Cache but keyed by the
     * given member token. Through this route, details of all members could be
     * obtained.
     * returns a Map containing key-value pairs constituting data pertaining to
     * the member's details
     *
     * @param memberToken   identity token of the member process
     * @return Map  <Serializable, Serializable>
     */

    public Map<Serializable, Serializable> getMemberDetails (
                                                final String memberToken )
    {
        return ctx.getDistributedStateCache()
                .getFromCacheForPattern( MEMBER_DETAILS, memberToken  );
    }

    public Map<Serializable, Serializable> getAllMemberDetails(
                                            final Serializable key){

        final Map<Serializable, Serializable> retval =
                new HashMap<Serializable, Serializable>();
        final Map<GMSCacheable, Object> ret = ctx.getDistributedStateCache()
                                                .getFromCache( key );

        for(GMSCacheable c : ret.keySet()){
            if(c.getComponentName().equals( MEMBER_DETAILS )){
                retval.put( c.getMemberTokenId(), ( Serializable )ret.get( c ));
            }
        }
        return retval;
    }

    /**
     * for this serverToken, use the map to derive key value pairs
     * that constitute data pertaining to this member's details
     * @param serverToken - member token id for this member.
     * @param keyValuePairs - a Map containing key-value pairs
     * @throws com.sun.enterprise.ee.cms.core.GMSException wraps underlying exception that caused adding of member details to fail.
     */
    public void setMemberDetails ( final String serverToken,
                  final Map<? extends Object, ? extends Object> keyValuePairs)
            throws GMSException
    {
        for(Object key : keyValuePairs.keySet()){
            ctx.getDistributedStateCache()
                    .addToLocalCache(MEMBER_DETAILS,
                                    serverToken,
                                    (Serializable)key,
                                    (Serializable) keyValuePairs.get(key) );
        }
    }

    public void join() throws GMSException {
        logger.log(Level.FINE, "Connecting to group......");
        ctx.join();
    }

    /**
     * Called when the application layer is shutting down and this member needs to leave
     * the group formally for a graceful shutdown event.
     * @param shutdownType shutdown type corresponds to the shutdown types specified
     * in GMSConstants.shudownType enum.
     */
    private void leave(final GMSConstants.shutdownType shutdownType) {
        logger.log(Level.FINE, "Deregistering ActionFactory instances...");
        removeAllActionFactories();
        ctx.leave(shutdownType);
        GMSContextFactory.removeGMSContext(ctx.getGroupName());
    }

    private void removeAllActionFactories() {
        router.undocketAllDestinations();
    }

    /**
     * This method is used to announce that the group is about to be shutdown.
     * @param groupName name of group being shutdown.
     */
    public void announceGroupShutdown ( final String groupName,
                            final GMSConstants.shutdownState shutdownState) {
        final GMSContext gctx = GMSContextFactory.getGMSContext( groupName );
        logger.log(Level.FINE,
                   "GMS:Announcing GroupShutdown to group with State="+shutdownState);
        gctx.announceGroupShutdown( groupName, shutdownState );
        gctx.assumeGroupLeadership();
 
    }

}
