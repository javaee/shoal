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

import org.glassfish.ha.store.api.Storeable;
import org.shoal.adapter.store.commands.AbstractSaveCommand;
import org.shoal.adapter.store.commands.LoadResponseCommand;
import org.shoal.adapter.store.commands.SaveCommand;
import org.shoal.adapter.store.commands.TouchCommand;
import org.shoal.ha.cache.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;

/**
 * @author Mahesh Kannan
 */
public class StoreableDataStoreEntryUpdater<K, V extends Storeable>
        extends DataStoreEntryUpdater<K, V> {

    @Override
    public SaveCommand<K, V> createSaveCommand(DataStoreEntry<K, V> entry, K k, V v) {
        SaveCommand<K, V> cmd = new SaveCommand<K, V>(k, v, v._storeable_getVersion(),
                v._storeable_getLastAccessTime(), v._storeable_getMaxIdleTime());

        super.updateMetaInfoInDataStoreEntry(entry, cmd);
        entry.setIsReplicaNode(false);

        return cmd;
    }

    @Override
    public LoadResponseCommand<K, V> createLoadResponseCommand(DataStoreEntry<K, V> entry, K k, long minVersion)
            throws DataStoreException {
        LoadResponseCommand<K, V> cmd = null;
        if (entry != null && entry.isReplicaNode() && entry.getVersion() >= minVersion) {
            byte[] rawV = super.captureState(entry.getV());
            cmd = new LoadResponseCommand<K, V>(k, entry.getVersion(), rawV);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "StoreableDataStoreEntryUpdater Sending valid load response for key: " + k
                    + "; minVersion = " + minVersion + "; myVersion = " + entry.getVersion());
            }
        } else {
            if (_logger.isLoggable(Level.FINE)) {
                String entryMsg = (entry == null) ? "NULL ENTRY"
                        : (entry.getVersion() + " >= " + minVersion);
                _logger.log(Level.FINE, "StoreableDataStoreEntryUpdater.createLoadResp " + entryMsg
                   + "; rawV.length = " + (entry == null ? " null " : "" + entry.getRawV()));
            }
            cmd = new LoadResponseCommand<K, V>(k, Long.MIN_VALUE, null);
        }
        return cmd;
    }

    @Override
    public V extractVFrom(LoadResponseCommand<K, V> cmd)
            throws DataStoreException {
        return cmd.getRawV() == null ? null : super.deserializeV(cmd.getRawV());
    }

    @Override
    public void executeSave(DataStoreEntry<K, V> entry, SaveCommand<K, V> saveCmd)
            throws DataStoreException {

        if (entry.getV() == null) {
            //This is the only data that we have
            // So just deserialize and merge with subsequent data

            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "StoreableEntryUpdater.executeSave received (first copy) of key = "
                    + saveCmd.getKey()
                    + "; entry.version" + entry.getVersion()
                    + "; cmd.version" + saveCmd.getVersion());
            }
            V v = null;
            try {
                v = ctx.getValueClazz().newInstance();
                mergeIntoV(entry, v, saveCmd);
            } catch (Exception ex) {
                throw new DataStoreException(ex);
            }
            entry.setV(v);
            super.updateMetaInfoInDataStoreEntry(entry, saveCmd);
            super.printEntryInfo("Saved initial entry", entry, saveCmd.getKey());
            entry.setIsReplicaNode(true);
        } else {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "StoreableEntryUpdater received: key = " + saveCmd.getKey()
                    + "; entry.version" + entry.getVersion()
                    + "; cmd.version" + saveCmd.getVersion());
            }
            entry.addPendingUpdate(saveCmd);
            updateFromPendingUpdates(entry);
        }
    }


    @Override
    public void executeTouch(DataStoreEntry<K, V> entry, TouchCommand<K, V> touchCmd)
            throws DataStoreException {

        entry.addPendingUpdate(touchCmd);
        updateFromPendingUpdates(entry);
    }

    private void updateFromPendingUpdates(DataStoreEntry<K, V> entry)
        throws DataStoreException {
        Iterator<AbstractSaveCommand<K, V>> iter = entry.getPendingUpdates().iterator();
        while (iter.hasNext()) {
            AbstractSaveCommand<K, V> cmd = iter.next();
            if (entry.getVersion() + 1 == cmd.getVersion()) {
                iter.remove();
                mergeIntoV(entry, entry.getV(), cmd);
            } else {
                break;
            }
        }
        entry.setIsReplicaNode(true);
    }

    private void mergeIntoV(DataStoreEntry<K, V> entry, V v, AbstractSaveCommand<K, V> cmd)
            throws DataStoreException {
        v._storeable_setVersion(cmd.getVersion());
        v._storeable_setLastAccessTime(cmd.getLastAccessedAt());
        v._storeable_setMaxIdleTime(cmd.getMaxIdleTime());

        super.updateMetaInfoInDataStoreEntry(entry, cmd);

        if (cmd.hasState()) {
            ByteArrayInputStream bis = new ByteArrayInputStream(((SaveCommand) cmd).getRawV());
            try {
                v._storeable_readState(bis);
            } catch (Exception ex) {
                throw new DataStoreException("Error during updating existing V", ex);
            }
        }

        //Now that we have...
//        super.printEntryInfo("Merged entry from pending updates", entry, cmd.getKey());
    }

    @Override
    public byte[] getState(V v)
            throws DataStoreException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            v._storeable_writeState(bos);
        } catch (IOException ex) {
            throw new DataStoreException(ex);
        }

        byte[] data = bos.toByteArray();
//        System.out.println("Returning  a byte[] of length = " + data.length);

        return data;
    }

    @Override
    public V getV(DataStoreEntry<K, V> entry)
            throws DataStoreException {
        return entry.getV();
    }

}
