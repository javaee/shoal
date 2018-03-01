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
import com.sun.enterprise.ee.cms.spi.GroupCommunicationProvider;
import com.sun.enterprise.ee.cms.spi.MemberStates;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides contextual information about all useful GMS artifacts. These are
 * GMS objects that are tied to a particular group identity and thus scoped
 * to provide information within the group's context. There can be as many
 * GMSContext objects as there are groups within a single JVM process.
 *
 * @author Shreedhar Ganapathy
 * Date: Jan 12, 2004
 * @version $Revision$
 */
public interface GMSContext {
    /**
     * returns the serverIdentityToken pertaining to the process that
     * owns this GMS instance
     * @return java.lang.String
     */
    public String getServerIdentityToken();

    /**
     * returns the name of the group this context represents.
     * @return  the name of the group.
     */
    public String getGroupName();

    /**
     * returns Group handle
     * @return Group handle
     */
    GroupHandle getGroupHandle();

    /**
     * returns the router
     * @return router
     */
    Router getRouter();

    ViewWindow getViewWindow();

    DistributedStateCache getDistributedStateCache();

    GMSMonitor getGMSMonitor();

    void join() throws GMSException ;

    void leave(final GMSConstants.shutdownType shutdownType) ;

    boolean isShuttingDown ();

    long getStartTime();

    public void announceGroupStartup(final String groupName,
                                     final GMSConstants.groupStartupState startupState,
                                     final List<String> memberTokens);

    void announceGroupShutdown(final String groupName,
                           final GMSConstants.shutdownState shutdownState );

    boolean addToSuspectList( final String token );

    void removeFromSuspectList( final String token );

    boolean isSuspected ( final String token );

    List<String> getSuspectList();

    ShutdownHelper getShutdownHelper ();

    GroupCommunicationProvider getGroupCommunicationProvider();

    /**
     * lets this instance become a group leader explicitly
     * Typically this can be employed by an administrative member to become
     * a group leader prior to shutting down a group of members simultaneously.
     *
     * For underlying Group Communication Providers who don't support the feature
     * of a explicit leader role assumption, the implementation of this method
     * would be a no-op.
     * */
    void assumeGroupLeadership();

    boolean isGroupBeingShutdown(String groupName);

    boolean isGroupStartup();

    void setGroupStartup(boolean value);

    public GroupManagementService.MemberType getMemberType();

    public boolean isWatchdog();

    public AliveAndReadyView getPreviousAliveAndReadyView();

    public AliveAndReadyView getCurrentAliveAndReadyView();

    public Map<String, RejoinSubevent> getInstanceRejoins();

    public AliveAndReadyViewWindow  getAliveAndReadyViewWindow();

    public void setGroupStartupJoinMembers(Set<String> members);

    public boolean isGroupStartupComplete();

    public boolean setGroupStartupState(String member, MemberStates state);
}
