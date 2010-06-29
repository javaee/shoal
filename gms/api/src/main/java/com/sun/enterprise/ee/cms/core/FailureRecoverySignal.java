/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
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


