/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.enterprise.mgmt.transport;

import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.DataOutputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is a default {@link Message}'s implementation
 *
 * The byte array or ByteBuffer which represent this message's low level data will be cached if this message is not modified
 * Here are this message's structure
 * ----
 * [packet]
 * magic(4) + version(4) + type(4) + messages_length(4) + messages(message_length)
 * [messages]
 * message_count(4) + message_key1 + message_value1 + message_key2 + message_value2 + ...(message_count)
 * ----
 *
 * @author Bongjae Chang
 */
public class MessageImpl implements Message {

    static final long serialVersionUID = -3617083350698668655L;

    private static final Logger LOG = GMSLogDomain.getLogger( GMSLogDomain.GMS_LOGGER );

    public static final int DEFAULT_MAX_TOTAL_MESSAGE_LENGTH = 128 * 1024 + (2 * 1024);
    private static int maxTotalMessageLength = DEFAULT_MAX_TOTAL_MESSAGE_LENGTH;
    public static final int UNSPECIFIED_MESSAGE_LENGTH = -1;

    private static final int MAGIC_NUMBER = 770303;
    private static final int VERSION = 1;

    private static final int MAGIC_NUMBER_LENGTH = 4;
    private static final int VERSION_LENGTH = 4;
    private static final int TYPE_LENGTH = 4;
    private static final int MESSAGE_LENGTH = 4;
    public static final int HEADER_LENGTH = MAGIC_NUMBER_LENGTH + VERSION_LENGTH + TYPE_LENGTH + MESSAGE_LENGTH;

    private volatile int version;
    private volatile int type;

    private final Map<String, Serializable> messages = new HashMap<String, Serializable>();
    private final ReentrantLock messageLock = new ReentrantLock();
    private transient ByteBuffer byteBuffer;
    private boolean modified;

    public static int getMaxMessageLength() {
        return maxTotalMessageLength;
    }

    public static void setMaxMessageLength(int maxMsgLength) {
        maxTotalMessageLength = maxMsgLength;
    }

    public MessageImpl() {
    }

    public MessageImpl( final int type ) {
        initialize( type, null );
    }

    public MessageImpl( final int type, final Map<String, Serializable> messages ) {
        initialize( type, messages );
    }

