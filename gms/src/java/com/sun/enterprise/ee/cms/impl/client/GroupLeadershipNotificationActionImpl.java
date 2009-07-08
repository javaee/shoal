package com.sun.enterprise.ee.cms.impl.client;

import com.sun.enterprise.ee.cms.core.CallBack;
import com.sun.enterprise.ee.cms.core.Signal;
import com.sun.enterprise.ee.cms.core.ActionException;
import com.sun.enterprise.ee.cms.core.SignalAcquireException;
import com.sun.enterprise.ee.cms.core.SignalReleaseException;
import com.sun.enterprise.ee.cms.core.GroupLeadershipNotificationAction;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Reference Implementation of GroupLeadershipNotificationAction
 *
 * @author Bongjae Chang
 * @Date: June 25, 2008
 */
public class GroupLeadershipNotificationActionImpl implements GroupLeadershipNotificationAction {

    private Logger logger = GMSLogDomain.getLogger( GMSLogDomain.GMS_LOGGER );

    private final CallBack callBack;

    public GroupLeadershipNotificationActionImpl( final CallBack callBack ) {
        this.callBack = callBack;
    }

    /**
     * Implementations of consumeSignal should strive to return control
     * promptly back to the thread that has delivered the Signal.
     */
    public void consumeSignal( final Signal s ) throws ActionException {
        try {
            s.acquire();
            callBack.processNotification( s );
        } catch( SignalAcquireException e ) {
            logger.log( Level.SEVERE, e.getLocalizedMessage() );
        }

        try {
            s.release();
        } catch( SignalReleaseException e ) {
            logger.log( Level.SEVERE, e.getLocalizedMessage() );
        }
    }
}
