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
import org.shoal.ha.cache.impl.util.ReplicationState;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.util.*;
import org.shoal.ha.cache.impl.command.ReplicationCommandOpcode;

import java.io.IOException;

/**
 * @author Mahesh Kannan
 */
public class LoadResponseCommand<K, V>
        extends Command<K, V> {

    private K key;

    private V v;

    private long tokenId;

    private String originatingInstance;

    private LoadResponseCommand() {
        super(ReplicationCommandOpcode.LOAD_RESPONSE);
    }

    public LoadResponseCommand(K key, V v, long tokenId) {
        super(ReplicationCommandOpcode.LOAD_RESPONSE);
        this.key = key;
        this.v = v;
        this.tokenId = tokenId;
    }

    public void setOriginatingInstance(String originatingInstance) {
        this.originatingInstance = originatingInstance;
    }

    @Override
    protected LoadResponseCommand<K, V> createNewInstance() {
        return new LoadResponseCommand<K, V>();
    }

    @Override
    public void writeCommandPayload(DataStoreContext<K, V> trans, ReplicationOutputStream ros) throws IOException {
        int vMark = ros.mark();
        int vOffset = vMark;
        ros.write(Utility.intToBytes(vOffset));
        ros.write(Utility.longToBytes(tokenId));
        ReplicationIOUtils.writeLengthPrefixedString(ros, originatingInstance);
        trans.getDataStoreKeyHelper().writeKey(ros, key);
        vOffset = ros.mark() - vOffset;
        ros.reWrite(vMark, Utility.intToBytes(vOffset));
        ros.write(v == null ? 0 : 1);
        if (v != null) {
            trans.getDataStoreEntryHelper().writeObject(ros, v);
        }
    }

    @Override
    public void readCommandPayload(DataStoreContext<K, V> trans, byte[] data, int offset)
            throws IOException, DataStoreException {
        int vOffset = Utility.bytesToInt(data, offset);
        tokenId = Utility.bytesToLong(data, offset + 4);
        originatingInstance =
                ReplicationIOUtils.readLengthPrefixedString(data, offset + 12);
        int instOffset = 4 + ((originatingInstance == null) ? 0 : originatingInstance.length());
        key = (K) trans.getDataStoreKeyHelper().readKey(data, offset + 12 + instOffset);
        byte flag = data[offset + vOffset];
        if (flag != 0) {
            v = (V) trans.getDataStoreEntryHelper().readObject(data, offset + vOffset + 1);
        }
    }

    @Override
    protected void prepareToTransmit(DataStoreContext<K, V> ctx) {
        setTargetName(originatingInstance);
    }

    @Override
    public void execute(DataStoreContext<K, V> ctx) {
        ResponseMediator respMed = getDataStoreContext().getResponseMediator();
        CommandResponse resp = respMed.getCommandResponse(tokenId);
        if (resp != null) {
//            System.out.println("RECEIVED LOAD RESPONSE: " + tokenId + ", " + key + ", " + v + "  from " + originatingInstance);
            resp.setResult(v);
        }
    }


}