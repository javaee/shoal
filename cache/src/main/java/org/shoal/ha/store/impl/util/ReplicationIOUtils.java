package org.shoal.ha.store.impl.util;

import org.shoal.ha.store.api.DataStoreEntryHelper;
import org.shoal.ha.store.impl.util.Utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author Mahesh Kannan
 */
public class ReplicationIOUtils {

    public static final void writeLengthPrefixedString(ByteArrayOutputStream bos, String str) {
        try {
            bos.write(Utility.intToBytes(str.length()));
            bos.write(str.getBytes());
        } catch (IOException ex) {
            //TODO
            try {
                bos.write(Utility.intToBytes(0));
            } catch (IOException ioEx) {
            }
        }
    }

    public static final String readLengthPrefixedString(byte[] data, int offset) {
        int len = Utility.bytesToInt(data, offset);
        return new String(data, offset+4, len);
    }

    public static final void write(ByteArrayOutputStream bos, int data) {
        try {
            bos.write(Utility.intToBytes(data));
        } catch (IOException ioEx) {
            //TODO
        }
    }


    public static final <K, V> void writeLengthPrefixedKey(K key, DataStoreEntryHelper<K, V> helper,
                              ReplicationOutputStream ros) throws IOException {
        int mark = ros.mark();
        int keyDataLength = 0;
        ros.write(keyDataLength);
        keyDataLength = ros.mark();
        helper.writeObject(ros, key);
        keyDataLength = ros.mark() - keyDataLength;
        ros.reWrite(mark, Utility.intToBytes(keyDataLength));
    }

    public static final <K, V> void writeLengthPrefixedHashKey(K hashKey, DataStoreEntryHelper<K, V> helper,
                              ReplicationOutputStream ros) throws IOException {
        int mark = ros.mark();
        int keyDataLength = 0;
        ros.write(keyDataLength);
        keyDataLength = ros.mark();
        helper.writeObject(ros, hashKey);
        keyDataLength = ros.mark() - keyDataLength;
        ros.reWrite(mark, Utility.intToBytes(keyDataLength));
    }

    public static final <K> TransformedKeyInfo<K> readTransformedKey(
            DataStoreEntryHelper<K, ?> trans, byte[] data, int offset) throws IOException {
        TransformedKeyInfo<K> keyInfo = new TransformedKeyInfo<K>();
        keyInfo.keyLen = Utility.bytesToInt(data, offset);
        if (keyInfo.keyLen > 0) {
            keyInfo.key = (K) trans.readObject(data, offset+4);
        }
        return keyInfo;
    }

    public static final <K> TransformedKeyInfo<K> readTransformedHashKey(
            DataStoreEntryHelper<K, ?> trans, byte[] data, int offset) throws IOException {
        TransformedKeyInfo<K> keyInfo = new TransformedKeyInfo<K>();
        keyInfo.keyLen = Utility.bytesToInt(data, offset);
        if (keyInfo.keyLen > 0) {
            keyInfo.key = (K) trans.readObject(data, offset+4);
        }
        return keyInfo;
    }

    public static class TransformedKeyInfo<K> {
        public K key;

        public int keyLen;
    }

}
