package org.shoal.ha.cache.impl.util;

import org.shoal.ha.cache.api.DataStoreException;
import org.shoal.ha.cache.api.DataStoreKeyHelper;

import java.io.IOException;

/**
 * @author Mahesh Kannan
 */
public class StringKeyHelper
    implements DataStoreKeyHelper<String> {

    @Override
    public void writeKey(ReplicationOutputStream ros, String key) throws IOException {
        ros.write(Utility.intToBytes(key.length()));
        ros.write(key.getBytes());
    }

    @Override
    public String readKey(byte[] data, int index) throws DataStoreException {
        String key = null;
        int len = Utility.bytesToInt(data, index);
        key = new String(data, index+4, len);
        return key;
    }
}
