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
package com.sun.enterprise.ee.cms.impl.common;

import com.sun.enterprise.ee.cms.core.DistributedStateCache;
import com.sun.enterprise.ee.cms.core.FailureRecoveryActionFactory;
import com.sun.enterprise.ee.cms.core.GMSException;
import com.sun.enterprise.ee.cms.core.GroupManagementService;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Uses a specified algorithm to determine a member that will be selected
 * to handle recovery operations on failure of a member. The algorithms are
 * specified through a RecoverySelectorMode object that specifies typ safe enums
 * such as SIMPLESELECT indicating a simple ordered selection algorithm and
 * HOSTSELECT indicating a selection algorithm that ensures that the recovery
 * target is always on a different host from a single host.
 *
 * @author Shreedhar Ganapathy
 * Date: Jan 20, 2004
 * @version $Revision$
 */
public class RecoveryTargetSelector {
    static final String CORETYPE = GroupManagementService.MemberType.CORE.toString();
    private static final Logger logger = GMSLogDomain.getLogger( GMSLogDomain.GMS_LOGGER);

    private RecoveryTargetSelector () {
    }

    public static enum RecoverySelectorMode {SIMPLESELECT, HOSTSELECT}
    /**
     * Uses a resolution algorithm to determine whether the member this process
     * represents is indeed responsible for performing recovery operations.
     * @param mode - as specified by the RecoverySelectorMode
     * @param members - a vector of members from a view that existed prior to
     * the failure
     * @param failedMemberToken - failed member's identity token
     * @param ctx - The GMSContext pertaining to the failed member;s group.
     * @return boolean
     */
    public static boolean resolveRecoveryTarget(final RecoverySelectorMode mode,
                                         final List<GMSMember> members,
                                         final String failedMemberToken, 
                                         final GMSContext ctx)
    {
        final boolean recoveryServer;
        if(mode != null && mode.equals(RecoverySelectorMode.HOSTSELECT)){
            recoveryServer = resolveWithHostSelectionAlgorithm(members,
                                                               failedMemberToken,
                                               ctx.getGroupName());
        }
        else{
            recoveryServer = resolveWithSimpleSelectionAlgorithm(members,
                                                             failedMemberToken,
                                                 ctx.getGroupName());
        }
        return recoveryServer;
    }

    /**
     * Ensures that the selected recovery server is located on a machine
     * that is different from the one on which the failed member process was
     * running. This algorithm takes care of some of the risks such as
     * resources risk, cascading failure risk, etc associated with
     * selecting recovery targets on the same machine as the one on which failed
     * process was located.
     * If there are no members on other hosts (ex. Group of only two members
     * running only on one host), then the selection algorithm switches to the
     * simple algorithm mode.
     * @param members
     * @param failedMember
     * @return boolean
     */
    private static boolean resolveWithHostSelectionAlgorithm(
            final List<GMSMember> members,
            final String failedMember,
            final String groupName)
    {
        return false;
    }

    /**
     * Uses the Chronological Successor algorithm to determine whether this
     * process(member) is selected to perform recovery.
     * The members from the ordered view prior to failure is already cached.
     * From this cache, we determine the first live member that stood
     * <bold>immediately after</bold> the failed member, or where the failed
     * member was last in this ordered list, determine the first live member in
     * the ordered list. In both such cases, recovery is determined to be
     * performed by this member.
     * @return boolean true if recovery is to be performed by this process, 
     * false if not.
     */
    private static boolean resolveWithSimpleSelectionAlgorithm(
                                            final List<GMSMember> viewCache,
                                            final String failedMember,
                                            final String groupName)
    {
        boolean recover = false;
        String recoverer = null;
        final GMSContext ctx = GMSContextFactory.getGMSContext( groupName );
        final String self = ctx.getServerIdentityToken();
        final List<String> liveCache = getMemberTokens(viewCache,
                                                       ctx.getSuspectList());
        logger.log(Level.FINE, "LiveCache = "+liveCache);
        final List<String> vCache = getCoreMembers(viewCache);
        logger.log(Level.FINE, "vCache = "+vCache);

        for( int i=0; i<vCache.size(); i++ ) {
            final String member;
            //if current member in cached view is same as failed member
             if( vCache.get(i).equals(failedMember) ){
                //if this failed member is the last member
                if( i == ( vCache.size() - 1 ) ) {
                    member = vCache.get(0);
                    logger.log(Level.FINEST,
                       "Failed Member was last member of the previous view, "+
                   "The first live core member will be selected as recoverer");
                    logger.log(Level.FINEST,
                               "First Core Member is "+member);
                    logger.log(Level.FINEST, "Live members are :"+
                                             liveCache.toString());
                    //if the first member of the view cache is a live member
                    if(liveCache.contains(member)){
                        recoverer = member;
                    }
                }
                //if this failed member is not the last member
                else {//get the rest of the members
                    final List<String> subset = vCache.subList( i+1,
                                                             vCache.size());
                    for(final String mem : subset){
                        //pick the first live member based on the subset
                        if(liveCache.contains( mem )){
                            recoverer = mem;
                            break;
                        }
                    }
                }
                if(recoverer != null){
                    // if I am (this process is) the recoverer, then I
                    // select myself for recovery
                    if(recoverer.equals(self)) {
                        recover = true;
                    }
                    //this in effect will be set by every GMS instance
                    // regardless of whether they are the recovery server.
                    //this redundant action ensures that there is a group-wide
                    //record of this selection
                    setRecoverySelectionState(recoverer,
                                  failedMember,
                                  groupName);
                }
            }
        }
        return recover;
    }

