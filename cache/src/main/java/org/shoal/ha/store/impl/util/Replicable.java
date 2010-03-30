package org.shoal.ha.store.impl.util;

import java.io.IOException;

/**
 * @author Mahesh Kannan
 */
public interface Replicable {

    public void writeState(ReplicationOutputStream ros)
            throws IOException;

    public void readState(byte[] data, int offset);

}
