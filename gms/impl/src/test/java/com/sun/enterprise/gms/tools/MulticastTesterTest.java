/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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

package com.sun.enterprise.gms.tools;



import junit.framework.TestCase;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.enterprise.ee.cms.impl.common.GMSMonitor;


public class MulticastTesterTest extends TestCase {
    private MulticastTester tester;

    public MulticastTesterTest(String testName) {
        super(testName);
    }

    public void mySetUp() {
    }

    public void testValidateMulticastDefaults() {
        // validate all defaults EXCEPT the default timeout of 20 seconds.
        // (in interest of running junit test in minimum amount of time)
        String[] params = {"--timeout", "3"};
        tester = new MulticastTester();
        tester.run(params);
    }

    public void testValidateMulticastBadTimeout() {
        String[] params = {"--timeout", "five"};
        tester = new MulticastTester();
        assertTrue("validate detection of non-numeric parameter for --timeout", tester.run(params) != 0);
    }

    public void testValidateMulticastBadPort() {
        String[] params = {"--multicastport", "five"};
        tester = new MulticastTester();
        tester.run(params);
        assertTrue("validate detection of non-numeric parameter for --multicastport", tester.run(params) != 0);
    }

    public void testValidateMulticastBadTimeToLive() {
        String[] params = {"--timetolive", "five"};
        tester = new MulticastTester();
        tester.run(params);
        assertTrue("validate detection of non-numeric parameter for --timetolive", tester.run(params) != 0);
    }

    public void testValidateMulticastBadSendPeriod() {
        String[] params = {"--sendperiod", "five"};
        tester = new MulticastTester();
        tester.run(params);
        assertTrue("validate detection of non-numeric parameter for --period", tester.run(params) != 0);
    }

    public void testValidateMulticastNonDefaults() {
        String[] params = {"--timeout", "3", "--multicastport", "3001", "--bindinterface", "127.0.0.1", "--timetolive", "3", "--debug", "--multicastaddress", "228.9.9.1", "--sendperiod", "1000"};
        tester = new MulticastTester();
        tester.run(params);
    }

    public void testValidateMulticastHelp() {
        String[] params = {"--help"};
        tester = new MulticastTester();
        tester.run(params);
    }
}
