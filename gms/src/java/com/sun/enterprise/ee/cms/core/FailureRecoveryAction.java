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
  * Action type corresponding to a recovery oriented action.Implementations
 * consume a FailureRecoverySignal.
  * The acquisition of Signal corresponding to this Action ensures failure 
  * fencing, i.e. the failed server will not be allowed to join the group
  * once the Signal's <code>acquire</code> has been called and as long as this
  * Action has not called release on the Signal delivered
  * to it. The FailureRecoverySignal is delivered to the FailureRecoveryAction
  * instance on only one server that is selected by its GMS instance in a
  * distributed-consistent algorithm. 
  *
  *
  * @author Shreedhar Ganapathy
  * Date: ${DATE}
  * @version $Revision$
 */
public interface FailureRecoveryAction extends Action {

}
