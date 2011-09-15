package org.shoal.ha.cache.impl.util;

import org.glassfish.ha.store.util.KeyTransformer;

/**
 * @author Mahesh Kannan
 *
 */
public class StringKeyTransformer
    implements KeyTransformer<String> {

    public StringKeyTransformer() {}

    @Override
    public byte[] keyToByteArray(String str) {
        //System.out.println("@@@@@@@@@@@@@ StringKeyTransformer.keyTobyteArray(" + str +")");
        return str.getBytes();
    }

    @Override
    public String byteArrayToKey(byte[] bytes, int index, int len) {
        //System.out.println("@@@@@@@@@@@@@ StringKeyTransformer.byteArrayToKey(@[b...) => " + new String(bytes, index, len));
        return new String(bytes, index, len);
    }
}
