package org.shoal.ha.store.api;

import java.io.Serializable;

/**
 * @author Mahesh Kannan
 */
public interface DataStoreEntryEvaluator<K, V> {

    public enum Opcode {KEEP, SELECT, DELETE, UPDATED};

    public Opcode eval(DataStoreEntry<K, V> entry);
    
}
