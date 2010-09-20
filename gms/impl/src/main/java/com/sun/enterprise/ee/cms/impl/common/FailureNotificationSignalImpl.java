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

import com.sun.enterprise.ee.cms.core.AliveAndReadyView;
import com.sun.enterprise.ee.cms.core.FailureNotificationSignal;
import com.sun.enterprise.ee.cms.core.SignalAcquireException;
import com.sun.enterprise.ee.cms.core.SignalReleaseException;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements FailureNotificationSignal
 * @author Shreedhar Ganapathy
 * Date: Jan 21, 2004
 * @version $Revision$
 */
public class FailureNotificationSignalImpl implements FailureNotificationSignal {
    protected String failedMember = null ;
    protected String groupName = null;
    protected  static final String MEMBER_DETAILS = "MEMBERDETAILS";
    protected GMSContext ctx;

   //Logging related stuff
    protected static final Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    protected long startTime;
    private AliveAndReadyView previousView = null;
    private AliveAndReadyView currentView = null;

    FailureNotificationSignalImpl(){

    }

    public FailureNotificationSignalImpl(final String  failedMember, 
                                         final String groupName, 
                                         final long startTime){
        this.failedMember = failedMember;
        this.groupName = groupName;
        this.startTime = startTime;
        ctx = GMSContextFactory.getGMSContext(groupName);
    }

    FailureNotificationSignalImpl(final FailureNotificationSignal signal) {
        this.failedMember = signal.getMemberToken();
        this.groupName = signal.getGroupName();
        this.startTime = signal.getStartTime();
        ctx = GMSContextFactory.getGMSContext(groupName);
        this.previousView = signal.getPreviousView();
        this.currentView = signal.getCurrentView();
    }

    /**
     * Signal is acquired prior to processing of the signal
     * to protect group resources that are being
     * acquired from being affected by a race condition
     * @throws  com.sun.enterprise.ee.cms.core.SignalAcquireException
     */
    public void acquire() throws SignalAcquireException {
        logger.log(Level.FINE, "FailureNotificationSignal Acquired...");        
    }

    /**
     * Signal is released after processing of the signal to bring the
     * group resources to a state of availability
     * @throws com.sun.enterprise.ee.cms.core.SignalReleaseException
     */
    public void release() throws SignalReleaseException {
        failedMember=null;
        logger.log(Level.FINE, "FailureNotificationSignal Released...");
    }

    /**
     * returns the identity token of the failed member
     */
    public String getMemberToken() {
        return  this.failedMember;
    }

    /**
     * returns the identity token of the failed member
     * @return java.lang.String
     * @deprecated
     */
    public String getFailedMemberToken() {
        return  this.failedMember;
    }

    /**
     * returns the details of the member who caused this Signal to be generated
     * returns a Map containing key-value pairs constituting data pertaining to
     * the member's details
     * @return Map - <Serializable, Serializable>
     */
    public Map<Serializable, Serializable> getMemberDetails ( ) {
        return ctx.getDistributedStateCache()
                .getFromCacheForPattern(MEMBER_DETAILS, failedMember );
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
