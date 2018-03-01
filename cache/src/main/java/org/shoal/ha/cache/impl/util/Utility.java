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

package org.shoal.ha.cache.impl.util;

/**
 * @author V2.x
 *
 * Handy class full of static functions.
 */
public final class Utility {

    /**
     * Unmarshal a byte array to an integer.
     * Assume the bytes are in BIGENDIAN order.
     * i.e. array[offset] is the most-significant-byte
     * and  array[offset+3] is the least-significant-byte.
     *
     * @param array  The array of bytes.
     * @param offset The offset from which to start unmarshalling.
     */
    public static final int bytesToInt(byte[] array, int offset) {
        int b1, b2, b3, b4;

        return (array[offset] << 24) & 0xFF000000
                |(array[offset+1] << 16) & 0x00FF0000
                | (array[offset+2] << 8) & 0x0000FF00
                | (array[offset+3] << 0) & 0x000000FF;
    }

    /**
     * Marshal an integer to a byte array.
     * The bytes are in BIGENDIAN order.
     * i.e. array[offset] is the most-significant-byte
     * and  array[offset+3] is the least-significant-byte.
     */
    public static final byte[] intToBytes(int value) {
        byte[] data = new byte[4];
        intToBytes(value, data, 0);

        return data;
    }

    public static final void intToBytes(int value, byte[] array, int offset) {
        array[offset] = (byte) ((value >>> 24) & 0xFF);
        array[offset+1] = (byte) ((value >>> 16) & 0xFF);
        array[offset+2] = (byte) ((value >>> 8) & 0xFF);
        array[offset+3] = (byte) ((value >>> 0) & 0xFF);
    }

    /**
     * Unmarshal a byte array to an long.
     * Assume the bytes are in BIGENDIAN order.
     * i.e. array[offset] is the most-significant-byte
     * and  array[offset+7] is the least-significant-byte.
     */
    public static final byte[] longToBytes(long value) {
        byte[] data = new byte[8];
        longToBytes(value, data, 0);

        return data;
    }

    public static final long bytesToLong(byte[] array, int offset) {
        long l1, l2;

        return  ((long) bytesToInt(array, offset) << 32)
            | ((long) bytesToInt(array, offset + 4) & 0xFFFFFFFFL);

    }

    /**
     * Marshal an long to a byte array.
     * The bytes are in BIGENDIAN order.
     * i.e. array[offset] is the most-significant-byte
     * and  array[offset+7] is the least-significant-byte.
     *
     * @param array  The array of bytes.
     * @param offset The offset from which to start marshalling.
     */
    public static final void longToBytes(long value, byte[] array, int offset) {
        array[offset] = (byte) ((value >>> 56) & 0xFF);
        array[offset+1] = (byte) ((value >>> 48) & 0xFF);
        array[offset+2] = (byte) ((value >>> 40) & 0xFF);
        array[offset+3] = (byte) ((value >>> 32) & 0xFF);
        array[offset+4] = (byte) ((value >>> 24) & 0xFF);
        array[offset+5] = (byte) ((value >>> 16) & 0xFF);
        array[offset+6] = (byte) ((value >>> 8) & 0xFF);
        array[offset+7] = (byte) ((value >>> 0) & 0xFF);
    }


}
