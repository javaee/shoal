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
import org.shoal.ha.store.impl.util.ReplicationOutputStream;
import org.shoal.ha.store.impl.command.CommandManager;
import org.shoal.ha.store.impl.util.Utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @auhor Mahesh Kannan
 */
public class ReplicationFrame<K, V> {

    private byte frameCommand;

    private String serviceName;

    private String sourceInstanceName;

    private String targetInstanceName;

    private int seqNo;

    private int windowLen;

    private int minOutstandingPacketNumber;

    private int maxOutstandingPacketNumber;

    private List<Command<K, V>> commands
            = new LinkedList<Command<K, V>>();

    public ReplicationFrame(byte frameCommand, int seqNo, String serviceName, String sourceInstanceName) {
        this.frameCommand = frameCommand;
        this.seqNo = seqNo;
        this.serviceName = serviceName;
        this.sourceInstanceName = sourceInstanceName;
    }

    public ReplicationFrame(byte frameCommand, String serviceName, String sourceInstanceName) {
        this.frameCommand = frameCommand;
        this.serviceName = serviceName;
        this.sourceInstanceName = sourceInstanceName;
    }

    public byte getFrameCommand() {
        return frameCommand;
    }

    public String getSourceInstanceName() {
        return sourceInstanceName;
    }

    public String getTargetInstanceName() {
        return targetInstanceName;
    }

    public void setTargetInstanceName(String targetInstanceName) {
        this.targetInstanceName = targetInstanceName;
    }

    public int getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(int seqNo) {
        this.seqNo = seqNo;
    }

    public int getWindowLen() {
        return windowLen;
    }

    public void setWindowLen(int windowLen) {
        this.windowLen = windowLen;
    }

    public int getMinOutstandingPacketNumber() {
        return minOutstandingPacketNumber;
    }

    public void setMinOutstandingPacketNumber(int minOutstandingPacketNumber) {
        this.minOutstandingPacketNumber = minOutstandingPacketNumber;
    }

    public int getMaxOutstandingPacketNumber() {
        return maxOutstandingPacketNumber;
    }

    public void setMaxOutstandingPacketNumber(int maxOutstandingPacketNumber) {
        this.maxOutstandingPacketNumber = maxOutstandingPacketNumber;
    }

    public void addCommand(Command<K, V> cmd) {
        commands.add(cmd);
    }

    public List<Command<K, V>> getCommands() {
        return commands;
    }

    private static void writeStringToStream(ByteArrayOutputStream bos, String str)
            throws IOException {
        if (str == null) {
            bos.write(Utility.intToBytes(0));
        } else {
            bos.write(Utility.intToBytes(str.length()));
            bos.write(str.getBytes());
        }
    }

    private static String readStringFrom(byte[] data, int offset) {
        int len = Utility.bytesToInt(data, offset);
        return len == 0 ? null : new String(data, offset + 4, len);
    }

    public byte[] getSerializedData() {
        byte[] data = new byte[0];
        ReplicationOutputStream bos = new ReplicationOutputStream();
        try {
            bos.write(new byte[]{frameCommand});
            bos.write(Utility.intToBytes(seqNo));
            bos.write(Utility.intToBytes(windowLen));
            bos.write(Utility.intToBytes(minOutstandingPacketNumber));
            bos.write(Utility.intToBytes(maxOutstandingPacketNumber));
            writeStringToStream(bos, serviceName);
            writeStringToStream(bos, sourceInstanceName);
            writeStringToStream(bos, targetInstanceName);

            int cmdSz = commands.size();
            bos.write(Utility.intToBytes(cmdSz));

            byte[] cmdOffsets = new byte[4 * cmdSz];
            int offMark = bos.mark();
            bos.write(cmdOffsets);

            int base = bos.size();
            for (int i = 0; i < cmdSz; i++) {
                Utility.intToBytes(bos.size() - base, cmdOffsets, i * 4);
                Command cmd = commands.get(i);
                cmd.writeCommandState(bos);

                System.out.println("Wrote cmd[" + i + "] => opcode: " + cmd.getOpcode()
                        + "; offset: " + Utility.bytesToInt(cmdOffsets, i * 4) + " : " + offMark);
            }

            bos.reWrite(offMark, cmdOffsets);
            bos.flush();
            data = bos.toByteArray();

            System.out.println("Wrote " + cmdSz + " commands; totalBytes: " + data.length);
        } catch (IOException ioEx) {
        } finally {
            try {
                bos.close();
            } catch (Exception ex) {
                //Ignore
            }
        }

        return data;
    }

    public static <K, V> ReplicationFrame<K, V> toReplicationFrame(DataStoreContext<K, V> dsc,
                                                                   byte[] data, int offset) {


        System.out.println("Reading frame of totalBytes: " + data.length);
        ReplicationFrame<K, V> frame = null;
        byte com = data[offset];
        offset += 1;
        int seqNo = Utility.bytesToInt(data, offset);
        offset += 4;
        int wLen = Utility.bytesToInt(data, offset);
        offset += 4;
        int min = Utility.bytesToInt(data, offset);
        offset += 4;
        int max = Utility.bytesToInt(data, offset);
        offset += 4;

        String servName = readStringFrom(data, offset);
        offset += 4 + (servName == null ? 0 : servName.length());

        String srcName = readStringFrom(data, offset);
        offset += 4 + ((srcName == null) ? 0 : srcName.length());

        String tarName = readStringFrom(data, offset);
        offset += 4 + ((tarName == null) ? 0 : tarName.length());

        frame = new ReplicationFrame<K, V>(com, seqNo, servName, srcName);
        frame.setTargetInstanceName(tarName);
        frame.setMinOutstandingPacketNumber(min);
        frame.setMaxOutstandingPacketNumber(max);
        frame.setTargetInstanceName(tarName);
        frame.setWindowLen(wLen);

        int numStates = Utility.bytesToInt(data, offset);
        offset += 4;

        int base = offset + numStates * 4;
        int[] cmdOffsets = new int[numStates];
        for (int i = 0; i < numStates; i++) {
            cmdOffsets[i] = Utility.bytesToInt(data, offset);
            offset += 4;
            System.out.println("Read cmd[" + i + "/" + numStates + "] => " + cmdOffsets[i]
                    + "; opcode - " + data[cmdOffsets[i] + base]);
        }

        CommandManager<K, V> cm = dsc.getCommandManager();
        for (int i = 0; i < numStates; i++) {
            byte opcode = data[cmdOffsets[i] + base];
            Command<K, V> cmd = null;
            try {
                cm.createNewInstance(opcode, data, cmdOffsets[i] + base);
                frame.addCommand(cmd);
            } catch (IOException dse) {

            }
//            System.out.println("After createNewInstanceCmd[" + i + "]: " + cmd);
        }

        return frame;
    }

    @Override
    public String toString() {
        return "ReplicationFrame{" +
                "frameCommand=" + frameCommand +
                ", serviceName='" + serviceName + '\'' +
                ", sourceInstanceName='" + sourceInstanceName + '\'' +
                ", targetInstanceName='" + targetInstanceName + '\'' +
                ", seqNo=" + seqNo +
                ", windowLen=" + windowLen +
                ", minOutstandingPacketNumber=" + minOutstandingPacketNumber +
                ", maxOutstandingPacketNumber=" + maxOutstandingPacketNumber +
                ", state.size=" + commands.size() +
                '}';
    }
}
