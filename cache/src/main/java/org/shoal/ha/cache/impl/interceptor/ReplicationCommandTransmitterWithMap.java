/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.shoal.ha.cache.impl.interceptor;

import org.shoal.adapter.store.commands.NoOpCommand;
import org.shoal.ha.cache.api.DataStoreContext;
import org.shoal.ha.cache.api.ShoalCacheLoggerConstants;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.command.ReplicationCommandOpcode;
import org.shoal.ha.cache.impl.util.ASyncReplicationManager;
import org.shoal.ha.group.GroupService;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public class ReplicationCommandTransmitterWithMap<K, V>
        implements Runnable {


    private static final Logger _logger =
            Logger.getLogger(ShoalCacheLoggerConstants.CACHE_TRANSMIT_INTERCEPTOR);

    private DataStoreContext<K, V> dsc;

    private volatile String targetName;

    private ScheduledFuture future;

    private static final String TRANSMITTER_FREQUECNCY_PROP_NAME = "org.shoal.cache.transmitter.frequency.in.millis";

    private static final String MAX_BATCH_SIZE_PROP_NAME = "org.shoal.cache.transmitter.max.batch.size";

    private static int TRANSMITTER_FREQUECNCY_IN_MILLIS = 100;

    private int MAX_BATCH_SIZE = 20;

    private AtomicReference<BatchedCommandListDataFrame> mapRef;

    ASyncReplicationManager asyncReplicationManager = ASyncReplicationManager._getInstance();

    private long timeStamp = System.currentTimeMillis();

    ThreadPoolExecutor executor;

    public void initialize(String targetName, DataStoreContext<K, V> rsInfo) {

        this.executor = ASyncReplicationManager._getInstance().getExecutorService();
        this.targetName = targetName;
        this.dsc = rsInfo;

        try {
            TRANSMITTER_FREQUECNCY_IN_MILLIS =
                    Integer.getInteger(System.getProperty(TRANSMITTER_FREQUECNCY_PROP_NAME,
                            "" + TRANSMITTER_FREQUECNCY_IN_MILLIS));
        } catch (Exception ex) {
            //Ignore
        }

        try {
            MAX_BATCH_SIZE =
                    Integer.getInteger(System.getProperty(MAX_BATCH_SIZE_PROP_NAME,
                            "" + MAX_BATCH_SIZE));
        } catch (Exception ex) {
            //Ignore
        }

        BatchedCommandListDataFrame batch = new BatchedCommandListDataFrame();
        mapRef = new AtomicReference<BatchedCommandListDataFrame>(batch);


        future = asyncReplicationManager.getScheduledThreadPoolExecutor().scheduleAtFixedRate(this, TRANSMITTER_FREQUECNCY_IN_MILLIS,
                TRANSMITTER_FREQUECNCY_IN_MILLIS, TimeUnit.MILLISECONDS);
    }

    public void close() {
        try {
            future.cancel(false);
        } catch (Exception ex) {
            //Ignore
        }
    }

    public void addCommand(Command<K, V> cmd) {

        for (boolean done = false; !done;) {
            BatchedCommandListDataFrame batch = mapRef.get();
            done = batch.addCommand(cmd);
            if (!done) {
                BatchedCommandListDataFrame frame = new BatchedCommandListDataFrame();
                frame.addCommand(cmd);
                done = mapRef.compareAndSet(batch, frame);
            }
        }
    }

    public void run() {
        BatchedCommandListDataFrame batch = mapRef.get();
        batch.flushAndTransmit();
    }

    private class BatchedCommandListDataFrame
            implements Runnable {

        private AtomicInteger inFlightCount = new AtomicInteger(0);

        private AtomicBoolean batchThresholdReached = new AtomicBoolean(false);

        private AtomicBoolean alreadySent = new AtomicBoolean(false);

        private transient ConcurrentHashMap map = new ConcurrentHashMap();

        private long lastTS = System.currentTimeMillis();

        public boolean addCommand(Command cmd) {

            boolean result = false;
            inFlightCount.incrementAndGet();
            if (!batchThresholdReached.get()) {

                int size = map.size();
                if (size < MAX_BATCH_SIZE) {
                    Object key = cmd.getKey();
                    map.put(key, cmd);
                    result = true;
                    if (map.size() >= MAX_BATCH_SIZE) {
                        batchThresholdReached.compareAndSet(false, true);
                    }
                }
            }

            int count = inFlightCount.decrementAndGet();
            if (batchThresholdReached.get() && count == 0 && alreadySent.compareAndSet(false, true)) {
                asyncReplicationManager.getExecutorService().submit(this);
            }

            return result;
        }

        //Called by periodic task

        void flushAndTransmit() {
            if (map.size() > 0) {
                if (lastTS == timeStamp) {
                    boolean completed = false;
                    int index = 0;
                    NoOpCommand nc = null;
                    do {
                        nc = new NoOpCommand();
                        nc.setKey("DummyKey:"+index++);
                    } while (addCommand(nc));
                } else {
                    timeStamp = lastTS;
                }
            }
        }

        public void run() {

            ReplicationFramePayloadCommand rfCmd = new ReplicationFramePayloadCommand();
            rfCmd.setTargetInstance(targetName);
            try {
                int size = map.size();
                for (Object obj : map.values()) {
                    Command cmd = (Command) obj;
                    if (cmd.getOpcode() != ReplicationCommandOpcode.NOOP_COMMAND) {
                        rfCmd.addComamnd(cmd);
                    }
                }

                dsc.getCommandManager().execute(rfCmd);
            } catch (IOException ioEx) {
                _logger.log(Level.WARNING, "Batch operation (ASyncCommandList failed...", ioEx);
            }
        }
    }
}
