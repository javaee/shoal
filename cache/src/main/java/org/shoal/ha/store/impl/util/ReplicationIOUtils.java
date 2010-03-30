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
