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

import java.util.List;

/**
 * Signal corresponding to GroupLeadershipNotificationAction. This Signal enables the
 * consumer to get specifics about a GroupLeadership notification. This Signal type
 * will only be passed to a GroupLeadershipNotificationAction.  This Signal
 * is delivered to registered GMS Clients on all members of the group.
 *
 * @author Bongjae Chang
 * @Date: June 25, 2008
 */
public interface GroupLeadershipNotificationSignal extends Signal {
    /**
     * provides a list of the previous view's snapshot at time signal arrives.
     *
     * @return List containing the list of <code>GMSMember</code>s which are corresponding to the view
     */
    List<GMSMember> getPreviousView();

    /**
     * provides a list of the current view's snapshot at time signal arrives.
     *
     * @return List containing the list of <code>GMSMember</code>s which are corresponding to the view
     */
    List<GMSMember> getCurrentView();
    
    /**
     * provides a list of all live and current CORE designated members.
     *
     * @return List containing the list of member token ids of core members
     */
    List<String> getCurrentCoreMembers();

    /**
     * provides a list of all live members i.e. CORE and SPECTATOR members.
     *
     * @return List containing the list of member token ids of all members
     */
    List<String> getAllCurrentMembers();
}