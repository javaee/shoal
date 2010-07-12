/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.shoal.test.command;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.shoal.ha.cache.api.DataStoreException;
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
        try {
            cm.execute(new NoopCommand());
            assert (true);
        } catch (DataStoreException dse) {
            assert(false);
        }
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