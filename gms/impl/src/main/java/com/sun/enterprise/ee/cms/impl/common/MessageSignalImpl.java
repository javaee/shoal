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

package com.sun.enterprise.ee.cms.impl.common;

import com.sun.enterprise.ee.cms.core.MessageSignal;
import com.sun.enterprise.ee.cms.core.SignalAcquireException;
import com.sun.enterprise.ee.cms.core.SignalReleaseException;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;

/**
 * Implements MessageSignal and provides methods to access message sent by a
 * remote member.
 * @author Shreedhar Ganapathy
 * Date: Jan 20, 2004
 * @version $Revision$
 */
public class MessageSignalImpl implements MessageSignal {
    private byte[] message;
    private String targetComponent;
    private String sender;
    private String groupName;
    private long startTime;

    public MessageSignalImpl(final byte[] message, 
                             final String targetComponent, 
                             final String sender,
                             final String groupName,
                             final long startTime) {
        this.message=message;
        this.targetComponent=targetComponent;
        this.sender=sender;
        this.groupName = groupName;
        this.startTime = startTime;
    }

    /**
     * Returns the target component in this member to which this
     * message is addressed.
     * @return String targetComponent
     */
    public String getTargetComponent(){
        return targetComponent;
    }
    
           
    /**
     * Returns the message(payload) as a byte array.
     * @return byte[]
     */
    public byte[] getMessage() {
        return message;
    }
    
    /**
     * Signal is acquired prior to processing of the signal
     * to protect group resources being
     * acquired from being affected by a race condition
     * @throws  com.sun.enterprise.ee.cms.core.SignalAcquireException
     */
    public void acquire() throws SignalAcquireException {
    }

    /**
     * Signal is released after processing of the signal to bring the
     * group resources to a state of availability
     * @throws com.sun.enterprise.ee.cms.core.SignalReleaseException
     */
    public void release() throws SignalReleaseException {
        // JMF:  do not release message resources.
        //       was a bug when sending a message to null TargetComponent and more than one target component was registered.
         
        //message=null;
        //targetComponent=null;
        //sender=null;
    }

    public String getMemberToken() {
        return sender;
    }

    public Map<Serializable, Serializable> getMemberDetails () {
        return new Hashtable<Serializable, Serializable>();
    }
    
    /**
     * returns the group to which the member involved in the Signal belonged to
     * @return String
     */
    public String getGroupName( ){
        return groupName;        
    }

    public long getStartTime () {
        return startTime;
    }
}
