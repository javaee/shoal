package org.shoal.ha.cache.impl.store;

import org.shoal.adapter.store.commands.LoadResponseCommand;
import org.shoal.adapter.store.commands.SaveCommand;
import org.shoal.ha.cache.api.*;

import java.util.logging.Level;

/**
 * An entry updater used for plain Serializable POJOs
 *
 * @author Mahesh Kannan
 */
public class SimpleDataStoreEntryUpdater<K, V>
        extends DataStoreEntryUpdater<K, V> {

    @Override
    public SaveCommand<K, V> createSaveCommand(DataStoreEntry<K, V> entry, K k, V v) {
        SaveCommand<K, V> cmd = new SaveCommand<K, V>(k, v, entry.incrementAndGetVersion(),
                System.currentTimeMillis(), ctx.getDefaultMaxIdleTimeInMillis());
        
        //Update this entry's meta info
        super.updateMetaInfoInDataStoreEntry(entry, cmd);
        entry.setIsReplicaNode(false);
        
        return cmd;
    }

    @Override
    public LoadResponseCommand<K, V> createLoadResponseCommand(DataStoreEntry<K, V> entry, K k, long minVersion) {
        LoadResponseCommand<K, V> cmd = null;
        if (entry != null && entry.isReplicaNode() && entry.getVersion() >= minVersion) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "SimpleDataStoreEntryUpdater.createLoadResp "
                    + " entry.version " + entry.getVersion() + ">= " + minVersion
                    + "; rawV.length = " + entry.getRawV());
            }
            cmd = new LoadResponseCommand<K, V>(k, entry.getVersion(), entry.getRawV());
        } else {
            if (_logger.isLoggable(Level.FINE)) {
                String entryMsg = (entry == null) ? "NULL ENTRY"
                        : (entry.getVersion() + " < " + minVersion);
                _logger.log(Level.FINE, "SimpleDataStoreEntryUpdater.createLoadResp " + entryMsg
                   + "; rawV.length = " + (entry == null ? " null " : "" + entry.getRawV()));
            }
            cmd = new LoadResponseCommand<K, V>(k, Long.MIN_VALUE, null);
        }
        return cmd;
    }

    @Override
    public void executeSave(DataStoreEntry<K, V> entry, SaveCommand<K, V> cmd) {
        if (entry != null && entry.getVersion() < cmd.getVersion()) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "SimpleDataStoreEntryUpdater.executeSave. SAVING ... "
                    + "entry = " + entry + "; entry.version = " + entry.getVersion()
                    + "; cmd.version = " + cmd.getVersion() + "; cmd.maxIdle = " + cmd.getMaxIdleTime());
            }
            entry.setIsReplicaNode(true);
            super.updateMetaInfoInDataStoreEntry(entry, cmd);
            entry.setRawV(cmd.getRawV());
//            super.printEntryInfo("Updated", entry, cmd.getKey());
        } else {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "SimpleDataStoreEntryUpdater.executeSave. IGNORING ... "
                    + "entry = " + entry + "; entry.version = " + entry.getVersion()
                    + "; cmd.version = " + cmd.getVersion());
            }
        }
    }

    @Override
    public V getV(DataStoreEntry<K, V> entry)
            throws DataStoreException {
        V v = entry.getV();
        if (entry != null && v == null && entry.getRawV() != null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "SimpleDataStoreEntryUpdater.getV(): Reading from raw data: " + entry.getRawV().length);
            }

            v = super.deserializeV(entry.getRawV());
        }

        return v;
    }

    @Override
    public byte[] getState(V v)
            throws DataStoreException {
        return captureState(v);
    }

    @Override
    public V extractVFrom(LoadResponseCommand<K, V> cmd)
        throws DataStoreException {
        byte[] rawV = cmd.getRawV();
        return rawV == null ? null : super.deserializeV(rawV);
    }
}
