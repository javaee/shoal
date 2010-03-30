package org.shoal.ha.store.impl.util;

import java.io.ByteArrayOutputStream;

/**
 * @author Mahesh Kannan
 */
public class ReplicationOutputStream
    extends ByteArrayOutputStream {

    public int mark() {
        return size();
    }

    public void reWrite(int mark, byte[] data) {
        System.arraycopy(data, 0, buf, mark, data.length);
    }

}
