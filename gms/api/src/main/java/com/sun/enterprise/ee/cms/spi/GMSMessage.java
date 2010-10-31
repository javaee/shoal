/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.ee.cms.spi;

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
        if (componentName == null) {
            throw new IllegalArgumentException("parameter componentName must be non-null");    
        }
        if (groupName == null) {
            throw new IllegalArgumentException("parameter groupName must be non-null");
        }

        // a componentName of null acts as a wildcard and the message is delivered to all
        // target components. see GroupHandle.sendMessage javadoc for details.
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
        if (startTime == null) {
            return 0;
        }
        return startTime;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(30);
        sb.append("GMSMessage to componentName:").append(componentName);
        sb.append(" message size:" + message.length);
        sb.append(" payload:");
        if (message.length < 30) {
            sb.append(new String(message));
        } else {
            sb.append(new String(message).substring(0,15));
        }
        sb.append(" group:").append(groupName);
        return sb.toString();
    }
}
