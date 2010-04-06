package org.shoal.ha.cache.impl.interceptor;

import org.shoal.ha.cache.impl.command.Command;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mahesh Kannan
 */
public class CommandMonitorInterceptor<K, V>
    extends ExecutionInterceptor<K, V> {

    private AtomicInteger transCount = new AtomicInteger();

    private AtomicInteger recevCount = new AtomicInteger();

    @Override
    public void onTransmit(Command cmd) {
        super.onTransmit(cmd);
        transCount.incrementAndGet();
    }

    @Override
    public void onReceive(Command cmd) {
        recevCount.incrementAndGet();
        super.onReceive(cmd);
    }

    public int getTransmitCount() {
        return transCount.get();
    }

    public int getReceiveCount() {
        return recevCount.get();
    }
}
