/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.shoal.ha.cache.impl.store;

import org.shoal.adapter.store.commands.AbstractSaveCommand;
import org.shoal.adapter.store.commands.LoadResponseCommand;
import org.shoal.adapter.store.commands.SaveCommand;
import org.shoal.adapter.store.commands.TouchCommand;
import org.shoal.ha.cache.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public abstract class DataStoreEntryUpdater<K, V> {

    protected transient static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_SAVE_COMMAND);

    protected DataStoreContext<K, V> ctx;

    public void initialize(DataStoreContext<K, V> ctx) {
        this.ctx = ctx;
        _logger.log(Level.FINE, "** INITIALIZED DSEUpdater: " + this.getClass().getName());
    }

    protected byte[] captureState(V v)
        throws DataStoreException {
        byte[] result = null;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(v);
            oos.close();

            result = bos.toByteArray();
        } catch (Exception ex) {
            throw new DataStoreException("Error during prepareToTransmit()", ex);
        } finally {
            try { oos.close(); } catch (Exception ex) {_logger.log(Level.FINEST, "Ignorable error while closing ObjectOutputStream");}
            try { bos.close(); } catch (Exception ex) {_logger.log(Level.FINEST, "Ignorable error while closing ByteArrayOutputStream");}
        }

        return result;
    }

    protected V deserializeV(byte[] rawV)
            throws DataStoreException {
        ClassLoader loader = ctx.getClassLoader();
        V v = null;
        ByteArrayInputStream bis = new ByteArrayInputStream(rawV);
        ObjectInputStreamWithLoader ois = null;
        try {
            ois = new ObjectInputStreamWithLoader(bis, loader);
            v = (V) ois.readObject();
        } catch (Exception ex) {
            throw new DataStoreException(ex);
        } finally {
            try {
                ois.close();
            } catch (Exception ex) {
            }
            try {
                bis.close();
            } catch (Exception ex) {
            }
        }

        return v;
    }

    protected void updateMetaInfoInDataStoreEntry(DataStoreEntry<K, V> entry, AbstractSaveCommand<K, V> cmd) {
        entry.setVersion(cmd.getVersion());
        entry.setLastAccessedAt(cmd.getLastAccessedAt());
        entry.setMaxIdleTime(cmd.getMaxIdleTime());
    }

    protected void printEntryInfo(String msg, DataStoreEntry<K, V> entry, K key) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "executeSave:" + msg + " key = "
                    + key + "; entry.version = " + entry.getVersion() + " ; entry.lastAccess = " + entry.getLastAccessedAt()
                    + "; entry.maxIdle = " + entry.getMaxIdleTime());
        }
    }

    public abstract SaveCommand<K, V> createSaveCommand(DataStoreEntry<K, V> entry, K k, V v);

    public abstract LoadResponseCommand<K, V> createLoadResponseCommand(DataStoreEntry<K, V> entry, K k, long minVersion)
            throws DataStoreException;

    public abstract void executeSave(DataStoreEntry<K, V> entry, SaveCommand<K, V> cmd)
            throws DataStoreException;

    public abstract void executeTouch(DataStoreEntry<K, V> entry, TouchCommand<K, V> cmd)
            throws DataStoreException;

    public abstract V getV(DataStoreEntry<K, V> entry)
            throws DataStoreException;

    public abstract V extractVFrom(LoadResponseCommand<K, V> cmd)
            throws DataStoreException;
    
    public abstract byte[] getState(V v)
            throws DataStoreException;

}
