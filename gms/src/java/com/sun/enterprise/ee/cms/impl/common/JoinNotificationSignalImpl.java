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
import com.sun.enterprise.ee.cms.core.JoinNotificationSignal;
import com.sun.enterprise.ee.cms.core.SignalAcquireException;
import com.sun.enterprise.ee.cms.core.SignalReleaseException;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of JoinNotificationSignal
 * @author Shreedhar Ganapathy
 *         Date: Feb 22, 2005
 * @version $Revision$
 */
public class JoinNotificationSignalImpl implements JoinNotificationSignal{
    private String memberToken;
    private String groupName;
    private List<String> currentCoreMembers;
    private List<String> allCurrentMembers;
    private static final String MEMBER_DETAILS = "MEMBERDETAILS";
    private static GMSContext ctx;

    //Logging related stuff
     protected static final Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    private long startTime;

    public JoinNotificationSignalImpl(final String memberToken, 
                                      final List<String> currentCoreMembers,
                                      final List<String> allCurrentMembers, 
                                      final String groupName,
                                      final long startTime) {
        this.memberToken=memberToken;
        this.currentCoreMembers=currentCoreMembers;
        this.allCurrentMembers=allCurrentMembers;
        this.groupName = groupName;
        this.startTime=startTime;
        ctx = GMSContextFactory.getGMSContext( groupName );
    }

    JoinNotificationSignalImpl ( final JoinNotificationSignal signal ) {
        this.memberToken=signal.getMemberToken();
        this.currentCoreMembers = signal.getCurrentCoreMembers();
        this.allCurrentMembers = signal.getAllCurrentMembers();
        this.groupName = signal.getGroupName();
        this.startTime = signal.getStartTime();
        ctx = GMSContextFactory.getGMSContext( groupName );
    }

    /**
     * Signal is acquired prior to processing of the signal
     * to protect group resources being
     * acquired from being affected by a race condition
     *
     * @throws com.sun.enterprise.ee.cms.core.SignalAcquireException
     *
     */
    public void acquire() throws SignalAcquireException {

    }

    /**
     * Signal is released after processing of the signal to bring the
     * group resources to a state of availability
     *
     * @throws com.sun.enterprise.ee.cms.core.SignalReleaseException
     *
     */
    public void release() throws SignalReleaseException {
        memberToken=null;
        currentCoreMembers=null;
        allCurrentMembers=null;
    }

    /**
     * returns the identity token of the member that caused this signal to be generated.
     * For instance, in the case of a MessageSignal, this member token would be the sender.
     * In the case of a FailureNotificationSignal, this member token would be the failed member.
     * In the case of a JoinNotificationSignal or PlannedShutdownSignal, the member token would be
     * the member who joined or is being gracefully shutdown, respectively.
     */
    public String getMemberToken() {
        return memberToken;
    }

    public List<String> getCurrentCoreMembers() {
        return currentCoreMembers;
    }

    public List<String> getAllCurrentMembers() {
        return allCurrentMembers;
    }

    /**
     * returns the details of the member who caused this Signal to be generated
     * returns a Map containing key-value pairs constituting data pertaining to
     * the member's details
     * @return Map <Serializable, Serializable> 
     */
    public Map<Serializable, Serializable> getMemberDetails ( ) {
        Map<Serializable, Serializable>ret = new HashMap<Serializable, Serializable>();
        if(ctx == null) {
            ctx = GMSContextFactory.getGMSContext(groupName);
        }
        DistributedStateCache dsc = ctx.getDistributedStateCache();
        if(dsc != null){
            ret = dsc.getFromCacheForPattern(MEMBER_DETAILS, memberToken );
        }
        else {
            logger.log(Level.WARNING, "Could not get an instance of Distributed State Cache to fetch member details for  "+memberToken) ;
        }
        return ret;
    }

    /**
     * returns the group to which the member involved in the Signal belonged to
     * @return String
     */
    public String getGroupName( ){
        return groupName;
    }

    public long getStartTime () {
        return startTime;
    }
}
