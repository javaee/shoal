package org.shoal.test.command;

import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.interceptor.ExecutionInterceptor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mahesh Kannan
 */
public class NoopCommandInterceptor<K, V>
    extends ExecutionInterceptor<K, V> {

    private AtomicInteger totalTransCount = new AtomicInteger();

    private AtomicInteger noopTranscount = new AtomicInteger();

    private AtomicInteger noopRecvCount = new AtomicInteger();

    @Override
    public void onTransmit(Command cmd) {
        totalTransCount.incrementAndGet();
        System.out.println("**** NoopCommandInterceptor.onTransmit() got: " + cmd.getClass().getName());
        if (cmd instanceof NoopCommand) {
            noopTranscount.incrementAndGet();
            getDataStoreContext().getCommandManager().execute(new BatchedNoopCommand());
        } else {
            super.onTransmit(cmd);

        }
    }

    @Override
    public void onReceive(Command cmd) {
        if (cmd instanceof NoopCommand) {
            noopRecvCount.incrementAndGet();
            super.onReceive(cmd);
        }
    }

    public int getTotalTransCount() {
        return totalTransCount.get();
    }

    public int getNoopTransCount() {
        return noopTranscount.get();
    }

    public int getReceiveCount() {
        return noopRecvCount.get();
    }
}