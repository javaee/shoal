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

import java.io.ByteArrayInputStream;

/**
 * @author Mahesh Kannan
 */
public class ReplicationInputStream
    extends ByteArrayInputStream {

    private int minPos = 0;

    private int maxPos = -1;

    public ReplicationInputStream(byte[] data) {
        super(data);
        maxPos = data.length - 1;
    }

    public ReplicationInputStream(byte[] data, int offset, int len) {
        super(data, offset, len);
        minPos = offset;
        maxPos = offset + len - 1;
    }

    public int mark() {
        super.mark(0);
        return super.pos;
    }

    public void skipTo(int index) {
        if (index < minPos || index > maxPos) {
            throw new IllegalArgumentException("Illegal position (" + index + "). Valid values are from "
                + minPos + " to " + maxPos);
        }
        super.pos = index;
    }

    public final int readInt() {
        //TODO Check bounds
        int val = Utility.bytesToInt(buf, pos);
        pos += 4;
        return val;
    }

    public final long readLong() {
        //TODO Check bounds
        return ((long) readInt() << 32) | ((long) readInt() & 0xFFFFFFFFL);
    }

    public final String readLengthPrefixedString() {
        String str = null;
        int len = readInt();
        if (len > 0) {
            str = new String(buf, pos, len);
            pos += len;
        }

        return str;
    }

    public final byte[] readLengthPrefixedBytes() {
        byte[] data = null;
        int len = readInt();
        if (len > 0) {
            data = new byte[len];
            System.arraycopy(buf, pos, data, 0, len);
            pos += len;
        }

        return data;
    }

    public boolean readBoolean() {
        return buf[pos++] == 1; 
    }

    public byte[] getBuffer() {
        return buf;
    }

}
