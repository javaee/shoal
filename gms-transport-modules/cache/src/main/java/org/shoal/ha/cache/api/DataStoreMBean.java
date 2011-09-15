package org.shoal.ha.cache.api;

/**
 * @author Mahesh KannanR
 */
public interface DataStoreMBean {

    public String getStoreName();

    public String getKeyClassName();

    public String getValueClassName();

    public String getEntryUpdaterClassName();

    public String getKeyTransformerClassName();

    public int getSize();

    public int getSentSaveCount();

    public int getExecutedSaveCount();

    public int getBatchSentCount();

    public int getLoadCount();

    public int getLoadSuccessCount();

    public int getLocalLoadSuccessCount();

    public int getSimpleLoadSuccessCount();

    public int getBroadcastLoadSuccessCount();

    public int getSaveOnLoadCount();

    public int getLoadFailureCount();

    public int getBatchReceivedCount();

    public int getSentRemoveCount();

    public int getExecutedRemoveCount();
    
    public int getFlushThreadFlushedCount();

    public int getFlushThreadWakeupCount();

    public int getRemoveExpiredCallCount();

    public int getExpiredEntriesCount();

    public int getGmsSendCount();

    public long getGmsSendBytesCount();
}
