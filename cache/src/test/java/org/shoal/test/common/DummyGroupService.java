package org.shoal.test.common;

import org.shoal.ha.cache.impl.util.MessageReceiver;
import org.shoal.ha.group.GroupMemberEventListener;
import org.shoal.ha.group.GroupService;

/**
 * @author Mahesh Kannan
 */
public class DummyGroupService
    implements GroupService {

    private String memberName;

    private String groupName;

    public DummyGroupService(String memberName, String groupName) {

    }

    @Override
    public String getGroupName() {
        return groupName;
    }

    @Override
    public String getMemberName() {
        return memberName;
    }

    @Override
    public void registerGroupMemberEventListener(GroupMemberEventListener listener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeGroupMemberEventListener(GroupMemberEventListener listener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void registerGroupMessageReceiver(String messagetoken, MessageReceiver receiver) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean sendMessage(String targetMemberName, String token, byte[] data) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
