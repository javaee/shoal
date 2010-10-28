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

import org.shoal.ha.cache.impl.command.Command;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Mahesh Kannan
 */
public class DataStoreEntry<K, V> {

    private K key;

    private V v;

    private String replicaInstanceName;

    private List<Command<K, V>> pendingUpdates;

    private boolean removed;

    private long lastAccessedAt;

    private long version;

    private String reasonForRemoval;

    public DataStoreEntry() {

    }

    public void setKey(K key) {
        this.key = key;
    }

    public K getKey() {
        return key;
    }

    public V getV() {
        return v;
    }

    public void setV(V state) {
        this.v = state;
    }

    public String getReplicaInstanceName() {
        return replicaInstanceName;
    }

    public String setReplicaInstanceName(String replicaInstanceName) {
        String oldValue = this.replicaInstanceName;
        this.replicaInstanceName = replicaInstanceName;
        this.removed = false; // Because we just saved the data in a replica
        return oldValue == null ? null : oldValue.equals(replicaInstanceName) ? null : oldValue;
    }

    public List<Command<K, V>> getPendingUpdates() {
        return pendingUpdates;
    }


    public void clearPendingUpdates() {
        if (pendingUpdates != null) {
            pendingUpdates.clear();
        }
    }

    public void addPendingUpdate(Command<K, V> cmd) {
        if (pendingUpdates == null) {
            pendingUpdates = new ArrayList<Command<K, V>>();
        }
        this.pendingUpdates.add(cmd);
    }

    public boolean isRemoved() {
        return removed;
    }

    public void markAsRemoved(String reason) {
        this.removed = true;
        v = null;
        pendingUpdates = null;
        reasonForRemoval = reason;
    }

    public long getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(long lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public long getVersion() {
        return version;
    }

    public long incrementAndGetVersion() {
        return ++version;
    }

}
