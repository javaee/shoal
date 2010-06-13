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

package org.shoal.ha.cache.api;

import org.shoal.ha.cache.impl.store.DefaultDataStoreEntryHelper;
import org.shoal.ha.cache.impl.util.StringKeyHelper;
import org.shoal.ha.cache.impl.util.DefaultKeyMapper;
import org.shoal.ha.group.GroupMemberEventListener;
import org.shoal.ha.group.GroupService;
import org.shoal.ha.group.GroupServiceFactory;
import org.shoal.ha.mapper.KeyMapper;
import org.shoal.ha.cache.impl.store.ReplicatedDataStore;

/**
 * @author Mahesh Kannan
 */
public class DataStoreFactory {

    public static DataStore<String, Object> createDataStore(String storeName, String instanceName, String groupName) {
        DataStoreEntryHelper<String, Object> helper =
                new DefaultDataStoreEntryHelper<String, Object>(Thread.currentThread().getContextClassLoader());
        DataStoreKeyHelper<String> keyHelper = new StringKeyHelper();
        DefaultKeyMapper keyMapper = new DefaultKeyMapper(instanceName, groupName);

        return DataStoreFactory.createDataStore(storeName, instanceName, groupName,
                String.class, Object.class, Thread.currentThread().getContextClassLoader(),
                helper, keyHelper, keyMapper);
    }

    public static <K, V> DataStore<K, V> createDataStore(String storeName, String instanceName, String groupName,
                                                  Class<K> keyClazz, Class<V> vClazz, ClassLoader loader) {
        DataStoreEntryHelper<K, V> helper =
                new DefaultDataStoreEntryHelper<K, V>(vClazz.getClassLoader());
        DataStoreKeyHelper<K> keyHelper = new ObjectKeyHelper(loader);
        DefaultKeyMapper keyMapper = new DefaultKeyMapper(instanceName, groupName);
        
        return DataStoreFactory.createDataStore(storeName, instanceName, groupName, keyClazz, vClazz,
                loader, helper, keyHelper, keyMapper);
    }

    public static <K, V> DataStore<K, V> createDataStore(String storeName, String instanceName, String groupName,
                                                  Class<K> keyClazz, Class<V> vClazz, ClassLoader loader,
                                                  DataStoreEntryHelper<K, V> helper, DataStoreKeyHelper<K> keyHelper,
                                                  KeyMapper keyMapper) {
        GroupService gs = GroupServiceFactory.getInstance().getGroupService(instanceName, groupName, true);
        if (keyMapper instanceof GroupMemberEventListener) {
            GroupMemberEventListener groupListener = (GroupMemberEventListener) keyMapper;
            gs.registerGroupMemberEventListener(groupListener);
        }

        return new ReplicatedDataStore<K, V>(storeName, gs, loader, helper, keyHelper, keyMapper);
    }

    public static <K, V> DataStore<K, V> createDataStore(DataStoreConfigurator<K, V> conf) {
        GroupService gs = GroupServiceFactory.getInstance().getGroupService(conf.getInstanceName(), conf.getGroupName(), conf.isStartGMS());
        if (conf.getKeyMapper() instanceof GroupMemberEventListener) {
            GroupMemberEventListener groupListener = (GroupMemberEventListener) conf.getKeyMapper();
            gs.registerGroupMemberEventListener(groupListener);
        }

        if (conf.getKeyMapper() == null) {
            conf.setKeyMapper(new DefaultKeyMapper(conf.getInstanceName(), conf.getGroupName()));
        }
        return new ReplicatedDataStore<K, V>(conf.getStoreName(), gs, conf.getClassLoader(),
                conf.getDataStoreEntryHelper(), conf.getDataStoreKeyHelper(), conf.getKeyMapper());
    }

}
