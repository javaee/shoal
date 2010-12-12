package org.shoal.ha.cache.api;

/**
 * @author Mahesh KannanR
 */
public interface DataStoreMBean {

    public String getStoreName();

    public String getKeyClassName();

    public String getValueClassName();

    public int getSize();

    public int getSaveCount();

    public int getBatchSentCount();

    public int getLoadCount();

    public int getLoadSuccessCount();

    public int getLocalLoadSuccessCount();

    public int getSimpleLoadSuccessCount();

    public int getBroadcastLoadSuccessCount();

    public int getLoadFailureCount();

    public int getBatchReceivedCount();

    public int getRemoveCount();
    
    public int getFlushThreadFlushedCount();

    public int getFlushThreadWakeupCount();

}
