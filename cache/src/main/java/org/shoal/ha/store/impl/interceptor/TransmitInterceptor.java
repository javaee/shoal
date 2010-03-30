package org.shoal.ha.store.impl.interceptor;

import org.shoal.ha.store.impl.command.Command;


/**
 * @author Mahesh Kannan
 *
 */
public final class TransmitInterceptor
    extends ExecutionInterceptor {

    public void onTransmit(Command cmd) {
        //Must transmit    
    }

    public void onReceive(Command cmd) {

    }

}