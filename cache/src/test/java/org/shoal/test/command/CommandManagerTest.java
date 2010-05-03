package org.shoal.test.command;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.shoal.ha.cache.impl.interceptor.CommandMonitorInterceptor;
import org.shoal.test.common.DummyGroupService;
import org.shoal.ha.cache.api.DataStoreContext;
import org.shoal.ha.cache.impl.command.CommandManager;
import org.shoal.ha.group.GroupService;

/**
 * @author Mahesh Kannan
 */
public class CommandManagerTest
        extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public CommandManagerTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(CommandManagerTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testCreateCommandManager() {
        GroupService gs = new DummyGroupService("inst1", "grp1");
        DataStoreContext dsc = new DataStoreContext("test", gs, this.getClass().getClassLoader());
        CommandManager cm = new CommandManager(dsc);
        assertTrue(true);
    }

    public void testRegistercommands() {
        GroupService gs = new DummyGroupService("inst1", "grp1");
        DataStoreContext dsc = new DataStoreContext("test", gs, this.getClass().getClassLoader());
        CommandManager cm = new CommandManager(dsc);
        cm.registerCommand(new NoopCommand());
        cm.execute(new NoopCommand());
        assert (true);
    }

    /*
    public void testInterceptors() {
        GroupService gs = new DummyGroupService("inst1", "grp1");
        DataStoreContext dsc = new DataStoreContext("test", gs);
        CommandManager cm = dsc.getCommandManager();
        cm.registerCommand(new NoopCommand());
        CommandMonitorInterceptor cmi1 = new CommandMonitorInterceptor();
        CommandMonitorInterceptor cmi2 = new CommandMonitorInterceptor();
        cm.registerExecutionInterceptor(cmi1);
        cm.registerExecutionInterceptor(cmi2);
        cm.execute(new NoopCommand());

        boolean stat = cmi1.getTransmitCount() == 1;
        stat = stat && (cmi2.getTransmitCount() == cmi1.getTransmitCount());
        assertTrue(stat);
    }

    public void testLoopBackInterceptors() {
        GroupService gs = new DummyGroupService("inst1", "grp1");
        DataStoreContext dsc = new DataStoreContext("test", gs);
        CommandManager cm = dsc.getCommandManager();

        cm.registerCommand(new NoopCommand());
        cm.registerCommand(new BatchedNoopCommand());
        NoopCommandInterceptor cmi1 = new NoopCommandInterceptor();
        BatchedNoopCommandInterceptor bat = new BatchedNoopCommandInterceptor();
        cm.registerExecutionInterceptor(cmi1);
        cm.registerExecutionInterceptor(bat);
        cm.execute(new NoopCommand());

        System.out.println("****** testLoopBackInterceptors ******");
        System.out.println("* cmi1.getTotalTransCount(): " + cmi1.getTotalTransCount());
        System.out.println("* cmi1.getNoopTransCount(): " + cmi1.getNoopTransCount());
        System.out.println("* bat.getNoopTransCount(): " + bat.getTransmitCount());
        boolean stat = cmi1.getTotalTransCount() == 2;
        stat = stat && (cmi1.getNoopTransCount() == 1);
        stat = stat && (bat.getTransmitCount() == 1);
        assertTrue(stat);
    }
    */
}