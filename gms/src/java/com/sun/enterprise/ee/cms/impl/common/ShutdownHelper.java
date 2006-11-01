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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Provides support for maintaining information about an impending shutdown
 * announcement either in an instance context or in a group context. An instance
 * of this object is consulted for distinguishing an abnormal failure from a
 * planned shutdown resulting in a failure notification from heart beat agents.
 * Also consulted for determining if a member being suspected has already
 * announced shutdown or there is a group shutdown.
 * @author Shreedhar Ganapathy
 *         Date: Sep 21, 2005
 * @version $Revision$
 */
public class ShutdownHelper {
    private List<String> gracefulShutdownList = new Vector<String>();
    private List<String> groupShutdownList = new ArrayList<String>();

    public ShutdownHelper () {

    }

    public synchronized boolean isGroupBeingShutdown( final String groupName ){
        return groupShutdownList.contains( groupName );
    }

    public synchronized boolean isMemberBeingShutdown( final String memberToken ) {
        return gracefulShutdownList.contains( memberToken );
    }

    public void addToGroupShutdownList( final String groupName ) {
       synchronized( groupShutdownList) {
           groupShutdownList.add( groupName );
       }
    }

    public void addToGracefulShutdownList ( final String memberToken){
        synchronized(gracefulShutdownList){
            gracefulShutdownList.add( memberToken );
        }
    }

    public void removeFromGracefulShutdownList( final String memberToken ) {
        synchronized(gracefulShutdownList){
            gracefulShutdownList.remove( memberToken );
        }
    }

    public void removeFromGroupShutdownList( final String groupName ){
        synchronized( groupShutdownList ){
            groupShutdownList.remove( groupName );
        }
    }
}