    /**
     * {@inheritDoc}
     */
    public void initialize( final int type, final Map<String, Serializable> messages ) throws IllegalArgumentException {
        this.version = VERSION;
        // Let's allow unknown message types
        /*
        switch( type ) {
            case TYPE_CLUSTER_MANAGER_MESSAGE:
            case TYPE_HEALTH_MONITOR_MESSAGE:
            case TYPE_MASTER_NODE_MESSAGE:
                break;
            default:
                throw new IllegalArgumentException( "type is not valid" );
        }
        */
        this.type = type;
        if( messages != null ) {
            messageLock.lock();
            try {
                this.messages.clear();
                this.messages.putAll( messages );
            } finally {
                modified = true;
                messageLock.unlock();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public int parseHeader( final byte[] bytes, final int offset ) throws IllegalArgumentException {
        if( bytes == null )
            throw new IllegalArgumentException( "bytes must be initialized" );
        if( offset < 0 )
            throw new IllegalArgumentException( "offset is too small" );
        if( bytes.length < offset + HEADER_LENGTH )
            throw new IllegalArgumentException( "bytes' length is too small" );

        int messageLen;
        if (bytes.length - offset < HEADER_LENGTH) {
            throw new IllegalArgumentException("byte[] is too small");
    }

        int magicNumber = readInt(bytes, offset);
        if (magicNumber != MAGIC_NUMBER) {
            throw new IllegalArgumentException("magic number is not valid");
        }
        version = readInt(bytes, offset + 4);
        type = readInt(bytes, offset + 8);
        messageLen = readInt(bytes, offset + 12);
        return messageLen;
    }

    /**
     * {@inheritDoc}
     */
    public int parseHeader( final Buffer buffer, final int offset ) throws IllegalArgumentException {
        if( buffer == null )
            throw new IllegalArgumentException( "byte buffer must be initialized" );
        if( offset < 0 )
            throw new IllegalArgumentException( "offset is too small" );
        int messageLen;
        int restorePosition = buffer.position();
        try {
            buffer.position( offset );
            if( buffer.remaining() < HEADER_LENGTH )
                throw new IllegalArgumentException( "byte buffer's remaining() is too small" );
            int magicNumber = buffer.getInt();
            if( magicNumber != MAGIC_NUMBER )
                throw new IllegalArgumentException( "magic number is not valid" );
            version = buffer.getInt();
            type = buffer.getInt();
            messageLen = buffer.getInt();
        } finally {
            buffer.position( restorePosition );
        }
        return messageLen;
    }

    /**
     * {@inheritDoc}
     */
    public void parseMessage( final byte[] bytes, final int offset, final int length ) throws IllegalArgumentException, MessageIOException {
        if( bytes == null )
            throw new IllegalArgumentException( "bytes must be initialized" );
        if( offset < 0 )
            throw new IllegalArgumentException( "offset is too small" );
        if( length < 0 )
            throw new IllegalArgumentException( "length is too small" );
        if( bytes.length < offset + length )
            throw new IllegalArgumentException( "bytes' length is too small" );

        if( length > 0 ) {
            int msgSize = HEADER_LENGTH + length;
            if( msgSize > maxTotalMessageLength ) {
                if( LOG.isLoggable( Level.WARNING ) )
                    LOG.log( Level.WARNING,
                             "total message size is too big: size = " + msgSize + ", max size = " + maxTotalMessageLength );
    }

            if (bytes.length - offset < length) {
                throw new IllegalArgumentException("byte[] is too small");
            }

            ByteArrayInputStream bais = new ByteArrayInputStream( bytes, offset, length );
            try {
                readMessagesInputStream(bais);
            } finally {
                try {
                    bais.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void parseMessage( final Buffer buffer, final int offset, final int length ) throws IllegalArgumentException, MessageIOException {
        if( buffer == null )
            throw new IllegalArgumentException( "byte buffer must be initialized" );
        if( offset < 0 )
            throw new IllegalArgumentException( "offset is too small" );
        if( length < 0 )
            throw new IllegalArgumentException( "length is too small" );
        if( length > 0 ) {
            int msgSize = HEADER_LENGTH + length;
            if( msgSize > maxTotalMessageLength ) {
                if( LOG.isLoggable( Level.WARNING ) )
                    LOG.log( Level.WARNING,
                             "total message size is too big: size = " + msgSize + ", max size = " + maxTotalMessageLength );
            }
            int restorePosition = buffer.position();
            int restoreLimit = buffer.limit();

            try {
                buffer.position( offset );
                if( buffer.remaining() < length )
                    throw new IllegalArgumentException( "byte buffer's remaining() is too small" );

                buffer.limit(offset + length);
                readMessagesInputStream(new BufferInputStream(buffer));
            } finally {
                BufferUtils.setPositionLimit(buffer, restorePosition, restoreLimit);
            }
        }
    }

    private void readMessagesInputStream(InputStream is) throws IllegalArgumentException, MessageIOException {
        try {
            int messageCount = readInt(is);
            messageLock.lock();
            try {
                NetworkUtility.deserialize( is, messageCount, messages );
            } finally {
                modified = true;
                messageLock.unlock();
            }
        } catch( IOException ie ) {
            throw new MessageIOException( ie );
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getVersion() {
        return version;
    }

    /**
     * {@inheritDoc}
     */
    public int getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    public Object addMessageElement( final String key, final Serializable value ) {
        messageLock.lock();
        try {
            return messages.put( key, value );
        } finally {
            modified = true;
            messageLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getMessageElement( final String key ) {
        return messages.get( key );
    }

    /**
     * {@inheritDoc}
     */
    public Object removeMessageElement( final String key ) {
        messageLock.lock();
        Serializable removed = null;
        try {
            removed = messages.remove( key );
            return removed;
        } finally {
            if( removed != null )
                modified = true;
            messageLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set<Map.Entry<String, Serializable>> getMessageElements() {
        return Collections.unmodifiableSet( messages.entrySet() );
    }

    /**
     * {@inheritDoc}
     */
    public ByteBuffer getPlainByteBuffer() throws MessageIOException {
        messageLock.lock();
        try {
            if( byteBuffer != null && !modified )
                return byteBuffer;
            MessageByteArrayOutputStream mbaos = new MessageByteArrayOutputStream();
            DataOutputStream dos = null;
            try {
                dos = new DataOutputStream( mbaos );
                int tempInt = 0;
                dos.writeInt( tempInt );
                int messageCount = NetworkUtility.serialize( mbaos, messages );
                mbaos.writeIntWithoutCount( 0, messageCount );
            } catch( IOException ie ) {
                throw new MessageIOException( ie );
            } finally {
                if( dos != null ) {
                    try {
                        dos.close();
                    } catch( IOException e ) {
                    }
                }
            }
            int messageLen;
            byte[] messageBytes = mbaos.getPlainByteArray();
            if( messageBytes != null )
                messageLen = Math.min( messageBytes.length, mbaos.size() );
            else
                messageLen = 0;
            int msgSize = HEADER_LENGTH + messageLen;
            if( msgSize > maxTotalMessageLength ) {
                if( LOG.isLoggable( Level.WARNING ) ) { 
                    LOG.log( Level.WARNING,
                             "total message size is too big: size = " + msgSize + ", max size = " + maxTotalMessageLength );
                }
                throw new MessageIOException("total message size is too big: size = " + msgSize + ", max size = " + maxTotalMessageLength +
                toString());
            }
            byteBuffer = ByteBuffer.allocate( HEADER_LENGTH + messageLen );
            byteBuffer.putInt( MAGIC_NUMBER );
            byteBuffer.putInt( version );
            byteBuffer.putInt( type );
            byteBuffer.putInt( messageLen );
            byteBuffer.put( messageBytes, 0, messageLen );
            byteBuffer.flip();
            return byteBuffer;
        } finally {
            modified = false;
            messageLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public byte[] getPlainBytes() throws MessageIOException {
        messageLock.lock();
        try {
            return getPlainByteBuffer().array();
        } finally {
            messageLock.unlock();
        }
    }

    public static String getStringType( final int type ) {
        switch( type ) {
            case TYPE_CLUSTER_MANAGER_MESSAGE:
                return "CLUSTER_MANAGER_MESSAGE";
            case TYPE_HEALTH_MONITOR_MESSAGE:
                return "HEALTH_MONITOR_MESSAGE";
            case TYPE_MASTER_NODE_MESSAGE:
                return "MASTER_NODE_MESSAGE";
            case TYPE_MCAST_MESSAGE:
                return "MCAST_MESSAGE";
            case TYPE_PING_MESSAGE:
                return "PING_MESSAGE";
            case TYPE_PONG_MESSAGE:
                return "PONG_MESSAGE";
            default:
                return "UNKNOWN_MESSAGE(" + type + ")";
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(50);
        sb.append(MessageImpl.class.getSimpleName());
        sb.append("[v" + version + ":");
        sb.append(getStringType( type ) + ":");

        for (String elementName : messages.keySet()) {
            if (SOURCE_PEER_ID_TAG.compareTo(elementName) == 0) {
                sb.append(" Source: " + messages.get(SOURCE_PEER_ID_TAG) + ", ");
            } else if (TARGET_PEER_ID_TAG.compareTo(elementName) == 0) {
                sb.append(" Target: " + messages.get(TARGET_PEER_ID_TAG) + " , ");
            } else {
                sb.append(elementName + ", ");
            }
        }
        return sb.toString();
    }

    private static int readInt(InputStream is) throws IOException {
        int ch1 = is.read();
        int ch2 = is.read();
        int ch3 = is.read();
        int ch4 = is.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    private static int readInt(byte[] bytes, int offset) {
        int ch1 = bytes[offset] & 0xFF;
        int ch2 = bytes[offset + 1] & 0xFF;
        int ch3 = bytes[offset + 2] & 0xFF;
        int ch4 = bytes[offset + 3] & 0xFF;
        
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    private class MessageByteArrayOutputStream extends ByteArrayOutputStream {

        private MessageByteArrayOutputStream() {
            super();
        }

        private synchronized byte[] getPlainByteArray() {
            return buf;
        }

        private synchronized void writeIntWithoutCount( final int pos, final int value ) {
            NetworkUtility.writeIntToByteArray( buf, pos, value );
        }
    }

}