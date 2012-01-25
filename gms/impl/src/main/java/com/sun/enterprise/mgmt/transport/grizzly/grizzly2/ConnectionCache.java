/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.mgmt.transport.grizzly.grizzly2;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.SocketConnectorHandler;
import org.glassfish.grizzly.utils.Exceptions;
import org.glassfish.grizzly.utils.LinkedTransferQueue;

/**
 * Connection cache implementation.
 * 
 * @author Alexey Stashok
 */
public class ConnectionCache {
    private final SocketConnectorHandler socketConnectorHandler;

    private final int highWaterMark;
    private final int maxParallelConnections;
    private final int numberToReclaim;

    private final AtomicBoolean isClosed = new AtomicBoolean();

    private final AtomicInteger totalCachedConnectionsCount = new AtomicInteger();
    
    private final ConcurrentHashMap<SocketAddress, CacheRecord> cache =
            new ConcurrentHashMap<SocketAddress, CacheRecord>();

    // Connect timeout 5 seconds
    private final long connectTimeoutMillis = 5000;

    private final Connection.CloseListener removeCachedConnectionOnCloseListener =
            new RemoveCachedConnectionOnCloseListener();
    
    public ConnectionCache(SocketConnectorHandler socketConnectorHandler,
            int highWaterMark, int maxParallelConnections, int numberToReclaim) {
        this.socketConnectorHandler = socketConnectorHandler;

        this.highWaterMark = highWaterMark;
        this.maxParallelConnections = maxParallelConnections;
        this.numberToReclaim = numberToReclaim;
    }

    public Connection poll(final SocketAddress localAddress,
            final SocketAddress remoteAddress) throws IOException {

        final CacheRecord cacheRecord = obtainCacheRecord(remoteAddress);

        if (isClosed.get()) {
            // remove cache entry associated with the remoteAddress (only if we have the actual value)
            cache.remove(remoteAddress, cacheRecord);
            closeCacheRecord(cacheRecord);
            throw new IOException("ConnectionCache is closed");
        }

        // take connection from cache
        Connection connection = cacheRecord.connections.poll();
        if (connection != null) {
            // if we have one - just return it
            connection.removeCloseListener(removeCachedConnectionOnCloseListener);
            cacheRecord.idleConnectionsCount.decrementAndGet();
            return connection;
        }

        final Future<Connection> connectFuture =
                socketConnectorHandler.connect(remoteAddress, localAddress);

        try {
            connection = connectFuture.get(connectTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw Exceptions.makeIOException(e);
        }

        return connection;
    }
    
    public void offer(final Connection connection) {
        final SocketAddress remoteAddress = (SocketAddress) connection.getPeerAddress();

        final CacheRecord cacheRecord = obtainCacheRecord(remoteAddress);

        final int totalConnectionsN = totalCachedConnectionsCount.incrementAndGet();
        final int parallelConnectionN = cacheRecord.idleConnectionsCount.incrementAndGet();

        if (totalConnectionsN > highWaterMark ||
                parallelConnectionN > maxParallelConnections) {
            totalCachedConnectionsCount.decrementAndGet();
            cacheRecord.idleConnectionsCount.decrementAndGet();
        }

        connection.addCloseListener(removeCachedConnectionOnCloseListener);

        cacheRecord.connections.offer(connection);
        
        if (isClosed.get()) {
            // remove cache entry associated with the remoteAddress (only if we have the actual value)
            cache.remove(remoteAddress, cacheRecord);
            closeCacheRecord(cacheRecord);
        }
    }

    public void close() {
        if (!isClosed.getAndSet(true)) {
            for (SocketAddress key : cache.keySet()) {
                final CacheRecord cacheRecord = cache.remove(key);
                closeCacheRecord(cacheRecord);
            }
        }
    }

    private void closeCacheRecord(final CacheRecord cacheRecord) {
        if (cacheRecord == null) return;
        Connection connection;
        while ((connection = cacheRecord.connections.poll()) != null) {
            cacheRecord.idleConnectionsCount.decrementAndGet();
            connection.close();
        }
    }

    private CacheRecord obtainCacheRecord(final SocketAddress remoteAddress) {
        CacheRecord cacheRecord = cache.get(remoteAddress);
        if (cacheRecord == null) {
            // make sure we added CacheRecord corresponding to the remoteAddress
            final CacheRecord newCacheRecord = new CacheRecord();
            cacheRecord = cache.putIfAbsent(remoteAddress, newCacheRecord);
            if (cacheRecord == null) {
                cacheRecord = newCacheRecord;
            }
        }

        return cacheRecord;
    }

    private static final class CacheRecord {
        final AtomicInteger idleConnectionsCount =
                new AtomicInteger();

        final Queue<Connection> connections =
                new LinkedTransferQueue<Connection>();
        
    }

    private final class RemoveCachedConnectionOnCloseListener implements
            Connection.CloseListener {

        @Override
        public void onClosed(Connection connection, Connection.CloseType type) throws IOException {
            final SocketAddress remoteAddress =
                    (SocketAddress) connection.getPeerAddress();
            final CacheRecord cacheRecord = cache.get(remoteAddress);
            if (cacheRecord != null &&
                    cacheRecord.connections.remove(connection)) {
                cacheRecord.idleConnectionsCount.decrementAndGet();
            }
        }

    }
}
