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

package org.shoal.ha.cache.impl.util;

import org.shoal.ha.cache.api.DataStoreEntry;
import org.shoal.ha.cache.api.DataStoreEntryHelper;
import org.shoal.ha.cache.api.DataStoreException;
import org.shoal.ha.cache.api.ObjectInputOutputStreamFactory;

import java.io.*;

/**
 * @author Mahesh Kannan
 */
public class DefaultDataStoreEntryHelper<K, V>
        implements DataStoreEntryHelper<K, V> {

    private ObjectInputOutputStreamFactory factory;

    private ClassLoader loader;

    private long defaultMaxIdleTime;

    public DefaultDataStoreEntryHelper(ObjectInputOutputStreamFactory factory,
                                       ClassLoader loader, long defaultMaxIdleTime) {
        this.factory = factory;
        this.loader = loader;
        this.defaultMaxIdleTime = defaultMaxIdleTime;

        System.out.println("**Created DefaultDataStoreEntryHelper:: " + loader);
        (new Throwable("******* DefaultDataStoreEntryHelper ********")).printStackTrace();
    }

    @Override
    public V getV(DataStoreEntry<K, V> replicationEntry)
            throws DataStoreException {
        byte[] data = (byte[]) replicationEntry.getState();
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = null;
        V v = null;
        try {
            ois = factory.createObjectInputStream(bis, loader);
            v = (V) ois.readObject();
        } catch (IOException ioEx) {
            throw new DataStoreException(ioEx);
        } catch (ClassNotFoundException cnfEx) {
            throw new DataStoreException(cnfEx);
        } finally {
            try {
                ois.close();
            } catch (IOException ioEx1) {
            }
            try {
                bis.close();
            } catch (IOException ioEx2) {
            }
        }

        return v;
    }

    @Override
    public void writeObject(ReplicationOutputStream ros, Object obj)
            throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = factory.createObjectOutputStream(bos);
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

    @Override
    public Object readObject(byte[] data, int index) throws DataStoreException {
        try {
            int len = Utility.bytesToInt(data, index);
            ByteArrayInputStream bis = new ByteArrayInputStream(data, index + 4, len);
            ObjectInputStream ois = factory.createObjectInputStream(bis, loader);
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
        } catch (ClassNotFoundException cnfEx) {
            throw new DataStoreException("Cannot desrialize value", cnfEx);
        } catch (IOException ioEx) {
            throw new DataStoreException("Cannot desrialize value", ioEx);
        }
    }

    @Override
    public void updateState(K k, DataStoreEntry<K, V> kvDataStoreEntry, Object obj) {
        kvDataStoreEntry.setKey(k);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
        } catch (IOException ioEx) {
            //TODO
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

        byte[] data = bos.toByteArray();
        kvDataStoreEntry.setState(data);
        kvDataStoreEntry.setLastAccessedAt(System.currentTimeMillis());
        kvDataStoreEntry.setMaxIdleTime(this.defaultMaxIdleTime);
    }

    @Override
    public void updateMetadata(K k, DataStoreEntry<K, V> kvDataStoreEntry, Object obj) {
        //We do not know about the type of object here

        kvDataStoreEntry.setVersion(Long.MAX_VALUE - 1);
        kvDataStoreEntry.setLastAccessedAt(System.currentTimeMillis());
        kvDataStoreEntry.setMaxIdleTime(this.defaultMaxIdleTime);
    }
}
