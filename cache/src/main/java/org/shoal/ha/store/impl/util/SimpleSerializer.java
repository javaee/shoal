package org.shoal.ha.store.impl.util;

import java.io.*;

/**
 * @author Mahesh Kannan
 */
public class SimpleSerializer {

    public static void serializeString(ReplicationOutputStream ros, String str)
            throws IOException {
        int len = str == null ? 0 : str.length();
        ros.write(Utility.intToBytes(len));
        if (len > 0) {
            ros.write(str.getBytes());
        }
    }

    public static void serialize(ReplicationOutputStream ros, Object obj)
            throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        try {
            oos.writeObject(obj);
            oos.flush();
            byte[] data = bos.toByteArray();
            ros.write(Utility.intToBytes(data.length));
            ros.write(data);
        } finally {
            try {
                oos.close();
            } catch (IOException ioEx) {
            }
            try {
                bos.close();
            } catch (IOException ioEx) {
            }
        }
    }

    public static String deserializeString(byte[] data, int offset) {
        int len = Utility.bytesToInt(data, offset);
        return new String(data, offset+4, len);
    }

    public static Object deserialize(ClassLoader loader, byte[] data, int offset)
            throws ClassNotFoundException, IOException {
        int len = Utility.bytesToInt(data, offset);
        ByteArrayInputStream bis = new ByteArrayInputStream(data, offset + 4, len);
        ObjectInputStreamWithLoader ois = new ObjectInputStreamWithLoader(bis, loader);
        try {
            return ois.readObject();
        } finally {
            try {
                ois.close();
            } catch (IOException ioEx) {
            }
            try {
                bis.close();
            } catch (IOException ioEx) {
            }
        }
    }

}
