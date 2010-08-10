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

package org.shoal.adapter.store.commands;

import org.glassfish.ha.store.api.Storeable;
import org.shoal.ha.cache.api.DataStoreEntry;
import org.shoal.ha.cache.api.DataStoreException;
import org.shoal.ha.cache.api.ShoalCacheLoggerConstants;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.command.ReplicationCommandOpcode;
import org.shoal.ha.cache.impl.util.CommandResponse;
import org.shoal.ha.cache.impl.util.ReplicationInputStream;
import org.shoal.ha.cache.impl.util.ReplicationOutputStream;
import org.shoal.ha.cache.impl.util.ResponseMediator;

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
public class StoreableBroadcastLoadRequestCommand<K, V extends Storeable>
        extends Command<K, V> {

    private static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_LOAD_REQUEST_COMMAND);

    private K key;

    CommandResponse resp;

    private Future future;

    private long tokenId;

    private Long version;

    private String originatingInstance;


    public StoreableBroadcastLoadRequestCommand() {
        this(null, null);
    }

    public StoreableBroadcastLoadRequestCommand(K key, Long version) {
        super(ReplicationCommandOpcode.LOAD_REQUEST);
        this.key = key;
        this.version = version;
    }

    @Override
    protected StoreableBroadcastLoadRequestCommand<K, V> createNewInstance() {
        return new StoreableBroadcastLoadRequestCommand<K, V>();
    }

    @Override
    protected void writeCommandPayload(ReplicationOutputStream ros)
        throws IOException {
        originatingInstance = dsc.getInstanceName();
//        String targetName = ctx.getKeyMapper().findReplicaInstance(ctx.getGroupName(), key);
        setTargetName(null);
        ResponseMediator respMed = dsc.getResponseMediator();
        resp = respMed.createCommandResponse();

        future = resp.getFuture();


        ros.writeLong(resp.getTokenId());
        ros.writeLong(version == null ? Long.MIN_VALUE : version);
        dsc.getDataStoreKeyHelper().writeKey(ros, key);
        ros.writeLengthPrefixedString(originatingInstance);
        if (_logger.isLoggable(Level.INFO)) {
            _logger.log(Level.INFO, dsc.getInstanceName() + " sending broadcast_load " + key + " to " + getTargetName());
        }
    }

    @Override
    public void readCommandPayload(ReplicationInputStream ris)
        throws IOException {

        tokenId = ris.readLong();
        version = ris.readLong();
        key = dsc.getDataStoreKeyHelper().readKey(ris);
        originatingInstance = ris.readLengthPrefixedString();

    }

    @Override
    public void execute(String initiator) {
        if (_logger.isLoggable(Level.INFO)) {
            _logger.log(Level.INFO, dsc.getInstanceName() + " received broadcast_load " + key + " from " + originatingInstance);
        }
        try {
            DataStoreEntry<K, V> e = dsc.getReplicaStore().getEntry(key);

            //Since this is a broadcast, we ourself may get the load_request
            if (!originatingInstance.equals(dsc.getInstanceName())) {
                if (e != null) {
                    V v = e.getV();
                    if (v != null && v._storeable_getVersion() >= version) {
                        StoreableLoadResponseCommand<K, V> rsp = new StoreableLoadResponseCommand<K, V>(
                                key, dsc.getDataStoreEntryHelper().getV(e), tokenId);
                        rsp.setOriginatingInstance(originatingInstance);
                        getCommandManager().execute(rsp);
                    } else if (v != null) {
                        e.markAsRemoved("Removed due to stale version. requested: "
                                + version + " > " + v._storeable_getVersion());
                    }
                }
            } else {
                //Ignore
            }
        } catch (DataStoreException dsEx) {
            resp.setException(dsEx);
        }
    }

    public String getRespondingInstanceName() {
        return resp.getRespondingInstanceName();
    }

    public V getResult(long waitFor, TimeUnit unit)
            throws DataStoreException {
        try {
            Object result = future.get(waitFor, unit);
            if (result instanceof Exception) {
                throw new DataStoreException((Exception) result);
            }
            return (V) result;
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