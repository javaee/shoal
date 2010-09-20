/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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
    }

    @Override
    public V getV(DataStoreEntry<K, V> replicationEntry)
            throws DataStoreException {

        return replicationEntry == null ? null : replicationEntry.getV();
    }

    @Override
    public void writeObject(ReplicationOutputStream ros, Object obj)
            throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = factory.createObjectOutputStream(bos);
        try {
            oos.writeObject(obj);
            oos.flush();
            ros.writeLengthPrefixedBytes(bos.toByteArray());
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
    public Object readObject(ReplicationInputStream ris) throws DataStoreException {
        try {
            //TODO: Scope for optimization here!! Instead of returning a new byte[] can we wrap
            //  the buf inside ris?

            ByteArrayInputStream bis = new ByteArrayInputStream(ris.readLengthPrefixedBytes());
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

}
