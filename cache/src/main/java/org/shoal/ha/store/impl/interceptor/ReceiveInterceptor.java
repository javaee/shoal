package org.shoal.ha.store.impl.interceptor;

import org.shoal.ha.store.impl.command.Command;


/**
 * @author Mahesh Kannan
 *
 */
public final class ReceiveInterceptor
    extends ExecutionInterceptor {

    public void onTransmit(Command cmd) {
        //Noop
    }

    public void onReceive(Command cmd) {
            
    }

}