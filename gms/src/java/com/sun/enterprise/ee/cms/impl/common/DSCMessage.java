/*
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
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

import com.sun.enterprise.ee.cms.core.GMSCacheable;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a wrapper Serializable dedicated to support the DistributedStateCache
 * such that the message ncapsulates the operation for which the message is
 * intended at the receivers end.
 *
 * @author Shreedhar Ganapathy
 *         Date: May 9, 2005
 * @version $Revision$
 */
public class DSCMessage implements Serializable {

    public static enum OPERATION { ADD, REMOVE, ADDALLLOCAL,
        ADDALLREMOTE, REMOVEALL }

    private GMSCacheable key;
    private Object value;
    private String operation;
    private ConcurrentHashMap<GMSCacheable, Object> cache;
    private boolean isCoordinator = false;

    /**
     * This constructor expects a GMSCacheable object representing the
     * composite key comprising component, member id, and the state specific key,
     * followed by the value. The value object should strictly be only a byte[]
     * or a Serializable Object.
     * @param key
     * @param value
     * @param operation
     */
    public DSCMessage( final GMSCacheable key, final Object value,
                       final String operation){
        this.key = key;
        if(value instanceof Serializable){
            this.value = value;
        }
        else if( value instanceof byte[]){ //TODO:This instanceof could be suspect. Revisit 
            this.value = value;
        }
        else {
            this.value = null;
        }
        this.operation = operation;
    }

    public DSCMessage( final ConcurrentHashMap<GMSCacheable, Object> cache,
                       final String operation, final boolean isCoordinator ){
        this.cache = cache ;
        this.operation = operation;
        this.isCoordinator = isCoordinator;
    }

    public GMSCacheable getKey () {
        return key;
    }

    public Object getValue () {
        return value;
    }

    public String getOperation () {
        return operation;
    }

    public ConcurrentHashMap<GMSCacheable, Object> getCache () {
        return cache;
    }

    public boolean isCoordinator () {
        return isCoordinator;
    }
}
