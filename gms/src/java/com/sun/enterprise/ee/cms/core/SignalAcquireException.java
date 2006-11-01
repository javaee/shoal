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

/**
 * Raises exceptions occuring while acquiring signals.
 * If such an exception is raised avoidance of deadlocks on 
 * group resources will not be guaranteed.
 *
 * For example, if <code>FailureRecoverySignal</code> throws a <code>SignalAcquireException</code>,
 * it means that the failed server has returned to operation or that
 * it may not be possible to fence it out of the group.
 *
 * @author Shreedhar Ganapathy
 * Date: Jan 8, 2004
 * @version $Revision$
 */
public class SignalAcquireException extends Exception{
    public SignalAcquireException(){
        super();
    }

    public SignalAcquireException(final String message){
        super(message);
    }

    public SignalAcquireException(final Exception e) {
        super(e);
    }
}
