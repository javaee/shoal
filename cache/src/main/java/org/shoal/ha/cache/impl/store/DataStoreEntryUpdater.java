package org.shoal.ha.cache.impl.store;

import org.shoal.adapter.store.commands.AbstractSaveCommand;
import org.shoal.adapter.store.commands.LoadResponseCommand;
import org.shoal.adapter.store.commands.SaveCommand;
import org.shoal.adapter.store.commands.TouchCommand;
import org.shoal.ha.cache.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public abstract class DataStoreEntryUpdater<K, V> {

    protected transient static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_SAVE_COMMAND);

    protected DataStoreContext<K, V> ctx;

    public void initialize(DataStoreContext<K, V> ctx) {
        this.ctx = ctx;
        _logger.log(Level.FINE, "** INITIALIZED DSEUpdater: " + this.getClass().getName());
    }

    protected byte[] captureState(V v)
        throws DataStoreException {
        byte[] result = null;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(v);
            oos.close();

            result = bos.toByteArray();
        } catch (Exception ex) {
            throw new DataStoreException("Error during prepareToTransmit()", ex);
        } finally {
            try { oos.close(); } catch (Exception ex) {_logger.log(Level.FINEST, "Ignorable error while closing ObjectOutputStream");}
            try { bos.close(); } catch (Exception ex) {_logger.log(Level.FINEST, "Ignorable error while closing ByteArrayOutputStream");}
        }

        return result;
    }

    protected V deserializeV(byte[] rawV)
            throws DataStoreException {
        ClassLoader loader = ctx.getClassLoader();
        V v = null;
        ByteArrayInputStream bis = new ByteArrayInputStream(rawV);
        ObjectInputStreamWithLoader ois = null;
        try {
            ois = new ObjectInputStreamWithLoader(bis, loader);
            v = (V) ois.readObject();
        } catch (Exception ex) {
            throw new DataStoreException(ex);
        } finally {
            try {
                ois.close();
            } catch (Exception ex) {
            }
            try {
                bis.close();
            } catch (Exception ex) {
            }
        }

        return v;
    }

    protected void updateMetaInfoInDataStoreEntry(DataStoreEntry<K, V> entry, AbstractSaveCommand<K, V> cmd) {
        entry.setVersion(cmd.getVersion());
        entry.setLastAccessedAt(cmd.getLastAccessedAt());
        entry.setMaxIdleTime(cmd.getMaxIdleTime());
    }

    protected void printEntryInfo(String msg, DataStoreEntry<K, V> entry, K key) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "executeSave:" + msg + " key = "
                    + key + "; entry.version = " + entry.getVersion() + " ; entry.lastAccess = " + entry.getLastAccessedAt()
                    + "; entry.maxIdle = " + entry.getMaxIdleTime());
        }
    }

    public abstract SaveCommand<K, V> createSaveCommand(DataStoreEntry<K, V> entry, K k, V v);

    public abstract LoadResponseCommand<K, V> createLoadResponseCommand(DataStoreEntry<K, V> entry, K k, long minVersion)
            throws DataStoreException;

    public abstract void executeSave(DataStoreEntry<K, V> entry, SaveCommand<K, V> cmd)
            throws DataStoreException;

    public abstract void executeTouch(DataStoreEntry<K, V> entry, TouchCommand<K, V> cmd)
            throws DataStoreException;

    public abstract V getV(DataStoreEntry<K, V> entry)
            throws DataStoreException;

    public abstract V extractVFrom(LoadResponseCommand<K, V> cmd)
            throws DataStoreException;
    
    public abstract byte[] getState(V v)
            throws DataStoreException;

}