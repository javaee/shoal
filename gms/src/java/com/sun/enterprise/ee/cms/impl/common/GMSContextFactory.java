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

package com.sun.enterprise.ee.cms.impl.common;

import com.sun.enterprise.ee.cms.core.GMSConstants;
import com.sun.enterprise.ee.cms.core.GroupManagementService;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Produces and retains the GMSContext for the lifetime of the GMS instance
 * @author Shreedhar Ganapathy
 * Date: Jan 16, 2004
 * @version $Revision$
 */
public class GMSContextFactory {
    private static final Map<String, GMSContext> ctxCache =
                                new HashMap<String, GMSContext>();

    private GMSContextFactory () { }

    //TODO: Shreedhar's comment: The invocation of appropriate provider's context has got to get better
    static GMSContext produceGMSContext(final String serverToken,
                            final String groupName,
                            final GroupManagementService.MemberType memberType,
                            final Properties properties){
        GMSContext ctx;
        final String gmsContextProvider = GMSConstants.GROUP_COMMUNICATION_PROVIDER;
        if((ctx = ctxCache.get( groupName )) ==  null){
            if( gmsContextProvider.equalsIgnoreCase( GMSConstants.DEFAULT_GROUP_COMMUNICATION_PROVIDER ) ) {
                ctx = new com.sun.enterprise.ee.cms.impl.jxta.GMSContext( serverToken, groupName, memberType, properties );
            } else if( gmsContextProvider.equalsIgnoreCase( GMSConstants.GRIZZLY_GROUP_COMMUNICATION_PROVIDER ) ||
                       gmsContextProvider.equalsIgnoreCase( GMSConstants.JXTA_NEW_GROUP_COMMUNICATION_PROVIDER ) ) {
                ctx = new com.sun.enterprise.ee.cms.impl.base.GMSContextImpl( serverToken, groupName, memberType, properties );
            } else {
                ctx = new com.sun.enterprise.ee.cms.impl.jxta.GMSContext( serverToken, groupName, memberType, properties );
            }
            ctxCache.put(groupName, ctx);
        }
        return ctx;
    }

    public static GMSContext getGMSContext( final String groupName ){
        return ctxCache.get(groupName);
    }

    public static void removeGMSContext ( final String groupName ) {
        ctxCache.remove( groupName );
    }
}
