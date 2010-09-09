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

package org.shoal.ha.cache.impl.command;

import org.shoal.ha.cache.api.DataStoreContext;
import org.shoal.ha.cache.api.DataStoreException;
import org.shoal.ha.cache.api.ShoalCacheLoggerConstants;
import org.shoal.ha.cache.api.TooManyRetriesException;
import org.shoal.ha.cache.impl.util.ReplicationInputStream;
import org.shoal.ha.cache.impl.util.ReplicationOutputStream;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 * 
 */
public abstract class Command<K, V> {

    private static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_SAVE_COMMAND);

    private byte opcode;

    protected DataStoreContext<K, V> dsc;

    private CommandManager<K, V> cm;

    private ReplicationOutputStream cachedROS;

    private String commandName;

    protected String targetInstanceName;

    private int retryCount = 0;

    private long retryAfterMillis = 500;

    private boolean done = true;

    protected Command(byte opcode) {
        this.opcode = opcode;
        this.commandName = this.getClass().getName();
        int index = commandName.lastIndexOf('.');
        commandName = commandName.substring(index+1);
    }

    public final void initialize(DataStoreContext<K, V> rs) {
        this.dsc = rs;
        this.cm = rs.getCommandManager();
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

    public final boolean isRetried() {
        return retryCount > 0;
    }

    public final void prepareTransmit(DataStoreContext<K, V> ctx)
        throws IOException {
        cachedROS = new ReplicationOutputStream();
        cachedROS.write(getOpcode());

        if (computeTarget()) {
            writeCommandPayload(cachedROS);
        } else {
            _logger.log(Level.WARNING, "Aborting command transmission for " + getName() + " because computeTarget returned false");
        }
    }

    public final void write(ReplicationOutputStream globalROS)
        throws IOException {
        try {
            byte[] data = cachedROS.toByteArray();
            globalROS.write(data);
            globalROS.flush();
        } catch (IOException ex) {
           ex.printStackTrace();
        }
    }

    public final void prepareToExecute(ReplicationInputStream ris)
        throws IOException, DataStoreException {
        ris.read(); //Don't remove this
        readCommandPayload(ris);
    }

    protected void selectReplicaInstance(K key) {
        targetInstanceName = dsc.getKeyMapper().getMappedInstance(dsc.getGroupName(), key);
    }

    public String getKeyMappingInfo() {
        return targetInstanceName == null ? "" : targetInstanceName;
    }

    public final String getName() {
        return commandName + ":" + opcode;
    }

    public boolean computeTarget() {
        //WARNING: DO NOT DO:  setTargetName(null);
        return true;
    }

    protected final void reExecute()
        throws DataStoreException {
        if (retryCount++ < 3) {
            dsc.getCommandManager().reExecute(this);
        } else {
            throw new DataStoreException("Too many retries...");
        }
    }

    public void onSuccess() {
        retryCount++;
        done = true;
    }

    public void onError(Throwable th)
        throws DataStoreException {
        if ((retryCount++ < 4) && (!done)) {
            try {
                Thread.sleep(retryCount * retryAfterMillis);
            } catch (Exception ex) {
                //TODO
            }

            dsc.getCommandManager().reExecute(this);
        } else {
            String message = getName() + " giving up after " + retryCount + " retries...";
            _logger.log(Level.WARNING, message);
            throw new TooManyRetriesException(message);
        }
    }

    protected abstract void writeCommandPayload(ReplicationOutputStream ros)
        throws IOException;

    protected abstract void readCommandPayload(ReplicationInputStream ris)
        throws IOException;

    protected abstract Command<K, V> createNewInstance();

    public abstract void execute(String initiator)
            throws DataStoreException;

}
