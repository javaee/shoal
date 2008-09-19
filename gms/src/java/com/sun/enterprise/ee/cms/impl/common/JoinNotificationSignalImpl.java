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

import com.sun.enterprise.ee.cms.core.DistributedStateCache;
import com.sun.enterprise.ee.cms.core.JoinNotificationSignal;
import com.sun.enterprise.ee.cms.core.SignalAcquireException;
import com.sun.enterprise.ee.cms.core.SignalReleaseException;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import com.sun.enterprise.ee.cms.spi.GroupCommunicationProvider;
import com.sun.enterprise.ee.cms.spi.MemberStates;

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
    private GMSContext ctx;

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

    public MemberStates getMemberState() {
        if(ctx == null){
            ctx = GMSContextFactory.getGMSContext(groupName);
        }
        GroupCommunicationProvider gcp = ctx.getGroupCommunicationProvider();
        return gcp.getMemberState(memberToken);
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
            logger.log(Level.WARNING, "no.instance.dsc", new Object[] {memberToken}) ;
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
