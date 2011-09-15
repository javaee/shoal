/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.shoal.jointest;

import com.sun.enterprise.ee.cms.core.*;
import com.sun.enterprise.ee.cms.impl.client.JoinNotificationActionFactoryImpl;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.UUID;
import java.util.Properties;

public class SimpleJoinTest {

    private static final Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);

    private final String group = "TestGroup";

    public static void main( String[] args ) {
        String serverName = null;
        if( args != null && args.length == 1 ) {
            serverName = args[0];
        }

        SimpleJoinTest check = new SimpleJoinTest();
        try {
            check.runSimpleSample( serverName );
        } catch( GMSException e ) {
            logger.log( Level.SEVERE, "Exception occured while joining group:" + e );
        }
    }

    private void runSimpleSample( String serverName ) throws GMSException {
        logger.log( Level.INFO, "Starting SimpleJoinTest...." );

        if( serverName == null || serverName.length() <= 0 )
            serverName = UUID.randomUUID().toString();;

        //initialize Group Management Service
        GroupManagementService gms = initializeGMS( serverName, group );

        //register for Group Events
        logger.log( Level.INFO, "Registering for group event notifications" );
        gms.addActionFactory( new JoinNotificationActionFactoryImpl( new JoinNotificationCallBack( serverName ) ) );

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

    private class JoinNotificationCallBack implements CallBack {

        private String serverName;

        public JoinNotificationCallBack( String serverName ) {
            this.serverName = serverName;
        }

        public void processNotification( Signal notification ) {
            if ( !( notification instanceof JoinNotificationSignal ) )
                logger.log( Level.SEVERE, "received unkown notification type:" + notification );
            logger.log( Level.INFO, "***JoinNotification received: ServerName = " + serverName + ", Signal.getMemberToken() = " + notification.getMemberToken() );
        }
    }
}

