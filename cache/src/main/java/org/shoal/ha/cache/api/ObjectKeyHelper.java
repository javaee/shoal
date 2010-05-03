package org.shoal.ha.cache.api;

import org.shoal.ha.cache.impl.util.ReplicationOutputStream;
import org.shoal.ha.cache.impl.util.Utility;

import java.io.*;

/**
 * @author Mahesh Kannan
 */
public class ObjectKeyHelper<K>
        implements DataStoreKeyHelper<K> {

    private ClassLoader loader;

    public ObjectKeyHelper(ClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public void writeKey(ReplicationOutputStream ros, K k) throws IOException {
        ObjectInputOutputStreamFactory oiosFactory =
                ObjectInputOutputStreamFactoryRegistry.getObjectInputOutputStreamFactory();
        byte[] data = null;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = oiosFactory.createObjectOutputStream(bos);
            oos.writeObject(k);
            oos.flush();
            data = bos.toByteArray();
        } catch (Exception ex) {
            //
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException ioEx) {
                }
                if (bos != null) {
                    try {
                        bos.close();
                    } catch (IOException ioEx) {
                    }
                }

                int keyDataLength = data == null ? 0 : data.length;
                ros.write(Utility.intToBytes(keyDataLength));
                ros.write(data);
            }
        }
    }

    @Override
    public K readKey(byte[] data, int index)
            throws DataStoreException {

        ObjectInputOutputStreamFactory oiosFactory =
                ObjectInputOutputStreamFactoryRegistry.getObjectInputOutputStreamFactory();
        int len = Utility.bytesToInt(data, index);
        ByteArrayInputStream bis = new ByteArrayInputStream(data, index, len);
        ObjectInputStream ois = null;
        try {
            ois = oiosFactory.createObjectInputStream(bis, loader);
            return (K) ois.readObject();
        } catch (Exception ex) {
            throw new DataStoreException("Exception during readKey", ex);
        } finally {
            try {ois.close();} catch (Exception ex1) {}
            try {bis.close();} catch (Exception ex2) {}
        }
    }
}
