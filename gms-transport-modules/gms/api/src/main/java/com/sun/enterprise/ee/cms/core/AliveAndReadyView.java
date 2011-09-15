/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.ee.cms.core;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import com.sun.enterprise.ee.cms.core.Signal;


/**
 * A read-only view consisting of all the AliveAndReady CORE members of a GMS group.
 *
 * The GMS notification signals of JoinedAndReadyNotificationSignal, FailureNotificationSignal and PlannedShutdownSignal
 * transition from one of these views to the next view. When one of these signal occurs, the current view is terminated
 * by setting its signal.  While the view's signal is null, it is considered the current view. Once a terminating signal
 * occurs, than this view is considered the previous view and getSignal() returns the GMS notification that caused this 
 * view to conclude.
 */
public interface AliveAndReadyView {

    /**
     * These are members of this view BEFORE the GMS notification signal that terminated
     * this view as being the current view.
     *
     * @return an unmodifiable list of sorted CORE members who are alive and ready.
     *
     */
    SortedSet<String> getMembers();

    /**
     *
     * @return signal that caused transition from this view. returns null when this is the current view
     *         and no signal has occurred to cause a transition to the next view.
     */
     Signal getSignal();


    /**
     *
     * @return time this view ceased being the current view when its signal was set.
     */
    long getSignalTime();

    /**
     * Monotonically increasing id.  Each GMS notification signal for a core member that causes a new view to be created
     * results in this value being increased.
     * @return a generated id
     */
    long getViewId();

    /**
     *
     * @return duration in milliseconds that this view is/was the current view.
     *          If <code>getSignal</code> is null, this value is still growing each time this method is called.
     */
    long getViewDuration();


    /**
     *
     * @return time that this view got created.
     */
    long getViewCreationTime();
}
