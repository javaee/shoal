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
package com.sun.enterprise.ee.cms.core;

import java.io.Serializable;
import java.util.Map;

/**
 * Subtypes of Signal will define operations specific to their Signal 
 * functionalities.
 * 
 * <code>Action</code>s consume <code>Signal</code>s.
 *
 * Each Signal is delivered on its own thread.
 * 
 * The Signal signifies a group event. Each event is typified by subtypes of
 * Signal.
 * 
 * @author Shreedhar Ganapathy
 * Date: November 07, 2003
 * @version $Revision$
 */
public interface Signal {
    /**
     * Signal is acquired prior to processing of the signal 
     * to protect group resources being
     * acquired from being affected by a race condition
     * Signal must be mandatorily acquired before any processing for recovery
     * operations.
     * @throws  SignalAcquireException
     */
    void acquire() throws SignalAcquireException;

    /**
     * Signal is released after processing of the signal to bring the 
     * group resources to a state of availability
     * Signal should be madatorily released after recovery process is
     * completed.
     * @throws SignalReleaseException
     */
    void release() throws SignalReleaseException;

    /**
     * returns the identity token of the member that caused this signal to be
     * generated.
     * For instance, in the case of a MessageSignal, this member token would be
     * the sender.
     * In the case of a FailureNotificationSignal, this member token would be
     * the failed member.
     * In the case of a JoinNotificationSignal or GracefulShutdownSignal, the
     * member token would be the member who joined or is being gracefully
     * shutdown, respectively.
     */
    String getMemberToken();

    /**
     * returns the details of the member who caused this Signal to be generated
     * returns a Map containing key-value pairs constituting data pertaining to
     * the member's details
     * @return Map  <Serializable, Serializable> 
     */
    public Map<Serializable, Serializable> getMemberDetails ( );

    /**
     * returns the group to which the member involved in the Signal belonged to
     * @return String
     */
    public String getGroupName( );

    /**
     * returns the start time of the member involved in this Signal.
     * @return  long - time stamp of when this member started
     */
    long getStartTime ();
}
