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
 * Specific to recovery related signal. This Signal type will only be
 * delivered to the corresponding Action (i.e. an Action of the
 * FailureRecoveryAction type) on only one of the servers.
 *
 * In other words, automatic recovery services which wish only one
 * server to perform the recovery, should register an ActionFactory
 * which produces only FailureRecoveryAction on all participating GMS
 * instances. The registration code is identical in all servers.
 * For any given failure, which one of the servers will be selected to
 * receive a FailureRecoverySignal is unique and depends on a function
 * defined on the consistent membership view provided by the underlying
 * group infrastructure.
 *
 * This Signal's acquire() and release() methods (defined in the parent
 * interface Signal) have special meaning and <strong>must</strong> be called
 * by the client before and after, respectively, performing any recovery
 * operations.
 *
 * The <code>acquire()</code> method does the following:
 * Enables the caller to raise a logical fence on a specified target member
 * token's component.
 * <p>Failure Fencing is a group-wide protocol that, on one hand, requires
 * members to update a shared/distributed datastructure if any of their
 * components need to perform operations on another members' corresponding
 * component. On the other hand, the group-wide protocol requires members
 * to observe "Netiquette" during their startup so as to check if any of
 * their components are being operated upon by other group members.
 * Typically this check is performed by the respective components
 * themselves. See the <code>GroupHandle.isFenced()</code> method for
 * this check.
 * When the operation is completed by the remote member component, it
 * removes the entry from the shared datastructure by calling release()
 * method.
 * <p>Raising the fence, places an entry into the
 * <code>DistributedStateCache</code> that is accessed by other members during
 * their startup to check for any fence.
 *
 * The release() method does the following :
 * Enables the caller to lower the logical fence that was earlier raised on
 * a target member component. This is done when the recovery operation being
 * performed on the target member component has now completed.
 *
 * @author Shreedhar Ganapathy
 * Date: Nov 10, 2003
 * @version $Revision$
 */
public interface FailureRecoverySignal extends FailureNotificationSignal{
    String getComponentName();

}


