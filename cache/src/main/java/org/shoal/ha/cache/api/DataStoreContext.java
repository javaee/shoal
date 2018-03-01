/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.shoal.ha.cache.api;

import org.glassfish.ha.store.api.BackingStoreConfiguration;
import org.glassfish.ha.store.api.Storeable;
import org.glassfish.ha.store.util.KeyTransformer;
import org.shoal.ha.cache.impl.store.DataStoreEntry;
import org.shoal.ha.cache.impl.store.ReplicaStore;
import org.shoal.ha.group.GroupService;
import org.shoal.ha.cache.impl.command.CommandManager;
import org.shoal.ha.cache.impl.util.ResponseMediator;
import org.shoal.ha.mapper.KeyMapper;

import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public class DataStoreContext<K, V>
    extends DataStoreConfigurator<K, V> {

    private static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_DATA_STORE);

    private CommandManager<K, V> cm;

    private ResponseMediator responseMediator;

    private GroupService groupService;

    private ReplicaStore<K, V> replicaStore;

    private ReplicatedDataStoreStatsHolder dscMBean;

    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);

    public DataStoreContext(String serviceName, GroupService gs, ClassLoader loader) {
        super.setStoreName(serviceName);
        super.setInstanceName(gs.getMemberName());
        this.groupService = gs;
        super.setClassLoader(loader);
    }
    
    public DataStoreContext() {
        super();
    }

    public void acquireReadLock() {
        rwLock.readLock().lock();
    }

    public void releaseReadLock() {
        rwLock.readLock().unlock();
    }

    public void acquireWriteLock() {
        rwLock.writeLock().lock();
    }

    public void releaseWriteLock() {
        rwLock.writeLock().unlock();
    }

    public DataStoreContext(BackingStoreConfiguration conf) {
        setInstanceName(conf.getInstanceName())
                .setGroupName(conf.getClusterName())
                .setStoreName(conf.getStoreName())
                .setKeyClazz(conf.getKeyClazz())
                .setValueClazz(conf.getValueClazz());

        if (conf.getClassLoader() != null) {
            _logger.log(Level.FINE, "**DSC[" + conf.getStoreName() + "] Client supplied ClassLoader : " + conf.getClassLoader());
            setClassLoader(conf.getClassLoader());
        }

         Map<String, Object> vendorSpecificMap = conf.getVendorSpecificSettings();

        Object stGMS = vendorSpecificMap.get("start.gms");
        boolean startGMS = false;
        if (stGMS != null) {
            if (stGMS instanceof String) {
                try {
                    startGMS = Boolean.valueOf((String) stGMS);
                } catch (Throwable th) {
                    //Ignore
                }
            } else if (stGMS instanceof Boolean) {
                startGMS = (Boolean) stGMS;
            }
        }

        Object cacheLocally = vendorSpecificMap.get("local.caching");
        boolean enableLocalCaching = false;
        if (cacheLocally != null) {
            if (cacheLocally instanceof String) {
                try {
                    enableLocalCaching = Boolean.valueOf((String) cacheLocally);
                } catch (Throwable th) {
                    //Ignore
                }
            } else if (cacheLocally instanceof Boolean) {
                enableLocalCaching = (Boolean) stGMS;
            }
        }

        if (getClassLoader() == null) {
            ClassLoader cl = (ClassLoader) vendorSpecificMap.get("class.loader");
            _logger.log(Level.FINE, "**DSC[" + conf.getStoreName() + "] vendorMap.classLoader CLASS LOADER: " + cl);
            if (cl == null) {
                cl = conf.getValueClazz().getClassLoader();
                _logger.log(Level.FINE, "**DSC[" + conf.getStoreName() + "] USING VALUE CLASS CLASS LOADER: " + conf.getValueClazz().getName());
            }
            if (cl == null) {
                cl = ClassLoader.getSystemClassLoader();
                _logger.log(Level.FINE, "**DSC[" + conf.getStoreName() + "] USING system CLASS CLASS LOADER: " + cl);
            }

            _logger.log(Level.FINE, "**DSC[" + conf.getStoreName() + "] FINALLY USING CLASS CLASS LOADER: " + cl);
            setClassLoader(cl);
        }

        setStartGMS(startGMS)
                .setCacheLocally(enableLocalCaching);

        boolean asyncReplication = vendorSpecificMap.get("async.replication") == null
                ? true : (Boolean) vendorSpecificMap.get("async.replication");
        setDoSynchronousReplication(! asyncReplication);

        KeyMapper keyMapper = (KeyMapper) vendorSpecificMap.get("key.mapper");
        if (keyMapper != null) {
            setKeyMapper(keyMapper);
        }

        KeyTransformer<K> kt = (KeyTransformer<K>) vendorSpecificMap.get("key.transformer");
        if (kt != null) {
            super.setKeyTransformer(kt);
            _logger.log(Level.FINE, "** USING CLIENT DEFINED KeyTransfomer: " + super.getKeyTransformer().getClass().getName());
        }

        /*
        dsConf.addCommand(new SaveCommand<K, V>());
        dsConf.addCommand(new SimpleAckCommand<K, V>());
        dsConf.addCommand(new RemoveCommand<K, V>(null));
        dsConf.addCommand(new LoadRequestCommand<K, V>());
        dsConf.addCommand(new LoadResponseCommand<K, V>());
        dsConf.addCommand(new StaleCopyRemoveCommand<K, V>());
        dsConf.addCommand(new TouchCommand<K, V>());
        dsConf.addCommand(new SizeRequestCommand<K, V>());
        dsConf.addCommand(new SizeResponseCommand<K, V>());
        dsConf.addCommand(new NoOpCommand<K, V>());
        */


        Object idleTimeInSeconds = vendorSpecificMap.get("max.idle.timeout.in.seconds");
        if (idleTimeInSeconds != null) {
            long defaultMaxIdleTimeInMillis = -1;
            if (idleTimeInSeconds instanceof Long) {
                defaultMaxIdleTimeInMillis = (Long) idleTimeInSeconds;
            } else if (idleTimeInSeconds instanceof String) {
                try {
                    defaultMaxIdleTimeInMillis = Long.valueOf((String) idleTimeInSeconds);
                } catch (Exception ex) {
                    //Ignore
                }
            }
            
            setDefaultMaxIdleTimeInMillis(defaultMaxIdleTimeInMillis * 1000);
        }

        Object safeToDelayCaptureStateObj = vendorSpecificMap.get("value.class.is.thread.safe");
        if (safeToDelayCaptureStateObj != null) {
            boolean safeToDelayCaptureState = true;
            if (safeToDelayCaptureStateObj instanceof Boolean) {
                safeToDelayCaptureState = (Boolean) safeToDelayCaptureStateObj;
            } else if (safeToDelayCaptureStateObj instanceof String) {
                try {
                    safeToDelayCaptureState = Boolean.valueOf((String) safeToDelayCaptureStateObj);
                } catch (Exception ex) {
                    //Ignore
                }
            }

            setSafeToDelayCaptureState(safeToDelayCaptureState);
        }

        Object bcastRemExpObj = vendorSpecificMap.get("broadcast.remove.expired");
        if (bcastRemExpObj != null) {
            boolean bcastRemExp = true;
            if (bcastRemExpObj instanceof Boolean) {
                bcastRemExp = (Boolean) bcastRemExpObj;
            } else if (bcastRemExpObj instanceof String) {
                try {
                    bcastRemExp = Boolean.valueOf((String) bcastRemExpObj);
                } catch (Exception ex) {
                    //Ignore
                }
            }

            setBroadcastRemovedExpired(bcastRemExp);
        }
    }

    public void setDataStoreMBean(ReplicatedDataStoreStatsHolder<K, V> dscMBean) {
        this.dscMBean = dscMBean;
    }

    public ReplicatedDataStoreStatsHolder<K, V> getDataStoreMBean() {
        return dscMBean;
    }

    public String getServiceName() {
        return super.getStoreName();
    }

    public CommandManager<K, V> getCommandManager() {
        return cm;
    }

    public ResponseMediator getResponseMediator() {
        return responseMediator;
    }

    public void setResponseMediator(ResponseMediator responseMediator) {
        this.responseMediator = responseMediator;
    }

    public GroupService getGroupService() {
        return groupService;
    }

    public void setCommandManager(CommandManager<K, V> cm) {
        this.cm = cm;
    }

    public void setGroupService(GroupService groupService) {
        this.groupService = groupService;
    }

    public void setReplicaStore(ReplicaStore<K, V> replicaStore) {
        this.replicaStore = replicaStore;
    }

    public ReplicaStore<K, V> getReplicaStore() {
        return replicaStore;
    }

}
