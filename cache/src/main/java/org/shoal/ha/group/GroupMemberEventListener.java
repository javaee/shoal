package org.shoal.ha.group;

/**
 * @author Mahesh Kannan
 *
 */
public interface GroupMemberEventListener {

    public void memberReady(String instanceName, String groupName);

    public void memberLeft(String instanceName, String groupName, boolean isShutdown);

}
