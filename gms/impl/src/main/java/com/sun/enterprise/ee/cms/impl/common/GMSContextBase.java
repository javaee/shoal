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

import com.sun.enterprise.ee.cms.core.GroupManagementService;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import com.sun.enterprise.ee.cms.core.GMSMember;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * @author Shreedhar Ganapathy
 *         Date: Jan 31, 2006
 * @version $Revision$
 */
public abstract class GMSContextBase implements GMSContext {
    protected String serverToken = null;
    protected String groupName = null;
    protected Router router;
    protected ViewWindow viewWindow;
    protected static final Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    protected String memberType;
    protected GMSMember gmsMember;
    protected final ArrayList<String> suspectList;
    protected final Long startTime;
    protected boolean shuttingDown = false;
    protected final ShutdownHelper shutdownHelper;
    protected final GroupManagementService.MemberType gmsMemberType;

    protected GMSContextBase(final String serverToken, final String groupName,
                             final GroupManagementService.MemberType memberType) {
        this.serverToken = serverToken;
        this.groupName = groupName;
        this.gmsMemberType = memberType;
        this.memberType = getMemberType(memberType);
        startTime = System.currentTimeMillis();
        gmsMember = new GMSMember(serverToken, this.memberType, groupName,
                startTime);
        suspectList = new ArrayList<String>();
        shutdownHelper = new ShutdownHelper();
    }

    protected static String getMemberType(
            final GroupManagementService.MemberType memberType) {
        if (memberType == null)
            return GroupManagementService.MemberType.CORE.toString();
        else
            return memberType.toString();
    }

    public GroupManagementService.MemberType getMemberType() {
        return gmsMemberType;
    }

    /**
     * returns the serverIdentityToken pertaining to the process that
     * owns this GMS instance
     *
     * @return java.lang.String
     */
    public String getServerIdentityToken() {
        return serverToken;
    }

    /**
     * returns the name of the group this context represents
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * returns the router
     *
     * @return router
     */
    public Router getRouter() {
        return router;
    }

    protected abstract void createDistributedStateCache();

    /**
     * Return <code>true</code> if shutting down
     * @return <code>true</code> if shutting down
     */
    public boolean isShuttingDown() {
        return shuttingDown;
    }

    abstract public AliveAndReadyViewWindow  getAliveAndReadyViewWindow();

    abstract public GMSMonitor getGMSMonitor();
 }
