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

import org.glassfish.ha.store.util.SimpleMetadata;
import org.shoal.adapter.store.commands.AbstractSaveCommand;
import org.shoal.adapter.store.commands.LoadResponseCommand;
import org.shoal.adapter.store.commands.SaveCommand;
import org.shoal.adapter.store.commands.TouchCommand;
import org.shoal.ha.cache.api.*;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.logging.Level;

/**
 * @author Mahesh Kannan
 * 
 */
public class SimpleStoreableDataStoreEntryUpdater<K, V extends SimpleMetadata>
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
    public V extractVFrom(LoadResponseCommand<K, V> cmd)
        throws DataStoreException {
        return (V) new SimpleMetadata(cmd.getVersion(),
                    System.currentTimeMillis(), 600000, cmd.getRawV());
    }

    @Override
    public LoadResponseCommand<K, V> createLoadResponseCommand(DataStoreEntry<K, V> entry, K k, long minVersion) {
        LoadResponseCommand<K, V> cmd = null;
        if (entry != null && entry.isReplicaNode() && entry.getVersion() >= minVersion) {

            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "SimpleStoreableDataStoreEntryUpdater.createLoadResp "
                    + " entry.version " + entry.getVersion() + ">= " + minVersion
                    + "; rawV.length = " + entry.getRawV());
            }
            cmd = new LoadResponseCommand<K, V>(k, entry.getVersion(), entry.getRawV());
        } else {
            if (_logger.isLoggable(Level.FINE)) {
                String entryMsg = (entry == null) ? "NULL ENTRY"
                        : (entry.getVersion() + " >= " + minVersion);
                _logger.log(Level.FINE, "SimpleStoreableDataStoreEntryUpdater.createLoadResp " + entryMsg
                   + "; rawV.length = " + (entry == null ? " null " : "" + entry.getRawV()));
            }
            
            cmd = new LoadResponseCommand<K, V>(k, Long.MIN_VALUE, null);
        }
        return cmd;
    }

    @Override
    public void executeSave(DataStoreEntry<K, V> entry, SaveCommand<K, V> cmd) {
        if (entry != null && entry.getVersion() < cmd.getVersion()) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "SimpleStoreableDataStoreEntryUpdater.executeSave. SAVING ... "
                    + "entry = " + entry + "; entry.version = " + entry.getVersion()
                    + "; cmd.version = " + cmd.getVersion());
            }
            entry.setIsReplicaNode(true);
            super.updateMetaInfoInDataStoreEntry(entry, cmd);
            entry.setRawV(cmd.getRawV());
            super.printEntryInfo("SimpleStoreableDataStoreEntryUpdater:Updated", entry, cmd.getKey());
            updateFromPendingUpdates(entry);
            super.printEntryInfo("SimpleStoreableDataStoreEntryUpdater:Updated", entry, cmd.getKey());
        } else {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "SimpleStoreableDataStoreEntryUpdater.executeSave. IGNORING ... "
                    + "entry = " + entry + "; entry.version = " + entry.getVersion()
                    + "; cmd.version = " + cmd.getVersion());
            }
        }
    }

    @Override
    public void executeTouch(DataStoreEntry<K, V> entry, TouchCommand<K, V> touchCmd)
            throws DataStoreException {

        entry.addPendingUpdate(touchCmd);
        //TODO: For 'full save' mode there is no need to keep multiple touch commands
        updateFromPendingUpdates(entry);
        entry.setIsReplicaNode(true);
    }

    private void updateFromPendingUpdates(DataStoreEntry<K, V> entry) {
        TreeSet<AbstractSaveCommand<K, V>> pendingUpdates = entry.getPendingUpdates();
        if (pendingUpdates != null) {
            Iterator<AbstractSaveCommand<K, V>> iter = entry.getPendingUpdates().iterator();

            while (iter.hasNext()) {
                AbstractSaveCommand<K, V> pendingCmd = iter.next();
                if (entry.getVersion() > pendingCmd.getVersion()) {
                    iter.remove();
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE, "**Ignoring Pending touch because "
                                + entry.getVersion() + " > " + pendingCmd.getVersion());
                    }
                } else if (entry.getVersion() + 1 == pendingCmd.getVersion()) {
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE, "**Updated with Pending touch because, cmd.version = "
                                + entry.getVersion() + " & pending.version = " + pendingCmd.getVersion());
                    }
                    iter.remove();
                    super.updateMetaInfoInDataStoreEntry(entry, pendingCmd);
                } else {
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE, "**Added Touch as pending because, cmd.version = "
                                + entry.getVersion() + " & pending.version = " + pendingCmd.getVersion());
                    }
                    break;
                }
            }
        }
    }

    @Override
    public V getV(DataStoreEntry<K, V> entry)
        throws DataStoreException {
        V v = entry == null ? null : entry.getV();
        if (entry != null && v == null && entry.getRawV() != null) {
            SimpleMetadata ssm = new SimpleMetadata(entry.getVersion(),
                    entry.getLastAccessedAt(), entry.getMaxIdleTime(), entry.getRawV());
            v = (V) ssm;
        }

        return v;
    }


    @Override
    public byte[] getState(V v)
        throws DataStoreException {
        return v.getState();
    }

}
