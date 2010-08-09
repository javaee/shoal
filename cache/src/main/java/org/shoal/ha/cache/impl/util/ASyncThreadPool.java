package org.shoal.ha.cache.impl.util;

import java.util.concurrent.*;

/**
 * @author Mahesh Kannan
 *
 */
public class ASyncThreadPool
    extends ScheduledThreadPoolExecutor {

    public ASyncThreadPool(int corePoolSize) {
        super(corePoolSize);
    }
}
