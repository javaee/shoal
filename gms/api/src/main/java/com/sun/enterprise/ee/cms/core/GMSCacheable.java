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

package com.sun.enterprise.ee.cms.core;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Support class for DistributedStateCacheImpl. This class provides an
 * encapsulation for the details that represent a composite key for the
 * cache.
 *
 * @author Shreedhar Ganapathy
 *         Date: May 9, 2005
 * @version $Revision$
 */
public class GMSCacheable implements Serializable, Comparator {
    private final String componentName;
    private final String memberTokenId;
    private final Object key;
    private int hashCode = 0;

    public GMSCacheable(final String componentName, final String memberTokenId, final Serializable key) {
        this(componentName, memberTokenId, (Object)key);
    }

    public GMSCacheable(final String componentName, final String memberTokenId, final Object key) {
        if (componentName == null) {
            throw new IllegalArgumentException("GMSCacheable componentName must be non-null");
        }
        if (memberTokenId == null) {
            throw new IllegalArgumentException("GMSCacheable memberTokenId must be non-null");
        }
        if (key == null) {
            throw new IllegalArgumentException("GMSCacheable key must be non-null");
        }
        this.componentName = componentName;
        this.memberTokenId = memberTokenId;
        this.key = key;
    }

    public int compare(final Object o, final Object o1) {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = key.hashCode() * 17 + componentName.hashCode() + memberTokenId.hashCode();
        }
        return hashCode;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * We compare the contents of the GMSCacheable argument passed in with
     * the contents of this instance and determine if they are the same.
     *
     * @override
     */
    public boolean equals(final Object o) {
        boolean retval = false;
        boolean componentNameEqual = false;
        boolean memberTokenIdEqual = false;
        boolean keyEqual = false;

        if (o instanceof GMSCacheable) {
            if (this.componentName == null) {
                if (((GMSCacheable) o).componentName == null) {
                    componentNameEqual = true;
                }
            } else if (this.componentName.equals(((GMSCacheable) o).componentName)) {
                componentNameEqual = true;
            }
            if (this.memberTokenId == null) {
                if (((GMSCacheable) o).memberTokenId == null) {
                    memberTokenIdEqual = true;
                }
            } else if (this.memberTokenId.equals(((GMSCacheable) o).memberTokenId)) {
                memberTokenIdEqual = true;
            }
            if (this.key.equals(((GMSCacheable) o).key)) {
                keyEqual = true;
            }

            if (componentNameEqual && memberTokenIdEqual && keyEqual) {
                retval = true;
            }
        }
        return retval;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getMemberTokenId() {
        return memberTokenId;
    }

    public Object getKey() {
        return key;
    }

    public String toString() {
        return "GMSMember:" + memberTokenId + ":Component:" + componentName + ":key:" + key;
    }
}
