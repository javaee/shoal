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
        message=null;
        targetComponent=null;
        sender=null;
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
