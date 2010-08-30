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
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Used to listen for multicast messages.
 */
public class MultiCastReceiverThread extends Thread {

    static final StringManager sm = StringManager.getInstance();

    final AtomicBoolean receivedAnything = new AtomicBoolean(false);

    int mcPort;
    String mcAddress;
    String bindInterface;
    boolean debug;
    String targetData;

    public MultiCastReceiverThread(int mcPort, String mcAddress,
        String bindInterface, boolean debug, String targetData) {
        super("McastReceiver");
        this.mcPort = mcPort;
        this.mcAddress = mcAddress;
        this.bindInterface = bindInterface;
        this.debug = debug;
        this.targetData = targetData;
    }

    @Override
    public void run() {
        InetAddress group = null;
        MulticastSocket ms = null;
        try {
            byte[] buffer = new byte[8192];

            group = InetAddress.getByName(mcAddress);
            ms = new MulticastSocket(mcPort);
            if (bindInterface != null) {
                ms.setInterface(InetAddress.getByName(bindInterface));
            }
            ms.joinGroup(group);

            System.out.println(sm.get("listening.info"));
            Set<String> hosts = new HashSet<String>();
            while (!interrupted()) {
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                ms.receive(dp);
                String newData = new String(dp.getData()).trim();
                log(String.format("received '%s'", newData));
                if (hosts.add(newData)) {
                    if (targetData.equals(newData)) {
                        System.out.println(sm.get("loopback.from",
                            MulticastTester.trimDataString(newData)));
                    } else {
                        System.out.println(sm.get("received.from",
                            MulticastTester.trimDataString(newData)));
                        receivedAnything.set(true);
                    }
                }
            }
        } catch (InterruptedIOException iioe) {
            // this is fine. time to exit
            log(iioe.getMessage());
        } catch (Exception e) {
            System.err.println(sm.get("whoops", e.toString()));
        } finally {
            if (ms != null) {
                if (group != null) {
                    log("socket leaving group");
                    try {
                        ms.leaveGroup(group);
                    } catch (IOException ioe) {
                        System.err.println(sm.get("ignoring.exception.leaving",
                            getName(), ioe.toString()));
                    }
                }
                log("closing socket");
                ms.close();
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
