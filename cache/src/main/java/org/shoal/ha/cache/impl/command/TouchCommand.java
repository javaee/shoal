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
import org.shoal.ha.cache.impl.util.ReplicationOutputStream;
import org.shoal.ha.cache.impl.util.Utility;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public class TouchCommand<K, V>
    extends Command<K, V> {

    private static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_TOUCH_COMMAND);

    private K key;

    private long ts;

    private long version;

    private long ttl;

    public TouchCommand() {
        super(ReplicationCommandOpcode.TOUCH);
    }

    public TouchCommand(K key, long ts, long version, long ttl) {
        this();
        this.key = key;
        this.ts = ts;
        this.version = version;
        this.ttl = ttl;
    }

    @Override
    protected TouchCommand<K, V> createNewInstance() {
        return new TouchCommand<K, V>();
    }

    @Override
    protected void prepareToTransmit(DataStoreContext<K, V> ctx) {
        setTargetName(ctx.getKeyMapper().getMappedInstance(ctx.getGroupName(), key));
    }

    @Override
    public void writeCommandPayload(DataStoreContext<K, V> trans, ReplicationOutputStream ros) throws IOException {
        ros.write(Utility.longToBytes(ts));
        ros.write(Utility.longToBytes(version));
        ros.write(Utility.longToBytes(ttl));
        trans.getDataStoreKeyHelper().writeKey(ros, key);
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, trans.getInstanceName() + " sending touch " + key + " to " + getTargetName());
        }
    }

    @Override
    public void readCommandPayload(DataStoreContext<K, V> trans, byte[] data, int offset)
        throws DataStoreException {
        ts = Utility.bytesToLong(data, offset);
        version = Utility.bytesToLong(data, offset+8);
        ttl = Utility.bytesToLong(data, offset+16);
        key = (K) trans.getDataStoreKeyHelper().readKey(data, offset+24);
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, trans.getInstanceName() + " received touch " + key + " from " + getTargetName());
        }
    }

    @Override
    public void execute(DataStoreContext<K, V> ctx) {
        ctx.getReplicaStore().touch(key, version, ts, ttl);
    }

}