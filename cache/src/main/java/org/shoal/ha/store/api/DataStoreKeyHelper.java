package org.shoal.ha.store.api;

import org.shoal.ha.store.impl.util.ReplicationOutputStream;

import java.io.IOException;

/**
 * @author Mahesh Kannan
 */
public interface DataStoreKeyHelper<K> {

    public void writeKey(ReplicationOutputStream ros, K k)
            throws IOException;

    public K readKey(byte[] data, int index)
            throws DataStoreException;

}