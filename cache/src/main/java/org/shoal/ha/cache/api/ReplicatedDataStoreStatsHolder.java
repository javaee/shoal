/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.shoal.ha.cache.api;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Mahesh Kannan
 *
 */
public class ReplicatedDataStoreStatsHolder<K, V> implements DataStoreMBean {

    private DataStoreContext<K, V> dsc;

    private String keyClassName;

    private String valueClassName;

    private String keyTransformerClassName;

    private String entryUpdaterClassName;

    private AtomicInteger saveCount = new AtomicInteger(0);

    private AtomicInteger executedSaveCount = new AtomicInteger(0);

    private AtomicInteger loadCount = new AtomicInteger(0);

    private AtomicInteger loadSuccessCount = new AtomicInteger(0);

    private AtomicInteger localLoadSuccessCount = new AtomicInteger(0);

    private AtomicInteger simpleLoadSuccessCount = new AtomicInteger(0);

    private AtomicInteger broadcastLoadSuccessCount = new AtomicInteger(0);

    private AtomicInteger saveOnLoadCount = new AtomicInteger(0);

    private AtomicInteger loadFailureCount = new AtomicInteger(0);

    private AtomicInteger removeCount = new AtomicInteger(0);

    private AtomicInteger executedRemoveCount = new AtomicInteger(0);

    private AtomicInteger batchSentCount = new AtomicInteger(0);

    private AtomicInteger batchReceivedCount = new AtomicInteger(0);

    private AtomicInteger flushThreadWakeupCount = new AtomicInteger(0);

    private AtomicInteger flushThreadFlushedCount = new AtomicInteger(0);

    private AtomicInteger removeExpiredCallCount = new AtomicInteger(0);

    private AtomicInteger removeExpiredEntriesCount = new AtomicInteger(0);

    private AtomicInteger gmsSendCount = new AtomicInteger(0);

    private AtomicLong gmsSendBytesCount = new AtomicLong(0);


    public ReplicatedDataStoreStatsHolder(DataStoreContext<K, V> dsc) {
        this.dsc = dsc;

        this.keyClassName = (dsc.getKeyClazz() != null) ? dsc.getKeyClazz().getName() : "?";
        this.valueClassName = (dsc.getValueClazz() != null) ? dsc.getValueClazz().getName() : "?";
        this.keyTransformerClassName = (dsc.getKeyTransformer() != null)
                ? dsc.getKeyTransformer().getClass().getName() : "?";
        this.entryUpdaterClassName = (dsc.getDataStoreEntryUpdater() != null)
                ? dsc.getDataStoreEntryUpdater().getClass().getName() : "?";
    }

    //@Override
    public String getStoreName() {
        return dsc.getStoreName();
    }

    //@Override
    public String getKeyClassName() {
        return keyClassName;
    }

    //@Override
    public String getValueClassName() {
        return valueClassName;
    }

    public String getEntryUpdaterClassName() {
        return entryUpdaterClassName;
    }

    public String getKeyTransformerClassName() {
        return keyTransformerClassName;
    }

    //@Override
    public int getSize() {
        return dsc.getReplicaStore().size();
    }

    //@Override
    public int getSentSaveCount() {
        return saveCount.get();
    }

    //@Override
    public int getExecutedSaveCount() {
        return executedSaveCount.get();
    }

    //@Override
    public int getBatchSentCount() {
        return batchSentCount.get();
    }

    //@Override
    public int getLoadCount() {
        return loadCount.get();
    }

    //@Override
    public int getLoadSuccessCount() {
        return loadSuccessCount.get();
    }

    //@Override
    public int getLocalLoadSuccessCount() {
        return localLoadSuccessCount.get();
    }

    //@Override
    public int getSimpleLoadSuccessCount() {
        return simpleLoadSuccessCount.get();
    }

    //@Override
    public int getBroadcastLoadSuccessCount() {
        return broadcastLoadSuccessCount.get();
    }

    //@Override
    public int getLoadFailureCount() {
        return loadFailureCount.get();
    }

    //@Override
    public int getBatchReceivedCount() {
        return batchReceivedCount.get();
    }

    //@Override
    public int getSentRemoveCount() {
        return removeCount.get();
    }

    //@Override
    public int getExecutedRemoveCount() {
        return executedRemoveCount.get();
    }

    public int getFlushThreadFlushedCount() {
        return flushThreadFlushedCount.get();
    }

