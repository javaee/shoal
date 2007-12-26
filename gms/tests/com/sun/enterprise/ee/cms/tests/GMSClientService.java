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

 package com.sun.enterprise.ee.cms.tests;

import com.sun.enterprise.ee.cms.core.*;
import com.sun.enterprise.ee.cms.impl.client.*;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import static java.lang.Thread.sleep;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

 /**
 * This is a mock object that exists to demonstrate a GMS client.
 * This client is started by the mock ApplicationServer object which results in
 * this client retrieving an instance of GroupManagementService in order to
 * register action factories for its notification purposes.
 * It is assumed that this client module has already implemented the relevant
 * Action and ActionFactory pair for the particular notification requirements.
 * For Example, if this client requires Failure Notifications, it should
 * implement FailureNotificationActionFactory and FailureNotificationAction
 * and register the FailureNotificationActionFactory.
 *
 * @author Shreedhar Ganapathy
 *         Date: Mar 1, 2005
 * @version $Revision$
 */
public class GMSClientService implements Runnable, CallBack{
    private GroupManagementService gms;
     private Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    private Thread flag;
    private String memberToken;
    private boolean sendMessages;
    private String serviceName;
    private static final int MILLIS = 4000;
    public static final String IIOP_MEMBER_DETAILS_KEY = "IIOPListenerEndPoints";

    public GMSClientService(final String serviceName,
                            final String memberToken,
                            final boolean sendMessages){
        this.sendMessages = sendMessages;
        this.serviceName = serviceName;
        this.memberToken = memberToken;
        try {
            gms = GMSFactory.getGMSModule();
        }
        catch ( GMSException e ) {
            logger.log(Level.WARNING, e.getLocalizedMessage());
        }
        gms.addActionFactory(new PlannedShutdownActionFactoryImpl(this));
        gms.addActionFactory(new JoinNotificationActionFactoryImpl(this));
        gms.addActionFactory(new FailureNotificationActionFactoryImpl(this));
        gms.addActionFactory(serviceName,
                             new FailureRecoveryActionFactoryImpl(this));
        gms.addActionFactory(new MessageActionFactoryImpl(this), serviceName);
        gms.addActionFactory(new JoinedAndReadyNotificationActionFactoryImpl(this));
    }

    public void start(){
        flag = new Thread(this, "GMSClient:"+serviceName);
        flag.start();
    }
    
    public void run() {
        GroupHandle gh = gms.getGroupHandle();
        while((gh.isFenced( serviceName, memberToken))){
            try {
                logger.log(Level.FINEST, "Waiting for fence to be lowered");
                sleep(2000);
            }
            catch ( InterruptedException e ) {
                logger.log(Level.WARNING, "Thread interrupted:"+
                        e.getLocalizedMessage());
            }
        }
        logger.log(Level.INFO, serviceName+":"+memberToken+
                               ": is not fenced now, starting GMSclient:"+
                               serviceName);
        logger.log(Level.INFO, "DUMPING:"+
                   gms.getAllMemberDetails(IIOP_MEMBER_DETAILS_KEY));
/*        final Thread thisThread = Thread.currentThread();

        //if this client is being stopped by the parent thread through call
        // to stopClient(), this flag will be null.
        while(flag == thisThread)
        {
            try {
                sleep(10000);
                if(sendMessages)
                {
                    logger.log(Level.INFO,"Sending 10 messages");
                    for(int i=1; i<=10; i++){
                        try {
                           gms.getGroupHandle().getDistributedStateCache()
                                   .addToCache(serviceName,memberToken, "Message "+i,
                                   MessageFormat.format("Message {0} from {1}", i, serviceName));
                        } catch (GMSException e) {
                            logger.log(Level.INFO, e.getMessage());
                            break;
                        }
                    }
                }
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, e.getMessage());
            }
        } */
    }

    public synchronized void processNotification(final Signal notification){
        final String serverToken;
        logger.log(Level.FINEST, new StringBuffer().append(serviceName)
                .append(": Notification Received from:")
                .append(notification.getMemberToken())
                .append(":[")
                .append(notification.toString())
                .append("] has been processed")
                .toString());
        if( notification instanceof FailureRecoverySignal)
        {
            try {
                notification.acquire();
                extractMemberDetails( notification, notification.getMemberToken() );
                sleep(MILLIS);
                notification.release();
            }
            catch ( SignalAcquireException e ) {
                logger.log(Level.WARNING, e.getLocalizedMessage());
            }
            catch ( SignalReleaseException e ) {
                logger.log(Level.INFO, e.getLocalizedMessage());
            }
            catch ( InterruptedException e ) {
                logger.log(Level.INFO, e.getLocalizedMessage());
            }
        }
        else if(notification instanceof JoinNotificationSignal)
        {
            serverToken =
                    notification.getMemberToken();
            logger.info("Received JoinNotificationSignal for member "+serverToken+
             " with state set to "+((JoinNotificationSignal)notification).getMemberState().toString());
            extractMemberDetails( notification, serverToken );

        }
        else if(notification instanceof JoinedAndReadyNotificationSignal ||
                notification instanceof FailureNotificationSignal ||
                notification instanceof PlannedShutdownSignal ||
                notification instanceof FailureSuspectedSignal)
        {
            serverToken =
                    notification.getMemberToken();
            extractMemberDetails( notification, serverToken );

        }
    }

    private void extractMemberDetails (
            final Signal notification, final String serverToken ) {
        logger.log(Level.FINEST, serviceName+":Now getting member details...");
        final Map memberDetails =
                notification.getMemberDetails();
        if(memberDetails.size() ==0){
            logger.log(Level.FINEST, "No Details available");
        }
        else{
            logger.log(Level.FINEST, memberDetails.toString());
            for(Object key : memberDetails.keySet()){
                logger.log(Level.FINEST,
                           new StringBuffer()
                           .append( "Got Member Details for " )
                           .append( serverToken ).toString());
                logger.log(Level.FINEST,
                           new StringBuffer().append( "Key:" )
                           .append( key )
                           .append( ":Value:" )
                           .append( memberDetails.get( key )
                                    .toString() )
                           .toString() );
            }
        }
    }

    public void stopClient(){
        flag = null;
    }
}
