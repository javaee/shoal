/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.enterprise.gms.tools;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Used to periodically send multicast messages.
 */
public class MulticastSenderThread extends Thread {

    static final StringManager sm = StringManager.getInstance();

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
            byte [] data = dataString.getBytes();
            group = InetAddress.getByName(mcAddress);
            DatagramPacket datagramPacket = new DatagramPacket(data,
                data.length, group, mcPort);
            socket = new MulticastSocket(mcPort);
            if (bindInterface != null) {
                socket.setInterface(InetAddress.getByName(bindInterface));
            }
            if (ttl != -1) {
                try {
                    socket.setTimeToLive(ttl);
                } catch (Exception e) {
                    System.err.println(sm.get("could.not.set.ttl",
                        e.getLocalizedMessage()));
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

            while (!interrupted()) {
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

    private void log(String msg) {
        if (debug) {
            System.err.println(String.format("%s: %s",
                getName(), msg));
        }
    }
}
