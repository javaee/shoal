/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.shoal.ha.cache.impl.interceptor;

import org.shoal.adapter.store.commands.NoOpCommand;
import org.shoal.ha.cache.api.DataStoreContext;
import org.shoal.ha.cache.api.DataStoreException;
import org.shoal.ha.cache.api.ShoalCacheLoggerConstants;
import org.shoal.ha.cache.api.AbstractCommandInterceptor;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.command.ReplicationCommandOpcode;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 *
 */
public class ReplicationCommandTransmitterManager<K, V>
        extends AbstractCommandInterceptor<K, V> {

    private static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_COMMAND);

    private ConcurrentHashMap<String, CommandCollector<K, V>> transmitters
            = new ConcurrentHashMap<String, CommandCollector<K, V>>();

    private CommandCollector<K, V> broadcastTransmitter;

    public ReplicationCommandTransmitterManager() {
    }

    @Override
    public void initialize(DataStoreContext<K, V> dsc) {
        super.initialize(dsc);
        broadcastTransmitter = new ReplicationCommandTransmitterWithList<K, V>();
        broadcastTransmitter.initialize(null, dsc);

        _logger.log(Level.FINE, "ReplicationCommandTransmitterManager(" + dsc.getServiceName() + ") instantiated with: "
            + dsc.isUseMapToCacheCommands() + " : " + dsc.isSafeToDelayCaptureState());
    }

    @Override
    public void onTransmit(Command<K, V> cmd, String initiator)
        throws DataStoreException {
        switch (cmd.getOpcode()) {
            case ReplicationCommandOpcode.REPLICATION_FRAME_PAYLOAD:
                super.onTransmit(cmd, initiator);
                break;

            default:
                String target = cmd.getTargetName();
                if (target != null) {
                    CommandCollector<K, V> rft = transmitters.get(target);
                    if (rft == null) {
                        rft = dsc.isUseMapToCacheCommands()
                                ? new ReplicationCommandTransmitterWithMap<K, V>()
                                : new ReplicationCommandTransmitterWithList<K, V>();
                        rft.initialize(target, getDataStoreContext());
                        CommandCollector oldRCT = transmitters.putIfAbsent(target, rft);
                        if (oldRCT != null) {
                            rft = oldRCT;
                        }
                    }
                    if (cmd.getOpcode() == ReplicationCommandOpcode.REMOVE) {
                        rft.removeCommand(cmd);
                    } else {
                        rft.addCommand(cmd);
                    }
                } else {
                    broadcastTransmitter.addCommand(cmd);
                }
                break;
        }
    }

    public void close() {
        for (CommandCollector<K, V> cc : transmitters.values()) {
            cc.close();
        }

       try { broadcastTransmitter.addCommand(new NoOpCommand()); } catch (DataStoreException dsEx) {}
       broadcastTransmitter.close();
    }

}
