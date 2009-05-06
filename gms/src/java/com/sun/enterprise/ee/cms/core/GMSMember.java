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

import java.io.Serializable;
import static com.sun.enterprise.ee.cms.core.GroupManagementService.MemberType.WATCHDOG;
import static com.sun.enterprise.ee.cms.core.GroupManagementService.MemberType.CORE;

/**
 * Encapsulates the member token and the member type in a serializable
 *
 * @author Shreedhar Ganapathy
 *         Date: Mar 16, 2005
 * @version $Revision$
 */
public class GMSMember implements Serializable {
    private final String memberToken;
    private final String memberType;
    private long id;
    private final String groupName;
    private final Long startTime;

    /**
     * Constructor
     */
    public GMSMember(final String memberToken,
                     final String memberType,
                     final String groupName,
                     final Long startTime) {

        this.memberToken = memberToken;
        this.memberType = memberType;
        this.groupName = groupName;
        this.startTime = startTime;
    }

    /**
     * returns the member token
     *
     * @return String member token
     */
    public String getMemberToken() {
        return memberToken;
    }

    /**
     * returns the member type
     *
     * @return String member type
     */
    public String getMemberType() {
        return memberType;
    }

    public void setSnapShotId(final long id) {
        this.id = id;
    }

    public long getSnapShotId() {
        return id;
    }

    public String getGroupName() {
        return groupName;
    }

    /**
     * Returns the time the member joined the group.
     * @return the time the member joined the group
     */
    public long getStartTime() {
        return startTime;
    }

    public boolean isWatchDog() {
        return WATCHDOG.toString().equalsIgnoreCase(memberType);
    }

    public boolean isCore() {
        return CORE.toString().equalsIgnoreCase(memberType);
    }

    public String toString() {
        return "name:" + memberToken + " group:" + groupName + " MemberType:" + memberType + " startTime:" + startTime;
    }
}
