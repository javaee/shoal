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

package org.shoal.ha.cache.impl.command;

import org.shoal.ha.cache.api.DataStoreContext;
import org.shoal.ha.cache.impl.util.ReplicationInputStream;
import org.shoal.ha.cache.impl.util.ReplicationOutputStream;
import org.shoal.ha.cache.impl.util.Utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @auhor Mahesh Kannan
 */
public class ReplicationFrame<K, V> {

    private byte frameCommand;

    private String sourceInstanceName;

    private String targetInstanceName;

    private int seqNo;

    private int windowLen;

    private int minOutstandingPacketNumber;

    private int maxOutstandingPacketNumber;

    private List<Command<K, V>> commands
            = new LinkedList<Command<K, V>>();

    public ReplicationFrame(byte frameCommand, int seqNo, String sourceInstanceName) {
        this.frameCommand = frameCommand;
        this.seqNo = seqNo;
        this.sourceInstanceName = sourceInstanceName;
    }

    public ReplicationFrame(byte frameCommand, String sourceInstanceName) {
        this.frameCommand = frameCommand;
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
            bos.write(frameCommand);
            bos.writeInt(seqNo);
            bos.writeLengthPrefixedString(sourceInstanceName);
            bos.writeLengthPrefixedString(targetInstanceName);

            int cmdSz = commands.size();
            bos.writeInt(cmdSz);

            int[] cmdOffsets = new int[cmdSz];
            int offMark = bos.mark();
            bos.write(new byte[4 * cmdSz]);

            for (int i = 0; i < cmdSz; i++) {
                cmdOffsets[i] = bos.mark();
                bos.writeInt(0);
                Command cmd = commands.get(i);
                cmd.write(bos);
                int len = bos.size() - cmdOffsets[i] - 4;
                bos.moveTo(cmdOffsets[i]);
                bos.writeInt(len);
                bos.backToAppendMode();
            }

            bos.moveTo(offMark);
            for (int i=0; i<cmdSz; i++) {
                bos.writeInt(cmdOffsets[i]);
            }
            bos.backToAppendMode();
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
                                                                   ReplicationInputStream ris) {


        ReplicationFrame<K, V> frame = null;
        byte com = (byte) ris.read();
        int seqNo = ris.readInt();

        String srcName = ris.readLengthPrefixedString();
        String tarName = ris.readLengthPrefixedString();

        frame = new ReplicationFrame<K, V>(com, seqNo, srcName);
        frame.setTargetInstanceName(tarName);
        frame.setTargetInstanceName(tarName);

        int numStates = ris.readInt();

        int[] cmdOffsets = new int[numStates];
        for (int i = 0; i < numStates; i++) {
            cmdOffsets[i] = ris.readInt();
        }

        CommandManager<K, V> cm = dsc.getCommandManager();
        for (int i = 0; i < numStates; i++) {
            ris.skipTo(cmdOffsets[i]);
            int len = ris.readInt();
            byte opcode = (byte) ris.read();
            ReplicationInputStream cmdRIS = null;
            try {
                Command<K, V> cmd = cm.createNewInstance(opcode);
                cmdRIS = new ReplicationInputStream(ris.getBuffer(), cmdOffsets[i], len);
                cmd.prepareToExecute(cmdRIS);
                frame.addCommand(cmd);
            } catch (IOException dse) {
                dse.printStackTrace();
            } finally {
                try {cmdRIS.close();} catch (Exception ex) {}
            }
        }

        return frame;
    }

    @Override
    public String toString() {
        return "ReplicationFrame{" +
                "frameCommand=" + frameCommand +
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
