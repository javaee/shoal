/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import com.sun.enterprise.mgmt.transport.NetworkUtility;

import static com.sun.enterprise.ee.cms.core.GMSConstants.MINIMUM_MULTICAST_TIME_TO_LIVE;
import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.logging.Level;

/**
 * Used to periodically send multicast messages.
 */
public class MulticastSenderThread extends Thread {

    static final StringManager sm = StringManager.getInstance();

    volatile boolean done = false;

    int mcPort;
    String mcAddress;
    String bindInterface;
    int ttl;
    long msgPeriodInMillis;
    boolean debug;
    String dataString;

    public MulticastSenderThread(int mcPort, String mcAddress,
        String bindInterface, int ttl, long msgPeriodInMillis,
        boolean debug, String dataString) {
        super("McastSender");
        this.mcPort = mcPort;
        this.mcAddress = mcAddress;
        this.bindInterface = bindInterface;
        this.ttl = ttl;
        this.msgPeriodInMillis = msgPeriodInMillis;
        this.debug = debug;
        this.dataString = dataString;
    }

    @Override
    public void run() {
        InetAddress group = null;
        MulticastSocket socket = null;
        try {
            byte [] data = dataString.getBytes(Charset.defaultCharset());
            group = InetAddress.getByName(mcAddress);
            DatagramPacket datagramPacket = new DatagramPacket(data,
                data.length, group, mcPort);
            socket = new MulticastSocket(mcPort);

            if (bindInterface != null) {
                InetAddress iaddr = InetAddress.getByName(bindInterface);
                log("InetAddress.getByName returned: " + iaddr);

                // make sure the network interface is valid
                NetworkInterface ni = NetworkInterface.getByInetAddress(iaddr);
                if (ni != null && NetworkUtility.isUp(ni)) {
                    socket.setInterface(iaddr);
                    System.out.println(String.format(sm.get("configured.bindinterface", bindInterface,
                        ni.getName(), ni.getDisplayName(), NetworkUtility.isUp(ni),
                        ni.isLoopback())));
                } else {
                    if (ni != null) {
                        System.out.println(String.format(sm.get("invalid.bindinterface", bindInterface,
                            ni.getName(), ni.getDisplayName(), NetworkUtility.isUp(ni),
                            ni.isLoopback())));
                    } else {
                        System.err.println(sm.get("nonexistent.bindinterface",
                            bindInterface));
                    }
                    iaddr = getFirstAddress();
                    log("setting socket to: " + iaddr + " instead");
                    socket.setInterface(iaddr);
                }
            } else {
                InetAddress iaddr = getFirstAddress();
                log("setting socket to: " + iaddr);
                socket.setInterface(iaddr);
            }

            if (ttl != -1) {
                try {
                    socket.setTimeToLive(ttl);
                } catch (Exception e) {
                    System.err.println(sm.get("could.not.set.ttl",
                        e.getLocalizedMessage()));
                }
            } else {
                try {
                    int defaultTTL = socket.getTimeToLive();
                    if (defaultTTL < MINIMUM_MULTICAST_TIME_TO_LIVE) {
                        log(String.format(
                            "The default TTL for the socket is %d. " +
                            "Setting it to minimum %d instead.",
                            defaultTTL, MINIMUM_MULTICAST_TIME_TO_LIVE));
                        socket.setTimeToLive(MINIMUM_MULTICAST_TIME_TO_LIVE);
                    }
                } catch (IOException ioe) {
                    // who cares? we'll print it again a couple lines down
                }
            }

            // 'false' means do NOT disable
            log("setting loopback mode false on mcast socket");
            socket.setLoopbackMode(false);

            try {
                log(String.format("socket time to live set to %s",
                    socket.getTimeToLive()));
            } catch (IOException ioe) {
                log(ioe.getLocalizedMessage());
            }
            
            log(String.format("joining group: %s", group.toString()));
            socket.joinGroup(group);
            if (!debug) {
                dataString = MulticastTester.trimDataString(dataString);
            }
            System.out.println(sm.get("sending.message",
                dataString, msgPeriodInMillis));

            while (!done) {
                socket.send(datagramPacket);
                try {
                    Thread.sleep(msgPeriodInMillis);
                } catch (InterruptedException ie) {
                    log("interrupted");
                    break;
                }
            }            
        } catch (Exception e) {
            System.err.println(sm.get("whoops", e.toString()));
        } finally {
            if (socket != null) {
                if (group != null) {
                    log("socket leaving group");
                    try {
                        socket.leaveGroup(group);
                    } catch (IOException ioe) {
                        System.err.println(sm.get("ignoring.exception.leaving",
                            getName(), ioe.toString()));
                    }
                }
                log("closing socket");
                socket.close();
            }
        }
    }

    // utility so we can silence the shoal logger
    private InetAddress getFirstAddress() throws IOException {
        if (!debug) {
            GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER).setLevel(
                Level.SEVERE);
        }
        return NetworkUtility.getFirstInetAddress(false);
    }

    private void log(String msg) {
        if (debug) {
            System.err.println(String.format("%s: %s",
                getName(), msg));
        }
    }
}
