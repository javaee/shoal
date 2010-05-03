package org.shoal.ha.group.gms;

import com.sun.enterprise.ee.cms.core.*;
import com.sun.enterprise.ee.cms.impl.client.FailureNotificationActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.JoinNotificationActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.JoinedAndReadyNotificationActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.MessageActionFactoryImpl;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import com.sun.enterprise.ee.cms.spi.MemberStates;
import org.shoal.ha.cache.impl.util.MessageReceiver;
import org.shoal.ha.group.GroupMemberEventListener;
import org.shoal.ha.group.GroupService;

import java.util.Properties;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mahesh Kannan
 */
public class GroupServiceProvider
        implements GroupService, CallBack {

    private static final Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);

    private String myName;

    private String groupName;

    private Properties configProps = new Properties();

    private GroupManagementService gms;

    private GroupHandle groupHandle;

    private ConcurrentHashMap<String, String> aliveInstances = new ConcurrentHashMap<String, String>();

    private List<GroupMemberEventListener> listeners = new ArrayList<GroupMemberEventListener>();
    
    private boolean createdAndJoinedGMSGroup;

    public GroupServiceProvider(String myName, String groupName) {
        init(myName, groupName);
    }

    public void processNotification(Signal notification) {
        MemberStates[] states;
//        logger.info("[$$$Received notification] => " + notification.getMemberToken() + " ==> "
//            + gms.getGroupHandle().getMemberState(notification.getMemberToken()));
        if ((notification instanceof JoinedAndReadyNotificationSignal)
                || (notification instanceof JoinNotificationSignal)
                || (notification instanceof FailureNotificationSignal)
                || (notification instanceof FailureSuspectedSignal)) {
            // getMemberState constraint check for member being added.
            MemberStates state = gms.getGroupHandle().getMemberState(notification.getMemberToken());

            if (state == MemberStates.ALIVEANDREADY || state == MemberStates.READY) {
                JoinedAndReadyNotificationSignal readySignal = (JoinedAndReadyNotificationSignal) notification;
                List<String> currentCoreMembers = readySignal.getCurrentCoreMembers();
                states = new MemberStates[currentCoreMembers.size()];
                int i = 0;
                for (String instanceName : currentCoreMembers) {
                    states[i] = gms.getGroupHandle().getMemberState(instanceName, 6000, 3000);
                    switch (states[i]) {
                        case STARTING:
                        case ALIVE:
                        case READY:
                        case ALIVEANDREADY:
                            for (GroupMemberEventListener listener : listeners) {
                                if (aliveInstances.putIfAbsent(instanceName, instanceName) == null) {
                                    listener.memberReady(instanceName, groupName);
                                }
                            }
                            notifyOnMemberReady();
                            break;
                    }
                }
            } else if (state == MemberStates.STOPPED ||
                    state == MemberStates.CLUSTERSTOPPING ||
                    state == MemberStates.UNKNOWN) {

                String instance = notification.getMemberToken();
                aliveInstances.remove(instance);
                if (!myName.equals(instance)) {
                    for (GroupMemberEventListener listener : listeners) {
                        listener.memberLeft(instance, groupName, (state == MemberStates.CLUSTERSTOPPING));
                    }
                }
            }
        }
    }

    private void notifyOnMemberReady() {
        List<String> members = gms.getGroupHandle().getCurrentCoreMembers();
        List<GMSMember> prevGMSMembers = gms.getGroupHandle().getPreviousView();
        List<String> prevMembers = null;
        for (GMSMember member : prevGMSMembers) {
            prevMembers.add(member.getMemberToken());
        }
        for (String instanceName : members) {
            for (GroupMemberEventListener listener : listeners) {
                listener.memberReady(instanceName, groupName);
            }
        }
    }

    private void notifyCurrentAliveMembers() {
        System.out.println("notifyCurrentAliveMembers ==> Notifying...");
        List<String> members = gms.getGroupHandle().getCurrentCoreMembers();
        for (String instanceName : members) {
            for (GroupMemberEventListener listener : listeners) {
                aliveInstances.putIfAbsent(instanceName, instanceName);
                    listener.memberReady(instanceName, groupName);
            }
        }
    }

    private void init(String myName, String groupName) {
        try {
            gms = GMSFactory.getGMSModule(groupName);
            logger.config("GroupServiceProvider found gms module for group:" + groupName);
        } catch (Exception e){
            logger.severe("GMS module for group "+groupName+" not enabled");
        }
        createdAndJoinedGMSGroup = gms == null;
        if (createdAndJoinedGMSGroup) {
            logger.config("GroupServiceProvider creating gms module for group " + groupName);
            GroupManagementService.MemberType memberType = myName.equals("DAS") ? GroupManagementService.MemberType.SPECTATOR
                    : GroupManagementService.MemberType.CORE;

            configProps.put(ServiceProviderConfigurationKeys.MULTICASTADDRESS.toString(),
                    System.getProperty("MULTICASTADDRESS", "229.9.1.1"));
            configProps.put(ServiceProviderConfigurationKeys.MULTICASTPORT.toString(), 2299);
            logger.info("Is initial host=" + System.getProperty("IS_INITIAL_HOST"));
            configProps.put(ServiceProviderConfigurationKeys.IS_BOOTSTRAPPING_NODE.toString(),
                    System.getProperty("IS_INITIAL_HOST", "false"));
            if (System.getProperty("INITIAL_HOST_LIST") != null) {
                configProps.put(ServiceProviderConfigurationKeys.VIRTUAL_MULTICAST_URI_LIST.toString(),
                        myName.equals("DAS"));
            }
            configProps.put(ServiceProviderConfigurationKeys.FAILURE_DETECTION_RETRIES.toString(),
                    System.getProperty("MAX_MISSED_HEARTBEATS", "3"));
            configProps.put(ServiceProviderConfigurationKeys.FAILURE_DETECTION_TIMEOUT.toString(),
                    System.getProperty("HEARTBEAT_FREQUENCY", "2000"));
            //Uncomment this to receive loop back messages
            //configProps.put(ServiceProviderConfigurationKeys.LOOPBACK.toString(), "true");
            final String bindInterfaceAddress = System.getProperty("BIND_INTERFACE_ADDRESS");
            if (bindInterfaceAddress != null) {
                configProps.put(ServiceProviderConfigurationKeys.BIND_INTERFACE_ADDRESS.toString(), bindInterfaceAddress);
            }

            gms = (GroupManagementService) GMSFactory.startGMSModule(
                    myName, groupName, memberType, configProps);
        }
        this.groupHandle = gms.getGroupHandle();
        this.myName = myName;
        this.groupName = groupName;

        gms.addActionFactory(new JoinNotificationActionFactoryImpl(this));
        gms.addActionFactory(new JoinedAndReadyNotificationActionFactoryImpl(this));
        gms.addActionFactory(new FailureNotificationActionFactoryImpl(this));

        if (createdAndJoinedGMSGroup) {
            try {
                gms.join();
                Thread.sleep(3000);
                gms.reportJoinedAndReadyState(groupName);
            } catch (Exception ex) {
                //TODO
            }
        }
    }

    public void shutdown() {
        //gms.shutdown();
    }

    @Override
    public String getGroupName() {
        return groupName;
    }

    @Override
    public String getMemberName() {
        return myName;
    }

    @Override
    public boolean sendMessage(String targetMemberName, String token, byte[] data) {
        try {
            groupHandle.sendMessage(targetMemberName, token, data);
            return true;
        } catch (GMSException gmsEx) {
            //TODO Log
        }

        return false;
    }

    @Override
    public void registerGroupMessageReceiver(String messageToken, MessageReceiver receiver) {
        gms.addActionFactory(new MessageActionFactoryImpl(receiver), messageToken);
    }

    @Override
    public void registerGroupMemberEventListener(GroupMemberEventListener listener) {
        listeners.add(listener);
        notifyCurrentAliveMembers();
    }

    @Override
    public void removeGroupMemberEventListener(GroupMemberEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void close() {
        if (createdAndJoinedGMSGroup) {
            shutdown();
        }
        
    }
}
