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

    public GMSCacheable(final String componentName, final String memberTokenId, final Serializable key) {
        this.componentName = componentName;
        this.memberTokenId = memberTokenId;
        this.key = key;
    }

    public GMSCacheable(final String componentName, final String memberTokenId, final Object key) {
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
        return key.hashCode() * 17 + componentName.toLowerCase().hashCode() + memberTokenId.toLowerCase().hashCode();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * We compare the contents of the GMSCacheable argument passed in with
     * the contents of this instance and determine if they are the same.
     * @override
     */
    public boolean equals(final Object o) {
        boolean retval = false;
        boolean componentNameEqual = false;
        boolean memberTokenIdEqual = false;
        boolean keyEqual = false;

        if(o instanceof GMSCacheable){
            if (this.componentName == null) {
                if (((GMSCacheable)o).componentName == null) {
                    componentNameEqual = true;
                }
            } else if (this.componentName.equals(((GMSCacheable)o).componentName)) {
                componentNameEqual = true;
            }
            if (this.memberTokenId == null) {
                if (((GMSCacheable) o).memberTokenId == null) {
                    memberTokenIdEqual = true;
                }
            } else if (this.memberTokenId.equals(((GMSCacheable)o).memberTokenId)) {
                memberTokenIdEqual = true;
            }
            if (this.key.equals(((GMSCacheable)o).key)) {
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
