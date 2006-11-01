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
            if(gmsContextProvider.equals(
                    GMSConstants.DEFAULT_GROUP_COMMUNICATION_PROVIDER))
            {
                ctx =  new com.sun.enterprise.ee.cms.impl.jxta.GMSContext(
                            serverToken, groupName, memberType, properties);
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
