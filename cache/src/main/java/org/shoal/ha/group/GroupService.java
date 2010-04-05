package org.shoal.ha.group;

import org.shoal.ha.cache.impl.util.MessageReceiver;
import org.shoal.ha.group.GroupMemberEventListener;

/**
 * The minimal methods that a GS must implement to be used by the replication service.
 *
 * @author Mahesh Kannan
 */
public interface GroupService {

    public String getGroupName();

    public String getMemberName();

    public void registerGroupMemberEventListener(GroupMemberEventListener listener);

    public void removeGroupMemberEventListener(GroupMemberEventListener listener);

    public void close();

    public void registerGroupMessageReceiver(String messageToken, MessageReceiver receiver);

    public boolean sendMessage(String targetMemberName, String messageToken, byte[] data);

}
