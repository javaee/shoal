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
