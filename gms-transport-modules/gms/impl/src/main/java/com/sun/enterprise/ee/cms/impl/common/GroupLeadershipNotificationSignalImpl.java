/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
import com.sun.enterprise.ee.cms.core.GMSMember;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.Serializable;

/**
 * Implements GroupLeadershipNotificationSignal
 *
 * @author Bongjae Chang
 */
public class GroupLeadershipNotificationSignalImpl implements GroupLeadershipNotificationSignal {

    protected static final Logger logger = GMSLogDomain.getLogger( GMSLogDomain.GMS_LOGGER );

    private static final String MEMBER_DETAILS = "MEMBERDETAILS";
    private GMSContext ctx;

    private final String memberToken;
    private final String groupName;
    private final long startTime;
    private final List<GMSMember> previousView;
    private final List<GMSMember> currentView;
    private final List<String> currentCoreMembers;
    private final List<String> allCurrentMembers;

    public GroupLeadershipNotificationSignalImpl( final String memberToken,
                                                  final List<GMSMember> previousView,
                                                  final List<GMSMember> currentView,
                                                  final List<String> currentCoreMembers,
                                                  final List<String> allCurrentMembers,
                                                  final String groupName,
                                                  final long startTime ) {
        this.memberToken = memberToken;
        this.previousView = previousView;
        this.currentView = currentView;
        this.currentCoreMembers = currentCoreMembers;
        this.allCurrentMembers = allCurrentMembers;
        this.groupName = groupName;
        this.startTime = startTime;
        ctx = GMSContextFactory.getGMSContext( groupName );
    }

    GroupLeadershipNotificationSignalImpl( final GroupLeadershipNotificationSignal signal ) {
        this( signal.getMemberToken(),
              signal.getPreviousView(),
              signal.getCurrentView(),
              signal.getCurrentCoreMembers(),
              signal.getAllCurrentMembers(),
              signal.getGroupName(),
              signal.getStartTime() );
    }

    /**
     * {@inheritDoc}
     */
    public void acquire() throws SignalAcquireException {
    }

    /**
     * {@inheritDoc}
     */
    public void release() throws SignalReleaseException {
    }

    /**
     * {@inheritDoc}
     */
    public String getMemberToken() {
        return memberToken;
    }

    /**
     * {@inheritDoc}
     */
    public Map<Serializable, Serializable> getMemberDetails() {
        Map<Serializable, Serializable> ret = new HashMap<Serializable, Serializable>();
        if( ctx == null ) {
            ctx = GMSContextFactory.getGMSContext( groupName );
        }
        DistributedStateCache dsc = ctx.getDistributedStateCache();
        if( dsc != null ) {
            ret = dsc.getFromCacheForPattern( MEMBER_DETAILS, memberToken );
        } else {
            logger.log( Level.WARNING, "no.instance.dsc", new Object[]{ memberToken } );
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * {@inheritDoc}
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * {@inheritDoc}
     */
    public List<GMSMember> getPreviousView() {
        return previousView;
    }

    /**
     * {@inheritDoc}
     */
    public List<GMSMember> getCurrentView() {
        return currentView;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getCurrentCoreMembers() {
        return currentCoreMembers;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getAllCurrentMembers() {
        return allCurrentMembers;
    }
}
