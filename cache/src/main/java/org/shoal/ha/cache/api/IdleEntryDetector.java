package org.shoal.ha.cache.api;

/**
 * @author Mahesh Kannan
 */
public interface IdleEntryDetector<K, V> {

    public boolean isIdle(DataStoreEntry<K, V> entry, long nowInMillis);

}
