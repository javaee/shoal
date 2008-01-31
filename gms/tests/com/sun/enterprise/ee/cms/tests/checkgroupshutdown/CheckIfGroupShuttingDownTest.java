package com.sun.enterprise.ee.cms.tests.checkgroupshutdown;

import com.sun.enterprise.ee.cms.core.CallBack;
import com.sun.enterprise.ee.cms.core.GMSException;
import com.sun.enterprise.ee.cms.core.GroupManagementService;
import com.sun.enterprise.jxtamgmt.JxtaUtil;

import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.enterprise.ee.cms.core.*;
 import com.sun.enterprise.ee.cms.impl.client.*;
 import com.sun.enterprise.ee.cms.impl.common.GMSContextFactory;
 import com.sun.enterprise.jxtamgmt.JxtaUtil;

 import java.text.MessageFormat;
 import java.util.logging.Level;
 import java.util.logging.Logger;


/**
 * Created by IntelliJ IDEA.
 * User: sheetal
 * Date: Jan 31, 2008
 * Time: 1:22:36 PM
 * This test is for making sure that the API added to check if the
 * group is shutting down works fine.
 */
public class CheckIfGroupShuttingDownTest implements CallBack{

    final static Logger logger = Logger.getLogger("CheckIfGroupShuttingDownTest");
    final Object waitLock = new Object();

    public static void main(String[] args){
        JxtaUtil.setLogger(logger);
        JxtaUtil.setupLogHandler();
        CheckIfGroupShuttingDownTest check = new CheckIfGroupShuttingDownTest();
        try {
            check.runSimpleSample();
        } catch (GMSException e) {
            logger.log(Level.SEVERE, "Exception occured while joining group:" + e);
        }
    }

    /**
     * Runs this sample
     * @throws GMSException
     */
    private void runSimpleSample() throws GMSException {
        logger.log(Level.INFO, "Starting CheckIfGroupShuttingDownTest....");
        final String serverName = "server"+System.currentTimeMillis();
        final String group = "Group";

        //initialize Group Management Service
        GroupManagementService gms = initializeGMS(serverName, group);

        //register for Group Events
        registerForGroupEvents(gms);
        //join group
        joinGMSGroup(group, gms);

       try {
            waitForShutdown();
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, e.getMessage());
        }
        
        logger.info("Is the group shutting down ? : " + gms.isGroupBeingShutdown(group));
        logger.info("Adding group to ShutdownHelper's groupShutdownList...");
        //leave the group gracefully
        GMSContextFactory.getGMSContext(group).getShutdownHelper().addToGroupShutdownList(group);
        logger.info("Now is the group shutting down ? : " + gms.isGroupBeingShutdown(group));

        leaveGroupAndShutdown(serverName, gms);

        System.exit(0);
    }

    private GroupManagementService initializeGMS(String serverName, String groupName) {
         logger.log(Level.INFO, "Initializing Shoal for member: "+serverName+" group:"+groupName);
         return (GroupManagementService) GMSFactory.startGMSModule(serverName,
                 groupName, GroupManagementService.MemberType.CORE, null);
     }

     private void registerForGroupEvents(GroupManagementService gms) {
         logger.log(Level.INFO, "Registering for group event notifications");
         gms.addActionFactory(new JoinNotificationActionFactoryImpl(this));
         gms.addActionFactory(new FailureSuspectedActionFactoryImpl(this));
         gms.addActionFactory(new FailureNotificationActionFactoryImpl(this));
         gms.addActionFactory(new PlannedShutdownActionFactoryImpl(this));
         gms.addActionFactory(new JoinedAndReadyNotificationActionFactoryImpl(this));
     }

     private void joinGMSGroup(String groupName, GroupManagementService gms) throws GMSException {
         logger.log(Level.INFO, "Joining Group "+groupName);
         gms.join();
     }

        private void waitForShutdown() throws InterruptedException {
        logger.log(Level.INFO, "wait 10 secs to shutdown");
        synchronized(waitLock){
            waitLock.wait(10000);
        }
    }

    private void leaveGroupAndShutdown(String serverName, GroupManagementService gms) {
        logger.log(Level.INFO, "Shutting down gms " + gms + "for server " + serverName);
        gms.shutdown(GMSConstants.shutdownType.GROUP_SHUTDOWN);
    }

    public void processNotification(Signal notification) {
        logger.info("calling processNotification()...");
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
