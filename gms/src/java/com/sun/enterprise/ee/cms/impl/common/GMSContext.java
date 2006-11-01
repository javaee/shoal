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

import com.sun.enterprise.ee.cms.core.DistributedStateCache;
import com.sun.enterprise.ee.cms.core.GMSConstants;
import com.sun.enterprise.ee.cms.core.GMSException;
import com.sun.enterprise.ee.cms.core.GroupHandle;
import com.sun.enterprise.ee.cms.spi.GroupCommunicationProvider;

import java.util.List;

/**
 * Provides contextual information about all useful GMS artifacts. These are
 * GMS objects that are tied to a particular group identity and thus scoped
 * to provide information within the group's context. There can be as many
 * GMSContext objects as there are groups within a single JVM process.
 *
 * @author Shreedhar Ganapathy
 * Date: Jan 12, 2004
 * @version $Revision$
 */
public interface GMSContext {
    /**
     * returns the serverIdentityToken pertaining to the process that
     * owns this GMS instance
     * @return java.lang.String
     */
    public String getServerIdentityToken();

    /**
     * returns the name of the group this context represents
     */
    public String getGroupName();

    /**
     * returns Group handle
     * @return Group handle
     */
    GroupHandle getGroupHandle();

    /**
     * returns the router
     * @return router
     */
    Router getRouter();

    ViewWindow getViewWindow(); 

    DistributedStateCache getDistributedStateCache();

    void join() throws GMSException ;

    void leave(final GMSConstants.shutdownType shutdownType) ;
    
    boolean isShuttingDown ();

    long getStartTime();

    void announceGroupShutdown (final String groupName,
                           final GMSConstants.shutdownState shutdownState );
    
    boolean addToSuspectList( final String token );

    void removeFromSuspectList( final String token );

    boolean isSuspected ( final String token );

    List<String> getSuspectList();

    ShutdownHelper getShutdownHelper ();

    GroupCommunicationProvider getGroupCommunicationProvider();
}
