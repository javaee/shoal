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
import org.shoal.ha.cache.api.DataStoreEntry;
import org.shoal.ha.cache.api.DataStoreException;
import org.shoal.ha.cache.impl.util.ReplicationState;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.util.ReplicationOutputStream;
import org.shoal.ha.cache.impl.command.ReplicationCommandOpcode;

import java.io.IOException;

/**
 * @author Mahesh Kannan
 */
public class LoadRequestCommand<K, V>
        extends Command<K, V> {

    private K key;

    private DataStoreEntry<K, V> entry;

    public LoadRequestCommand() {
        this(null);
    }

    public LoadRequestCommand(K key) {
        super(ReplicationCommandOpcode.LOAD_REQUEST);
        this.key = key;
    }

    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public DataStoreEntry<K, V> getReplicationEntry() {
        return entry;
    }

    @Override
    protected LoadRequestCommand<K, V> createNewInstance() {
        return new LoadRequestCommand<K, V>();
    }

    @Override
    public void writeCommandPayload(DataStoreContext<K, V> trans, ReplicationOutputStream ros) throws IOException {
        trans.getDataStoreKeyHelper().writeKey(ros, key);
    }

    @Override
    public void readCommandPayload(DataStoreContext<K, V> trans, byte[] data, int offset)
        throws DataStoreException {
        setKey((K) trans.getDataStoreKeyHelper().readKey(data, offset));
    }

    @Override
    public void execute() {
        //System.out.println("LoadRequestCommand: Received FROM: " + ctx.getInitiatorName() + " - " + this);
        //ReplicationState result = getReplicationService().getReplicaCache().get(key);
        LoadResponseCommand<K, V> rsp = new LoadResponseCommand<K, V>(key);
        if (isMarkedForResponseRequired()) {
            rsp.setTokenId(getTokenId());
        }
        rsp.setReplicationState((ReplicationState<K, V>) result);
        //rsp.setDestinationName(ctx.getInitiatorName());
        getCommandManager().execute(rsp);
    }

    public DataStoreEntry<K, V> getResult() {
        if (isMarkedForResponseRequired()) {
            entry = (DataStoreEntry) getResult(15000);
        }

        return entry;
    }
}