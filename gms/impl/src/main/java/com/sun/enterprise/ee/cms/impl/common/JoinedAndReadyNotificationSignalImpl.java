/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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

import com.sun.enterprise.ee.cms.core.*;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.io.Serializable;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements JoinedAndReadyNotificationSignal
 * @author Sheetal Vartak
 * Date: 11/13/07
 */
public class JoinedAndReadyNotificationSignalImpl implements JoinedAndReadyNotificationSignal {

    private String memberToken;
    private String groupName;
    private List<String> currentCoreMembers;
    private List<String> allCurrentMembers;
    private static final String MEMBER_DETAILS = "MEMBERDETAILS";
    private GMSContext ctx;
    private GMSConstants.startupType startupKind;
    private long startTime;
    private RejoinSubevent rs;
    private AliveAndReadyView currentView = null;
    private AliveAndReadyView previousView = null;

    //Logging related stuff
    protected static final Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);

    void setStartupKind(GMSConstants.startupType startupKind) {
        this.startupKind = startupKind;
    }

    public JoinedAndReadyNotificationSignalImpl(final String memberToken,
                                      final List<String> currentCoreMembers,
                                      final List<String> allCurrentMembers,
                                      final String groupName,
                                      final long startTime,
                                      final GMSConstants.startupType startupKind,
                                      final RejoinSubevent rs) {
        this.memberToken=memberToken;
        this.currentCoreMembers=currentCoreMembers;
        this.allCurrentMembers=allCurrentMembers;
        this.groupName = groupName;
        this.startTime=startTime;
        this.startupKind = startupKind;
        this.rs = rs;

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("JoinAndReadyNotificationSignalImpl ctor: member=" + memberToken + " group=" + groupName +  " startupKind=" + startupKind.toString());
        }
    }

    JoinedAndReadyNotificationSignalImpl ( final JoinedAndReadyNotificationSignal signal ) {
        this(signal.getMemberToken(), signal.getCurrentCoreMembers(), signal.getAllCurrentMembers(),
            signal.getGroupName(), signal.getStartTime(), signal.getEventSubType(), signal.getRejoinSubevent());
        currentView = signal.getCurrentView();
        previousView = signal.getPreviousView();
    }

    /**
     * Signal is acquired prior to processing of the signal
     * to protect group resources being
     * acquired from being affected by a race condition
     *
     * @throws com.sun.enterprise.ee.cms.core.SignalAcquireException
     *
     */
    @Override
    public void acquire() throws SignalAcquireException {
        // TODO??
    }

    /**
     * Signal is released after processing of the signal to bring the
     * group resources to a state of availability
     *
     * @throws com.sun.enterprise.ee.cms.core.SignalReleaseException
     *
     */
    @Override
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
    @Override
    public String getMemberToken() {
        return memberToken;
    }

    @Override
    public List<String> getCurrentCoreMembers() {
        return currentCoreMembers;
    }

    @Override
    public List<String> getAllCurrentMembers() {
        return allCurrentMembers;
    }

    /**
     * returns the details of the member who caused this Signal to be generated
     * returns a Map containing key-value pairs constituting data pertaining to
     * the member's details
     * @return Map <Serializable, Serializable>
     */
    @Override
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
    @Override
    public String getGroupName( ){
        return groupName;
    }

    @Override
    public long getStartTime () {
        return startTime;
    }

    @Override
    public GMSConstants.startupType getEventSubType() {
        return startupKind;
    }

    @Override
    public RejoinSubevent getRejoinSubevent() {
        return rs;
    }

    @Override
    public AliveAndReadyView getCurrentView() {
        return currentView;
    }

    @Override
    public AliveAndReadyView getPreviousView() {
        return previousView;
    }

    void setCurrentView(AliveAndReadyView current) {
        currentView = current;
    }

    void setPreviousView(AliveAndReadyView previous) {
        previousView = previous;
    }

}
