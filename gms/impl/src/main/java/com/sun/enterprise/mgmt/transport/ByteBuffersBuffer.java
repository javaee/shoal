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

package com.sun.enterprise.mgmt.transport;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 *
 * @author Alexey Stashok
 */
public final class ByteBuffersBuffer implements Buffer {
    /**
     * Construct <tt>ByteBuffersBuffer</tt>.
     */
    public ByteBuffersBuffer() {
        this((ByteBuffer[]) null);
    }

    public ByteBuffersBuffer(ByteBuffer[] buffers) {
        set(buffers);
    }

    public ByteBuffersBuffer(ByteBuffersBuffer that) {
        copy(that);
    }
    private ByteOrder byteOrder = ByteOrder.nativeOrder();

    // absolute position
    private int position;

    // absolute limit
    private int limit;

    // absolute capacity
    private int capacity;

    // List of wrapped buffers
    private int[] bufferBounds;
    private ByteBuffer[] buffers;
    private int buffersSize;

    private int lastSegmentIndex;
    private int lowerBound;
    private int upperBound;
    private int activeBufferLowerBound;
    private ByteBuffer activeBuffer;


    private void set(ByteBuffer[] buffers) {
        if (buffers != null || this.buffers == null) {
            initBuffers(buffers, buffersSize);
            calcCapacity();
            this.limit = capacity;
        }
    }

    private void initBuffers(ByteBuffer[] buffers, int bufferSize) {
        this.buffers = buffers != null ? buffers : new ByteBuffer[4];
        this.buffersSize = bufferSize;
        this.bufferBounds = new int[this.buffers.length];
    }

    private ByteBuffersBuffer copy(ByteBuffersBuffer that) {
        initBuffers(Arrays.copyOf(that.buffers, that.buffers.length), that.buffersSize);
        System.arraycopy(that.bufferBounds, 0, this.bufferBounds, 0, that.buffersSize);

        this.position = that.position;
        this.limit = that.limit;
        this.capacity = that.capacity;

        return this;
    }

    @Override
    public void dispose() {
        removeBuffers();
    }

    public ByteBuffersBuffer append(ByteBuffer buffer) {
        ensureBuffersCapacity(1);

        capacity += buffer.remaining();
        bufferBounds[buffersSize] = capacity;
        buffers[buffersSize++] = buffer;

        limit = capacity;
        return this;
    }

    public ByteBuffersBuffer prepend(ByteBuffer buffer) {
        ensureBuffersCapacity(1);
        System.arraycopy(buffers, 0, buffers, 1, buffersSize);
        buffers[0] = buffer;

        buffersSize++;
        calcCapacity();
        limit = capacity;

        resetLastLocation();

        return this;
    }

    private void ensureBuffersCapacity(final int newElementsNum) {
        final int newSize = buffersSize + newElementsNum;

        if (newSize > buffers.length) {
            final int newCapacity = Math.max(newSize, (buffers.length * 3) / 2 + 1);

            buffers = Arrays.copyOf(buffers, newCapacity);
            bufferBounds = Arrays.copyOf(bufferBounds, newCapacity);
        }
    }

    @Override
    public ByteBuffer[] underlying() {
        return buffers;
    }

    @Override
    public int position() {
        return position;
    }

    @Override
    public ByteBuffersBuffer position(int newPosition) {
        if (newPosition > limit)
            throw new IllegalArgumentException("Position exceeds a limit: " + newPosition + ">" + limit);

        position = newPosition;
        return this;
    }

    @Override
    public int limit() {
        return limit;
    }

