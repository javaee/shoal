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

import com.sun.enterprise.mgmt.transport.Buffer;

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

    // absolute position
    private int position;

    // absolute limit
    private int limit;

    // absolute capacity
    private int capacity;

    // Location of the last <tt>ByteBuffersBuffer</tt> access
    private long lastLocatedInfo = -1;
    private int lastLocatedPosition = -1;

    // List of wrapped buffers
    private ByteBuffer[] buffers;
    private int buffersSize;

    private ByteOrder byteOrder = ByteOrder.nativeOrder();


    private void set(ByteBuffer[] buffers) {
        if (buffers == null) {
            this.buffers = new ByteBuffer[4];
        } else {
            this.buffers = buffers;
            buffersSize = buffers.length;
        }

        capacity = calcCapacity();

        this.limit = capacity;
    }

    private ByteBuffersBuffer copy(ByteBuffersBuffer that) {
        this.buffers = Arrays.copyOf(that.buffers, that.buffers.length);
        this.buffersSize = that.buffersSize;
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

        buffers[buffersSize++] = buffer;
        capacity += buffer.remaining();
        limit = capacity;

        return this;
    }

    public ByteBuffersBuffer prepend(ByteBuffer buffer) {
        ensureBuffersCapacity(1);
        System.arraycopy(buffers, 0, buffers, 1, buffersSize);
        buffers[0] = buffer;

        buffersSize++;
        capacity += buffer.remaining();
        limit = capacity;
        return this;
    }

    private void ensureBuffersCapacity(int newElementsNum) {
        final int newSize = buffersSize + newElementsNum;

        if (newSize > buffers.length) {
            buffers = Arrays.copyOf(buffers,
                    Math.max(newSize, (buffers.length * 3) / 2 + 1));
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
        setPosLim(newPosition, limit);
        return this;
    }

    @Override
    public int limit() {
        return limit;
    }

    @Override
    public ByteBuffersBuffer limit(int newLimit) {
        setPosLim(position, newLimit);
        return this;
    }

    public void recalcCapacity() {
        capacity = calcCapacity();
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
        capacity = calcCapacity();
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
    public boolean disposeUnused() {
        if (position == limit) {
            removeBuffers();
            return true;
        }

        final long posLocation = locateBufferPosition(position);
        final long limitLocation = locateBufferLimit(limit);

        final int posBufferIndex = getBufferIndex(posLocation);
        final int limitBufferIndex = getBufferIndex(limitLocation);

        final int leftTrim = posBufferIndex;
        final int rightTrim = buffersSize - limitBufferIndex - 1;

        if (leftTrim == 0 && rightTrim == 0) {
            return false;
        }

        for(int i=0; i<leftTrim; i++) {
            final ByteBuffer buffer = buffers[i];
            final int bufferSize = buffer.remaining();
            setPosLim(position - bufferSize, limit - bufferSize);
            capacity -= bufferSize;
        }

        for(int i=0; i<rightTrim; i++) {
            final int idx = buffersSize - i - 1;
            final ByteBuffer buffer = buffers[idx];
            buffers[idx] = null;
            final int bufferSize = buffer.remaining();
            capacity -= bufferSize;
        }

        buffersSize -= (leftTrim + rightTrim);
        resetLastLocation();

        if (leftTrim > 0) {
            System.arraycopy(buffers, leftTrim, buffers, 0, buffersSize);
            Arrays.fill(buffers, buffersSize, buffersSize + leftTrim, null);
        }

        return false;
    }

    @Override
    public byte get() {
        long location = locateBufferPosition(position++);
        return bufferGet(location);
    }

    @Override
    public ByteBuffersBuffer put(byte b) {
        long location = locateBufferPosition(position++);
        return bufferPut(location, b);
    }

    @Override
    public byte get(int index) {
        long location = locateBufferPosition(index);
        return bufferGet(location);
    }

    @Override
    public ByteBuffersBuffer put(int index, byte b) {
        long location = locateBufferPosition(index);
        return bufferPut(location, b);
    }

    @Override
    public ByteBuffersBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }

    @Override
    public ByteBuffersBuffer get(byte[] dst, int offset, int length) {
        if (length == 0) return this;

        if (remaining() < length) throw new BufferUnderflowException();

        final long location = locateBufferPosition(position);
        int bufferIdx = getBufferIndex(location);
        ByteBuffer buffer = buffers[bufferIdx];
        int bufferPosition = getBufferPosition(location);

        while(true) {
            int oldPos = buffer.position();
            buffer.position(bufferPosition);
            int bytesToCopy = Math.min(buffer.remaining(), length);
            BufferUtils.get(buffer, dst, offset, bytesToCopy);
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
    public ByteBuffersBuffer put(byte[] src) {
        return put(src, 0, src.length);
    }

    @Override
    public ByteBuffersBuffer put(byte[] src, int offset, int length) {
        if (remaining() < length) throw new BufferOverflowException();

        final long location = locateBufferPosition(position);
        int bufferIdx = getBufferIndex(location);
        ByteBuffer buffer = buffers[bufferIdx];
        int bufferPosition = getBufferPosition(location);

        while(true) {
            int oldPos = buffer.position();
            buffer.position(bufferPosition);
            int bytesToCopy = Math.min(buffer.remaining(), length);
            BufferUtils.put(src, offset, bytesToCopy, buffer);
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
        int ch1 = get() & 0xFF;
        int ch2 = get() & 0xFF;

        return (char) ((ch1 << 8) + (ch2 << 0));
    }

    @Override
    public ByteBuffersBuffer putChar(char value) {
        put((byte) (value >>> 8));
        put((byte) (value & 0xFF));

        return this;
    }

    @Override
    public char getChar(int index) {
        long location = locateBufferPosition(index);
        final int bufferIndex = getBufferIndex(location);
        final int bufferPosition = getBufferPosition(location);
        final ByteBuffer buffer = buffers[bufferIndex];

        if (buffer.limit() - bufferPosition >= 2) {
            return buffer.getChar(bufferPosition);
        } else {
            int ch1 = buffer.get(bufferPosition) & 0xFF;

            location = incLocation(location);
            int ch2 = bufferGet(location) & 0xFF;

            return (char) ((ch1 << 8) + (ch2 << 0));
        }
    }

    @Override
    public ByteBuffersBuffer putChar(int index, char value) {
        long location = locateBufferPosition(index);
        final int bufferIndex = getBufferIndex(location);
        final int bufferPosition = getBufferPosition(location);
        final ByteBuffer buffer = buffers[bufferIndex];

        if (buffer.limit() - bufferPosition >= 2) {
            buffer.putChar(bufferPosition, value);
        } else {
            buffer.put(bufferPosition, (byte) (value >>> 8));

            location = incLocation(location);
            bufferPut(location, (byte) (value & 0xFF));
        }

        return this;
    }

    @Override
    public short getShort() {
        final byte v1 = get();
        final byte v2 = get();

        final short shortValue = (short) (((v1 & 0xFF) << 8) | (v2 & 0xFF));
        return shortValue;
    }

    @Override
    public ByteBuffersBuffer putShort(short value) {
        put((byte) (value >>> 8));
        put((byte) (value & 0xFF));

        return this;
    }

    @Override
    public short getShort(int index) {
        long location = locateBufferPosition(index);
        final int bufferIndex = getBufferIndex(location);
        final int bufferPosition = getBufferPosition(location);
        final ByteBuffer buffer = buffers[bufferIndex];

        if (buffer.limit() - bufferPosition >= 2) {
            return buffer.getShort(bufferPosition);
        } else {
            int ch1 = buffer.get(bufferPosition) & 0xFF;

            location = incLocation(location);
            int ch2 = bufferGet(location) & 0xFF;

            return (short) ((ch1 << 8) + (ch2 << 0));
        }
    }

    @Override
    public ByteBuffersBuffer putShort(int index, short value) {
        long location = locateBufferPosition(index);
        final int bufferIndex = getBufferIndex(location);
        final int bufferPosition = getBufferPosition(location);
        final ByteBuffer buffer = buffers[bufferIndex];

        if (buffer.limit() - bufferPosition >= 2) {
            buffer.putShort(bufferPosition, value);
        } else {
            buffer.put(bufferPosition, (byte) (value >>> 8));

            location = incLocation(location);
            bufferPut(location, (byte) (value & 0xFF));
        }

        return this;
    }

    @Override
    public int getInt() {
        final short v1 = getShort();
        final short v2 = getShort();

        final int intValue = ((v1 & 0xFFFF) << 16) | (v2 & 0xFFFF);
        return intValue;
    }

    @Override
    public ByteBuffersBuffer putInt(int value) {
        put((byte) ((value >>> 24) & 0xFF));
        put((byte) ((value >>> 16) & 0xFF));
        put((byte) ((value >>>  8) & 0xFF));
        put((byte) ((value >>>  0) & 0xFF));

        return this;
    }

    @Override
    public int getInt(int index) {
        long location = locateBufferPosition(index);
        final int bufferIndex = getBufferIndex(location);
        final int bufferPosition = getBufferPosition(location);
        final ByteBuffer buffer = buffers[bufferIndex];

        if (buffer.limit() - bufferPosition >= 4) {
            return buffer.getInt(bufferPosition);
        } else {
            int ch1 = buffer.get(bufferPosition) & 0xFF;

            location = incLocation(location);
            int ch2 = bufferGet(location) & 0xFF;

            location = incLocation(location);
            int ch3 = bufferGet(location) & 0xFF;

            location = incLocation(location);
            int ch4 = bufferGet(location) & 0xFF;

            return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
        }
    }

    @Override
    public ByteBuffersBuffer putInt(int index, int value) {
        long location = locateBufferPosition(index);
        final int bufferIndex = getBufferIndex(location);
        final int bufferPosition = getBufferPosition(location);
        final ByteBuffer buffer = buffers[bufferIndex];

        if (buffer.limit() - bufferPosition >= 4) {
            buffer.putInt(bufferPosition, value);
        } else {
            buffer.put(bufferPosition, (byte) ((value >>> 24) & 0xFF));

            location = incLocation(location);
            bufferPut(location, (byte) ((value >>> 16) & 0xFF));

            location = incLocation(location);
            bufferPut(location, (byte) ((value >>> 8) & 0xFF));

            location = incLocation(location);
            bufferPut(location, (byte) ((value >>> 0) & 0xFF));
        }

        return this;
    }

    @Override
    public long getLong() {
        final int v1 = getInt();
        final int v2 = getInt();

        final long longValue = ((v1 & 0xFFFFFFFFL) << 32) | (v2 & 0xFFFFFFFFL);
        return longValue;
    }

    @Override
    public ByteBuffersBuffer putLong(long value) {
        put((byte) ((value >>> 56) & 0xFF));
        put((byte) ((value >>> 48) & 0xFF));
        put((byte) ((value >>> 40) & 0xFF));
        put((byte) ((value >>> 32) & 0xFF));
        put((byte) ((value >>> 24) & 0xFF));
        put((byte) ((value >>> 16) & 0xFF));
        put((byte) ((value >>> 8) & 0xFF));
        put((byte) ((value >>> 0) & 0xFF));

        return this;
    }

    @Override
    public long getLong(int index) {
        long location = locateBufferPosition(index);
        final int bufferIndex = getBufferIndex(location);
        final int bufferPosition = getBufferPosition(location);
        final ByteBuffer buffer = buffers[bufferIndex];

        if (buffer.limit() - bufferPosition >= 8) {
            return buffer.getLong(bufferPosition);
        } else {
            int ch1 = buffer.get(bufferPosition);

            location = incLocation(location);
            int ch2 = bufferGet(location) & 0xFF;

            location = incLocation(location);
            int ch3 = bufferGet(location) & 0xFF;

            location = incLocation(location);
            int ch4 = bufferGet(location) & 0xFF;

            location = incLocation(location);
            int ch5 = bufferGet(location) & 0xFF;

            location = incLocation(location);
            int ch6 = bufferGet(location) & 0xFF;

            location = incLocation(location);
            int ch7 = bufferGet(location) & 0xFF;

            location = incLocation(location);
            int ch8 = bufferGet(location) & 0xFF;

            return (((long) ch1 << 56) +
                ((long) ch2 << 48) +
		((long) ch3 << 40) +
                ((long) ch4 << 32) +
                ((long) ch5 << 24) +
                (ch6 << 16) +
                (ch7 <<  8) +
                (ch8 <<  0));
        }
    }

    @Override
    public ByteBuffersBuffer putLong(int index, long value) {
        long location = locateBufferPosition(index);
        final int bufferIndex = getBufferIndex(location);
        final int bufferPosition = getBufferPosition(location);
        final ByteBuffer buffer = buffers[bufferIndex];

        if (buffer.limit() - bufferPosition >= 8) {
            buffer.putLong(bufferPosition, value);
        } else {
            buffer.put(bufferPosition, (byte) ((value >>> 56) & 0xFF));

            location = incLocation(location);
            bufferPut(location, (byte) ((value >>> 48) & 0xFF));

            location = incLocation(location);
            bufferPut(location, (byte) ((value >>> 40) & 0xFF));

            location = incLocation(location);
            bufferPut(location, (byte) ((value >>> 32) & 0xFF));

            location = incLocation(location);
            bufferPut(location, (byte) ((value >>> 24) & 0xFF));

            location = incLocation(location);
            bufferPut(location, (byte) ((value >>> 16) & 0xFF));

            location = incLocation(location);
            bufferPut(location, (byte) ((value >>> 8) & 0xFF));

            location = incLocation(location);
            bufferPut(location, (byte) ((value >>> 0) & 0xFF));
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
	    if ((v1 != v1) && (v2 != v2)) 	// For float and double
		continue;
	    if (v1 < v2)
		return -1;
	    return +1;
	}
	return this.remaining() - that.remaining();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ByteBuffersBuffer (" + System.identityHashCode(this) + ") [");
        sb.append("pos=").append(position);
        sb.append(" lim=").append(limit);
        sb.append(" cap=").append(capacity);
        sb.append(" bufferSize=").append(buffersSize);
        sb.append(" buffers=" + Arrays.toString(buffers));
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
    public String toStringContent(Charset charset, int position, int limit) {
        if (charset == null) {
            charset = Charset.defaultCharset();
        }

        final long posLocation = locateBufferPosition(position);
        final long limLocation = locateBufferLimit(limit);

        final ByteBuffer posBuffer = buffers[getBufferIndex(posLocation)];
        final ByteBuffer limBuffer = buffers[getBufferIndex(limLocation)];

        if (posBuffer == limBuffer) {
            return BufferUtils.toStringContent(posBuffer, charset,
                    getBufferPosition(posLocation),
                    getBufferPosition(limLocation));
        } else {
            byte[] tmpBuffer = new byte[limit - position];

            int oldPosition = this.position;
            int oldLimit = this.limit;

            setPosLim(position, limit);
            get(tmpBuffer);
            setPosLim(oldPosition, oldLimit);
            return new String(tmpBuffer, charset);
        }
    }

    private void removeBuffers() {
        position = 0;
        limit = 0;
        capacity = 0;
        buffersSize = 0;
        Arrays.fill(buffers, null);
        resetLastLocation();
    }

    /**
     * Locates the internal buffer index and the internal buffer position, which
     * corresponds to this {@link CompositeBuffer}'s position.
     *
     * @param position this {@link CompositeBuffer} position.
     * @return long value, which contains  the internal buffer index and the
     * internal buffer position, which corresponds to this {@link CompositeBuffer}'s position.
     * This value is packed following way: ((internalBufferIndex << 32) | internalBufferPosition)
     */
    private long locateBufferPosition(int position) {
        if (buffersSize == 0) return -1;

        if (lastLocatedPosition != -1) {
            int diff = position - lastLocatedPosition;

            if (diff > 0) {
                lastLocatedInfo = moveForward(lastLocatedInfo, diff);
                lastLocatedPosition = position;
            } else if (diff < 0) {
                lastLocatedInfo = moveBack(lastLocatedInfo, -diff);
                lastLocatedPosition = position;
            }

            return lastLocatedInfo;
        }

        lastLocatedInfo = moveForward(buffers[0].position(), position);
        lastLocatedPosition = position;

        return lastLocatedInfo;
    }

    private long moveForward(long currentLocation, int steps) {
        int bufferIdx = getBufferIndex(currentLocation);
        int bufferPosition = getBufferPosition(currentLocation);

        ByteBuffer buffer = buffers[bufferIdx];
        if (bufferPosition + steps < buffer.limit()) {
            return makeLocation(bufferIdx, bufferPosition + steps);
        }

        steps -= (buffer.limit() - bufferPosition);
        bufferIdx++;

        for (int i = bufferIdx; i < buffersSize; i++) {
            buffer = buffers[i];

            if (steps < buffer.remaining()) {
                return makeLocation(i, buffer.position() + steps);
            }

            steps -= buffer.remaining();
        }

        if (steps == 0) {
            return makeLocation(buffersSize - 1, buffers[buffersSize - 1].limit());
        }

        throw new IndexOutOfBoundsException("Position " + position + " is out of bounds");
    }

    private long moveBack(long currentLocation, int steps) {
        int bufferIdx = getBufferIndex(currentLocation);
        int bufferPosition = getBufferPosition(currentLocation);

        ByteBuffer buffer = buffers[bufferIdx];
        if (bufferPosition - steps >= buffer.position()) {
            return makeLocation(bufferIdx, bufferPosition - steps);
        }

        steps -= (bufferPosition - buffer.position());
        bufferIdx--;

        for (int i = bufferIdx; i >= 0; i--) {
            buffer = buffers[i];

            if (steps <= buffer.remaining()) {
                return makeLocation(i, buffer.limit() - steps);
            }

            steps -= buffer.remaining();
        }

        throw new IndexOutOfBoundsException("Position " + position + " is out of bounds");
    }

    /**
     * Locates the internal buffer index and the internal buffer limit, which
     * corresponds to this {@link CompositeBuffer}'s limit.
     *
     * @param limit this {@link CompositeBuffer} limit.
     * @return long value, which contains  the internal buffer index and the
     * internal buffer limit, which corresponds to this {@link CompositeBuffer}'s limit.
     * This value is packed following way: ((internalBufferIndex << 32) | internalBufferLimit)
     */
    public long locateBufferLimit(int limit) {
        if (buffersSize == 0) return -1;

        ByteBuffer buffer = buffers[0];
        int currentOffset = buffer.remaining();
        if (limit <= currentOffset) {
            return limit + buffer.position();
        }

        for (int i = 1; i < buffersSize; i++) {
            buffer = buffers[i];

            final int newOffset = currentOffset + buffer.remaining();
            if (limit <= newOffset) {
                return makeLocation(i, buffer.position() + limit - currentOffset);
            }

            currentOffset = newOffset;
        }

        throw new IndexOutOfBoundsException("Limit " + limit + " is out of bounds");
    }

    private long incLocation(final long location) {
        int bufferIndex = getBufferIndex(location);
        int bufferPosition = getBufferPosition(location);
        ByteBuffer buffer = buffers[bufferIndex];

        if (bufferPosition + 1 < buffer.limit()) {
            return location + 1;
        }

        for (int i = bufferIndex + 1; i < buffersSize; i++) {
            buffer = buffers[bufferIndex];
            if (buffer.hasRemaining()) {
                return makeLocation(i, buffer.position());
            }
        }

        throw new IndexOutOfBoundsException();
    }

    private byte bufferGet(final long location) {
        final int bufferIndex = getBufferIndex(location);
        final int bufferPosition = getBufferPosition(location);
        final ByteBuffer buffer = buffers[bufferIndex];

        return buffer.get(bufferPosition);
    }

    private ByteBuffersBuffer bufferPut(final long location, final byte value) {
        final int bufferIndex = getBufferIndex(location);
        final int bufferPosition = getBufferPosition(location);
        final ByteBuffer buffer = buffers[bufferIndex];

        buffer.put(bufferPosition, value);
        return this;
    }

    private static int getBufferIndex(long bufferLocation) {
        return (int) (bufferLocation >>> 32);
    }

    private static int getBufferPosition(long bufferLocation) {
        return (int) (bufferLocation & 0xFFFFFFFF);
    }

    private static long makeLocation(final int bufferIndex,
            final int bufferPosition) {
        return (long) (((long) bufferIndex) << 32) | (long) bufferPosition;
    }

    private void setPosLim(int position, int limit) {
        if (position > limit) {
            throw new IllegalArgumentException("Position exceeds a limit: " + position + ">" + limit);
        }

        this.position = position;
        this.limit = limit;
    }

    private int calcCapacity() {
        int currentCapacity = 0;
        for(int i=0; i<buffersSize; i++) {
            currentCapacity += buffers[i].remaining();
        }

        return currentCapacity;
    }

    private void resetLastLocation() {
        lastLocatedPosition = -1;
        lastLocatedInfo = -1;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ByteBuffersBuffer) {
            ByteBuffersBuffer that = (ByteBuffersBuffer) obj;
            if (this.remaining() != that.remaining()) {
                return false;
            }
            int p = this.position();
            for (int i = this.limit() - 1, j = that.limit() - 1; i >= p; i--, j--) {
                byte v1 = this.get(i);
                byte v2 = that.get(j);
                if (v1 != v2) {
                    if ((v1 != v1) && (v2 != v2)) // For float and double
                    {
                        continue;
                    }
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


