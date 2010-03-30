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

package org.shoal.ha.store.impl.command;

import org.shoal.ha.store.api.DataStoreContext;
import org.shoal.ha.store.impl.command.Command;
import org.shoal.ha.store.impl.command.ReplicationFramePayloadCommand;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mahesh Kannan
 */
public class ReplicationCommandTransmitter<K, V>
        implements Runnable {

    private AtomicInteger outgoingSeqno = new AtomicInteger();

    private AtomicInteger acknowledgedSeqno = new AtomicInteger();

    private DataStoreContext<K, V> dsc;

    private String targetName;

    private Thread thread;

    private ConcurrentLinkedQueue<Command<K, V>> list
            = new ConcurrentLinkedQueue<Command<K, V>>();

    public void initialize(String targetName, DataStoreContext<K, V> rsInfo) {
        this.targetName = targetName;
        this.dsc = rsInfo;

        thread = new Thread(this, "ReplicationCommandTransmitter[" + targetName + "]");
        thread.setDaemon(true);
        thread.start();
    }

    public void send(String targetName, Command<K, V> cmd) {
        list.add(cmd);
        System.out.println("ReplicationCommandTransmitter[" + targetName + "] just "
        + " accumulated: " + cmd.getOpcode());
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(15);
                if (list.peek() != null) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ReplicationFrame<K, V> frame = new ReplicationFrame<K, V>((byte)255,
                            dsc.getServiceName(), dsc.getInstanceName());

                    Command<K, V> cmd = list.poll();
                    while (cmd != null) {
                        frame.addCommand(cmd);
                        cmd = list.poll();

                        if (frame.getCommands().size() >= 20) {
                            transmitFramePayload(frame);
                            frame = new ReplicationFrame<K, V>((byte)255,
                            dsc.getServiceName(), dsc.getInstanceName());
                        }
                    }

                    if (frame.getCommands().size() > 0) {
                        transmitFramePayload(frame);
                    }


                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void transmitFramePayload(ReplicationFrame<K, V> frame) {
        frame.setSeqNo(outgoingSeqno.incrementAndGet());
        frame.setMinOutstandingPacketNumber(acknowledgedSeqno.get());
        frame.setTargetInstanceName(targetName);
        ReplicationFramePayloadCommand<K, V> cmd = new ReplicationFramePayloadCommand<K, V>();
        cmd.setReplicationFrame(frame);

        dsc.getCommandManager().execute(cmd);


    }

}
