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
import org.shoal.adapter.store.commands.SaveCommand;
import org.shoal.ha.cache.api.DataStoreContext;
import org.shoal.ha.cache.api.ShoalCacheLoggerConstants;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.command.ReplicationCommandOpcode;
import org.shoal.ha.cache.impl.util.ASyncReplicationManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
        implements Runnable, CommandCollector<K,V> {


    private static final Logger _logger =
            Logger.getLogger(ShoalCacheLoggerConstants.CACHE_TRANSMIT_INTERCEPTOR);

    private static final Logger _statsLogger =
            Logger.getLogger(ShoalCacheLoggerConstants.CACHE_STATS);

    private DataStoreContext<K, V> dsc;

    private volatile String targetName;

    private ScheduledFuture future;

    private static final String TRANSMITTER_FREQUECNCY_PROP_NAME = "org.shoal.cache.transmitter.frequency.in.millis";

    private static final String MAX_BATCH_SIZE_PROP_NAME = "org.shoal.cache.transmitter.max.batch.size";

    private static int TRANSMITTER_FREQUECNCY_IN_MILLIS = 100;

    private static int MAX_BATCH_SIZE = 30;

    private AtomicReference<BatchedCommandMapDataFrame> mapRef;

    ASyncReplicationManager asyncReplicationManager = ASyncReplicationManager._getInstance();

    private long timeStamp = System.currentTimeMillis();

    ThreadPoolExecutor executor;

    static {
        try {
            TRANSMITTER_FREQUECNCY_IN_MILLIS =
                    Integer.valueOf(System.getProperty(TRANSMITTER_FREQUECNCY_PROP_NAME,
                            "" + TRANSMITTER_FREQUECNCY_IN_MILLIS));
            _statsLogger.log(Level.CONFIG, "USING " + TRANSMITTER_FREQUECNCY_PROP_NAME + " = " + TRANSMITTER_FREQUECNCY_IN_MILLIS);
        } catch (Exception ex) {
            _statsLogger.log(Level.CONFIG, "USING " + TRANSMITTER_FREQUECNCY_PROP_NAME + " = " + TRANSMITTER_FREQUECNCY_IN_MILLIS);
        }

        try {
            MAX_BATCH_SIZE =
                    Integer.valueOf(System.getProperty(MAX_BATCH_SIZE_PROP_NAME,
                            "" + MAX_BATCH_SIZE));
            _statsLogger.log(Level.CONFIG, "USING " + MAX_BATCH_SIZE_PROP_NAME + " = " + MAX_BATCH_SIZE);
        } catch (Exception ex) {
            _statsLogger.log(Level.CONFIG, "USING " + MAX_BATCH_SIZE_PROP_NAME + " = " + MAX_BATCH_SIZE);
        }

        _logger.log(Level.FINE, "USING ReplicationCommandTransmitterWithMap");
    }

    @Override
    public void initialize(String targetName, DataStoreContext<K, V> rsInfo) {

        this.executor = ASyncReplicationManager._getInstance().getExecutorService();
        this.targetName = targetName;
        this.dsc = rsInfo;



        BatchedCommandMapDataFrame batch = new BatchedCommandMapDataFrame();
        mapRef = new AtomicReference<BatchedCommandMapDataFrame>(batch);


        future = asyncReplicationManager.getScheduledThreadPoolExecutor().scheduleAtFixedRate(this, TRANSMITTER_FREQUECNCY_IN_MILLIS,
                TRANSMITTER_FREQUECNCY_IN_MILLIS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        try {
            future.cancel(false);
        } catch (Exception ex) {
            //Ignore
        }
    }

    @Override
    public void addCommand(Command<K, V> cmd) {

        for (boolean done = false; !done;) {
            BatchedCommandMapDataFrame batch = mapRef.get();
            done = batch.doAddOrRemove(cmd, true);
            if (!done) {
                BatchedCommandMapDataFrame frame = new BatchedCommandMapDataFrame();
                frame.doAddOrRemove(cmd, true);
                done = mapRef.compareAndSet(batch, frame);
            }
        }
    }

    @Override
    public void removeCommand(Command<K, V> cmd) {

        for (boolean done = false; !done;) {
            BatchedCommandMapDataFrame batch = mapRef.get();
            done = batch.doAddOrRemove(cmd, false);
            if (!done) {
                BatchedCommandMapDataFrame frame = new BatchedCommandMapDataFrame();
                frame.doAddOrRemove(cmd, false);
                done = mapRef.compareAndSet(batch, frame);
            }
        }
    }

    public void run() {
        BatchedCommandMapDataFrame batch = mapRef.get();
        batch.flushAndTransmit();
    }



    private static AtomicInteger _sendBatchCount = new AtomicInteger(0);

    private class BatchedCommandMapDataFrame
            implements Runnable {

        private int myBatchNumber;

        private AtomicInteger inFlightCount = new AtomicInteger(0);

        private AtomicBoolean batchThresholdReached = new AtomicBoolean(false);

        private AtomicBoolean alreadySent = new AtomicBoolean(false);

        private volatile ConcurrentHashMap<Object, ConcurrentLinkedQueue<Command>> map
                = new ConcurrentHashMap<Object, ConcurrentLinkedQueue<Command>>();

        private AtomicInteger  removedKeysSize = new AtomicInteger(0);

        private volatile ConcurrentLinkedQueue removedKeys = new ConcurrentLinkedQueue();

        private volatile long lastTS = System.currentTimeMillis();

        BatchedCommandMapDataFrame() {
            myBatchNumber = _sendBatchCount.incrementAndGet();
        }

        public boolean doAddOrRemove(Command cmd, boolean isAdd) {

            boolean result = false;
            if (! batchThresholdReached.get()) {
                
                int inCount = 0;
                try {
                    inFlightCount.incrementAndGet();
                    if (! batchThresholdReached.get()) {
                        if (isAdd) {
                            ConcurrentLinkedQueue<Command> cmdList = map.get(cmd.getKey());
                            if (cmdList == null) {
                                cmdList = new ConcurrentLinkedQueue<Command>();
                                ConcurrentLinkedQueue<Command> cmdList1
                                        = map.putIfAbsent(cmd.getKey(), cmdList);
                                cmdList = cmdList1 != null ? cmdList1 : cmdList;
                            }

                            cmdList.add(cmd);
                            result = true;
                            if (map.size() >= MAX_BATCH_SIZE) {
                                batchThresholdReached.compareAndSet(false, true);
                            }
                        } else {
                            map.remove(cmd.getKey());
                            removedKeys.add(cmd.getKey());
                            int removedSz = removedKeysSize.incrementAndGet();
                            result = true;
                            if (removedSz >= (2 * MAX_BATCH_SIZE)) {
                                batchThresholdReached.compareAndSet(false, true);
                            }
                        }

                    }
                } finally {
                    inCount = inFlightCount.decrementAndGet();
                }

                if (batchThresholdReached.get() && inCount == 0 && alreadySent.compareAndSet(false, true)) {
                    if (_statsLogger.isLoggable(Level.FINE)) {
                        _statsLogger.log(Level.FINE, "doAddOrRemove batchThresholdReached.get()="  + batchThresholdReached.get()
                            + "; inFlightCount = " + inCount + "; ");

                        _statsLogger.log(Level.FINE, "Sending batch# "  + myBatchNumber
                                + " to " + targetName + "; wasActive for ("
                                + (System.currentTimeMillis() - lastTS) + " millis");
                    }
                    asyncReplicationManager.getExecutorService().submit(this);
                    dsc.getDataStoreMBean().incrementBatchSentCount();
                }
            }

            return result;
        }

        //Called by periodic task
        void flushAndTransmit() {
            dsc.getDataStoreMBean().incrementFlushThreadWakeupCount();
            if ((!alreadySent.get()) && ((map.size() > 0) || (removedKeysSize.get() > 0))) {
                if (lastTS == timeStamp) {
                    if (_statsLogger.isLoggable(Level.FINE)) {
                        _statsLogger.log(Level.FINE, "flushAndTransmit will flush data because lastTS = " + lastTS
                                + "; timeStamp = " + timeStamp + "; lastTS = " + lastTS
                                + "; map.size() = " + map.size()
                                + "; removedKeys.size() = " +removedKeysSize.get());
                    }

                    NoOpCommand nc = null;
                    do {
                        nc = new NoOpCommand();
                    } while (doAddOrRemove(nc, true));
                    dsc.getDataStoreMBean().incrementFlushThreadFlushedCount();
                } else {
                    if (_statsLogger.isLoggable(Level.FINER)) {
                        _statsLogger.log(Level.FINER, "flushAndTransmit will NOT flush data because lastTS = " + lastTS
                                + "; timeStamp = " + timeStamp + "; lastTS = " + lastTS
                                + "; map.size() = " + map.size()
                                + "; removedKeys.size() = " +removedKeysSize.get());
                    }
                    timeStamp = lastTS;
                }
            }
        }

        public void run() {

            ReplicationFramePayloadCommand rfCmd = new ReplicationFramePayloadCommand();
            rfCmd.setTargetInstance(targetName);
            try {
                for (ConcurrentLinkedQueue<Command> cmdList : map.values()) {
                    SaveCommand saveCmd = null;
                    for (Command cmd : cmdList) {
                        if (cmd.getOpcode() == ReplicationCommandOpcode.NOOP_COMMAND) {
                            //No need to add the noop commands
                        } else if (cmd.getOpcode() == ReplicationCommandOpcode.SAVE) {
                            SaveCommand thisSaveCommand = (SaveCommand) cmd;
                            if (saveCmd == null || saveCmd.getVersion() < thisSaveCommand.getVersion()) {
                                saveCmd = thisSaveCommand;
                            }
                        } else {
                            //Commands like Load{Requests|Response} Touch etc.
                            rfCmd.addComamnd(cmd);
                        }
                    }

                    if (saveCmd != null) {
                        rfCmd.addComamnd(saveCmd);
                    }
                }

                rfCmd.setRemovedKeys(removedKeys);
                dsc.getCommandManager().execute(rfCmd);

            } catch (IOException ioEx) {
                _logger.log(Level.WARNING, "Batch operation (ASyncCommandList failed...", ioEx);
            }
        }

        
    }
}
