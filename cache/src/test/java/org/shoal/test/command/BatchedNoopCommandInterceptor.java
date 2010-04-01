package org.shoal.test.command;

import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.interceptor.ExecutionInterceptor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mahesh Kannan
 */
public class BatchedNoopCommandInterceptor<K, V>
        extends ExecutionInterceptor<K, V> {

    private AtomicInteger batchedTransCount = new AtomicInteger();

    private AtomicInteger batchedRecvCount = new AtomicInteger();

    @Override
    public void onTransmit(Command cmd) {
        System.out.println("**** BatchedNoopCommandInterceptor.onTransmit() got: " + cmd.getClass().getName());
        batchedTransCount.incrementAndGet();
        super.onTransmit(cmd);
    }

    @Override
    public void onReceive(Command cmd) {
        batchedRecvCount.incrementAndGet();
        super.onReceive(cmd);
    }

    public int getTransmitCount() {
        return batchedTransCount.get();
    }

    public int getReceiveCount() {
        return batchedRecvCount.get();
    }
}