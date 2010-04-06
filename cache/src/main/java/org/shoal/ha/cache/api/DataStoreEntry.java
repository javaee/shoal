/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.shoal.ha.cache.api;

import org.shoal.ha.cache.impl.util.ReplicationOutputStream;
import org.shoal.ha.cache.impl.util.Utility;

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
}