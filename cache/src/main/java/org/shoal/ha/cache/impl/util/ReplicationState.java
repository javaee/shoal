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

    protected void writePayLoadState(DataStoreEntryHelper<K, V> transformer, ReplicationOutputStream ros)
            throws IOException {


        ros.write(Utility.intToBytes(deletedAttributes.size()));
        for (String key : deletedAttributes) {
            ReplicationIOUtils.writeLengthPrefixedString(ros, key);
        }

        int attrLen = attributes.size();
        ros.write(Utility.intToBytes(attrLen));

        byte[] attrOffset = new byte[4 * attributes.size()];
        int offsetMark = ros.mark();
        ros.write(attrOffset);
        int index = 0;
        int base = ros.size();
        for (String k : attributes.keySet()) {
            Utility.intToBytes(ros.size() - base, attrOffset, index*4);
            System.out.println("[*RS*] write => " + k + " :=> " + (ros.size() - base));
            index++;
            ReplicationIOUtils.writeLengthPrefixedString(ros, k);
            byte[] v = attributes.get(k);
            ros.write(Utility.intToBytes(v.length));
            ros.write(v);
        }
        ros.reWrite(offsetMark, attrOffset);
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
        int[] attrOffsets = new int[attrSz];

        for (int i = 0; i < attrSz; i++) {
            attrOffsets[i] = Utility.bytesToInt(data, index);
            index+=4;
            System.out.println("[*RS*] write => " + getKey() + " <=: " + attrOffsets[i]);
        }
        int base = index;
        for (int i = 0; i < attrSz; i++) {
            String k = ReplicationIOUtils.readLengthPrefixedString(data, attrOffsets[i]+base);
            int len = Utility.bytesToInt(data, attrOffsets[i]+base+k.length());

            byte[] v = new byte[len];
            System.arraycopy(data, attrOffsets[i]+base+k.length()+len, v, 0, len);
            attributes.put(k, v);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("ReplicationState: [");
        sb.append(super.toString())
                .append("; ").append(attributes.size());
        for (String a : attributes.keySet()) {
            sb.append("\n\t").append(a).append("; ");
        }

        return sb.toString();
    }


}
