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

package com.sun.enterprise.mgmt.transport.buffers;

import java.io.UnsupportedEncodingException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 *
 * @author Alexey Stashok
 */
public class BufferUtils {

    public static final ByteBuffer EMPTY_BYTE_BUFFER = ByteBuffer.allocate(0);
    public static final ByteBuffer[] EMPTY_BYTE_BUFFER_ARRAY = new ByteBuffer[0];

    /**
     * Slice {@link ByteBuffer} of required size from big chunk.
     * Passed chunk position will be changed, after the slicing (chunk.position += size).
     *
     * @param chunk big {@link ByteBuffer} pool.
     * @param size required slice size.
     *
     * @return sliced {@link ByteBuffer} of required size.
     */
    public static ByteBuffer slice(ByteBuffer chunk, int size) {
        chunk.limit(chunk.position() + size);
        ByteBuffer view = chunk.slice();
        chunk.position(chunk.limit());
        chunk.limit(chunk.capacity());

        return view;
    }


    /**
     * Get the {@link ByteBuffer}'s slice basing on its passed position and limit.
     * Position and limit values of the passed {@link ByteBuffer} won't be changed.
     * The result {@link ByteBuffer} position will be equal to 0, and limit
     * equal to number of sliced bytes (limit - position).
     *
     * @param byteBuffer {@link ByteBuffer} to slice/
     * @param position the position in the passed byteBuffer, the slice will start from.
     * @param limit the limit in the passed byteBuffer, the slice will be ended.
     *
     * @return sliced {@link ByteBuffer} of required size.
     */
    public static ByteBuffer slice(final ByteBuffer byteBuffer,
            final int position, final int limit) {
        final int oldPos = byteBuffer.position();
        final int oldLimit = byteBuffer.limit();

        setPositionLimit(byteBuffer, position, limit);

        final ByteBuffer slice = byteBuffer.slice();

        setPositionLimit(byteBuffer, oldPos, oldLimit);

        return slice;
    }

    public static String toStringContent(ByteBuffer byteBuffer, Charset charset,
            int position, int limit) {

        if (charset == null) {
            charset = Charset.defaultCharset();
        }

        if (byteBuffer.hasArray()) {
            try {
                return new String(byteBuffer.array(),
                        position + byteBuffer.arrayOffset(),
                        limit - position, charset.name());
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
//            Uncomment, when StringDecoder will not create copy of byte[]
//            return new String(byteBuffer.array(),
//                    position + byteBuffer.arrayOffset(),
//                    limit - position, charset);
        } else {
            int oldPosition = byteBuffer.position();
            int oldLimit = byteBuffer.limit();
            setPositionLimit(byteBuffer, position, limit);

            byte[] tmpBuffer = new byte[limit - position];
            byteBuffer.get(tmpBuffer);

            setPositionLimit(byteBuffer, oldPosition, oldLimit);

            try {
                return new String(tmpBuffer, charset.name());
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }

//            Uncomment, when StringDecoder will not create copy of byte[]
//            return new String(tmpBuffer, charset);
        }
    }

    public static void setPositionLimit(final ByteBuffer buffer, final int position, final int limit) {
            buffer.limit(limit);
            buffer.position(position);
        }

    public static void setPositionLimit(final Buffer buffer, final int position, final int limit) {
            buffer.limit(limit);
            buffer.position(position);
        }

    public static void put(ByteBuffer srcBuffer, int srcOffset, int length,
            ByteBuffer dstBuffer) {

        if (dstBuffer.remaining() < length) {
            throw new BufferOverflowException();
        }

        if (srcBuffer.hasArray() && dstBuffer.hasArray()) {

            System.arraycopy(srcBuffer.array(),
                    srcBuffer.arrayOffset() + srcOffset,
                    dstBuffer.array(),
                    dstBuffer.arrayOffset() + dstBuffer.position(),
                    length);
            dstBuffer.position(dstBuffer.position() + length);
        } else {
            int oldPos = srcBuffer.position();
            int oldLim = srcBuffer.limit();
            setPositionLimit(srcBuffer, srcOffset, srcOffset + length);

            dstBuffer.put(srcBuffer);

            srcBuffer.position(oldPos);
            srcBuffer.limit(oldLim);
        }
    }
   
    public static void get(ByteBuffer srcBuffer,
            byte[] dstBytes, int dstOffset, int length) {

        if (srcBuffer.hasArray()) {
            if (length > srcBuffer.remaining()) {
                throw new BufferUnderflowException();
            }

            System.arraycopy(srcBuffer.array(),
                    srcBuffer.arrayOffset() + srcBuffer.position(),
                    dstBytes, dstOffset, length);
            srcBuffer.position(srcBuffer.position() + length);
        } else {
            srcBuffer.get(dstBytes, dstOffset, length);
        }
    }

    public static void put(byte[] srcBytes, int srcOffset, int length,
            ByteBuffer dstBuffer) {
        if (dstBuffer.hasArray()) {
            if (length > dstBuffer.remaining()) {
                throw new BufferOverflowException();
            }

            System.arraycopy(srcBytes, srcOffset, dstBuffer.array(),
                    dstBuffer.arrayOffset() + dstBuffer.position(), length);
            dstBuffer.position(dstBuffer.position() + length);
        } else {
            dstBuffer.put(srcBytes, srcOffset, length);
        }
    }
}
