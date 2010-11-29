package org.shoal.ha.cache.impl.store;

import org.glassfish.ha.store.api.Storeable;
import org.shoal.adapter.store.commands.LoadResponseCommand;
import org.shoal.adapter.store.commands.SaveCommand;
import org.shoal.ha.cache.api.*;
import org.shoal.ha.cache.impl.util.SimpleStoreableMetadata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author Mahesh Kannan
 */
public class
        StoreableDataStoreEntryUpdater<K, V extends Storeable>
        extends DataStoreEntryUpdater<K, V> {

    @Override
    public SaveCommand<K, V> createSaveCommand(DataStoreEntry<K, V> entry, K k, V v) {
        SaveCommand<K, V> cmd = new SaveCommand<K, V>(k, v, v._storeable_getVersion(),
                v._storeable_getLastAccessTime(), v._storeable_getMaxIdleTime());

        super.updateMetaInfoInDataStoreEntry(entry, cmd);
        entry.setIsReplicaNode(false);

        return cmd;
    }

    @Override
    public LoadResponseCommand<K, V> createLoadResponseCommand(DataStoreEntry<K, V> entry, K k, long minVersion)
            throws DataStoreException {
        LoadResponseCommand<K, V> cmd = null;
        if (entry != null && entry.isReplicaNode() && entry.getVersion() >= minVersion) {
            byte[] rawV = super.captureState(entry.getV());
            cmd = new LoadResponseCommand<K, V>(k, entry.getVersion(), rawV);
//            System.out.println("Sending valid load response for key: " + k
//                    + "; minVersion = " + minVersion + "; myVersion = " + entry.getVersion());
        } else {
            cmd = new LoadResponseCommand<K, V>(k, Long.MIN_VALUE, null);
//            System.out.println("Sending NotFound load response for key: " + k);
        }
        return cmd;
    }

    @Override
    public V extractVFrom(LoadResponseCommand<K, V> cmd)
            throws DataStoreException {
        return cmd.getRawV() == null ? null : super.deserializeV(cmd.getRawV());
    }

    @Override
    public void executeSave(DataStoreEntry<K, V> entry, SaveCommand<K, V> saveCmd)
            throws DataStoreException {

        if (entry.getV() == null) {
            //This is the only data that we have
            // So just deserialize and merge with subsequent data

            V v = null;
            try {
                v = ctx.getValueClazz().newInstance();
                mergeIntoV(entry, v, saveCmd);
            } catch (Exception ex) {
                throw new DataStoreException(ex);
            }
            entry.setV(v);
            super.updateMetaInfoInDataStoreEntry(entry, saveCmd);
            super.printEntryInfo("Saved initial entry", entry, saveCmd.getKey());
            entry.setIsReplicaNode(true);
        } else {
//            System.out.println("Added to pending update: key = " + saveCmd.getKey()
//                    + "; entry.version" + entry.getVersion()
//                    + "; cmd.version" + saveCmd.getVersion());
            entry.addPendingUpdate(saveCmd);
            Iterator<SaveCommand<K, V>> iter = entry.getPendingUpdates().iterator();
            while (iter.hasNext()) {
                SaveCommand<K, V> cmd = iter.next();
                if (entry.getVersion() + 1 == cmd.getVersion()) {
                    iter.remove();
                    mergeIntoV(entry, entry.getV(), cmd);
                }
            }
            entry.setIsReplicaNode(true);
        }
    }

    private void mergeIntoV(DataStoreEntry<K, V> entry, V v, SaveCommand<K, V> cmd)
            throws DataStoreException {
        v._storeable_setVersion(cmd.getVersion());
        v._storeable_setLastAccessTime(cmd.getLastAccessedAt());
        v._storeable_setMaxIdleTime(cmd.getMaxIdleTime());

        super.updateMetaInfoInDataStoreEntry(entry, cmd);

        ByteArrayInputStream bis = new ByteArrayInputStream(cmd.getRawV());
        try {
            v._storeable_readState(bis);
        } catch (Exception ex) {
            throw new DataStoreException("Error during updating existing V", ex);
        }

        //Now that we have...
//        super.printEntryInfo("Merged entry from pending updates", entry, cmd.getKey());
    }

    @Override
    public byte[] getState(V v)
            throws DataStoreException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            v._storeable_writeState(bos);
        } catch (IOException ex) {
            throw new DataStoreException(ex);
        }

        byte[] data = bos.toByteArray();
//        System.out.println("Returning  a byte[] of length = " + data.length);

        return data;
    }

    @Override
    public V getV(DataStoreEntry<K, V> entry)
            throws DataStoreException {
        return entry.getV();
    }

}