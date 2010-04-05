package org.shoal.ha.group;

import org.shoal.ha.group.GroupService;
import org.shoal.ha.group.gms.GroupServiceProvider;

import java.util.concurrent.ConcurrentHashMap;


/**
 * @author Mahesh Kannan
 *
 */
public class GroupServiceFactory {

    private ConcurrentHashMap<String, GroupServiceProvider> groupHandles
            = new ConcurrentHashMap<String, GroupServiceProvider>();

    private static final GroupServiceFactory _instance = new GroupServiceFactory();

    private GroupServiceFactory() {
    }

    public static GroupServiceFactory getInstance() {
        return _instance;
    }

    public synchronized GroupService getGroupService(String myName, String groupName) {
        String key = makeKey(myName, groupName);
        GroupServiceProvider server = groupHandles.get(key);
        if (server == null) {
            server = new GroupServiceProvider(myName, groupName);
            groupHandles.put(key, server);
        }

        return server;
    }

    private static String makeKey(String myName, String groupName) {
        return myName + ":" + groupName;
    }

    public void shutdown(String myName, String groupName) {
        String key = makeKey(myName, groupName);
        GroupServiceProvider server = groupHandles.remove(key);
        if (server != null) {
            server.shutdown();
        }
    }

    public static void main(String[] args)
            throws Exception {
        GroupServiceFactory factory = GroupServiceFactory.getInstance();
        factory.getGroupService(args[0], args[1]);
    }
}
