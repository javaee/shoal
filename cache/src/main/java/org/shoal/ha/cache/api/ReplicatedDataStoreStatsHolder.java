package org.shoal.ha.cache.api;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mahesh Kannan
 *
 */
public class ReplicatedDataStoreStatsHolder<K, V> implements DataStoreMBean {

    private DataStoreContext<K, V> dsc;

    private String keyClassName;

    private String valueClassName;

    private AtomicInteger saveCount = new AtomicInteger(0);

    private AtomicInteger loadCount = new AtomicInteger(0);

    private AtomicInteger loadSuccessCount = new AtomicInteger(0);

    private AtomicInteger localLoadSuccessCount = new AtomicInteger(0);

    private AtomicInteger simpleLoadSuccessCount = new AtomicInteger(0);

    private AtomicInteger broadcastLoadSuccessCount = new AtomicInteger(0);

    private AtomicInteger loadFailureCount = new AtomicInteger(0);

    private AtomicInteger removeCount = new AtomicInteger(0);

    private AtomicInteger batchSentCount = new AtomicInteger(0);

    private AtomicInteger batchReceivedCount = new AtomicInteger(0);

    private AtomicInteger flushThreadWakeupCount = new AtomicInteger(0);

    private AtomicInteger flushThreadFlushedCount = new AtomicInteger(0);


    public ReplicatedDataStoreStatsHolder(DataStoreContext<K, V> dsc) {
        this.dsc = dsc;

        this.keyClassName = (dsc.getKeyClazz() != null) ? dsc.getKeyClazz().getName() : "?";
        this.valueClassName = (dsc.getValueClazz() != null) ? dsc.getValueClazz().getName() : "?";
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

    //@Override
    public int getSize() {
        return dsc.getReplicaStore().size();
    }

    
    //@Override
    public int getSaveCount() {
        return saveCount.get();
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
    public int getRemoveCount() {
        return removeCount.get();
    }

    public int getFlushThreadFlushedCount() {
        return flushThreadFlushedCount.get();
    }

    public int getFlushThreadWakeupCount() {
        return flushThreadWakeupCount.get();
    }

    public int incrementBatchSentCount() {
        return batchSentCount.incrementAndGet();
    }

    public int incrementSaveCount() {
        return saveCount.incrementAndGet();
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

    public int incrementBatchReceivedCount() {
        return batchReceivedCount.incrementAndGet();
    }


    public int incrementFlushThreadWakeupCount() {
        return flushThreadWakeupCount.incrementAndGet();
    }


    public int incrementFlushThreadFlushhedCount() {
        return flushThreadFlushedCount.incrementAndGet();
    }

    //@Override
    public String toString() {
        return "ReplicatedDataStoreStatsHolder{" +
                "name=" + getStoreName() +
                ", keyClassName='" + getKeyClassName() + '\'' +
                ", valueClassName='" + getValueClassName() + '\'' +
                ", saveCount=" + getSaveCount() +
                ", loadCount=" + getLoadCount() +
                ", localLoadSuccessCount=" + getLocalLoadSuccessCount() +
                ", simpleLoadSuccessCount=" + getSimpleLoadSuccessCount() +
                ", broadcastLoadSuccessCount=" + getBroadcastLoadSuccessCount() +
                ", loadSuccessCount=" + getLoadSuccessCount() +
                ", loadFailureCount=" + getLoadFailureCount() +
                ", removeCount=" + getRemoveCount() +
                ", batchSentCount=" + getBatchSentCount() +
                ", batchReceivedCount=" + getBatchReceivedCount() +
                ", flushThreadWakeupCount=" + getFlushThreadWakeupCount() +
                ", flushThreadFlushedCount=" + getFlushThreadFlushedCount() +
                '}';
    }
}
