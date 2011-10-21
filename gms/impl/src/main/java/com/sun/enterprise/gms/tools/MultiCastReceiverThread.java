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

package com.sun.enterprise.gms.tools;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.sun.enterprise.gms.tools.MulticastTester.SEP;

/**
 * Used to listen for multicast messages.
 */
public class MultiCastReceiverThread extends Thread {

    static final StringManager sm = StringManager.getInstance();

    final AtomicBoolean receivedAnything = new AtomicBoolean(false);

    volatile boolean done = false;

    int mcPort;
    String mcAddress;
    boolean debug;
    String targetData;
    MulticastSocket ms;
    String expectedPrefix;

    public MultiCastReceiverThread(int mcPort, String mcAddress,
        boolean debug, String targetData) {
        super("McastReceiver");
        this.mcPort = mcPort;
        this.mcAddress = mcAddress;
        this.debug = debug;
        this.targetData = targetData;
        StringTokenizer st = new StringTokenizer(targetData, SEP);
        expectedPrefix = st.nextToken() + SEP;
    }

    @Override
    public void run() {
        log(String.format("expected message prefix is '%s'", expectedPrefix));
        try {
            final int bufferSize = 8192;

            InetAddress group = InetAddress.getByName(mcAddress);
            ms = new MulticastSocket(mcPort);
            ms.joinGroup(group);

            System.out.println(sm.get("listening.info"));
            Set<String> uniqueData = new HashSet<String>();

            /*
             * 'done' will almost never be read as true here unless
             * there is some unusual timing. But we have leaveGroup
             * and socket closing code here anyway to be polite. Maybe
             * the thread interrupt call will interrupt the receive call
             * in a different JDK impl/version.
             */
            while (!done) {
                DatagramPacket dp = new DatagramPacket(new byte[bufferSize],
                    bufferSize);
                ms.receive(dp);
                String newData = new String(dp.getData()).trim();
                log(String.format("received '%s'", newData));

                // if data is from some other process, ignore
                if (!newData.startsWith(expectedPrefix)) {
                    log("Ignoring previous data");
                    continue;
                }

                if (uniqueData.add(newData)) {
                    if (targetData.equals(newData)) {
                        System.out.println(sm.get("loopback.from",
                            MulticastTester.trimDataString(newData)));
                        receivedAnything.set(true);
                    } else {
                        System.out.println(sm.get("received.from",
                            MulticastTester.trimDataString(newData)));
                        receivedAnything.set(true);
                    }
                }
            }
        } catch (SocketException se) {
            log("caught socket exception as expected");
        } catch (Exception e) {
            System.err.println(sm.get("whoops", e.toString()));
            if (debug) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void interrupt() {
        if (ms != null) {
            log("closing socket in interrupt");
            try {
                ms.close();
            } catch (Throwable ignored) {
                log(ignored.getMessage());
            } finally {
                super.interrupt();
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
