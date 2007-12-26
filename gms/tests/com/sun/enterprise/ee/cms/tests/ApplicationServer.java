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
import com.sun.enterprise.ee.cms.impl.common.GroupManagementServiceImpl;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import com.sun.enterprise.jxtamgmt.JxtaUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is an example used to demonstrate the application layer that controls the
 * lifecycle of the GMS module. It also provides an example of the actions taken
 * in response to a recover call from the GMS layer.
 *
 * @author Shreedhar Ganapathy"
 * @version $Revision$
 */
public class ApplicationServer implements Runnable {
    private static final Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);

    private GroupManagementService gms = null;
    private GMSClientService gcs1;
    private GMSClientService gcs2;
    private String serverName;
    private String groupName;

    public ApplicationServer(final String serverName, final String groupName,
                             final GroupManagementService.MemberType memberType,
                             final Properties props) {
        this.serverName = serverName;
        this.groupName = groupName;
        gms = (GroupManagementService) GMSFactory.startGMSModule(serverName, groupName, memberType, props);
        initClientServices(Boolean.valueOf(System.getProperty("MESSAGING_MODE", "true")));
    }

    private void initClientServices(boolean sendMessages) {
        gcs1 = new GMSClientService("EJBContainer", serverName, sendMessages);
        gcs2 = new GMSClientService("TransactionService", serverName, false);
    }

    /*    private static void setupLogHandler() {
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
    */
    public void run() {
        startGMS();
        addMemberDetails();
        startClientServices();
        logger.log(Level.FINE,"reporting joined and ready state...");
        gms.reportJoinedAndReadyState(groupName);
        try {
            Thread.sleep(Integer.parseInt(System.getProperty("LIFEINMILLIS", "15000")));
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage());
        }
        stopClientServices();
        stopGMS();
        System.exit(0);
    }

    private void addMemberDetails() {
        final Map<Object, Object> details = new Hashtable<Object, Object>();
        final ArrayList<ArrayList> ar1 = new ArrayList<ArrayList>();
        final ArrayList<String> ar2 = new ArrayList<String>();
        final int port = 3700;
        final int port1 = 3800;
        try {
            ar2.add(InetAddress.getLocalHost() + ":" + port);
            ar2.add(InetAddress.getLocalHost() + ":" + port1);
        }
        catch (UnknownHostException e) {
            logger.log(Level.WARNING, e.getLocalizedMessage());
        }
        ar1.add(ar2);
        details.put(GMSClientService.IIOP_MEMBER_DETAILS_KEY, ar1);
        try {
            ((GroupManagementServiceImpl) gms).setMemberDetails(serverName, details);
        }
        catch (GMSException e) {
            logger.log(Level.WARNING, e.getLocalizedMessage());
        }
    }

    public void startClientServices() {
        logger.log(Level.FINE, "ApplicationServer: Starting GMSClient");
        gcs1.start();
        gcs2.start();
    }

    public void startGMS() {
        logger.log(Level.FINE, "ApplicationServer: Starting GMS service");
        try{
            gms.join();
        } catch (GMSException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage());
        }
    }

    public void stopGMS() {
        logger.log(Level.FINE, "ApplicationServer: Stopping GMS service");
        gms.shutdown(GMSConstants.shutdownType.INSTANCE_SHUTDOWN);
    }

    private void stopClientServices() {
        logger.log(Level.FINE, "ApplicationServer: Stopping GMSClient");
        gcs1.stopClient();
        gcs2.stopClient();
    }

    public void sendMessage(final String message) {
        final GroupHandle gh = gms.getGroupHandle();
        try {
            gh.sendMessage(null, message.getBytes());
        } catch (GMSException e) {
            logger.log(Level.INFO, e.getLocalizedMessage());
        }
    }

    public static void main(final String[] args) {
        if (args.length > 0 && "--usage".equals(args[1])) {
            logger.log(Level.INFO, new StringBuffer().append("USAGE: java -DMEMBERTYPE <CORE|SPECTATOR>")
                    .append(" -DINSTANCEID=<instanceid>")
                    .append(" -DCLUSTERNAME=<clustername")
                    .append(" -DLIFEINMILLIS= <length of time for this demo")
                    .append(" -DMESSAGING_MODE=[true|false] ApplicationServer")
                    .toString());
        }
        JxtaUtil.setLogger(logger);
        JxtaUtil.setupLogHandler();
        final ApplicationServer applicationServer;
        final GroupManagementService.MemberType memberType;

        if ("CORE".equals(System.getProperty("MEMBERTYPE", "CORE").toUpperCase())) {
            memberType = GroupManagementService.MemberType.CORE;
        } else {
            memberType = GroupManagementService.MemberType.SPECTATOR;
        }
        Properties configProps = new Properties();
        configProps.put(ServiceProviderConfigurationKeys.MULTICASTADDRESS.toString(),
                                    System.getProperty("MULTICASTADDRESS", "229.9.1.1"));
        configProps.put(ServiceProviderConfigurationKeys.MULTICASTPORT.toString(), 2299);
        logger.fine("Is initial host="+System.getProperty("IS_INITIAL_HOST"));
        configProps.put(ServiceProviderConfigurationKeys.IS_BOOTSTRAPPING_NODE.toString(),
                System.getProperty("IS_INITIAL_HOST", "false"));
        if(System.getProperty("INITIAL_HOST_LIST") != null){
            configProps.put(ServiceProviderConfigurationKeys.VIRTUAL_MULTICAST_URI_LIST.toString(),
                System.getProperty("INITIAL_HOST_LIST"));
        }
        configProps.put(ServiceProviderConfigurationKeys.FAILURE_DETECTION_RETRIES.toString(), "2");
        //Uncomment this to receive loop back messages
        //configProps.put(ServiceProviderConfigurationKeys.LOOPBACK.toString(), "true");

        applicationServer = new ApplicationServer(System.getProperty("INSTANCEID"), System.getProperty("CLUSTERNAME"), memberType, configProps);

        final Thread appServThread = new Thread(applicationServer, "ApplicationServer");
        appServThread.start();
        try {
            appServThread.join();
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage());
        }
    }
}
