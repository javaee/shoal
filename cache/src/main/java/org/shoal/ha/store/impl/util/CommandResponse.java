package org.shoal.ha.store.impl.util;

import org.shoal.ha.store.impl.util.ResponseMediator;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicLong;


/**
 * @author Mahesh Kannan
 *
 */
public class CommandResponse
    implements Callable {

    private static final AtomicLong tokenCounter = new AtomicLong(0);

    private long tokenId;

    private Object result;

    private FutureTask future;

    private ResponseMediator mediator;

    public CommandResponse(ResponseMediator mediator) {
        this.mediator = mediator;
        this.tokenId = tokenCounter.incrementAndGet();
        this.future = new FutureTask(this);
    }

    public long getTokenId() {
        return tokenId;
    }

    public FutureTask getFuture() {
        return future;
    }

    public void setResult(Object v) {
        this.result = v;
        mediator.removeCommandResponse(tokenId);
        future.run(); //Which calls our call()
    }

    public Object call() {
        return result;
    }
}
