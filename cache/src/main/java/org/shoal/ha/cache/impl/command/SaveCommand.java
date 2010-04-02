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
import org.shoal.ha.cache.impl.util.ReplicationOutputStream;
import org.shoal.ha.cache.impl.util.Utility;

import java.io.IOException;

/**
 * @author Mahesh Kannan
 */
public class SaveCommand<K, V>
    extends Command<K, V> {

    private K k;

    private V v;

    public SaveCommand() {
        super(ReplicationCommandOpcode.SAVE);
    }

    public SaveCommand(K k, V v) {
        this();
        setKey(k);
        setValue(v);
    }

    public void setKey(K k) {
        this.k = k;
    }

    public void setValue(V v) {
        this.v = v;
    }

    @Override
    protected SaveCommand<K, V> createNewInstance() {
        return new SaveCommand<K, V>();
    }

    @Override
    protected void beforeTransmit(DataStoreContext<K, V> ctx) {
        setTargetName(ctx.getKeyMapper().getMappedInstance(ctx.getGroupName(), k));
    }

    @Override
    public void writeCommandPayload(DataStoreContext<K, V> ctx, ReplicationOutputStream bos)
        throws IOException {
        int keyLenMark = bos.mark();
        bos.write(Utility.intToBytes(0));
        ctx.getDataStoreKeyHelper().writeKey(bos, k);
        int valueOffset = bos.mark() - keyLenMark;
        bos.reWrite(keyLenMark, Utility.intToBytes(valueOffset));
        ctx.getDataStoreEntryHelper().writeObject(bos, v);
    }

    @Override
    public void readCommandPayload(DataStoreContext<K, V> ctx, byte[] data, int offset)
        throws IOException {
        int valueOffset = Utility.bytesToInt(data, offset);
        k = ctx.getDataStoreKeyHelper().readKey(data, offset+4);
        v = (V) ctx.getDataStoreEntryHelper().readObject(data, offset + valueOffset);
        //setReplicationEntry(entry);
    }

    @Override
    public void execute(DataStoreContext<K, V> ctx) {
        //getReplicaCache().put(entry);
//        System.out.println("SaveCommand["+getDataStoreContext().getMyName()+"] received: " + entry);
    }

}
