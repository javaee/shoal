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

import org.shoal.ha.cache.api.DataStoreException;
import org.shoal.ha.cache.api.ShoalCacheLoggerConstants;
import org.shoal.ha.cache.impl.command.ReplicationCommandOpcode;
import org.shoal.ha.cache.impl.store.DataStoreEntry;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public abstract class AbstractSaveCommand<K, V>
    extends AcknowledgedCommand<K, V> {

    protected transient static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_SAVE_COMMAND);

    protected long version;

    protected long lastAccessedAt;

    protected long maxIdleTime;

    private transient String targetInstanceName;

    protected AbstractSaveCommand(byte opcode) {
        super(opcode);
    }

    public AbstractSaveCommand(byte opcode, K k, long version, long lastAccessedAt, long maxIdleTime) {
        this(opcode);
        super.setKey(k);
        this.version = version;
        this.lastAccessedAt = lastAccessedAt;
        this.maxIdleTime = maxIdleTime;
    }

    public boolean beforeTransmit() {
        targetInstanceName = dsc.getKeyMapper().getMappedInstance(dsc.getGroupName(), getKey());
        super.setTargetName(targetInstanceName);
        super.beforeTransmit();
        return getTargetName() != null;
    }

    public abstract void execute(String initiator)
        throws DataStoreException;

    public String toString() {
        return getName() + "(" + getKey() + ")";
    }

    @Override
    public String getKeyMappingInfo() {
        return targetInstanceName;
    }

    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        out.writeLong(version);
        out.writeLong(lastAccessedAt);
        out.writeLong(maxIdleTime);
        
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, dsc.getServiceName() + " sending state for key = " + getKey() + "; version = " + version + "; lastAccessedAt = " + lastAccessedAt + "; to " + getTargetName());
        }
    }

    public long getVersion() {
        return version;
    }

    public long getLastAccessedAt() {
        return lastAccessedAt;
    }

    public long getMaxIdleTime() {
        return maxIdleTime;
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        version = in.readLong();
        lastAccessedAt = in.readLong();
        maxIdleTime = in.readLong();
    }

    public boolean hasState() {
        return false;
    }
    
}
