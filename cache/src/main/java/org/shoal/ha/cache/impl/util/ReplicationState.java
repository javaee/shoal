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

package org.shoal.ha.cache.impl.util;

import org.shoal.ha.cache.api.DataStoreEntryHelper;
import org.shoal.ha.cache.api.DataStoreEntry;
import org.shoal.ha.cache.impl.util.ReplicationIOUtils;
import org.shoal.ha.cache.impl.util.ReplicationOutputStream;
import org.shoal.ha.cache.impl.util.Utility;

import java.io.IOException;
import java.util.*;


/**
 * @author Mahesh Kannan
 *
 */
public final class ReplicationState<K, V>
    extends DataStoreEntry<K, V> {

    private Map<String, byte[]> attributes = new HashMap<String, byte[]>();

    private Set<String> deletedAttributes = new HashSet<String>();

    public void setAttribute(String name, byte[] data) {
        attributes.put(name, data);
    }

    public byte[] getAttribute(String name) {
        return attributes.get(name);
    }
    public void removeAttribute(String... names) {
        for (String name : names) {
            attributes.remove(name);
            deletedAttributes.add(name);
        }
    }

    //Assumes that the caller synchronizes the access
    public void update(ReplicationState<K, V> rs2) {
        setMaxIdleTime(rs2.getMaxIdleTime());
        setLastAccessedAt(rs2.getLastAccessedAt());
        for (String d : rs2.deletedAttributes) {
            removeAttribute(d);
        }

        for (String k : rs2.attributes.keySet()) {
            byte[] attr = rs2.attributes.get(k);
            attributes.put(k, attr);
        }
    }

    protected void writePayloadState(DataStoreEntryHelper<K, V> transformer, ReplicationOutputStream ros)
            throws IOException {

        ros.write(Utility.intToBytes(deletedAttributes.size()));
        for (String key : deletedAttributes) {
            ReplicationIOUtils.writeLengthPrefixedString(ros, key);
        }

        int attrLen = attributes.size();
        ros.write(Utility.intToBytes(attrLen));

        for (String k : attributes.keySet()) {
            ReplicationIOUtils.writeLengthPrefixedString(ros, k);
            byte[] v = attributes.get(k);
            ros.write(Utility.intToBytes(v.length));
            ros.write(v);
        }
    }

    protected void readPayloadState(DataStoreEntryHelper<K, V> trans, byte[] data, int index) {
        int delAttrSz = Utility.bytesToInt(data, index);
        index += 4;

        for (int i = 0; i < delAttrSz; i++) {
            int dKeySz = Utility.bytesToInt(data, index);
            index += 4;
            deletedAttributes.add(new String(data, index, dKeySz));
            index += dKeySz;
        }

        int attrSz = Utility.bytesToInt(data, index);
        index += 4;

        for (int i = 0; i < attrSz; i++) {
            String k = ReplicationIOUtils.readLengthPrefixedString(data, index);
            index += 4 + k.length();

            int len = Utility.bytesToInt(data, index);
            byte[] v = new byte[len];
            System.arraycopy(data, index+4, v, 0, len);
            attributes.put(k, v);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("ReplicationState: [");
        sb.append(super.toString())
                .append("; ").append(attributes.size());
        for (String a : attributes.keySet()) {
            sb.append("\n\t").append(a).append("; ").append(attributes.get(a).length).append("; ");
        }

        return sb.toString();
    }


}
