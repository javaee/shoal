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

        // deprecate fencing in gms proper. transaction handling fencing itself.
//        try {
//            final GroupHandle gh = ctx.getGroupHandle();
//            if(gh.isMemberAlive( failedMember ) ){
//                throw new GMSException ("Cannot raise fence on "+ failedMember + " as it is already alive");
//            }
//            gh.raiseFence(componentName, failedMember);
//            logger.log(Level.FINE, "raised fence for component "+componentName+" and member "+ failedMember);
//        }
//        catch ( GMSException e ) {
//            throw new SignalAcquireException( e );
//        }
    }

    /**
     * Must be called by client after recovery operation is complete
     * to bring the group state up-to-date on this recovery operation.
     * Not doing so will leave a stale entry in the group's state.
     */
    @Override public void release() throws SignalReleaseException
    {
        try {
            // deprecated fencining in gms proper.
//          ctx.getGroupHandle().lowerFence(componentName, failedMember);
//          logger.log(Level.FINE, "lowered fence for component "+ componentName +" and member "+ failedMember);

            // GMS will reissue FailureRecovery if instance appointed as Recovery Agent fails before removing
            // its appointment.
            ctx.getGroupHandle().removeRecoveryAppointments(failedMember, componentName);
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
