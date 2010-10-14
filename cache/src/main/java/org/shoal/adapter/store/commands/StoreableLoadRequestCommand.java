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
public class StoreableLoadRequestCommand<K, V extends Storeable>
        extends Command<K, V> {

    private static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_LOAD_REQUEST_COMMAND);

    private K key;

    private Long minimumRequiredVersion = Long.MIN_VALUE;

    CommandResponse resp;

    private Future future;

    private long tokenId;

    private String originatingInstance;

    private String replicaLocationHint;

    public StoreableLoadRequestCommand() {
        super(ReplicationCommandOpcode.STOREABLE_UNICAST_LOAD_REQUEST);
    }

    public StoreableLoadRequestCommand(K key, long requestHint, String targetReplicaInstance) {
        this();
        this.key = key;
        minimumRequiredVersion = requestHint;
        replicaLocationHint = targetReplicaInstance;
    }

    @Override
    protected StoreableLoadRequestCommand<K, V> createNewInstance() {
        return new StoreableLoadRequestCommand<K, V>();
    }

    public void setKey(K key) {
        this.key = key;
    }

    @Override
    protected void writeCommandPayload(ReplicationOutputStream ros)
        throws IOException {
        originatingInstance = dsc.getInstanceName();

        setTargetName(replicaLocationHint);
        
        ResponseMediator respMed = dsc.getResponseMediator();
        resp = respMed.createCommandResponse();

        future = resp.getFuture();

        ros.writeLong(resp.getTokenId());
        ros.writeLong(minimumRequiredVersion);
        dsc.getDataStoreKeyHelper().writeKey(ros, key);
        ros.writeLengthPrefixedString(originatingInstance);
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, dsc.getInstanceName() + getName() + " sending load_request command for " + key + "to " + replicaLocationHint);
        }
    }

    @Override
    public void readCommandPayload(ReplicationInputStream ris)
        throws IOException {

        tokenId = ris.readLong();
        minimumRequiredVersion = ris.readLong();
        key = dsc.getDataStoreKeyHelper().readKey(ris);
        originatingInstance = ris.readLengthPrefixedString();
    }

    @Override
    public void execute(String initiator) {

        try {
            DataStoreEntry<K, V> e = dsc.getReplicaStore().getEntry(key);
            V result = null;
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, dsc.getInstanceName() + getName() + " received load_request command for " + key + "from " + initiator);
            }
            if (e != null) {
                synchronized (e) {
                    Storeable v = (Storeable) e.getV();

                    if (!e.isRemoved()) {
                        if (v != null && (v._storeable_getVersion() >= minimumRequiredVersion)) {
                            result = e.getV();
                        }
                    } else {
                        //Remove this stale / useless entry
                        e.markAsRemoved("Removed by StoreableLoadRequestCommand");
                    }
                }
            }
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, dsc.getInstanceName() + getName() + " will send " + key + " => " + result + " to " + initiator);
            }
            
            if (! originatingInstance.equals(dsc.getInstanceName())) {
                StoreableLoadResponseCommand<K, V> rsp = new StoreableLoadResponseCommand<K, V>(key, result, tokenId);
                rsp.setOriginatingInstance(originatingInstance);
                getCommandManager().execute(rsp);
            } else {
                resp.setResult(result);
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
        V result = null;
        try {
            result = (V) future.get(waitFor, unit);
            if (result instanceof Exception) {
                throw new DataStoreException((Exception) result);
            }
        } catch (DataStoreException dsEx) {
            throw dsEx;
        } catch (InterruptedException inEx) {
            _logger.log(Level.WARNING, "LoadRequestCommand Interrupted while waiting for result", inEx);
            throw new DataStoreException(inEx);
        } catch (TimeoutException timeoutEx) {
            _logger.log(Level.WARNING, "LoadRequestCommand timed out while waiting for result" + timeoutEx);
            _logger.log(Level.FINE, "LoadRequestCommand timed out while waiting for result", timeoutEx);
        } catch (ExecutionException exeEx) {
            _logger.log(Level.WARNING, "LoadRequestCommand got an exception while waiting for result", exeEx);
            throw new DataStoreException(exeEx);
        }

        return result;
    }

    @Override
    public String toString() {
        return "StoreableLoadRequestCommand{" +
                "key=" + key +
                ", minimumRequiredVersion=" + minimumRequiredVersion +
                ", resp=" + resp +
                ", tokenId=" + tokenId +
                ", originatingInstance='" + originatingInstance + '\'' +
                '}';
    }

}
