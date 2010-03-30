package org.shoal.ha.group;

/**
 * @author Mahesh Kannan
 */
public interface GroupMessageReceiver {

    public void handleMessage(String sourceMemberName, String token, byte[] data);

}
