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
package com.sun.enterprise.ee.cms.tests;

import com.sun.enterprise.ee.cms.core.*;
import com.sun.enterprise.ee.cms.impl.common.GroupManagementServiceImpl;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import com.sun.enterprise.jxtamgmt.JxtaUtil;
import com.sun.enterprise.jxtamgmt.NiceLogFormatter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is an example used to demonstrate the application layer that controls the
 * lifecycle of the GMS module. It also provides an example of the actions taken
 * in response to a recover call from the GMS layer.
 * @author Shreedhar Ganapathy"
 * @version $Revision$
 */
public class ApplicationServer implements Runnable{
    private static final Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);

    private GroupManagementService gms = null;
    private GMSClientService gcs1;
    private GMSClientService gcs2;
    private String serverName;
    public ApplicationServer(final String serverName, final String groupName,
                             final GroupManagementService.MemberType memberType,
                             final Properties props)
    {
        this.serverName = serverName;
        gms = (GroupManagementService) GMSFactory.startGMSModule(serverName, groupName,
                                                        memberType, props);
        initClientServices(Boolean.valueOf(
                System.getProperty("MESSAGING_MODE", "true")));
    }

    private void initClientServices (boolean sendMessages) {
        gcs1 = new GMSClientService("EJBContainer", serverName, sendMessages);
        gcs2 = new GMSClientService("TransactionService", serverName, false);
    }

    private static void setupLogHandler() {
        final ConsoleHandler consoleHandler = new ConsoleHandler();
        try {
            consoleHandler.setLevel(Level.ALL);
            consoleHandler.setFormatter(new NiceLogFormatter());
        } catch( SecurityException e ) {
            new ErrorManager().error(
                 "Exception caught in setting up ConsoleHandler ",
                 e, ErrorManager.GENERIC_FAILURE );
        }
        logger.addHandler(consoleHandler);
        logger.setUseParentHandlers(false);
        final String level = System.getProperty("LOG_LEVEL","FINEST");
        logger.setLevel(Level.parse(level));
    }

    public void run() {
        startGMS();
        addMemberDetails();
        startClientServices();
        try {
            Thread.sleep(Integer.parseInt(System.getProperty("LIFEINMILLIS",
                                                             "15000")));
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage());
        }
        stopClientServices();
        stopGMS();
        System.exit(0);
    }

    private void addMemberDetails () {
        final Map<Object, Object> details  = new Hashtable<Object, Object>();
        final ArrayList<ArrayList> ar1 = new ArrayList<ArrayList>();
        final ArrayList<String> ar2 = new ArrayList<String>();
        final int port = 3700 ;
        final int port1 = 3800;
        try {
            ar2.add(InetAddress.getLocalHost()+":"+port);
            ar2.add(InetAddress.getLocalHost()+":"+port1);
        }
        catch ( UnknownHostException e ) {
            logger.log(Level.WARNING, e.getLocalizedMessage());
        }
        ar1.add( ar2);
        details.put(GMSClientService.IIOP_MEMBER_DETAILS_KEY, ar1);
        try {
            ((GroupManagementServiceImpl)gms).setMemberDetails( serverName, details );
        }
        catch ( GMSException e ) {
            logger.log(Level.WARNING, e.getLocalizedMessage());
        }
    }

    public void startClientServices() {
        logger.log(Level.FINE,"ApplicationServer: Starting GMSClient");
        gcs1.start();
        gcs2.start();
    }

    public void startGMS(){
        logger.log(Level.FINE,"ApplicationServer: Starting GMS service");
        final Thread gservice = new Thread((Runnable)gms, "GMSThread");
        gservice.start();

        try {
            gservice.join();//wait for this thread to die
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage());
        }
    }

    public void stopGMS() {
        logger.log(Level.FINE,"ApplicationServer: Stopping GMS service");
        gms.shutdown(GMSConstants.shutdownType.INSTANCE_SHUTDOWN);
    }

    private void stopClientServices() {
        logger.log(Level.FINE,"ApplicationServer: Stopping GMSClient");
        gcs1.stopClient();
        gcs2.stopClient();
    }

    public void sendMessage(final String message){
        final GroupHandle gh = gms.getGroupHandle();
        try {
            gh.sendMessage(null,message.getBytes());
        } catch (GMSException e) {
            logger.log(Level.INFO, e.getLocalizedMessage() );
        }
    }

    public static void main(final String[] args){
        if(args.length > 0 && "--usage".equals(args[1]))
        {
            logger.log(Level.INFO, new StringBuffer().append("USAGE: java -DMEMBERTYPE <CORE|SPECTATOR>")
                    .append(" -DINSTANCEID=<instanceid>")
                    .append(" -DCLUSTERNAME=<clustername")
                    .append(" -DLIFEINMILLIS= <length of time for this demo")
                    .append(" -DMESSAGING_MODE=[true|false] ApplicationServer")
                    .toString());
        }
        JxtaUtil.setLogger( logger );
        JxtaUtil.setupLogHandler();
        final ApplicationServer as;
        final GroupManagementService.MemberType memberType;

        if ("CORE".equals(System.getProperty("MEMBERTYPE", "CORE")
                .toUpperCase()))
        {
            memberType = GroupManagementService.MemberType.CORE;
        } else {
            memberType = GroupManagementService.MemberType.SPECTATOR;
        }

        as = new ApplicationServer(
                System.getProperty("INSTANCEID"),
                System.getProperty("CLUSTERNAME"),
                memberType,
                null);

        final Thread appServ = new Thread(as, "ApplicationServer");
        appServ.start();
        try {
            appServ.join();
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage());
        }
    }
}
