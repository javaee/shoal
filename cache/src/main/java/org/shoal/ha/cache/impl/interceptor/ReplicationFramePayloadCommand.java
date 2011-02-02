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

package org.shoal.ha.cache.impl.interceptor;

import org.glassfish.ha.store.util.KeyTransformer;
import org.shoal.ha.cache.api.DataStoreException;
import org.shoal.ha.cache.api.ShoalCacheLoggerConstants;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.command.ReplicationCommandOpcode;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public class ReplicationFramePayloadCommand<K, V>
        extends Command {

    private transient static final Logger _logger =
            Logger.getLogger(ShoalCacheLoggerConstants.CACHE_REPLICATION_FRAME_COMMAND);

    private String targetInstanceName;

    private List<Command<K, V>> commands = new ArrayList<Command<K, V>>();

    private Collection<K> removedKeys = new ArrayList<K>();

    private List<byte[]> rawRemovedKeys = new ArrayList<byte[]>();

    public ReplicationFramePayloadCommand() {
        super(ReplicationCommandOpcode.REPLICATION_FRAME_PAYLOAD);
        setKey("RepFP:" + System.identityHashCode(this));
    }

    public void addComamnd(Command<K, V> cmd) {
        commands.add(cmd);
    }

    public void setTargetInstance(String target) {
        targetInstanceName = target;
    }

    void setRemovedKeys(Collection<K> removedKeys) {
        this.removedKeys = removedKeys;
    }

    protected boolean beforeTransmit()
        throws DataStoreException {
        setTargetName(targetInstanceName);
        return targetInstanceName != null;
    }

    private void writeObject(ObjectOutputStream ros)
            throws IOException {
        try {
            ros.writeObject(commands);
            ros.writeBoolean(dsc.getKeyTransformer() == null);
            if (dsc.getKeyTransformer() == null) {
                ros.writeObject(removedKeys);
            } else {
                KeyTransformer<K> kt = dsc.getKeyTransformer();
                int sz = removedKeys.size();
                rawRemovedKeys = new ArrayList<byte[]>();
                for (K k : removedKeys) {
                    rawRemovedKeys.add(kt.keyToByteArray(k));
                }

                ros.writeObject(rawRemovedKeys);
            }
        } catch (IOException ioEx) {
            _logger.log(Level.INFO, "Error during ReplicationFramePayloadCommand.writeObject ", ioEx);
            throw ioEx;
        }
    }

    private void readObject(ObjectInputStream ris)
            throws IOException, ClassNotFoundException {
        try {
            commands = (List<Command<K, V>>) ris.readObject();
            boolean ktAbsent = ris.readBoolean();
            if (ktAbsent) {
                removedKeys = (Collection<K>) ris.readObject();
            } else {
                rawRemovedKeys = (List<byte[]>) ris.readObject();
            }
        } catch (IOException ioEx) {
            _logger.log(Level.INFO, "Error during ReplicationFramePayloadCommand.readObject ", ioEx);
            throw ioEx;
        }

    }

    @Override
    public void execute(String initiator)
            throws DataStoreException {
        /*
        int sz = list.size();
        commands = new ArrayList<Command<K, V>>();
        for (int i = 0; i < sz; i++) {
            ByteArrayInputStream bis = null;
            ObjectInputStreamWithLoader ois = null;
            try {
                bis = new ByteArrayInputStream(list.get(i));
                ois = new ObjectInputStreamWithLoader(bis, dsc.getClassLoader());
                Command<K, V> cmd = (Command<K, V>) ois.readObject();

                commands.add(cmd);
                cmd.initialize(dsc);
            } catch (Exception ex) {
                _logger.log(Level.WARNING, "Error during execute ", ex);
            } finally {
                try { ois.close(); } catch (Exception ex) {}
                try { bis.close(); } catch (Exception ex) {}
            }
        }
        */

        if (rawRemovedKeys != null) {
            KeyTransformer<K> kt = dsc.getKeyTransformer();
            removedKeys = new ArrayList<K>();
            for (byte[] bytes : rawRemovedKeys) {
                K k = kt.byteArrayToKey(bytes, 0, bytes.length);
                removedKeys.add(k);
            }
        }
        
        for (Command<K, V> cmd : commands) {
            cmd.initialize(dsc);
            getCommandManager().executeCommand(cmd, false, initiator);
        }

        int executedRemoveCount = 0;
        if (removedKeys != null) {
            for (K k : removedKeys) {
                dsc.getReplicaStore().remove(k);
                executedRemoveCount++;
            }

            if (dsc.getDataStoreMBean() != null) {
                dsc.getDataStoreMBean().updateExecutedRemoveCount(executedRemoveCount);
            }
        }
    }

    @Override
    public void onFailure() {
        int sz = commands.size();
        for (int i = 0; i < sz; i++) {
            Command cmd = commands.get(i);
            cmd.onFailure();
        }
    }

    @Override
    protected boolean isArtificialKey() {
        return true;
    }

    public String toString() {
        return "ReplicationFramePayloadCommand: contains " + commands.size() + " commands";
    }
}
