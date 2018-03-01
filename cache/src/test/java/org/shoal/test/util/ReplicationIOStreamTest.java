/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.shoal.test.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.shoal.ha.cache.impl.util.ReplicationInputStream;
import org.shoal.ha.cache.impl.util.ReplicationOutputStream;

import java.io.IOException;
import java.util.Random;

/**
 * Unit test for simple App.
 */
public class ReplicationIOStreamTest
        extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ReplicationIOStreamTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(ReplicationIOStreamTest.class);
    }

    public void testReplicationTestSetup() {
        assert(true);
    }
    /*
    public void testReplicationBooleanIO() {
        boolean result = false;
        try {
            ReplicationOutputStream ros = new ReplicationOutputStream();
            ros.writeBoolean(true);
            Boolean b = new Boolean(false);
            ros.writeBoolean(b);

            ReplicationInputStream ris = new ReplicationInputStream(ros.toByteArray());
            result = ris.readBoolean() && (! ris.readBoolean());
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        assert(result);
    }



    public void testReplicationIntIO() {
        boolean result = false;
        try {
            ReplicationOutputStream ros = new ReplicationOutputStream();
            ros.writeInt(34);
            ros.writeBoolean(true);
            ros.write(79);
            ros.writeInt(7);
            ReplicationInputStream ris = new ReplicationInputStream(ros.toByteArray());
            result =
                    ris.readInt() == 34 && ris.readBoolean() && ris.read() == 79 && ris.readInt() == 7;
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        assert(result);
    }

    public void testReplicationStringIO() {
        boolean result = false;
        try {
            ReplicationOutputStream ros = new ReplicationOutputStream();
            ros.writeInt(34);
            ros.writeBoolean(true);
            ros.write(79);
            ros.writeInt(7);
            ros.writeLengthPrefixedString(ReplicationIOStreamTest.class.getName());
            ros.writeLong(789);
            ReplicationInputStream ris = new ReplicationInputStream(ros.toByteArray());
            result =
                    ris.readInt() == 34 && ris.readBoolean() && ris.read() == 79
                            && ris.readInt() == 7
                    && (ris.readLengthPrefixedString().equals(ReplicationIOStreamTest.class.getName()))
                    && ris.readLong() == 789;
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        assert(result);
    }

    public void testReplicationByteIO() {

        boolean result = false;
        try {
            ReplicationOutputStream ros = new ReplicationOutputStream();
            ros.writeInt(34);
            ros.writeBoolean(true);
            ros.write(79);
            ros.writeInt(7);
            ros.writeLengthPrefixedString(ReplicationIOStreamTest.class.getName());
            ros.writeLong(789);
            byte[] data = ros.toByteArray();
            ros.writeLengthPrefixedBytes(data);

            ReplicationInputStream ris = new ReplicationInputStream(ros.toByteArray());
            boolean result1 =
                    ris.readInt() == 34 && ris.readBoolean() && ris.read() == 79
                            && ris.readInt() == 7
                    && (ris.readLengthPrefixedString().equals(ReplicationIOStreamTest.class.getName()))
                    && ris.readLong() == 789;
            data = ris.readLengthPrefixedBytes();

            ReplicationInputStream ris2 = new ReplicationInputStream(data);
            boolean result2 =
                    ris2.readInt() == 34 && ris2.readBoolean() && ris2.read() == 79
                            && ris2.readInt() == 7
                    && (ris2.readLengthPrefixedString().equals(ReplicationIOStreamTest.class.getName()))
                    && ris2.readLong() == 789;


            result = result1 && result2;
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        assert(result);
    }

    public void testMarkAndMoveTo() {
        boolean result = false;
        try {
            ReplicationOutputStream ros = new ReplicationOutputStream();
            ros.writeInt(7);
            ros.writeBoolean(true);
            int markPos = ros.mark();
            ros.writeInt(1645);
            ros.write(79);
            ros.writeInt(7);

            ros.moveTo(markPos);
            ros.writeInt(7);

            ros.backToAppendMode();

            ReplicationInputStream ris = new ReplicationInputStream(ros.toByteArray());
            result =
                    ris.readInt() == 7 && ris.readBoolean() && ris.readInt() == 7
                            && ris.read() == 79
                    && ris.readInt() == 7;
            
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        assert(result);
    }

    public void testMark() {
        boolean result = false;
        try {
            ReplicationOutputStream ros = new ReplicationOutputStream();
            ros.writeInt(34);
            ros.writeBoolean(true);
            ros.write(79);
            ros.writeInt(7);

            int lenPos = ros.mark();

            System.out.println("**Marked [1]: " + lenPos);
            ros.writeInt(0);
            byte[] strData = ReplicationIOStreamTest.class.getName().getBytes();
            ros.write(strData);
            ros.writeLong(789);
            System.out.println("[1] Now pos: " + ros.size());

            ros.moveTo(lenPos);
            ros.writeInt(strData.length);

            ros.backToAppendMode();
            System.out.println("[2] Now pos: " + ros.size());

            ros.writeInt(34);
            ros.writeBoolean(true);
            ros.write(79);

            System.out.println("**So far len: " + ros.size() + "; strLen: " + strData.length);

            byte[] data1 = ros.toByteArray();
            ros.writeLengthPrefixedBytes(data1);

            System.out.println("**So far len: " + ros.size());
            
            ReplicationInputStream ris = new ReplicationInputStream(ros.toByteArray());
            boolean result1 =
                    ris.readInt() == 34 && ris.readBoolean() && ris.read() == 79
                            && ris.readInt() == 7
                    && (ris.readLengthPrefixedString().equals(ReplicationIOStreamTest.class.getName()))
                    && ris.readLong() == 789 && ris.readInt() == 34
                    && ris.readBoolean() && ris.read() == 79;
            byte[] data = ris.readLengthPrefixedBytes();

            boolean byteResult = true;
            for (int i=0; i<data.length; i++) {
                if (data1[i] != data[i]) {
                    byteResult = false;
                }
            }

            System.out.println("**Read embedded data: " + data.length + ";   RESULT: " + byteResult);
            ReplicationInputStream ris2 = new ReplicationInputStream(data);
            boolean result2 =
                    ris2.readInt() == 34 && ris2.readBoolean() && ris2.read() == 79
                            && ris2.readInt() == 7;
            result2 = result2 && (ris2.readLengthPrefixedString().equals(ReplicationIOStreamTest.class.getName()));
            result2 = result2 && ris2.readLong() == 789;
            result2 = result2 && ris2.readInt() == 34;
            result2 = result2 && ris2.readBoolean();
            result2 = result2 && ris2.read() == 79;


            result = result1 && result2;
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        assert(result);
    }

    public void testInputStreamMarkAndSkipTo() {
        boolean result = false;
        try {
            ReplicationOutputStream ros = new ReplicationOutputStream();
            int SZ = 7;
            int[] offsets = new int[SZ];
            for (int i=0; i<SZ; i++) {
                ros.writeInt(offsets[i]);
            }

            String[] strData = new String[SZ];
            for (int i=0; i<SZ; i++) {
                offsets[i] = ros.mark();
                ros.writeInt(i);
                int randLen = (new Random()).nextInt(16);
                StringBuilder sbldr = new StringBuilder("My String Data[" + i + "]: ");
                sbldr.append(randLen);
                for (int j=0; j<randLen; j++) {
                    sbldr.append(":").append(j).append(":");
                }
                strData[i] = sbldr.toString();
                ros.writeLengthPrefixedString(strData[i]);
                ros.writeInt(strData[i].length());

                System.out.println("Wrote[" + i + "] : " + strData[i] + " ; offset: " + offsets[i] + "; LENGTH: " + strData[i].length());
            }

            ros.moveTo(0);
            for (int i=0; i<SZ; i++) {
                ros.writeInt(offsets[i]);
            }
            ros.backToAppendMode();
            
            ReplicationInputStream ris = new ReplicationInputStream(ros.toByteArray());
            result = true;
            for (int i=0; i<SZ; i++) {
                System.out.println("** testInputStreamMarkAndSkipTo[" + i + "] Trying to skip to: " + offsets[i]);
                ris.skipTo(offsets[i]);
                int index = ris.readInt();
                String str = ris.readLengthPrefixedString();
                int len = ris.readInt();

                if (index == i && str.equals(strData[i]) && str.length() == len) {
                    System.out.println("Passed[" + i + "]....");
                }  else {
                    result = false;
                    System.out.println("** testInputStreamMarkAndSkipTo FAILED[" + i + "]: "
                        + " index : " + index + "; len: " + len + "; ");
                }
            }
            
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        assert(result);
    }
    */
    
}
