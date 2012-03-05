package org.shoal.ha.cache.impl.util;

import org.glassfish.ha.store.util.KeyTransformer;

import java.nio.charset.Charset;

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
        return str.getBytes(Charset.defaultCharset());
    }

    @Override
    public String byteArrayToKey(byte[] bytes, int index, int len) {
        //System.out.println("@@@@@@@@@@@@@ StringKeyTransformer.byteArrayToKey(@[b...) => " + new String(bytes, index, len));
        return new String(bytes, index, len, Charset.defaultCharset());
    }
}
