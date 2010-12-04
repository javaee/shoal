package org.shoal.ha.cache.impl.util;

import org.glassfish.ha.store.util.KeyTransformer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

/**
 * @author Mahesh Kannan
 */
public class DefaultKeyTransformer<K>
    implements KeyTransformer<K> {

    private ClassLoader loader;

    public DefaultKeyTransformer(ClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public byte[] keyToByteArray(K k) {
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(k);
            try {oos.close();} catch (Exception ex) {}
            return bos.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
            //TODO FIXME This cannot be RuntimeException
        } finally {
            try {oos.close();} catch (Exception ex) {}
            try {bos.close();} catch (Exception ex) {}
        }
    }

    @Override
    public K byteArrayToKey(byte[] bytes, int index, int len) {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes, index, len);
        ObjectInputStreamWithLoader ois = null;
        try {
            ois = new ObjectInputStreamWithLoader(bis, loader);
            return (K) ois.readObject();
        } catch (Exception ex) {
            throw new RuntimeException (ex);
        } finally {
            try {ois.close();} catch (Exception ex) {}
            try {bis.close();} catch (Exception ex) {}
        }
    }

}
