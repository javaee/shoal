package org.shoal.ha.store.api;

import org.shoal.ha.store.impl.util.ReplicationIOUtils;
import org.shoal.ha.store.impl.util.ReplicationOutputStream;
import org.shoal.ha.store.impl.util.SimpleSerializer;
import org.shoal.ha.store.impl.util.Utility;

import java.io.IOException;


/**
 * @author Mahesh Kannan
 */
public abstract class DataStoreEntry<K, V> {

    private K key;

    private long maxIdleTime;

    private long lastAccessedAt;

    public void setKey(K key) {
        this.key = key;
    }

    public K getKey() {
        return key;
    }

    public long getMaxIdleTime() {
        return maxIdleTime;
    }

    public void setMaxIdleTime(long maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    public long getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(long lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public final void writeDataStoreEntry(DataStoreContext<K, V> ctx,
                                 ReplicationOutputStream ros)
            throws IOException {
        int stateSizeMark = ros.mark();
        int stateSize = 0;
        ros.write(Utility.intToBytes(stateSize));
        stateSize = ros.mark();

        int headerSizeMark = ros.mark();
        int headerSize = 0;
        ros.write(Utility.intToBytes(headerSize));
        headerSize = ros.mark();

        ros.write(Utility.longToBytes(getMaxIdleTime()));
        ros.write(Utility.longToBytes(getLastAccessedAt()));

        ctx.getDataStoreKeyHelper().writeKey(ros, key);

        headerSize = ros.mark() - headerSize;
        ros.reWrite(headerSizeMark, Utility.intToBytes(headerSize));

        writePayloadState(ctx.getDataStoreEntryHelper(), ros);
        stateSize = ros.mark() - stateSize;
        ros.reWrite(stateSizeMark, Utility.intToBytes(stateSize));
    }

    public void readDataStoreEntry(DataStoreContext<K, V> ctx,
                          byte[] data, int index) throws IOException {
        int origIndex = index;
        int totalSz = Utility.bytesToInt(data, index);
        int headerSz = Utility.bytesToInt(data, index + 4);
        index += 8;

        setMaxIdleTime(Utility.bytesToLong(data, index));
        setLastAccessedAt(Utility.bytesToLong(data, index + 8));
        index += 16;

        setKey(ctx.getDataStoreKeyHelper().readKey(data, index));
        readPayloadState(ctx.getDataStoreEntryHelper(),
                data, origIndex + headerSz);

    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder("ReplicationState: [");
        sb.append(key).append("; ")
                .append("; ").append(maxIdleTime)
                .append("; ").append(lastAccessedAt);

        return sb.toString();
    }

    protected void writePayloadState(DataStoreEntryHelper<K, V> helper,
                                     ReplicationOutputStream ros)
            throws IOException {
    }

    protected void readPayloadState(DataStoreEntryHelper<K, V> helper,
                                    byte[] data, int index) {
    }

    protected V getV() {
        return null;
    }

}