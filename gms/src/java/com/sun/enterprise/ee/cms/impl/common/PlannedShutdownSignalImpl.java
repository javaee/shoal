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

import com.sun.enterprise.ee.cms.core.GMSConstants;
import com.sun.enterprise.ee.cms.core.PlannedShutdownSignal;
import com.sun.enterprise.ee.cms.core.SignalAcquireException;
import com.sun.enterprise.ee.cms.core.SignalReleaseException;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.io.Serializable;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Implementation of PlannedShutdownSignal.
 * @author Shreedhar Ganapathy
 *         Date: Feb 22, 2005
 * @version $Revision$
 */
public class PlannedShutdownSignalImpl implements PlannedShutdownSignal {
    private String memberToken;
    private String groupName;
    //Logging related stuff
    protected static final Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    private static final String MEMBER_DETAILS = "MEMBERDETAILS";
    private static GMSContext ctx ;
    private long startTime;
    private GMSConstants.shutdownType shutdownType;

    public PlannedShutdownSignalImpl(final String memberToken,
                                     final String groupName,
                                     final long startTime,
                                final GMSConstants.shutdownType shutdownType) {
        this.memberToken=memberToken;
        this.groupName = groupName;
        this.startTime = startTime;
        this.shutdownType = shutdownType;
        ctx = GMSContextFactory.getGMSContext( groupName );
    }

    PlannedShutdownSignalImpl(final PlannedShutdownSignal signal) {
        this.memberToken = signal.getMemberToken();
        this.groupName = signal.getGroupName();
        this.startTime = signal.getStartTime();
        this.shutdownType = signal.getEventSubType();
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
    }

    public String getMemberToken() {
        return memberToken;
    }

    /**
     * returns the details of the member who caused this Signal to be generated
     * returns a Map containing key-value pairs constituting data pertaining to
     * the member's details
     * @return Map - <Serializable, Serializable>
     */
    public Map<Serializable, Serializable> getMemberDetails ( ) {
        return ctx.getDistributedStateCache()
                .getFromCacheForPattern(MEMBER_DETAILS, memberToken );
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

    /**
     * Planned shutdown events can be one of two types, Group Shutdown or
     * Instance Shutdown. These types are defined in an enum in the class
     * GMSConstants.shutdownType
     * @see com.sun.enterprise.ee.cms.core.GMSConstants
     * @return GMSConstants.shutdownType
     */
    public GMSConstants.shutdownType getEventSubType () {
        return shutdownType;
    }
}
