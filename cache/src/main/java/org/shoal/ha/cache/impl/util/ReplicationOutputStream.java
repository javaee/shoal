/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author Mahesh Kannan
 */
public class ReplicationOutputStream
    extends ByteArrayOutputStream {

    public ReplicationOutputStream() {
        super();
    }

    private int maxCount;

    public int mark() {
        return size();
    }

    public void reWrite(int mark, byte[] data) {
        System.arraycopy(data, 0, buf, mark, data.length);
    }
    
    public void writeInt(int value)
        throws IOException {
        write(Utility.intToBytes(value));
    }
    
    public void writeLong(long value)
        throws IOException {
        write(Utility.longToBytes(value));
    }

    public void writeLengthPrefixedString(String str)
        throws IOException {
        if (str == null) {
            writeInt(0);
        } else {
            byte[] data = str.getBytes(Charset.defaultCharset());
            writeInt(data.length);
            write(data);
        }
    }

    public void writeLengthPrefixedBytes(byte[] data)
        throws IOException {
        if (data == null) {
            writeInt(0);
        } else {
            writeInt(data.length);
            write(data);
        }
    }

    public void writeBoolean(boolean b)
        throws IOException {
        write(b ? 1 : 0); //Writes one byte
    }

    public int moveTo(int pos) {
        if (count > maxCount) {
            maxCount = count;
        }

        int oldPos = count;
        if (pos >= 0 && pos <= count) {
            count = pos;
        }

        return oldPos;
    }

    /**
     * Note: This must be used only after a call to moveTo
     */
    public void backToAppendMode() {
        if (count < maxCount) {
            count = maxCount;
        }
    }

    public byte[] toByteArray() {
        backToAppendMode();
        return super.toByteArray();
    }
    
}
