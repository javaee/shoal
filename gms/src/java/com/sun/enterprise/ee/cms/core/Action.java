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
 * Base interface to denote an Action.
 * An Action consumes a Signal which represents an Event.
 * An Action instance is produced by an ActionFactory.
 * All Action types inherit this base Action interface. Action
 * subtypes are defined in conjunction with a corresponding Signal
 * subtype and ActionFactory.
 * For each specific event, Users of the system implement
 * the corresponding ActionFactory type and Action type.
 * Users of the system register an ActionFactory type for a given
 * type of Signal they wish to receive.
 *
 * @author Shreedhar Ganapathy
 * Date: November 07, 2004
 * @version $Revision$
 */
public interface Action {

    /**
     * Implementations of consumeSignal should strive to return control 
     * promptly back to the thread that has delivered the Signal.
     * @param signal A Signal specifying a particular event in the group
     * @throws ActionException thrown when a exception condition occurs
     * wihle the Signal is consumed.
     */
    void consumeSignal(Signal signal) throws ActionException;
}
