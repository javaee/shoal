package org.shoal.ha.store.impl.store;

import org.shoal.ha.store.api.DataStoreEntry;
import org.shoal.ha.store.api.DataStoreEntryEvaluator;
import org.shoal.ha.store.api.DataStoreEntryHelper;
import org.shoal.ha.store.api.DataStoreException;
import org.shoal.ha.store.impl.util.ReplicationOutputStream;
import org.shoal.ha.store.impl.util.ReplicationState;
import org.shoal.ha.store.impl.util.SimpleSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * @author Mahesh Kannan
 */
public class DefaultDataStoreEntryHelper<K, V>
        implements DataStoreEntryHelper<K, V> {

    private ClassLoader loader;

    public DefaultDataStoreEntryHelper(ClassLoader cl) {
        this.loader = cl;
    }

    @Override
    public DataStoreEntry<K, V> createDataStoreEntry() {
        return new ReplicationState<K, V>();
    }

    @Override
    public DataStoreEntry<K, V> createDataStoreEntry(K k, V obj) {
        ReplicationState<K, V> state = new ReplicationState<K, V>();
        state.setKey(k);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            byte[] data = bos.toByteArray();
            state.setAttribute("value", data);
        } catch (IOException ioEx) {
            //TODO
        } finally {
            try {
                oos.close();
            } catch (IOException ioEx) {
            }
            try {
                bos.close();
            } catch (IOException ioEx) {
            }
        }

        return state;
    }

    @Override
    public V getV(DataStoreEntry<K, V> replicationEntry)
            throws DataStoreException {
        try {
            return (V) SimpleSerializer.deserialize(loader, ((ReplicationState<K, V>) replicationEntry).getAttribute("value"), 0);
        } catch (ClassNotFoundException cnfEx) {
            throw new DataStoreException("Cannot desrialize value", cnfEx);
        } catch (IOException ioEx) {
            throw new DataStoreException("Cannot desrialize value", ioEx);
        }
    }

    @Override
    public void writeObject(ReplicationOutputStream ros, Object obj)
            throws IOException {
        SimpleSerializer.serialize(ros, obj);
    }

    @Override
    public DataStoreEntryEvaluator<K, V> readObject(byte[] data, int index) throws DataStoreException {
        try {
            return (DataStoreEntryEvaluator<K, V>) SimpleSerializer.deserialize(loader, data, index);
        } catch (ClassNotFoundException cnfEx) {
            throw new DataStoreException("Cannot desrialize value", cnfEx);
        } catch (IOException ioEx) {
            throw new DataStoreException("Cannot desrialize value", ioEx);
        }
    }

    @Override
    public void updateDelta(K k, DataStoreEntry<K, V> kvDataStoreEntry, Object obj) {
        throw new UnsupportedOperationException("updateDelta(K k, DataStoreEntry<K, V> kvDataStoreEntry, Object obj) not supported");
    }

}
