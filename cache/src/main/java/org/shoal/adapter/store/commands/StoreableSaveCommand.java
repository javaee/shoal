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

package org.shoal.adapter.store.commands;

import org.glassfish.ha.store.api.Storeable;
import org.shoal.ha.cache.api.DataStoreEntry;
import org.shoal.ha.cache.api.DataStoreException;
import org.shoal.ha.cache.api.ShoalCacheLoggerConstants;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.command.ReplicationCommandOpcode;
import org.shoal.ha.cache.impl.util.ReplicationInputStream;
import org.shoal.ha.cache.impl.util.ReplicationOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public class StoreableSaveCommand<K, V extends Storeable>
    extends AcknowledgedCommand<K, V> {

    private static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_SAVE_COMMAND);

    private K k;

    private Storeable v;

    private long version;

    private transient byte[] rawReadState;

    private boolean wasFullWrite;

    private DataStoreEntry<K, V> entry;

    private String replicaChoices;

    public StoreableSaveCommand() {
        super(ReplicationCommandOpcode.STOREABLE_SAVE);
    }

    public StoreableSaveCommand(K k, V v) {
        this();
        setKey(k);
        setValue(v);
        version = v._storeable_getVersion();
    }

    public void setKey(K k) {
        this.k = k;
    }

    public void setValue(V v) {
        this.v = v;
        version = v._storeable_getVersion();
    }

    public void setEntry(DataStoreEntry<K, V> entry) {
        this.entry = entry;
    }

    @Override
    protected StoreableSaveCommand<K, V> createNewInstance() {
        return new StoreableSaveCommand<K, V>();
    }

    @Override
    protected void writeCommandPayload(ReplicationOutputStream ros)
        throws IOException {
        if (dsc.isDoSynchronousReplication()) {
            super.writeAcknowledgementId(ros);
        }

        version = v._storeable_getVersion();
        boolean requiresFullSave = true;
        if (entry.getReplicaInstanceName() != null) {
            //FIXME: This requires more stronger check
            requiresFullSave = ! (entry.getReplicaInstanceName().equals(getTargetName()));
        }

        dsc.getDataStoreKeyHelper().writeKey(ros, k);
        ros.writeLong(v._storeable_getVersion());
        ros.writeBoolean(requiresFullSave);
        if (requiresFullSave) {
            dsc.getDataStoreEntryHelper().writeObject(ros, (V) v);
        } else {
            byte[] data = new byte[0];
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                v._storeable_writeState(bos);
                data = bos.toByteArray();
            } finally {
                try {bos.close(); } catch (Exception ex) {}
            }
            ros.writeLengthPrefixedBytes(data);
        }
    }


    @Override
    public boolean computeTarget() {

        replicaChoices = dsc.getKeyMapper().getMappedInstance(dsc.getGroupName(), k);
        super.setTargetName(replicaChoices);

        return getTargetName() != null;
    }

    @Override
    public void readCommandPayload(ReplicationInputStream ris)
        throws IOException {
        if (dsc.isDoSynchronousReplication()) {
            super.readAcknowledgementId(ris);
        }

        k = dsc.getDataStoreKeyHelper().readKey(ris);
        version = ris.readLong();
        wasFullWrite = ris.readBoolean();
        if (wasFullWrite) {
            v = (V) dsc.getDataStoreEntryHelper().readObject(ris);
        } else {
            rawReadState = ris.readLengthPrefixedBytes();
        }
    }

    @Override
    public void execute(String initiator)
        throws DataStoreException {

        DataStoreEntry<K, V> entry = dsc.getReplicaStore().getOrCreateEntry(k);
        synchronized (entry) {
            if (wasFullWrite) {
                entry.setV((V) v);
                entry.clearPendingUpdates();
            } else {
                V entryV = entry.getV();
                if (entryV != null) {
                    if (entryV._storeable_getVersion()  <= version) {
                        ByteArrayInputStream bis = new ByteArrayInputStream(rawReadState);
                        try {
                            entryV._storeable_readState(bis);
                        } catch (IOException ex) {
                            throw new DataStoreException(ex);
                        } finally {
                            try { bis.close(); } catch (Exception ex2) {}
                        }
                    } else if (entryV._storeable_getVersion() > version) {
                        _logger.log(Level.FINE, "Ignoring stale data " + entryV._storeable_getVersion() + " > " + version + "; for key: " + k);
                    }
                } else {
                    List<Command<K, V>> commands = entry.getPendingUpdates();
                    _logger.log(Level.FINE, "Added to pending updates[2].... for key: " + k);
                }
            }
        }

        if (dsc.isDoSynchronousReplication()) {
            _logger.log(Level.WARNING, "StoreableSaveCommand Sending SIMPLE_ACK");
            super.sendAcknowledgement();
        }
        
    }

    public String toString() {
        return getName() + "(" + k + ")";
    }

    @Override
    public String getKeyMappingInfo() {
        return replicaChoices;
    }

    @Override
    public void onSuccess() {
        if (dsc.isDoSynchronousReplication()) {
            try {
                super.onSuccess();
                super.waitForAck();
            } catch (Exception ex) {
               _logger.log(Level.WARNING, "** Got exception: " + ex);
            }
        }
    }

}
