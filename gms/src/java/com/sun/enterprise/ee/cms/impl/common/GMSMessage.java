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
 * This is a wrapper Serializable so that a message sent to a remote member can 
 * be further filtered to a target component in that remote member.
 * @author Shreedhar Ganapathy
 *         Date: Mar 14, 2005
 * @version $Revision$
 */

public class GMSMessage implements Serializable {
    private final String componentName;
    private final byte[] message;
    private final String groupName;
    private final Long startTime;

    public GMSMessage(final String componentName,
                      final byte[] message,
                      final String groupName, 
                      final Long startTime){
        this.componentName=componentName;
        this.message=message;
        this.groupName = groupName;
        this.startTime = startTime;
    }

    public String getComponentName(){
        return componentName;
    }

    public byte[] getMessage(){
        return message;
    }

    public String getGroupName(){
        return groupName;
    }

    public long getStartTime () {
        return startTime;
    }
}