    public int getFlushThreadWakeupCount() {
        return flushThreadWakeupCount.get();
    }

    public int getSaveOnLoadCount() {
        return saveOnLoadCount.get();
    }

    public int getExpiredEntriesCount() {
        return removeExpiredEntriesCount.get();
    }

    public int getRemoveExpiredCallCount() {
        return removeExpiredCallCount.get();
    }

    public int getGmsSendCount() {
        return gmsSendCount.get();
    }

    public long getGmsSendBytesCount() {
        return gmsSendBytesCount.get();
    }


    //Mutators

    public int incrementBatchSentCount() {
        return batchSentCount.incrementAndGet();
    }

    public int incrementSaveCount() {
        return saveCount.incrementAndGet();
    }

    public int incrementExecutedSaveCount() {
        return executedSaveCount.incrementAndGet();
    }

    public int incrementLoadCount() {
        return loadCount.incrementAndGet();
    }

    public int incrementLoadSuccessCount() {
        return loadSuccessCount.incrementAndGet();
    }

    public int incrementLocalLoadSuccessCount() {
        return localLoadSuccessCount.incrementAndGet();
    }

    public int incrementSimpleLoadSuccessCount() {
        return simpleLoadSuccessCount.incrementAndGet();
    }

    public int incrementBroadcastLoadSuccessCount() {
        return broadcastLoadSuccessCount.incrementAndGet();
    }

    public int incrementLoadFailureCount() {
        return loadFailureCount.incrementAndGet();
    }

    public int incrementRemoveCount() {
        return removeCount.incrementAndGet();
    }

    public int incrementExecutedRemoveCount() {
        return executedRemoveCount.incrementAndGet();
    }

    public int incrementBatchReceivedCount() {
        return batchReceivedCount.incrementAndGet();
    }


    public int incrementFlushThreadWakeupCount() {
        return flushThreadWakeupCount.incrementAndGet();
    }


    public int incrementFlushThreadFlushedCount() {
        return flushThreadFlushedCount.incrementAndGet();
    }

    public int incrementSaveOnLoadCount() {
        return saveOnLoadCount.incrementAndGet();
    }


    public int incrementRemoveExpiredCallCount() {
        return removeExpiredCallCount.incrementAndGet();
    }

    public int incrementRemoveExpiredEntriesCount(int delta) {
        return removeExpiredEntriesCount.addAndGet(delta);
    }

    public int incrementGmsSendCount() {
        return gmsSendCount.incrementAndGet();
    }

    public long incrementGmsSendBytesCount(int delta) {
        return gmsSendBytesCount.addAndGet(delta);
    }


    public int updateExecutedRemoveCount(int delta) {
        return executedRemoveCount.addAndGet(delta);
    }

    //@Override
    public String toString() {
        return "ReplicatedDataStoreStatsHolder{" +
                "name=" + getStoreName() +
                ", keyClassName='" + getKeyClassName() + '\'' +
                ", valueClassName='" + getValueClassName() + '\'' +
                ", sentSaveCount=" + getSentSaveCount() +
                ", executedSaveCount=" + getExecutedSaveCount() +
                ", saveOnLoadCount=" + getSaveOnLoadCount() +
                ", loadCount=" + getLoadCount() +
                ", localLoadSuccessCount=" + getLocalLoadSuccessCount() +
                ", simpleLoadSuccessCount=" + getSimpleLoadSuccessCount() +
                ", broadcastLoadSuccessCount=" + getBroadcastLoadSuccessCount() +
                ", loadSuccessCount=" + getLoadSuccessCount() +
                ", loadFailureCount=" + getLoadFailureCount() +
                ", sentRemoveCount=" + getSentRemoveCount() +
                ", executedRemoveCount=" + getExecutedRemoveCount() +
                ", batchSentCount=" + getBatchSentCount() +
                ", batchReceivedCount=" + getBatchReceivedCount() +
                ", flushThreadWakeupCount=" + getFlushThreadWakeupCount() +
                ", flushThreadFlushedCount=" + getFlushThreadFlushedCount() +
                ", removeExpiredCallCount=" + getRemoveExpiredCallCount() +
                ", expiredEntriesCount=" + getExpiredEntriesCount() +
                ", gmsSendCount=" + getGmsSendCount() +
                ", gmsSendBytesCount=" + getGmsSendBytesCount() +
                '}';
    }
}
