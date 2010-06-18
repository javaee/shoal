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

import java.io.*;

/**
 * @author Mahesh Kannan
 */
public class ObjectKeyHelper<K>
        implements DataStoreKeyHelper<K> {

    private ObjectInputOutputStreamFactory factory;

    private ClassLoader loader;

    public ObjectKeyHelper(ClassLoader loader, ObjectInputOutputStreamFactory factory) {
        this.loader = loader;
        this.factory = factory;
    }

    @Override
    public void writeKey(ReplicationOutputStream ros, K k) throws IOException {
        byte[] data = null;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = factory.createObjectOutputStream(bos);
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

        int len = Utility.bytesToInt(data, index);
        ByteArrayInputStream bis = new ByteArrayInputStream(data, index+4, len);
        ObjectInputStream ois = null;
        try {
            ois = factory.createObjectInputStream(bis, loader);
            return (K) ois.readObject();
        } catch (Exception ex) {
            throw new DataStoreException("Exception during readKey", ex);
        } finally {
            try {ois.close();} catch (Exception ex1) {}
            try {bis.close();} catch (Exception ex2) {}
        }
    }
}
