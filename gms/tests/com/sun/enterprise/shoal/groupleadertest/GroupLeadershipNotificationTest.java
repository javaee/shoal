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

package com.sun.enterprise.shoal.groupleadertest;

import com.sun.enterprise.ee.cms.core.*;
import com.sun.enterprise.ee.cms.impl.client.GroupLeadershipNotificationActionFactoryImpl;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.UUID;
import java.util.Properties;

public class GroupLeadershipNotificationTest {

    private final static Logger logger = Logger.getLogger( "GroupLeaderShipNotificationTest" );

    private final String group = "TestGroup";

    public static void main( String[] args ) {
        GroupLeadershipNotificationTest check = new GroupLeadershipNotificationTest();
        try {
            check.runSimpleSample();
        } catch( GMSException e ) {
            logger.log( Level.SEVERE, "Exception occured while joining group:" + e );
        }
    }

    private void runSimpleSample() throws GMSException {
        logger.log( Level.INFO, "Starting GroupLeaderShipNotificationTest...." );

        String serverName = UUID.randomUUID().toString();

        //initialize Group Management Service
        GroupManagementService gms = initializeGMS( serverName, group );

        //register for Group Events
        logger.log( Level.INFO, "Registering for group event notifications" );
        gms.addActionFactory( new GroupLeadershipNotificationActionFactoryImpl( new GroupLeadershipNotificationTest.GroupLeaderShipNotificationCallBack( serverName ) ) );

        //join group
        logger.log( Level.INFO, "Joining Group " + group );
        gms.join();

        //leaveGroupAndShutdown( serverName, gms );
    }

    private GroupManagementService initializeGMS( String serverName, String groupName ) {
        logger.log( Level.INFO, "Initializing Shoal for member: " + serverName + " group:" + groupName );
        return (GroupManagementService)GMSFactory.startGMSModule( serverName,
                                                                  groupName,
                                                                  GroupManagementService.MemberType.CORE,
                                                                  //null ); // Now if properties is null, NPE occurred.
                                                                  new Properties() );
    }

    private void leaveGroupAndShutdown( String serverName, GroupManagementService gms ) {
        logger.log( Level.INFO, "Shutting down gms " + gms + "for server " + serverName );
        gms.shutdown( GMSConstants.shutdownType.INSTANCE_SHUTDOWN );
    }

    private class GroupLeaderShipNotificationCallBack implements CallBack {

        private String serverName;

        public GroupLeaderShipNotificationCallBack( String serverName ) {
            this.serverName = serverName;
        }

        public void processNotification( Signal notification ) {
            if( !( notification instanceof GroupLeadershipNotificationSignal ) ) {
                logger.log( Level.SEVERE, "received unkown notification type:" + notification );
                return;
            }
            GroupLeadershipNotificationSignal groupLeadershipNotification = (GroupLeadershipNotificationSignal)notification;
            GroupManagementService gms = null;
            try {
                gms = (GroupManagementService)GMSFactory.getGMSModule();
            } catch( GMSException e ) {
                e.printStackTrace();
                return;
            }
            GroupHandle groupHandle = gms.getGroupHandle();
            logger.log( Level.INFO,
                        "***GroupLeaderShipNotification received: GroupLeader = " + groupHandle.isGroupLeader() +
                        ", Signal.getMemberToken() = " + groupLeadershipNotification.getMemberToken() +
                        ", Signal.getGroupName() = " + groupLeadershipNotification.getGroupName() +
                        ", Signal.getPreviousView() = " + groupLeadershipNotification.getPreviousView() +
                        ", Signal.getCurrentView() = " + groupLeadershipNotification.getCurrentView() +
                        ", Signal.getCurrentCoreMembers() = " + groupLeadershipNotification.getCurrentCoreMembers() +
                        ", Signal.getAllCurrentMembers() = " + groupLeadershipNotification.getAllCurrentMembers() +
                        ", ServerName = " + serverName +
                        ", Leader = " + groupHandle.getGroupLeader() );
        }
    }
}