    @Override
    public ByteBuffersBuffer limit(int newLimit) {
        limit = newLimit;
        if (position > limit) {
            position = limit;
    }

        return this;
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public ByteBuffersBuffer mark() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ByteBuffersBuffer reset() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ByteBuffersBuffer clear() {
        calcCapacity();
        setPosLim(0, capacity);
        return this;
    }

    @Override
    public ByteBuffersBuffer flip() {
        setPosLim(0, position);
        return this;
    }

    @Override
    public ByteBuffersBuffer rewind() {
        setPosLim(0, limit);
	return this;
    }

    @Override
    public int remaining() {
        return limit - position;
    }

    @Override
    public boolean hasRemaining() {
        return limit > position;
    }

    @Override
    public ByteBuffer trimLeft() {
        final ByteBuffer releasedBuffer;
        
        if (position == limit) {
            if (buffersSize > 0) {
                releasedBuffer = buffers[0];
                releasedBuffer.clear();
            } else {
                releasedBuffer = null;
        }

            removeBuffers();

            return releasedBuffer;
        }

        checkIndex(position);
        final int posBufferIndex = lastSegmentIndex;

        int shift = 0;

        for (int i = 0; i < posBufferIndex; i++) {
            final ByteBuffer buffer = buffers[i];
            shift += buffer.remaining();
        }

        setPosLim(position - shift, limit - shift);

        if (posBufferIndex > 0) {
            releasedBuffer = buffers[0];
            
            buffersSize -= posBufferIndex;

            System.arraycopy(buffers, posBufferIndex, buffers, 0, buffersSize);
            Arrays.fill(buffers, buffersSize, buffersSize + posBufferIndex, null);
        } else {
            releasedBuffer = null;
        }

        calcCapacity();
        resetLastLocation();

        return releasedBuffer;
        }

    @Override
    public byte get() {
       return get(position++);
    }

    @Override
    public ByteBuffersBuffer put(byte b) {
        return put(position++, b);
    }

    @Override
    public byte get(final int index) {
        checkIndex(index);

        return activeBuffer.get(toActiveBufferPos(index));

    }

    @Override
    public ByteBuffersBuffer put(int index, byte b) {
        checkIndex(index);

        activeBuffer.put(toActiveBufferPos(index), b);

        return this;
    }


    private void checkIndex(final int index) {
        if (index >= lowerBound & index < upperBound) {
            return;
        }

        recalcIndex(index);
    }

    private void recalcIndex(final int index) {
        final int idx = ArrayUtils.binarySearch(bufferBounds, 0,
                buffersSize - 1, index + 1);
        activeBuffer = buffers[idx];

        upperBound = bufferBounds[idx];
        lowerBound = upperBound - activeBuffer.remaining();
        lastSegmentIndex = idx;

        activeBufferLowerBound = lowerBound - activeBuffer.position();
    }

    private int toActiveBufferPos(final int index) {
        return index - activeBufferLowerBound;
    }

    @Override
    public ByteBuffersBuffer get(final byte[] dst) {
        return get(dst, 0, dst.length);
    }

    @Override
    public ByteBuffersBuffer get(final byte[] dst, int offset, int length) {
        if (length == 0) return this;

        if (remaining() < length) throw new BufferUnderflowException();

        checkIndex(position);

        int bufferIdx = lastSegmentIndex;
        ByteBuffer buffer = activeBuffer;
        int bufferPosition = toActiveBufferPos(position);


        while(true) {
            int oldPos = buffer.position();
            buffer.position(bufferPosition);
            final int bytesToCopy = Math.min(buffer.remaining(), length);
            buffer.get(dst, offset, bytesToCopy);
            buffer.position(oldPos);

            length -= bytesToCopy;
            offset += bytesToCopy;
            position += bytesToCopy;

            if (length == 0) break;

            bufferIdx++;
            buffer = buffers[bufferIdx];
            bufferPosition = buffer.position();
        }

        return this;
    }

    @Override
    public ByteBuffersBuffer put(final byte[] src) {
        return put(src, 0, src.length);
    }

    @Override
    public ByteBuffersBuffer put(final byte[] src, int offset, int length) {
        if (remaining() < length) throw new BufferOverflowException();

        checkIndex(position);

        int bufferIdx = lastSegmentIndex;
        ByteBuffer buffer = activeBuffer;
        int bufferPosition = toActiveBufferPos(position);

        while(true) {
            int oldPos = buffer.position();
            buffer.position(bufferPosition);
            int bytesToCopy = Math.min(buffer.remaining(), length);
            buffer.put(src, offset, bytesToCopy);
            buffer.position(oldPos);
            bufferPosition += (bytesToCopy - 1);

            length -= bytesToCopy;
            offset += bytesToCopy;
            position += bytesToCopy;

            if (length == 0) break;

            bufferIdx++;
            buffer = buffers[bufferIdx];
            bufferPosition = buffer.position();
        }

        return this;
    }

    @Override
    public char getChar() {
        final char value = getChar(position);
        position += 2;

        return value;
    }

    @Override
    public ByteBuffersBuffer putChar(final char value) {
        putChar(position, value);
        position += 2;
        return this;
    }

    @Override
    public char getChar(int index) {
        checkIndex(index);

        if (upperBound - index >= 2) {
            return activeBuffer.getChar(toActiveBufferPos(index));
        } else {
            final int ch1 = activeBuffer.get(toActiveBufferPos(index)) & 0xFF;

            checkIndex(++index);
            final int ch2 = activeBuffer.get(toActiveBufferPos(index)) & 0xFF;

            return (char) ((ch1 << 8) + (ch2));
        }
    }

    @Override
    public ByteBuffersBuffer putChar(int index, final char value) {
        checkIndex(index);

        if (upperBound - index >= 2) {
            activeBuffer.putChar(toActiveBufferPos(index), value);
        } else {
            activeBuffer.put(toActiveBufferPos(index), (byte) (value >>> 8));

            checkIndex(++index);
            activeBuffer.put(toActiveBufferPos(index), (byte) (value & 0xFF));
        }

        return this;
    }

    @Override
    public short getShort() {
        final short value = getShort(position);
        position += 2;

        return value;
    }

    @Override
    public ByteBuffersBuffer putShort(final short value) {
        putShort(position, value);
        position += 2;
        return this;
    }

    @Override
    public short getShort(int index) {
        checkIndex(index);

        if (upperBound - index >= 2) {
            return activeBuffer.getShort(toActiveBufferPos(index));
        } else {
            final int ch1 = activeBuffer.get(toActiveBufferPos(index)) & 0xFF;

            checkIndex(++index);

            final int ch2 = activeBuffer.get(toActiveBufferPos(index)) & 0xFF;

            return (short) ((ch1 << 8) + (ch2));
        }
    }

    @Override
    public ByteBuffersBuffer putShort(int index, final short value) {
        checkIndex(index);

        if (upperBound - index >= 2) {
            activeBuffer.putShort(toActiveBufferPos(index), value);
        } else {
            activeBuffer.put(toActiveBufferPos(index), (byte) (value >>> 8));

            checkIndex(++index);

            activeBuffer.put(toActiveBufferPos(index), (byte) (value & 0xFF));
        }

        return this;
    }

    @Override
    public int getInt() {
        final int value = getInt(position);
        position += 4;

        return value;    }

    @Override
    public ByteBuffersBuffer putInt(final int value) {
        putInt(position, value);
        position += 4;
        return this;
    }

    @Override
    public int getInt(int index) {
        checkIndex(index);

        if (upperBound - index >= 4) {
            return activeBuffer.getInt(toActiveBufferPos(index));
        } else {
            final int ch1 = activeBuffer.get(toActiveBufferPos(index)) & 0xFF;

            checkIndex(++index);
            final int ch2 = activeBuffer.get(toActiveBufferPos(index)) & 0xFF;

            checkIndex(++index);
            final int ch3 = activeBuffer.get(toActiveBufferPos(index)) & 0xFF;

            checkIndex(++index);
            final int ch4 = activeBuffer.get(toActiveBufferPos(index)) & 0xFF;

            return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
        }
    }

    @Override
    public ByteBuffersBuffer putInt(int index, final int value) {
        checkIndex(index);

        if (upperBound - index >= 4) {
            activeBuffer.putInt(toActiveBufferPos(index), value);
        } else {
            activeBuffer.put(toActiveBufferPos(index), (byte) ((value >>> 24) & 0xFF));

            checkIndex(++index);
            activeBuffer.put(toActiveBufferPos(index), (byte) ((value >>> 16) & 0xFF));

            checkIndex(++index);
            activeBuffer.put(toActiveBufferPos(index), (byte) ((value >>> 8) & 0xFF));

            checkIndex(++index);
            activeBuffer.put(toActiveBufferPos(index), (byte) ((value) & 0xFF));
        }

        return this;
    }

    @Override
    public long getLong() {
        final long value = getLong(position);
        position += 8;

        return value;
    }

    @Override
    public ByteBuffersBuffer putLong(final long value) {
        putLong(position, value);
        position += 8;

        return this;
    }

    @Override
    public long getLong(int index) {
        checkIndex(index);

        if (upperBound - index >= 8) {
            return activeBuffer.getLong(toActiveBufferPos(index));
        } else {
            final int ch1 = activeBuffer.get(toActiveBufferPos(index)) & 0xFF;

            checkIndex(++index);
            final int ch2 = activeBuffer.get(toActiveBufferPos(index)) & 0xFF;

            checkIndex(++index);
            final int ch3 = activeBuffer.get(toActiveBufferPos(index)) & 0xFF;

            checkIndex(++index);
            final int ch4 = activeBuffer.get(toActiveBufferPos(index)) & 0xFF;

            checkIndex(++index);
            final int ch5 = activeBuffer.get(toActiveBufferPos(index)) & 0xFF;

            checkIndex(++index);
            final int ch6 = activeBuffer.get(toActiveBufferPos(index)) & 0xFF;

            checkIndex(++index);
            final int ch7 = activeBuffer.get(toActiveBufferPos(index)) & 0xFF;

            checkIndex(++index);
            final int ch8 = activeBuffer.get(toActiveBufferPos(index)) & 0xFF;

            return (((long) ch1 << 56) +
                ((long) ch2 << 48) +
		((long) ch3 << 40) +
                ((long) ch4 << 32) +
                ((long) ch5 << 24) +
                (ch6 << 16) +
                (ch7 <<  8) +
                (ch8));
        }
    }

    @Override
    public ByteBuffersBuffer putLong(int index, final long value) {
        checkIndex(index);

        if (upperBound - index >= 8) {
            activeBuffer.putLong(toActiveBufferPos(index), value);
        } else {
            activeBuffer.put(toActiveBufferPos(index), (byte) ((value >>> 56) & 0xFF));

            checkIndex(++index);
            activeBuffer.put(toActiveBufferPos(index), (byte) ((value >>> 48) & 0xFF));

            checkIndex(++index);
            activeBuffer.put(toActiveBufferPos(index), (byte) ((value >>> 40) & 0xFF));

            checkIndex(++index);
            activeBuffer.put(toActiveBufferPos(index), (byte) ((value >>> 32) & 0xFF));

            checkIndex(++index);
            activeBuffer.put(toActiveBufferPos(index), (byte) ((value >>> 24) & 0xFF));

            checkIndex(++index);
            activeBuffer.put(toActiveBufferPos(index), (byte) ((value >>> 16) & 0xFF));

            checkIndex(++index);
            activeBuffer.put(toActiveBufferPos(index), (byte) ((value >>> 8) & 0xFF));

            checkIndex(++index);
            activeBuffer.put(toActiveBufferPos(index), (byte) ((value) & 0xFF));
        }

        return this;
    }

    @Override
    public float getFloat() {
        return Float.intBitsToFloat(getInt());
    }

    @Override
    public ByteBuffersBuffer putFloat(float value) {
        return putInt(Float.floatToIntBits(value));
    }

    @Override
    public float getFloat(int index) {
        return Float.intBitsToFloat(getInt(index));
    }

    @Override
    public ByteBuffersBuffer putFloat(int index, float value) {
        return putInt(index, Float.floatToIntBits(value));
    }

    @Override
    public double getDouble() {
        return Double.longBitsToDouble(getLong());
    }

    @Override
    public ByteBuffersBuffer putDouble(double value) {
        return putLong(Double.doubleToLongBits(value));
    }

    @Override
    public double getDouble(int index) {
        return Double.longBitsToDouble(getLong(index));
    }

    @Override
    public ByteBuffersBuffer putDouble(int index, double value) {
        return putLong(index, Double.doubleToLongBits(value));
    }

    @Override
    public int compareTo(Buffer that) {
	int n = this.position() + Math.min(this.remaining(), that.remaining());
	for (int i = this.position(), j = that.position(); i < n; i++, j++) {
	    byte v1 = this.get(i);
	    byte v2 = that.get(j);
	    if (v1 == v2)
		continue;
	    if (v1 < v2)
		return -1;
	    return +1;
	}
	return this.remaining() - that.remaining();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("BuffersBuffer (" + System.identityHashCode(this) + ") [");
        sb.append("pos=").append(position);
        sb.append(" lim=").append(limit);
        sb.append(" cap=").append(capacity);
        sb.append(" bufferSize=").append(buffersSize);
        sb.append(" buffers=").append(Arrays.toString(buffers));
        sb.append(']');
        return sb.toString();
    }

    @Override
    public String toStringContent() {
        return toStringContent(null, position, limit);
    }

    @Override
    public String toStringContent(Charset charset) {
        return toStringContent(charset, position, limit);
    }

    @Override
    public String toStringContent(Charset charset, final int position,
            final int limit) {
        if (charset == null) {
            charset = Charset.defaultCharset();
        }

        final byte[] tmpBuffer = new byte[limit - position];

            int oldPosition = this.position;
            int oldLimit = this.limit;

            setPosLim(position, limit);
            get(tmpBuffer);
            setPosLim(oldPosition, oldLimit);
            return new String(tmpBuffer, charset);
        }

    private void removeBuffers() {
        position = 0;
        limit = 0;
        capacity = 0;
        Arrays.fill(buffers, 0, buffersSize, null);

        buffersSize = 0;
        resetLastLocation();
    }

    private void setPosLim(final int position, final int limit) {
        if (position > limit) {
            throw new IllegalArgumentException("Position exceeds a limit: " + position + ">" + limit);
        }

        this.position = position;
        this.limit = limit;
    }

    public void calcCapacity() {
        int currentCapacity = 0;
        for(int i = 0; i < buffersSize; i++) {
            currentCapacity += buffers[i].remaining();
            bufferBounds[i] = currentCapacity;
        }

        capacity = currentCapacity;
    }

    private void resetLastLocation() {
        lowerBound = 0;
        upperBound = 0;
        activeBuffer = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Buffer) {
            Buffer that = (Buffer) obj;
            if (this.remaining() != that.remaining()) {
                return false;
            }
            int p = this.position();
            for (int i = this.limit() - 1, j = that.limit() - 1; i >= p; i--, j--) {
                byte v1 = this.get(i);
                byte v2 = that.get(j);
                if (v1 != v2) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    /**
     * Returns the current hash code of this buffer.
     *
     * <p> The hash code of a byte buffer depends only upon its remaining
     * elements; that is, upon the elements from <tt>position()</tt> up to, and
     * including, the element at <tt>limit()</tt>&nbsp;-&nbsp;<tt>1</tt>.
     *
     * <p> Because buffer hash codes are content-dependent, it is inadvisable
     * to use buffers as keys in hash maps or similar data structures unless it
     * is known that their contents will not change.  </p>
     *
     * @return  The current hash code of this buffer
     */
    @Override
    public int hashCode() {
	int h = 1;
	int p = position();
	for (int i = limit() - 1; i >= p; i--)
	    h = 31 * h + (int)get(i);
	return h;
    }
}


