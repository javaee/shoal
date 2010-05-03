package org.shoal.ha.cache.api;

/**
 * @author Mahesh Kannan
 *
 */
public class ObjectInputOutputStreamFactoryRegistry {

    private static ObjectInputOutputStreamFactory _factory;

    public static ObjectInputOutputStreamFactory getObjectInputOutputStreamFactory() {
        return _factory;
    }
}
