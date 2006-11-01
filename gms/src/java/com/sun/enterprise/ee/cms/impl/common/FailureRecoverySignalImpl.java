/*
 * Copyright 2004-2005 Sun Microsystems, Inc.  All rights reserved.
 * Use is subject to license terms.
 */
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

import com.sun.enterprise.ee.cms.core.*;

import java.util.logging.Level;

/**
 * Implements the FailureRecoverySignal Interface and provides operations
 * corresponding to a recovery oriented Signal's behavior
 *
 * @author Shreedhar Ganapathy
 * Date: November 07, 2003
 * @version $Revision$
 */
public class FailureRecoverySignalImpl extends FailureNotificationSignalImpl
        implements FailureRecoverySignal
 {
    private String componentName;
    public FailureRecoverySignalImpl(final String componentName,
                                     final String failedMember, 
                                     final String groupName, 
                                     final long startTime)
    {
        this.failedMember = failedMember;
        this.componentName = componentName;
        this.groupName = groupName;
        this.startTime = startTime;
        this.ctx = GMSContextFactory.getGMSContext(groupName);
    }

    FailureRecoverySignalImpl ( final FailureRecoverySignal signal ) {
        this.failedMember = signal.getMemberToken();
        this.componentName = signal.getComponentName();
        this.groupName = signal.getGroupName();
        this.startTime = signal.getStartTime();
        this.ctx = GMSContextFactory.getGMSContext(groupName);
    }

    /**
     * Must be called by client before beginning any recovery operation
     * in order to get support of failure fencing.
     * @throws SignalAcquireException
     */
    @Override public void acquire() throws SignalAcquireException {
        try {
            final GroupHandle gh = ctx.getGroupHandle();
            if(gh.isMemberAlive( failedMember ) ){
                throw new GMSException ("Cannot raise fence on "+ failedMember
                                        + " as it is already alive");
            }
            gh.raiseFence(componentName, failedMember);
            logger.log(Level.FINE, "raised fence for component "+
                                     componentName+" and member "+
                                     failedMember);
        }
        catch ( GMSException e ) {
            throw new SignalAcquireException( e );
        }
    }

    /**
     * Must be called by client after recovery operation is complete
     * to bring the group state up-to-date on this recovery operation.
     * Not doing so will leave a stale entry in the group's state.
     */
    @Override public void release() throws SignalReleaseException
    {
        try {
            GMSContextFactory
                    .getGMSContext( groupName )
                    .getGroupHandle()
                    .lowerFence(componentName, failedMember);
            logger.log(Level.FINE, "lowered fence for component "+
                                     componentName+" and member "+
                                     failedMember);
            failedMember=null;
        }
        catch ( GMSException e ) {
            throw new SignalReleaseException( e );
        }
    }

    public String getComponentName () {
        return componentName;
    }
}