    private static List<String> getCoreMembers (final List<GMSMember> viewCache)
    {
        final List<String> temp = new ArrayList<String>();
        for(final GMSMember member : viewCache){
            if(member.getMemberType().equals( CORETYPE )){
                temp.add( member.getMemberToken() );
            }
        }
        return temp;
    }

    private static void dWait ( final long damperWaitOnMultiFails ) {
        try{
            Thread.sleep(damperWaitOnMultiFails);
        } catch ( InterruptedException e){
            logger.log(Level.FINEST, e.getLocalizedMessage());
        }
    }

/*  //COMMENTED OUT AS THIS IS A PLACEHOLDER FOR A FUTURE SELECTION ALGORITHM
    private static boolean resolveWithLoadBalancingAlgorithm (                                            final List<GMSMember> viewCache,
                                            final List<String> exclusionList,
                                            final GMSMember failedMember,
                                            final String groupName)
    {
        boolean recover = false;
        final GMSContext ctx = GMSContextFactory.getGMSContext( groupName );
        final String self = ctx.getServerIdentityToken();
        final GMSMember[] cache = getMemberTokens(viewCache, exclusionList);

        // select a member who is not currently performing recovery
        String recoverer = selectNonRecoveringMember(cache, ctx);
        //if no such member is available pick the one that is performing least
        //number of recoveries.
        if(recoverer == null){
            recoverer = selectLeastRecoveryLoadedMember(cache, ctx);
        }
        //this in effect will be set by every GMS instance
        // regardless of whether they are the recovery server.
        //this redundant action ensures that there is a group-wide
        //record of this selection
        setRecoverySelectionState(recoverer,
                                failedMember.getMemberToken(),
                                groupName);
        // if I am (this process is) the first member, then I
        // select myself for recovery
        if(recoverer.equals(self)) {
            recover = true;
        }
        return recover;
    }

    private static String selectNonRecoveringMember (
            final GMSMember[] cache, final GMSContext ctx)
    {
        final String recoverer;
        final DistributedStateCache dsc = ctx.getDistributedStateCache();
        Map<GMSCacheable, Object> entries;
        final List<GMSMember> candidates = new ArrayList<GMSMember>();
        for(final GMSMember member : cache){
            entries = dsc.getFromCache( member );
            final int counter = getNumRecoveries( entries,
                                                  member.getMemberToken() );
            if(counter < 1 ) {//this member is free to perform recoveries
                candidates.add(member);
            }
        }
        if(candidates.isEmpty()){//all members are doing some recovery
            recoverer = null;
        }
        else {
            recoverer = candidates.get( 0 ); //pick the first from shuffled list
        }
        return recoverer;
    }

    private static String selectLeastRecoveryLoadedMember (
            final GMSMember[] cache, final GMSContext ctx )
    {
        String recoverer = null;
        final DistributedStateCache dsc = ctx.getDistributedStateCache();
        Map<GMSCacheable, Object> entries;
        final Map<String, Integer> candidates = new HashMap<String, Integer>();
        int lowest = Integer.MAX_VALUE;
        for(final GMSMember member : cache){
            int counter = 0;
            entries = dsc.getFromCache( member );
            counter = getNumRecoveries( entries, member.getMemberToken() );
            if(counter < lowest){
                lowest = counter;
            }
            candidates.put(member, new Integer(counter));
        }
        for(String member : candidates.keySet()){
            final int value = candidates.get(member);
            if(value == lowest ) {
                recoverer = member;
                break;
            }
        }
        return recoverer;
    }

    private static int getNumRecoveries(final Map<GMSCacheable, Object> entries,
        final String member )
    {
        int counter = 0;
        Object entry;
        for(GMSCacheable c : entries.keySet()){
            // if this member is not performing recovery for self or others
            if(member.equals( c.getMemberTokenId() ))
            {
                if((entry = entries.get(c)) instanceof String
                    && ((String)entry).startsWith( REC_PROGRESS_STATE))
                {
                    counter++;
                }
            }
        }
        return counter;
    }
  */
    public static void setRecoverySelectionState (
            final String recovererMemberToken,
            final String failedMemberToken,
            final String groupName)
    {
        logger.log(Level.INFO, new StringBuffer()
                                .append( "Appointed Recovery Server:" )
                                .append( recovererMemberToken )
                                .append( ":for failed member:" )
                                .append( failedMemberToken )
                                .append( ":for group:" )
                                .append( groupName ).toString());
        final GMSContext ctx = GMSContextFactory.getGMSContext( groupName );
        final DistributedStateCache dsc = ctx.getDistributedStateCache();
        final Hashtable<String,FailureRecoveryActionFactory> reg =
                        ctx.getRouter().getFailureRecoveryAFRegistrations();

        for(String component : reg.keySet())
        {
            try {
                dsc.addToCache(component,
                                recovererMemberToken,
                                failedMemberToken,
                                setStateAndTime()
                                );
            }
            catch ( GMSException e ) {
                logger.log(Level.WARNING, e.getLocalizedMessage());
            }
        }
    }

    private static String setStateAndTime() {
        return GroupManagementService
                    .RECOVERY_STATE
                    .RECOVERY_SERVER_APPOINTED.toString() + '|' +
                System.currentTimeMillis();

    }

    private static List<String> getMemberTokens(final List<GMSMember> members,
        final List<String> exclusionList )
    {
        final List<String> temp = new ArrayList<String>();
        String token;
        for(GMSMember member : members){
            token = member.getMemberToken();
            if(member.getMemberType().equals(CORETYPE)
                &&
            !exclusionList.contains( token ))//only send in non excluded members
            {
                temp.add( token );
            }
        }
        logger.log(Level.FINEST, "SuspectedMembers: "+exclusionList.toString());
        logger.log(Level.FINEST, "LiveMembers: "+temp.toString());
        return temp;
    }
}
