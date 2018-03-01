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

import org.shoal.ha.cache.impl.store.DataStoreEntry;
import org.shoal.ha.cache.api.DataStoreException;
import org.shoal.ha.cache.api.ShoalCacheLoggerConstants;
import org.shoal.ha.cache.impl.command.ReplicationCommandOpcode;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public class SaveCommand<K, V>
    extends AbstractSaveCommand<K, V> {

    private transient V v;

    private transient byte[] rawV;

    public SaveCommand() {
        super(ReplicationCommandOpcode.SAVE);
    }

    public SaveCommand(K k, V v, long version, long lastAccessedAt, long maxIdleTime) {
        super(ReplicationCommandOpcode.SAVE, k, version, lastAccessedAt, maxIdleTime);
        this.v = v;
    }

    @Override
    public void execute(String initiator)
        throws DataStoreException {

        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, dsc.getServiceName() + getName()
                    + " received save_command for key = " + getKey() + " from " + initiator
                    + "; version = " + getVersion());
        }

        DataStoreEntry<K, V> entry = dsc.getReplicaStore().getOrCreateEntry(getKey());
        synchronized (entry) {
            dsc.getDataStoreEntryUpdater().executeSave(entry, this);
        }

        if (dsc.isDoSynchronousReplication()) {
            _logger.log(Level.FINE, "SaveCommand Sending SIMPLE_ACK");
            super.sendAcknowledgement();
        }

        dsc.getDataStoreMBean().incrementExecutedSaveCount();
    }

    public String toString() {
        return getName() + "(" + getKey() + ")";
    }

    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {

        rawV = dsc.getDataStoreEntryUpdater().getState(v);
        out.writeObject(rawV);
        
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, dsc.getServiceName() + " sending save_command for key = " + getKey() + "; version = " + version + "; lastAccessedAt = " + lastAccessedAt + "; to " + getTargetName());
        }
    }

    public boolean hasState() {
        return true;
    }

    public byte[] getRawV() {
        return rawV;
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {

        rawV = (byte[]) in.readObject();
    }
    
}
