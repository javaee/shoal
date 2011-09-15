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
    static final long serialVersionUID = -3594369933952520038L;

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
