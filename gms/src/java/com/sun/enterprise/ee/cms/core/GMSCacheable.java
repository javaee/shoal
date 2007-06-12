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
