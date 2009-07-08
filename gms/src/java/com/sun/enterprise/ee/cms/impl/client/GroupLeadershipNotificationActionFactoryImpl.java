package com.sun.enterprise.ee.cms.impl.client;

import com.sun.enterprise.ee.cms.core.CallBack;
import com.sun.enterprise.ee.cms.core.Action;
import com.sun.enterprise.ee.cms.core.GroupLeadershipNotificationActionFactory;

/**
 * Reference Implementation of GroupLeadershipNotificationActionFactory
 *
 * @author Bongjae Chang
 * @Date: June 25, 2008
 */
public class GroupLeadershipNotificationActionFactoryImpl implements GroupLeadershipNotificationActionFactory {

    private final CallBack callBack;

    public GroupLeadershipNotificationActionFactoryImpl( final CallBack callBack ) {
        this.callBack = callBack;
    }

    /**
     * Produces an Action instance.
     *
     * @return com.sun.enterprise.ee.cms.Action
     */
    public Action produceAction() {
        return new GroupLeadershipNotificationActionImpl( callBack );
    }
}
