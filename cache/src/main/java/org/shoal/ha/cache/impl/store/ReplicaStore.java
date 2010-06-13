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

package org.shoal.ha.cache.impl.store;

import org.shoal.ha.cache.api.DataStoreContext;
import org.shoal.ha.cache.api.DataStoreEntry;
import org.shoal.ha.cache.api.DataStoreEntryHelper;
import org.shoal.ha.cache.api.DataStoreException;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mahesh Kannan
 */
public class ReplicaStore<K, V> {

    private DataStoreContext<K, V> ctx;

    private ConcurrentHashMap<K, DataStoreEntry<K, V>> map =
            new ConcurrentHashMap<K, DataStoreEntry<K, V>>();

    public ReplicaStore(DataStoreContext<K, V> ctx) {
        this.ctx = ctx;
    }

    public void put(K k, V v) {
        DataStoreEntryHelper<K, V> helper = ctx.getDataStoreEntryHelper();
        DataStoreEntry<K, V> e = helper.createDataStoreEntry(k, v);
        map.put(k, e);
        System.out.println("** ReplicaStore::put("+k+", "+v);
        //TODO Need to take care of out of order messages
    }

    public DataStoreEntry<K, V> get(K k) {
        return map.get(k);

    }

    public void remove(K k) {
        map.remove(k);
    }

    public void touch(K k, long ts, long version, long ttl) {
        DataStoreEntry<K, V> e = map.get(k);
        if (e != null) {
            e.setLastAccessedAt(ts);
            e.setMaxIdleTime(ttl);
            e.setVersion(version);
        }
    }
}
