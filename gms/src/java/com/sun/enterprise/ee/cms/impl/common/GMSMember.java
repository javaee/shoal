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
package com.sun.enterprise.ee.cms.impl.common;

import java.io.Serializable;

/**
 * Encapsulates the member token and the member type in a serializable
 * @author Shreedhar Ganapathy
 *         Date: Mar 16, 2005
 * @version $Revision$
 */
public class GMSMember implements Serializable{
    private final String memberToken;
    private final String memberType;
    private long id;
    private final String groupName;
    private final Long startTime;

    /**
     * Constructor
     */
    public GMSMember (final String memberToken, 
                      final String memberType, 
                      final String groupName, 
                      final Long startTime){

        this.memberToken = memberToken;
        this.memberType = memberType;
        this.groupName = groupName;
        this.startTime = startTime;
    }

    /**
     * returns the member token
     * @return String member token
     */
    public String getMemberToken(){
        return memberToken;
    }

    /**
     * returns the member type
     * @return String member type
     */
    public String getMemberType(){
        return memberType;
    }

    public void setSnapShotId ( final long id ) {
        this.id = id;
    }

    public long getSnapShotId(){
        return id;
    }

    public String getGroupName(){
        return groupName;
    }

    public long getStartTime(){
        return startTime.longValue();
    }
}
