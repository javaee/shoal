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
package com.sun.enterprise.ee.cms.impl.jxta;

import com.sun.enterprise.ee.cms.core.*;
import com.sun.enterprise.ee.cms.impl.common.GMSContextBase;
import com.sun.enterprise.ee.cms.impl.common.ShutdownHelper;
import com.sun.enterprise.ee.cms.spi.GMSMessage;
import com.sun.enterprise.ee.cms.spi.GroupCommunicationProvider;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;

/**
 * @author Shreedhar Ganapathy
 *         Date: Jun 26, 2006
 * @version $Revision$
 */
public class GMSContext extends GMSContextBase {
    private ArrayBlockingQueue<EventPacket> viewQueue;
    private static final int MAX_VIEWS_IN_QUEUE = 200;
    private ArrayBlockingQueue<MessagePacket> messageQueue;
    private static final int MAX_MSGS_IN_QUEUE = 500;
    private ViewWindow viewWindow;
    private GroupCommunicationProvider groupCommunicationProvider;
    private DistributedStateCache distributedStateCache;
    private GroupHandle gh;
    private Properties configProperties;

    public GMSContext(final String serverToken, final String groupName,
                      final GroupManagementService.MemberType memberType,
                      final Properties configProperties) {
        super(serverToken, groupName, memberType);
        this.configProperties = configProperties;
        groupCommunicationProvider =
                new GroupCommunicationProviderImpl(groupName);

        viewQueue = new ArrayBlockingQueue<EventPacket>(MAX_VIEWS_IN_QUEUE,
                Boolean.TRUE);

        viewWindow = new ViewWindow(groupName, viewQueue);
        messageQueue = new ArrayBlockingQueue<MessagePacket>(MAX_MSGS_IN_QUEUE,
                Boolean.TRUE);

        gh = new GroupHandleImpl(groupName, serverToken);
        //TODO: consider untying the Dist State Cache creation from GMSContext.
        // It should be driven independent of GMSContext through a factory as
        // other impls of this interface can exist
        createDistributedStateCache();
        logger.log(Level.INFO, "Initialized Group Communication System....");
    }

    protected void createDistributedStateCache() {
        distributedStateCache = DistributedStateCacheImpl.getInstance(groupName);
    }

    /**
     * returns Group handle
     *
     * @return Group handle
     */
    public GroupHandle getGroupHandle() {
        return gh;
    }

    public DistributedStateCache getDistributedStateCache() {
        if (distributedStateCache == null) {
            createDistributedStateCache();
        }
        return distributedStateCache;
    }

    public void join() throws GMSException {
        final Thread viewWindowThread =
                new Thread(viewWindow, "ViewWindowThread");
        MessageWindow messageWindow = new MessageWindow(groupName, messageQueue);

        final Thread messageWindowThread =
                new Thread(messageWindow, "MessageWindowThread");
        messageWindowThread.start();
        viewWindowThread.start();

        final Map<String, String> idMap = new HashMap<String, String>();
        idMap.put(CustomTagNames.MEMBER_TYPE.toString(), memberType);
        idMap.put(CustomTagNames.GROUP_NAME.toString(), groupName);
        idMap.put(CustomTagNames.START_TIME.toString(), startTime.toString());

        groupCommunicationProvider.initializeGroupCommunicationProvider(
                serverToken, groupName, idMap, configProperties);
        groupCommunicationProvider.join();
    }

    public void leave(final GMSConstants.shutdownType shutdownType) {
        if(shutdownHelper.isGroupBeingShutdown(groupName)){
            groupCommunicationProvider.leave(true);
            shutdownHelper.removeFromGroupShutdownList(groupName);
        }
        else {
            groupCommunicationProvider.leave(false);
        }
    }

    public long getStartTime() {
        return startTime;
    }

    public void announceGroupShutdown(final String groupName,
                                      final GMSConstants.shutdownState shutdownState) {
        groupCommunicationProvider.
                announceClusterShutdown(
                        new GMSMessage(GMSConstants.shutdownType.GROUP_SHUTDOWN.toString(), null,
                                groupName, null));
    }

    public boolean addToSuspectList(final String token) {
        boolean retval = false;
        synchronized (suspectList) {
            if (!suspectList.contains(token)) {
                suspectList.add(token);
                retval = true;
            }
        }
        return retval;
    }

    public void removeFromSuspectList(final String token) {
        synchronized (suspectList) {
            if (suspectList.contains(token)) {
                suspectList.remove(token);
            }
        }
    }

    public boolean isSuspected(final String token) {
        boolean retval = false;
        synchronized (suspectList) {
            if (suspectList.contains(token)) {
                retval = true;
            }
        }
        return retval;
    }

    public List<String> getSuspectList() {
        final List<String> retval;
        synchronized (suspectList) {
            retval = new ArrayList<String>(suspectList);
        }
        return retval;
    }

    public ShutdownHelper getShutdownHelper() {
        return shutdownHelper;
    }

    ArrayBlockingQueue<EventPacket> getViewQueue() {
        return viewQueue;
    }

    ArrayBlockingQueue<MessagePacket> getMessageQueue() {
        return messageQueue;
    }

    public GroupCommunicationProvider getGroupCommunicationProvider() {
        return groupCommunicationProvider;
    }

    public com.sun.enterprise.ee.cms.impl.common.ViewWindow getViewWindow() {
        return viewWindow;
    }
}
