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

import org.shoal.ha.mapper.KeyMapper;

/**
 * @author Mahesh Kannan
 */
public class DataStoreConfigurator<K, V> {

    private String instanceName;

    private String groupName;

    private String storeName;

    private Class<K> keyClazz;

    private Class<V> valueClazz;

    private KeyMapper keyMapper;

    private DataStoreKeyHelper<K> dataStoreKeyHelper;

    private boolean startGMS;

    private ClassLoader clazzLoader;

    private DataStoreEntryHelper<K, V> dataStoreEntryHelper;

    private ObjectInputOutputStreamFactory objectInputOutputStreamFactory;

    public String getInstanceName() {
        return instanceName;
    }

    public DataStoreConfigurator<K, V> setInstanceName(String instanceName) {
        this.instanceName = instanceName;
        return this;
    }

    public String getGroupName() {
        return groupName;
    }

    public DataStoreConfigurator<K, V> setGroupName(String groupName) {
        this.groupName = groupName;
        return this;
    }

    public String getStoreName() {
        return storeName;
    }

    public DataStoreConfigurator<K, V> setStoreName(String storeName) {
        this.storeName = storeName;
        return this;
    }

    public Class<K> getKeyClazz() {
        return keyClazz;
    }

    public DataStoreConfigurator<K, V> setKeyClazz(Class<K> kClazz) {
        this.keyClazz = kClazz;
        return this;
    }

    public Class<V> getValueClazz() {
        return valueClazz;
    }

    public DataStoreConfigurator<K, V> setValueClazz(Class<V> vClazz) {
        this.valueClazz = vClazz;
        return this;
    }

    public KeyMapper getKeyMapper() {
        return keyMapper;
    }

    public DataStoreConfigurator<K, V> setKeyMapper(KeyMapper keyMapper) {
        this.keyMapper = keyMapper;
        return this;
    }

    public DataStoreKeyHelper<K> getDataStoreKeyHelper() {
        return dataStoreKeyHelper;
    }

    public DataStoreConfigurator<K, V> setDataStoreKeyHelper(DataStoreKeyHelper<K> dataStoreKeyHelper) {
        this.dataStoreKeyHelper = dataStoreKeyHelper;
        return this;
    }

    public DataStoreEntryHelper<K, V> getDataStoreEntryHelper() {
        return dataStoreEntryHelper;
    }

    public DataStoreConfigurator<K, V> setDataStoreEntryHelper(DataStoreEntryHelper<K, V> dataStoreEntryHelper) {
        this.dataStoreEntryHelper = dataStoreEntryHelper;
        return this;
    }

    public boolean isStartGMS() {
        return startGMS;
    }

    public DataStoreConfigurator<K, V> setStartGMS(boolean startGMS) {
        this.startGMS = startGMS;
        return this;
    }

    public ClassLoader getClassLoader() {
        return clazzLoader;
    }

    public DataStoreConfigurator<K, V> setClassLoader(ClassLoader clazzLoader) {
        this.clazzLoader = clazzLoader;
        return this;
    }

    public ObjectInputOutputStreamFactory getObjectInputOutputStreamFactory() {
        return objectInputOutputStreamFactory;
    }

    public void setObjectInputOutputStreamFactory(ObjectInputOutputStreamFactory objectInputOutputStreamFactory) {
        this.objectInputOutputStreamFactory = objectInputOutputStreamFactory;
    }
}
