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

import com.sun.enterprise.ee.cms.core.GroupManagementService;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * @author Shreedhar Ganapathy
 *         Date: Jan 31, 2006
 * @version $Revision$
 */
public abstract class GMSContextBase implements GMSContext{
    protected String serverToken=null;
    protected String groupName=null;
    protected Router router;
    protected ViewWindow viewWindow;
    protected static final Logger logger =
            GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    protected String memberType;
    protected GMSMember gmsMember;
    protected ArrayList<String> suspectList;
    protected final Long startTime;
    protected boolean shuttingDown = false;
    protected final ShutdownHelper shutdownHelper;

    protected GMSContextBase (final String serverToken, final String groupName,
               final GroupManagementService.MemberType memberType)
    {
        this.serverToken= serverToken;
        this.groupName=groupName;
        this.memberType = getMemberType(memberType);
        startTime = new Long(System.currentTimeMillis());
        gmsMember = new GMSMember(serverToken, this.memberType, groupName,
                                  startTime);
        suspectList = new ArrayList<String>();
        router = new Router();
        shutdownHelper = new ShutdownHelper();
    }

    protected static String getMemberType(
            final GroupManagementService.MemberType memberType)
    {
        if(memberType == null)
            return GroupManagementService.MemberType.CORE.toString();
        else
            return memberType.toString();
    }

    /**
     * returns the serverIdentityToken pertaining to the process that
     * owns this GMS instance
     * @return java.lang.String
     */
    public String getServerIdentityToken(){
        return serverToken;
    }

    /**
     * returns the name of the group this context represents
     */
    public String getGroupName(){
        return groupName;
    }

    /**
     * returns the router
     * @return router
     */
    public Router getRouter(){
        return router;
    }

    protected abstract void createDistributedStateCache();

    public boolean isShuttingDown () {
        return shuttingDown;
    }

}
