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

import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.command.CommandManager;
import org.shoal.ha.cache.impl.store.ReplicaStore;
import org.shoal.ha.cache.impl.util.DefaultDataStoreEntryHelper;
import org.shoal.ha.mapper.DefaultKeyMapper;
import org.shoal.ha.group.GroupMemberEventListener;
import org.shoal.ha.group.GroupService;
import org.shoal.ha.group.GroupServiceFactory;
import org.shoal.ha.mapper.KeyMapper;

import java.io.Serializable;

/**
 * @author Mahesh Kannan
 */
public class ReplicationFramework<K, V extends Serializable> {

    private String storeName;

    private String instanceName;

    private String groupName;

    private GroupService gs;

    private CommandManager<K, V> cm;

    private DataStoreEntryHelper<K, V> entryHelper;

    private DataStoreContext<K, V> dsc;

    private DataStoreConfigurator<K, V> conf;

    private ReplicaStore<K, V> replicaStore;

    public ReplicationFramework(DataStoreConfigurator<K, V> conf) {
        this.conf = conf;
        this.storeName = conf.getStoreName();

        GroupService gs = GroupServiceFactory.getInstance().getGroupService(conf.getInstanceName(), conf.getGroupName(), conf.isStartGMS());


        this.gs = gs;
        this.instanceName = gs.getMemberName();
        this.groupName = gs.getGroupName();
        
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

        initialize(conf, gs);
    }

    private void initialize(DataStoreConfigurator<K, V> conf, GroupService gs) {
        this.dsc = new DataStoreContext<K, V>(
                storeName, gs, conf.getClassLoader());

        this.entryHelper = conf.getDataStoreEntryHelper();
        dsc.setDataStoreEntryHelper(entryHelper);
        dsc.setDataStoreKeyHelper(conf.getDataStoreKeyHelper());
        dsc.setKeyMapper(conf.getKeyMapper());
        cm = dsc.getCommandManager();

        if (conf.getCommands() != null) {
            for (Command<K, ? super V> cmd : conf.getCommands()) {
                cm.registerCommand(cmd);
            }
        }

        dsc.setDoSyncReplication(conf.isDoSyncReplication());
        
        KeyMapper<K> keyMapper = conf.getKeyMapper();
        if ((keyMapper != null) && (keyMapper instanceof DefaultKeyMapper)) {
            gs.registerGroupMemberEventListener((DefaultKeyMapper) keyMapper);
        }

        gs.registerGroupMessageReceiver(storeName, cm);

        replicaStore = dsc.getReplicaStore();
    }

    public void execute(Command<K, V> cmd)
        throws DataStoreException {
        cm.execute(cmd);
    }

    public String getStoreName() {
        return storeName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public String getGroupName() {
        return groupName;
    }

    public GroupService getGroupService() {
        return gs;
    }

    public CommandManager<K, V> getCommandManager() {
        return cm;
    }

    public DataStoreEntryHelper<K, V> getDataStoreEntryHelper() {
        return entryHelper;
    }

    public DataStoreContext<K, V> getDataStoreContext() {
        return dsc;
    }

    public DataStoreConfigurator<K, V> getDataStoreConfigurator() {
        return conf;
    }

    public ReplicaStore<K, V> getReplicaStore() {
        return replicaStore;
    }
}