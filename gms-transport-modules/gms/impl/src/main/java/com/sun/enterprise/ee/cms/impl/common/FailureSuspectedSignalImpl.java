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

import com.sun.enterprise.ee.cms.core.FailureSuspectedSignal;
import com.sun.enterprise.ee.cms.core.SignalAcquireException;
import com.sun.enterprise.ee.cms.core.SignalReleaseException;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Shreedhar Ganapathy
 *         Date: Sep 14, 2005
 * @version $Revision$
 */
public class FailureSuspectedSignalImpl implements FailureSuspectedSignal {
    protected String failedMember = null ;
    protected String groupName = null;
    protected  static final String MEMBER_DETAILS = "MEMBERDETAILS";
    protected GMSContext ctx;

   //Logging related stuff
    protected static final Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    protected long startTime;

    FailureSuspectedSignalImpl(){

    }

    public FailureSuspectedSignalImpl(final String  failedMember,
                                         final String groupName,
                                         final long startTime){
        this.failedMember = failedMember;
        this.groupName = groupName;
        this.startTime = startTime;
        ctx = GMSContextFactory.getGMSContext(groupName);
    }

    FailureSuspectedSignalImpl(final FailureSuspectedSignal signal) {
        this.failedMember = signal.getMemberToken();
        this.groupName = signal.getGroupName();
        this.startTime = signal.getStartTime();
        ctx = GMSContextFactory.getGMSContext(groupName);
    }


    /**
     * Signal is acquired prior to processing of the signal to protect group
     * resources being acquired from being affected by a race condition Signal
     * must be mandatorily acquired before any processing for recovery
     * operations.
     *
     * @throws com.sun.enterprise.ee.cms.core.SignalAcquireException
     *
     */
    public void acquire () throws SignalAcquireException {
        logger.log(Level.FINE, "FailureSuspectedSignal Acquired...");
    }

    /**
     * Signal is released after processing of the signal to bring the group
     * resources to a state of availability Signal should be madatorily released
     * after recovery process is completed.
     *
     * @throws com.sun.enterprise.ee.cms.core.SignalReleaseException
     *
     */
    public void release () throws SignalReleaseException {
        failedMember=null;
        logger.log(Level.FINE, "FailureSuspectedSignal Released...");
    }

    /**
     * returns the identity token of the member that caused this signal to be
     * generated. For instance, in the case of a MessageSignal, this member
     * token would be the sender. In the case of a FailureNotificationSignal,
     * this member token would be the failed member. In the case of a
     * JoinNotificationSignal or GracefulShutdownSignal, the member token would
     * be the member who joined or is being gracefully shutdown, respectively.
     */
    public String getMemberToken () {
        return  this.failedMember;
    }

    /**
     * returns the details of the member who caused this Signal to be generated
     * returns a Map containing key-value pairs constituting data pertaining to
     * the member's details
     *
     * @return Map  <Serializable, Serializable>
     */
    public Map<Serializable, Serializable> getMemberDetails () {
        return ctx.getDistributedStateCache()
                .getFromCacheForPattern(MEMBER_DETAILS, failedMember );
    }

    /**
     * returns the group to which the member involved in the Signal belonged to
     *
     * @return String
     */
    public String getGroupName () {
        return groupName;
    }

    /**
     * returns the start time of the member involved in this Signal.
     *
     * @return long - time stamp of when this member started
     */
    public long getStartTime () {
        return startTime;
    }
}
