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
import java.io.DataInputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Bongjae Chang
 */
// [packet]
// magic(4) + version(4) + type(4) + messages_length(4) + messages(message_length)
// [messages]
// message_count(4) + message_key1 + message_value1 + message_key2 + message_value2 + ...(message_count)
public class MessageImpl implements Message {

    static final long serialVersionUID = -3617083350698668655L;

    private static final Logger LOG = GMSLogDomain.getLogger( GMSLogDomain.GMS_LOGGER );

    public static final int MAX_TOTAL_MESSAGE_LENGTH = 8192;
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

    public MessageImpl() {
    }

    public MessageImpl( final int type ) {
        initialize( type, null );
    }

    public MessageImpl( final int type, final Map<String, Serializable> messages ) {
        initialize( type, messages );
    }

    public void initialize( final int type, final Map<String, Serializable> messages ) throws IllegalArgumentException {
        this.version = VERSION;
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

    public int parseHeader( final byte[] bytes, final int offset ) throws IllegalArgumentException {
        if( bytes == null )
            throw new IllegalArgumentException( "bytes must be initialized" );
        if( offset < 0 )
            throw new IllegalArgumentException( "offset is too small" );
        if( bytes.length < offset + HEADER_LENGTH )
            new IllegalArgumentException( "bytes' length is too small" );
        return parseHeader( ByteBuffer.wrap( bytes, offset, HEADER_LENGTH ), offset );
    }

    public int parseHeader( final ByteBuffer byteBuffer, final int offset ) throws IllegalArgumentException {
        if( byteBuffer == null )
            throw new IllegalArgumentException( "byte buffer must be initialized" );
        if( offset < 0 )
            throw new IllegalArgumentException( "offset is too small" );
        int messageLen;
        int restorePosition = byteBuffer.position();
        try {
            byteBuffer.position( offset );
            if( byteBuffer.remaining() < HEADER_LENGTH )
                throw new IllegalArgumentException( "byte buffer's remaining() is too small" );
            int magicNumber = byteBuffer.getInt();
            if( magicNumber != MAGIC_NUMBER )
                throw new IllegalArgumentException( "magic number is not valid" );
            version = byteBuffer.getInt();
            type = byteBuffer.getInt();
            messageLen = byteBuffer.getInt();
        } finally {
            byteBuffer.position( restorePosition );
        }
        return messageLen;
    }

    public void parseMessage( final byte[] bytes, final int offset, final int length ) throws IllegalArgumentException, MessageIOException {
        if( bytes == null )
            throw new IllegalArgumentException( "bytes must be initialized" );
        if( offset < 0 )
            throw new IllegalArgumentException( "offset is too small" );
        if( length < 0 )
            throw new IllegalArgumentException( "length is too small" );
        if( bytes.length < offset + length )
            new IllegalArgumentException( "bytes' length is too small" );
        parseMessage( ByteBuffer.wrap( bytes, offset, length ), offset, length );
    }

    public void parseMessage( final ByteBuffer byteBuffer, final int offset, final int length ) throws IllegalArgumentException, MessageIOException {
        if( byteBuffer == null )
            throw new IllegalArgumentException( "byte buffer must be initialized" );
        if( offset < 0 )
            throw new IllegalArgumentException( "offset is too small" );
        if( length < 0 )
            throw new IllegalArgumentException( "length is too small" );
        if( length > 0 ) {
            if( HEADER_LENGTH + length > MAX_TOTAL_MESSAGE_LENGTH ) {
                if( LOG.isLoggable( Level.WARNING ) )
                    LOG.log( Level.WARNING,
                             "total message size is too big: size = " + HEADER_LENGTH + length + ", max size = " + MAX_TOTAL_MESSAGE_LENGTH );
            }
            int restorePosition = byteBuffer.position();
            try {
                byteBuffer.position( offset );
                if( byteBuffer.remaining() < length )
                    throw new IllegalArgumentException( "byte buffer's remaining() is too small" );
                byte[] bytes = new byte[length];
                byteBuffer.get( bytes );
                readMessagesFromBytes( bytes, 0, length );
            } finally {
                byteBuffer.position( restorePosition );
            }
        }
    }

    private void readMessagesFromBytes( final byte[] bytes, final int offset, final int length ) throws IllegalArgumentException, MessageIOException {
        if( bytes == null )
            return;
        if( bytes.length < offset + length )
            new IllegalArgumentException( "bytes' length is too small" );
        ByteArrayInputStream bais = new ByteArrayInputStream( bytes, offset, length );
        DataInputStream dis = null;
        try {
            dis = new DataInputStream( bais );
            int messageCount = dis.readInt();
            messageLock.lock();
            try {
                NetworkUtility.deserialize( bais, messageCount, messages );
            } finally {
                modified = true;
                messageLock.unlock();
            }
        } catch( IOException ie ) {
            throw new MessageIOException( ie );
        } finally {
            if( dis != null ) {
                try {
                    dis.close();
                } catch( IOException e ) {
                }
            }
        }
    }

    public int getVersion() {
        return version;
    }

    public int getType() {
        return type;
    }

    public Object addMessageElement( final String key, final Serializable value ) {
        messageLock.lock();
        try {
            return messages.put( key, value );
        } finally {
            modified = true;
            messageLock.unlock();
        }
    }

    public Object getMessageElement( final String key ) {
        return messages.get( key );
    }

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

    public Set<Map.Entry<String, Serializable>> getMessageElements() {
        return Collections.unmodifiableSet( messages.entrySet() );
    }

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
            if( HEADER_LENGTH + messageLen > MAX_TOTAL_MESSAGE_LENGTH ) {
                if( LOG.isLoggable( Level.WARNING ) )
                    LOG.log( Level.WARNING,
                             "total message size is too big: size = " + HEADER_LENGTH + messageLen + ", max size = " + MAX_TOTAL_MESSAGE_LENGTH );
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
        return MessageImpl.class.getSimpleName() + "[v" + version + ":" + getStringType( type ) + ":" + messages + "]";
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

    public static void main( String[] args ) {
        try {
            Message message = new MessageImpl( TYPE_CLUSTER_MANAGER_MESSAGE );
            System.out.println( "empty message = " + message );
            String key1 = "test_key1";
            String value1 = new String( "test message1" );
            String key2 = "test_key2";
            String value2 = new String( "test message2" );
            message.addMessageElement( key1, value1 );
            message.addMessageElement( key2, value2 );
            System.out.println( "message = " + message );

            Message message2 = new MessageImpl();
            byte[] plainBytes = message.getPlainBytes();
            int messageLen = message2.parseHeader( plainBytes, 0 );
            message2.parseMessage( plainBytes, HEADER_LENGTH, messageLen );

            System.out.println( "message from bytes = " + message2 );
            message.removeMessageElement( key2 );

            Message message3 = new MessageImpl();
            plainBytes = message.getPlainBytes();
            messageLen = message3.parseHeader( plainBytes, 0 );
            message3.parseMessage( plainBytes, HEADER_LENGTH, messageLen );
            System.out.println( "removed message from bytes = " + message3 );

            ByteBuffer plainByteBuffer = message.getPlainByteBuffer();
            Message message4 = new MessageImpl();
            messageLen = message4.parseHeader( plainByteBuffer, 0 );
            message4.parseMessage( plainByteBuffer, HEADER_LENGTH, messageLen );
            System.out.println( "message from byte buffer = " + message4 );
            message.removeMessageElement( key1 );

            plainByteBuffer = message.getPlainByteBuffer();
            Message message5 = new MessageImpl();
            messageLen = message5.parseHeader( plainByteBuffer, 0 );
            message5.parseMessage( plainByteBuffer, HEADER_LENGTH, messageLen );
            System.out.println( "removed message from byte buffer = " + message5 );
        } catch( Exception e ) {
            e.printStackTrace();
        }
    }
}
