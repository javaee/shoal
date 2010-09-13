package org.shoal.adapter.store;

import org.glassfish.ha.store.api.BackingStore;
import org.glassfish.ha.store.api.BackingStoreConfiguration;
import org.shoal.ha.cache.api.DataStoreContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mahesh Kannan
 *
 */
public class RepliatedBackingStoreRegistry {


    private static Map<String, DataStoreContext> _contexts
            = new ConcurrentHashMap<String, DataStoreContext>();

    private static Map<String, BackingStoreConfiguration> _confs
            = new ConcurrentHashMap<String, BackingStoreConfiguration>();

    public static synchronized final void registerStore(String name, BackingStoreConfiguration conf,
                                                        DataStoreContext ctx) {
        _contexts.put(name, ctx);
        _confs.put(name, conf);
    }

    public static synchronized final void unregisterStore(String name) {
        _contexts.remove(name);
    }

    public static final Collection<String> getStoreNames() {
        return _contexts.keySet();
    }

    public static final Collection<BackingStoreConfiguration> getConfigurations() {
        return _confs.values();
    }

    public static final DataStoreContext getContext(String name) {
        return _contexts.get(name);
    }

}
