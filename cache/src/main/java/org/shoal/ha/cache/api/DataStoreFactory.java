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

package org.shoal.ha.cache.api;

import org.shoal.adapter.store.RepliatedBackingStoreRegistry;
import org.shoal.adapter.store.commands.*;
import org.shoal.adapter.store.commands.monitor.ListBackingStoreConfigurationCommand;
import org.shoal.adapter.store.commands.monitor.ListBackingStoreConfigurationResponseCommand;
import org.shoal.adapter.store.commands.monitor.ListReplicaStoreEntriesCommand;
import org.shoal.ha.cache.impl.util.DefaultDataStoreEntryHelper;
import org.shoal.ha.cache.impl.util.StringKeyHelper;
import org.shoal.ha.mapper.DefaultKeyMapper;
import org.shoal.ha.group.GroupMemberEventListener;
import org.shoal.ha.group.GroupService;
import org.shoal.ha.group.GroupServiceFactory;
import org.shoal.ha.mapper.KeyMapper;
import org.shoal.ha.cache.impl.store.ReplicatedDataStore;

import java.io.Serializable;

/**
 * @author Mahesh Kannan
 */
public class DataStoreFactory {

    public static DataStore<String, Serializable> createDataStore(String storeName, String instanceName, String groupName) {
        DataStoreKeyHelper<String> keyHelper = new StringKeyHelper();
        DefaultKeyMapper keyMapper = new DefaultKeyMapper(instanceName, groupName);

        Class<Serializable> vClazz = Serializable.class;
        ClassLoader loader = vClazz.getClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        DataStoreConfigurator<String, Serializable> conf = new DataStoreConfigurator<String, Serializable>();
        conf.setStartGMS(true);
        conf.setStoreName(storeName)
                .setInstanceName(instanceName)
                .setGroupName(groupName)
                .setKeyClazz(String.class)
                .setValueClazz(vClazz)
                .setClassLoader(loader)
                .setDataStoreKeyHelper(keyHelper)
                .setKeyMapper(keyMapper)
                .setDoAddCommands()
                .setDoASyncReplication(false)
                .setObjectInputOutputStreamFactory(new DefaultObjectInputOutputStreamFactory());

        return createDataStore(conf);
    }

    public static <K, V extends Serializable> DataStore<K, V> createDataStore(String storeName, String instanceName, String groupName,
                                                  Class<K> keyClazz, Class<V> vClazz, ClassLoader loader) {

        DefaultObjectInputOutputStreamFactory factory = new DefaultObjectInputOutputStreamFactory();
        DataStoreKeyHelper<K> keyHelper = new ObjectKeyHelper(loader, factory);
        DefaultKeyMapper keyMapper = new DefaultKeyMapper(instanceName, groupName);
        
        DataStoreConfigurator<K, V> conf = new DataStoreConfigurator<K, V>();
        conf.setStartGMS(true);
        conf.setStoreName(storeName)
                .setInstanceName(instanceName)
                .setGroupName(groupName)
                .setKeyClazz(keyClazz)
                .setValueClazz(vClazz)
                .setClassLoader(loader)
                .setDataStoreKeyHelper(keyHelper)
                .setKeyMapper(keyMapper)
                .setObjectInputOutputStreamFactory(factory);

        return createDataStore(conf);
    }

    public static <K, V extends Serializable> DataStore<K, V> createDataStore(String storeName, String instanceName, String groupName,
                                                  Class<K> keyClazz, Class<V> vClazz, ClassLoader loader,
                                                  DataStoreEntryHelper<K, V> helper, DataStoreKeyHelper<K> keyHelper,
                                                  KeyMapper keyMapper) {
        DataStoreConfigurator<K, V> conf = new DataStoreConfigurator<K, V>();
        conf.setStartGMS(true);
        conf.setStoreName(storeName)
                .setInstanceName(instanceName)
                .setGroupName(groupName)
                .setKeyClazz(keyClazz)
                .setValueClazz(vClazz)
                .setClassLoader(loader)
                .setDataStoreEntryHelper(helper)
                .setDataStoreKeyHelper(keyHelper)
                .setKeyMapper(keyMapper)
                .setObjectInputOutputStreamFactory(new DefaultObjectInputOutputStreamFactory());

        return createDataStore(conf);
    }

    public static <K, V extends Serializable> DataStore<K, V> createDataStore(DataStoreConfigurator<K, V> conf) {
        GroupService gs = GroupServiceFactory.getInstance().getGroupService(conf.getInstanceName(), conf.getGroupName(), conf.isStartGMS());

        if (conf.getKeyMapper() == null) {
            conf.setKeyMapper(new DefaultKeyMapper(conf.getInstanceName(), conf.getGroupName()));
        }

        if (conf.getKeyMapper() instanceof GroupMemberEventListener) {
            GroupMemberEventListener groupListener = (GroupMemberEventListener) conf.getKeyMapper();
            gs.registerGroupMemberEventListener(groupListener);
        }

        if (conf.getObjectInputOutputStreamFactory() == null) {
            conf.setObjectInputOutputStreamFactory(new DefaultObjectInputOutputStreamFactory());
        }

        if (conf.getDataStoreEntryHelper() == null) {
            conf.setDataStoreEntryHelper(
                new DefaultDataStoreEntryHelper<K, V>(conf.getObjectInputOutputStreamFactory(),
                        conf.getClassLoader(), 10 * 60 * 1000));
        }

        if (conf.getDataStoreKeyHelper() == null) {
            conf.setDataStoreKeyHelper(new ObjectKeyHelper<K>(
                    conf.getClassLoader(), conf.getObjectInputOutputStreamFactory()));
        }

        if (conf.isDoAddCommands()) {
            conf.addCommand(new SaveCommand<K, V>());
            conf.addCommand(new SimpleAckCommand<K, V>());
            conf.addCommand(new RemoveCommand<K, V>());
            conf.addCommand(new LoadRequestCommand<K, V>());
            conf.addCommand(new BroadcastLoadRequestCommand<K, V>());
            conf.addCommand(new LoadResponseCommand<K, V>());
            conf.addCommand(new StaleCopyRemoveCommand<K, V>());
            conf.addCommand(new TouchCommand<K, V>());
            conf.addCommand(new UpdateDeltaCommand<K, V>());

            conf.addCommand(new ListBackingStoreConfigurationCommand());
            conf.addCommand(new ListBackingStoreConfigurationResponseCommand());
            conf.addCommand(new ListReplicaStoreEntriesCommand(null));
        }
        DataStore<K, V> ds = new ReplicatedDataStore<K, V>(conf, gs);

        return ds;
    }

}
