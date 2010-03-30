package org.shoal.ha.store.api;

import org.shoal.ha.store.impl.util.ReplicationOutputStream;

import java.io.IOException;

/**
 * @author Mahesh Kannan
 */
public interface DataStoreEntryHelper<K, V> {

    public DataStoreEntry<K, V> createDataStoreEntry();

    public DataStoreEntry<K, V> createDataStoreEntry(K k, V v);

    public V getV(DataStoreEntry<K, V> entry)
            throws DataStoreException;

    public void writeObject(ReplicationOutputStream ros, Object obj)
            throws IOException;

    public Object readObject(byte[] delta, int offset)
        throws DataStoreException;

    public void updateDelta(K k, DataStoreEntry<K, V> entry, Object obj);

}
