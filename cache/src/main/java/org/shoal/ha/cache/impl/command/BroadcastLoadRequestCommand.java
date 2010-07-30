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
import org.shoal.ha.cache.api.ShoalCacheLoggerConstants;
import org.shoal.ha.cache.impl.util.*;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public class BroadcastLoadRequestCommand<K, V>
        extends Command<K, V> {

    private static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_TOUCH_COMMAND);

    private K key;

    CommandResponse resp;

    private Future future;

    private long tokenId;

    private String originatingInstance;


    public BroadcastLoadRequestCommand() {
        this(null);
    }

    public BroadcastLoadRequestCommand(K key) {
        super(ReplicationCommandOpcode.LOAD_REQUEST);
        this.key = key;
    }

    @Override
    protected BroadcastLoadRequestCommand<K, V> createNewInstance() {
        return new BroadcastLoadRequestCommand<K, V>();
    }

    @Override
    public void writeCommandPayload(DataStoreContext<K, V> trans, ReplicationOutputStream ros) throws IOException {
        ros.write(Utility.longToBytes(resp.getTokenId()));
        int keyLen = ReplicationIOUtils.writeLengthPrefixedKey(key, trans.getDataStoreKeyHelper(), ros);
        ReplicationIOUtils.writeLengthPrefixedString(ros, originatingInstance);
        trans.getDataStoreKeyHelper().writeKey(ros, key);
        if (_logger.isLoggable(Level.INFO)) {
            _logger.log(Level.INFO, trans.getInstanceName() + " sending load " + key + " to " + getTargetName());
        }
    }

    @Override
    public void readCommandPayload(DataStoreContext<K, V> trans, byte[] data, int offset)
            throws IOException {
        tokenId = Utility.bytesToLong(data, offset);
        ReplicationIOUtils.KeyInfo keyInfo = ReplicationIOUtils.readLengthPrefixedKey(
                trans.getDataStoreKeyHelper(), data, offset + 8);
        key = (K) keyInfo.key;

        originatingInstance = ReplicationIOUtils.readLengthPrefixedString(
                data, offset + 8 + 4 + keyInfo.keyLen);
        if (_logger.isLoggable(Level.INFO)) {
            _logger.log(Level.INFO, trans.getInstanceName() + " received load " + key + " from " + originatingInstance);
        }
    }


    @Override
    protected void prepareToTransmit(DataStoreContext<K, V> ctx) {
        originatingInstance = ctx.getInstanceName();
//        String targetName = ctx.getKeyMapper().findReplicaInstance(ctx.getGroupName(), key);
//        setTargetName(targetName);
        ResponseMediator respMed = ctx.getResponseMediator();
        resp = respMed.createCommandResponse();

        future = resp.getFuture();
    }

    @Override
    public void execute(DataStoreContext<K, V> ctx) {
        try {
        DataStoreEntry<K, V> e = ctx.getReplicaStore().get(key);
        if (!originatingInstance.equals(ctx.getInstanceName())) {
            LoadResponseCommand<K, V> rsp = new LoadResponseCommand<K, V>(key, e, tokenId);
            rsp.setOriginatingInstance(originatingInstance);
            getCommandManager().execute(rsp);
        } else {
            resp.setResult(e);
        }
        } catch (DataStoreException dsEx) {
            resp.setException(dsEx);
        }
    }

    public DataStoreEntry<K, V> getResult(long waitFor, TimeUnit unit)
            throws DataStoreException {
        try {
            Object result = future.get(waitFor, unit);
            if (result instanceof Exception) {
                throw new DataStoreException((Exception) result);
            }
            return (DataStoreEntry<K, V>) result;
        } catch (DataStoreException dsEx) {
            throw dsEx;
        } catch (InterruptedException inEx) {
            _logger.log(Level.WARNING, "LoadRequestCommand Interrupted while waiting for result", inEx);
            throw new DataStoreException(inEx);
        } catch (TimeoutException timeoutEx) {
            _logger.log(Level.WARNING, "LoadRequestCommand timed out while waiting for result", timeoutEx);
            throw new DataStoreException(timeoutEx);
        } catch (ExecutionException exeEx) {
            _logger.log(Level.WARNING, "LoadRequestCommand got an exception while waiting for result", exeEx);
            throw new DataStoreException(exeEx);
        }
    }
}