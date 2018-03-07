/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

import com.sun.enterprise.ee.cms.spi.MemberStates;

import java.util.List;

/**
 * Signal corresponding to JoinNotificationAction. This Signal enables the
 * consumer to get specifics about a Join notification. This Signal type
 * will only be passed to a JoinNotificationAction.  This Signal
 * is delivered to registered GMS Clients on all members of the group.
 * @author Shreedhar Ganapathy
 *         Date: Feb 3, 2005
 * @version $Revision$
 */
public interface JoinNotificationSignal extends Signal,
    GroupStartupNotificationSignal, RejoinableEvent {
    
    /**
     * provides a list of all live and current CORE designated members.
     *
     * @return List containing the list of member token ids of core members
     */
    List<String> getCurrentCoreMembers();

    /**
     * provides a list of all live members i.e. CORE and SPECTATOR members.
     * @return List containing the list of member token ids of all members.
     */
    List<String> getAllCurrentMembers();

    /**
     * Provides the current liveness state of the member whose joining the group is being
     * signaled by this JoinNotification Signal. The state corresponds to one of the states enumerated
     * by the MemberStates enum
     * @return MemberStates
     */
    MemberStates getMemberState(); 
}
