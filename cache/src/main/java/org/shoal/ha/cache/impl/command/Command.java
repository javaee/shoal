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

package org.shoal.ha.cache.impl.command;

import org.shoal.ha.cache.api.DataStoreContext;
import org.shoal.ha.cache.api.DataStoreException;
import org.shoal.ha.cache.api.ShoalCacheLoggerConstants;
import org.shoal.ha.cache.api.TooManyRetriesException;
import org.shoal.ha.cache.impl.util.ReplicationInputStream;
import org.shoal.ha.cache.impl.util.ReplicationOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 * 
 */
public abstract class Command<K, V>
    implements Serializable {

    private transient static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_SAVE_COMMAND);

    private byte opcode;

    private K key;

    protected transient DataStoreContext<K, V> dsc;

    private transient CommandManager<K, V> cm;

    private transient byte[] cachedSerializedState;

    private transient String commandName;

    protected transient String targetInstanceName;

    protected Command(byte opcode) {
        this.opcode = opcode;
    }

    public final void initialize(DataStoreContext<K, V> rs) {
        this.dsc = rs;
        this.cm = rs.getCommandManager();
        
        this.commandName = this.getClass().getName();
        int index = commandName.lastIndexOf('.');
        commandName = commandName.substring(index+1);
    }

    protected final void setKey(K k) {
        this.key = k;
    }

    public final K getKey() {
        return key;
    }

    protected final DataStoreContext<K, V> getDataStoreContext() {
        return dsc;
    }

    protected final CommandManager<K, V> getCommandManager() {
        return cm;
    }

    public String getTargetName() {
        return targetInstanceName;
    }
    
    public final byte getOpcode() {
        return opcode;
    }

    protected void setTargetName(String val) {
        this.targetInstanceName = val;
    }

    public final void prepareTransmit(DataStoreContext<K, V> ctx)
            throws IOException {

        if (beforeTransmit()) {
            if (! dsc.isSafeToDelayCaptureState()) {
                cachedSerializedState = captureState(this);
            }

        } else {
            _logger.log(Level.WARNING, "Aborting command transmission for " + getName() + " because beforeTransmit returned false");
        }
    }

    protected static byte[] captureState(Object obj)
        throws DataStoreException {
        byte[] result = null;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.close();

            result = bos.toByteArray();
        } catch (Exception ex) {
            throw new DataStoreException("Error during prepareToTransmit()", ex);
        } finally {
            try { oos.close(); } catch (Exception ex) {}
            try { bos.close(); } catch (Exception ex) {}
        }

        return result;
    }

    public final byte[] getSerializedState()
        throws DataStoreException {
        if (dsc.isSafeToDelayCaptureState()) {
            cachedSerializedState = captureState(this);
        }
        return cachedSerializedState;
    }

    public String getKeyMappingInfo() {
        return targetInstanceName == null ? "" : targetInstanceName;
    }

    public final String getName() {
        return commandName + ":" + opcode;
    }

    public abstract void execute(String initiator)
            throws DataStoreException;

    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        out.writeByte(opcode);
        out.writeObject(key);
        int len = cachedSerializedState != null ? cachedSerializedState.length : 0;
        out.writeInt(len);
        if (cachedSerializedState != null) {
            out.write(cachedSerializedState);
        }
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        opcode = in.readByte();
        key = (K) in.readObject();
        int len = in.readInt();
        cachedSerializedState = new byte[len];
        if (len > 0) {
            in.readFully(cachedSerializedState);
        }
    }

    public void onSuccess() {

    }

    public void onFailure() {
        
    }
    public String toString() {
        return getName() + "(" + key + ")";
    }

    protected abstract boolean beforeTransmit()
            throws IOException;

}
