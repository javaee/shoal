/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.mgmt.transport;

import junit.framework.TestCase;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class NetworkUtilityTest extends TestCase {

    /*
     * Separate test in case there is an error that prevents test
     * from running.
     */
    public void testBindInterfaceValidLocalHost() {
        InetAddress localhost;
        try {
            localhost = InetAddress.getLocalHost();
        } catch (UnknownHostException uhe) {
            throw new RuntimeException(uhe);
        }
        String s = localhost.getHostAddress();
        assertTrue(String.format("Expected true result for '%s'", s),
            NetworkUtility.isBindAddressValid(s));
        s = localhost.getHostName();
        assertTrue(String.format("Expected true result for '%s'", s),
            NetworkUtility.isBindAddressValid(s));
    }

    /*
     * Change the values to test specific addresses. This has been tested
     * with my local IP and IPv6 addresses, but those values cannot
     * obviously be checked in.
     */
    public void testBindInterfaceValid() {
        final String local [] = {
            "127.0.0.1",
            "127.0.1", // same as 127.0.0.1
            "127.1",   // ditto
            "localhost"
//            "::1" // ipv6 version of 127.0.0.1
        };
        final String notLocalOrValid [] = {
            "_",
            "99999999999999",
            "www.oracle.com"
        };

        for (String s : local) {
            assertTrue(String.format("Expected true result for '%s'", s),
                NetworkUtility.isBindAddressValid(s));
        }
        for (String s : notLocalOrValid) {
            assertFalse(String.format("Expected false result for '%s'", s),
                NetworkUtility.isBindAddressValid(s));
        }
    }

    public void testAllLocalAddresses() {
        List<InetAddress> locals = NetworkUtility.getAllLocalAddresses();
        for (InetAddress local : locals) {
            assertTrue(NetworkUtility.isBindAddressValid(
                local.getHostAddress()));
        }
    }

    public void testGetFirstAddress() throws IOException {
       System.out.println( "AllLocalAddresses() = " + NetworkUtility.getAllLocalAddresses() );
       System.out.println( "getFirstNetworkInterface() = " + NetworkUtility.getFirstNetworkInterface() );
       System.out.println( "getFirstInetAddress( true ) = " + NetworkUtility.getFirstInetAddress(true) );
       System.out.println( "getFirstInetAddress( false ) = " + NetworkUtility.getFirstInetAddress(false) );
       System.out.println( "getFirstNetworkInteface() = " + NetworkUtility.getFirstNetworkInterface());
       System.out.println( "getFirstInetAddress(firstNetworkInteface, true) = " +
               NetworkUtility.getFirstInetAddress(NetworkUtility.getFirstNetworkInterface(),true));
       System.out.println( "getFirstInetAddress(firstNetworkInteface, false) = " +
               NetworkUtility.getFirstInetAddress(NetworkUtility.getFirstNetworkInterface(),false));

    }
}
