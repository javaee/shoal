/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.mgmt;

import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.Date;

import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import com.sun.enterprise.mgmt.transport.Message;
import com.sun.enterprise.mgmt.transport.MessageImpl;
import junit.framework.TestCase;

public class ReliableMulticastTest extends TestCase  {

    static final long TEST_EXPIRATION_DURATION_MS = 500;  // 1/2 second.
    private ReliableMulticast rm;
    private static final String RFC_3339_DATE_FORMAT =
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final SimpleDateFormat dateFormatter =
            new SimpleDateFormat( RFC_3339_DATE_FORMAT );

    public ReliableMulticastTest( String testName ) {
        super( testName );
        GMSLogDomain.getMcastLogger().setLevel(Level.FINEST);
    }


    private Message createMessage(long seqId) {
        Message msg = new MessageImpl();
        msg.addMessageElement(MasterNode.MASTERVIEWSEQ, seqId);
        return msg;
    }


    private void mySetup() {
        rm = new ReliableMulticast(TEST_EXPIRATION_DURATION_MS);
        rm.add(createMessage(1), TEST_EXPIRATION_DURATION_MS);
        rm.add(createMessage(2), TEST_EXPIRATION_DURATION_MS);
        rm.add(createMessage(3), TEST_EXPIRATION_DURATION_MS);
        rm.add(createMessage(4), TEST_EXPIRATION_DURATION_MS);
        assertTrue(rm.sendHistorySize() == 4);
    }

    public void testReaperExpiration() {
        mySetup();
        try {
            Thread.sleep(TEST_EXPIRATION_DURATION_MS);
        } catch (InterruptedException ie) {
        }
        assertTrue(rm.sendHistorySize() == 4);
        try {
            Thread.sleep(TEST_EXPIRATION_DURATION_MS * 3);
        } catch (InterruptedException ie) {
        }
        assertTrue(rm.sendHistorySize() == 0);
        rm.stop();
    }


    public void testAdd() {
        mySetup();
        rm.stop();
    }

    public void testExpiration() {
        testAdd();
        rm.processExpired();
        assertTrue(rm.sendHistorySize() == 4);
         try {
            Thread.sleep(TEST_EXPIRATION_DURATION_MS * 3);
        } catch (InterruptedException ie) {}
        rm.processExpired();
        GMSLogDomain.getMcastLogger().info("sendHistorySize=" + rm.sendHistorySize());
        assertTrue(rm.sendHistorySize() == 0);
        rm.stop();
    }


}
