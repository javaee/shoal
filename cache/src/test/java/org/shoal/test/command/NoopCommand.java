package org.shoal.test.command;

import org.shoal.ha.cache.api.DataStoreContext;
import org.shoal.ha.cache.api.DataStoreException;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.util.ReplicationOutputStream;

import java.io.IOException;

/**
 * @author Mahesh Kannan
 */
public class NoopCommand
    extends Command {

    public NoopCommand() {
        super((byte) 123);
    }

    @Override
    protected NoopCommand createNewInstance() {
        return new NoopCommand();
    }

    @Override
    protected void writeCommandPayload(DataStoreContext t, ReplicationOutputStream ros) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void readCommandPayload(DataStoreContext t, byte[] data, int offset) throws IOException, DataStoreException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void execute(DataStoreContext ctx) {
        System.out.println("***>> Executed Noop command");
    }
}
