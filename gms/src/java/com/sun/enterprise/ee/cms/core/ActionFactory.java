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
 * Produces Action instances. This pattern enables implementors 
 * to provide Action instances only when called, along with the
 * flexibility to produce Actions from either a pool or by creating 
 * a new instance each time or by using a Singleton, etc.
 *
 * The method <code>produceAction</code> will be called before delivery
 * of Signal to the Action "produced."
 *
 * Each instance of an Action is guaranteed to be served an instance 
 * of Signal on a separate thread.
 *
 * If the ActionFactory always returns the same Action instance, that
 * instance should be written to take into account the possibility of multiple
 * threads delivering distinct signals.
 *
 * @author Shreedhar Ganapathy
 * Date: Jan 13, 2004
 * @version $Revision$
 */
public interface ActionFactory {
    /**
     * Produces an Action instance.
     * @return com.sun.enterprise.ee.cms.Action
     */
    Action produceAction();
}

