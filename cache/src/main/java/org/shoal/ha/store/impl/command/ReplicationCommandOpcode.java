package org.shoal.ha.store.impl.command;

/**
 * @author Mahesh Kannan
 */
public class ReplicationCommandOpcode {

    public static final byte SAVE = 33;

    public static final byte SAVE_WITH_DSEE = 34;

    public static final byte LOAD_REQUEST = 35;

    public static final byte REMOVE = 36;

    public static final byte LOAD_RESPONSE = 37;


    public static final byte REPLICATION_FRAME_PAYLOAD = 51;

}
