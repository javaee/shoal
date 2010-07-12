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
import org.shoal.ha.cache.impl.util.CommandResponse;
import org.shoal.ha.cache.impl.util.ReplicationOutputStream;
import org.shoal.ha.cache.impl.util.Utility;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Mahesh Kannan
 * 
 */
public abstract class Command<K, V> {

    private byte opcode;

    private DataStoreContext<K, V> dsc;

    private CommandManager<K, V> cm;

    private String targetName;

    private boolean markedForResponseRequired;

    private CommandResponse cr;

    private long tokenId;

    protected Object result;

    private static final byte[] RESP_NOT_REQUIRED = new byte[] {0};

    private static final byte[] RESP_REQUIRED = new byte[] {1};

    protected Command(byte opcode) {
        this.opcode = opcode;
    }

    public void initialize(DataStoreContext<K, V> rs) {
        this.dsc = rs;
        this.cm = rs.getCommandManager();
    }

    protected DataStoreContext<K, V> getDataStoreContext() {
        return dsc;
    }

    protected CommandManager<K, V> getCommandManager() {
        return cm;
    }

    public String getTargetName() {
        return targetName;
    }

    public byte getOpcode() {
        return opcode;
    }

    protected void setTargetName(String val) {
        targetName = val;
    }

    protected void prepareToTransmit(DataStoreContext<K, V> ctx) {

    }

    public final void writeCommandState(ReplicationOutputStream bos)
        throws IOException {
        try {
            bos.write(new byte[] {getOpcode()});
            bos.write(markedForResponseRequired ? RESP_REQUIRED : RESP_NOT_REQUIRED);
            if (markedForResponseRequired) {
                bos.write(Utility.longToBytes(cr.getTokenId()));
            }
            writeCommandPayload(dsc, bos);
        } catch (IOException ex) {
            //TODO
        }
    }

    final void readCommandState(byte[] data, int offset)
        throws IOException, DataStoreException {
        if (data[offset+1] != 0) {
            markedForResponseRequired = true;
            tokenId = Utility.bytesToLong(data, offset+2);
            offset += 10;
            System.out.println("Just received a command that requires a response for: " + tokenId);
        } else {
            offset += 2;
        }
        readCommandPayload(dsc, data, offset);
    }

    protected abstract Command<K, V> createNewInstance();

    protected abstract void writeCommandPayload(DataStoreContext<K, V> t, ReplicationOutputStream ros)
            throws IOException;

    protected abstract void readCommandPayload(DataStoreContext<K, V> t, byte[] data, int offset)
            throws IOException, DataStoreException;

    public abstract void execute(DataStoreContext<K, V> ctx)
            throws DataStoreException;

    public void postTransmit(String target, boolean status) {

    }

}
