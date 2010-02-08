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

package com.sun.enterprise.mgmt.transport.grizzly;

import com.sun.grizzly.ProtocolParser;
import com.sun.grizzly.SSLConfig;
import com.sun.grizzly.util.WorkerThread;
import com.sun.grizzly.filter.*;
import com.sun.enterprise.mgmt.transport.*;
import com.sun.enterprise.mgmt.transport.Message;

import java.nio.ByteBuffer;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author Bongjae Chang
 */
public class GrizzlyMessageProtocolParser implements ProtocolParser<Message> {

    private static final Logger LOG = GrizzlyUtil.getLogger();

    private SSLConfig sslConfig;

    private Message message;
    private ByteBuffer byteBuffer;
    private boolean expectingMoreData;
    private boolean hasMoreBytesToParse;
    private int nextMsgStartPos;
    private boolean error;
    private int messageLength;

    protected GrizzlyMessageProtocolParser() {
    }

    protected GrizzlyMessageProtocolParser( SSLConfig sslConfig ) {
        this.sslConfig = sslConfig;
    }

    public static ParserProtocolFilter createParserProtocolFilter( final SSLConfig sslConfig ) {
        return new ParserProtocolFilter() {
            public ProtocolParser newProtocolParser() {
                return new GrizzlyMessageProtocolParser( sslConfig );
            }
        };
    }

    public boolean isExpectingMoreData() {
        return expectingMoreData;
    }

    public boolean hasMoreBytesToParse() {
        return hasMoreBytesToParse;
    }

    public Message getNextMessage() {
        Message tmp = message;
        if( error ) {
            hasMoreBytesToParse = false;
        } else {
            int totalMessageLength = MessageImpl.HEADER_LENGTH + messageLength;
            hasMoreBytesToParse = getByteBufferMsgBytes() > totalMessageLength;
            if( LOG.isLoggable( Level.FINEST ) )
                LOG.log( Level.FINEST, logState( "getNextMessage()" ) );
            nextMsgStartPos += totalMessageLength;
            message = null;
        }
        expectingMoreData = false;
        return tmp;
    }

    public boolean hasNextMessage() {
        hasMoreBytesToParse = false;
        try {
            if( message == null ) {
                if( getByteBufferMsgBytes() < MessageImpl.HEADER_LENGTH ) {
                    if( !byteBufferHasEnoughSpace( MessageImpl.HEADER_LENGTH - getByteBufferMsgBytes(), byteBuffer ) ) {
                        byteBuffer.position( nextMsgStartPos );
                        giveGrizzlyNewByteBuffer( byteBuffer );
                        nextMsgStartPos = 0;
                    }
                    expectingMoreData = true;
                    if( LOG.isLoggable( Level.FINER ) )
                        LOG.log( Level.FINER, logState( "hasNextMessage()" ) );
                    return false;
                }
                Message incomingMessage = new MessageImpl();
                messageLength = incomingMessage.parseHeader( byteBuffer, nextMsgStartPos );
                message = incomingMessage;
            }
            if( messageLength > 0 ) {
                if( messageLength + MessageImpl.HEADER_LENGTH > MessageImpl.MAX_TOTAL_MESSAGE_LENGTH )
                    throw new Exception( "too large message" );
                if( getByteBufferMsgBytes() < MessageImpl.HEADER_LENGTH + messageLength ) {
                    if( !byteBufferHasEnoughSpace( MessageImpl.HEADER_LENGTH + messageLength - getByteBufferMsgBytes(), byteBuffer ) ) {
                        byteBuffer.position( nextMsgStartPos );
                        giveGrizzlyNewByteBuffer( byteBuffer );
                        nextMsgStartPos = 0;
                    }
                    expectingMoreData = true;
                    if( LOG.isLoggable( Level.FINER ) )
                        LOG.log( Level.FINER, logState( "hasNextMessage()" ) );
                    return false;
                }
                message.parseHeader( byteBuffer, nextMsgStartPos );
                message.parseMessage( byteBuffer, nextMsgStartPos + MessageImpl.HEADER_LENGTH, messageLength );
            }
            expectingMoreData = false;
        } catch( Throwable t ) {
            if( LOG.isLoggable( Level.INFO ) )
                LOG.log( Level.INFO, logState( "hasNextMessage()" ), t );
            expectingMoreData = false;
            error = true;
        }
        return !expectingMoreData;
    }

    private int getByteBufferMsgBytes() {
        return byteBuffer.position() - nextMsgStartPos;
    }

    public void startBuffer( ByteBuffer bb ) {
        this.byteBuffer = bb;
        if( byteBuffer.capacity() < MessageImpl.MAX_TOTAL_MESSAGE_LENGTH ) {
            if( LOG.isLoggable( Level.WARNING ) )
                LOG.log( Level.WARNING, "byte buffer capacity is too small: capacity = " + byteBuffer.capacity() );
        }
    }

    public boolean releaseBuffer() {
        if( !expectingMoreData ) {
            hasMoreBytesToParse = false;
            nextMsgStartPos = 0;
            messageLength = 0;
            error = false;
            byteBuffer.clear();
        }
        return expectingMoreData;
    }

    private String logState( String where ) {
        return where + " "
               + "Thread:" + Thread.currentThread().getName()
               + ",position:" + byteBuffer.position()
               + ",nextMsgStartPos:" + nextMsgStartPos
               + ",expectingMoreData:" + expectingMoreData
               + ",hasMoreBytesToParse:" + hasMoreBytesToParse
               + ",error:" + error
               + ",msg size:" + messageLength
               + ",message: " + message;
    }

    /**
     * @param neededBytes number of bytes that the given buffer should be able to hold
     * @param buf         the bytebuffer which is queried for free space
     *
     * @return if buf can hold additinasl neededBytes
     */
    private static boolean byteBufferHasEnoughSpace( int neededBytes, ByteBuffer buf ) {
        return ( buf.capacity() - buf.position() ) >= neededBytes;
    }

    /**
     * Gives  current Thread a completely new Bytebuffer of @see Message.MessageMaxLength
     * with the given byteBuffer copied into it.
     *
     * @param buf the buffer which should be put into the newly created byteBuffer.
     */
    private static void giveGrizzlyNewByteBuffer( ByteBuffer buf ) {
        ByteBuffer newSpace = ByteBuffer.allocate( MessageImpl.MAX_TOTAL_MESSAGE_LENGTH );
        newSpace.put( buf );
        WorkerThread workerThread = (WorkerThread)Thread.currentThread();
        workerThread.setByteBuffer( newSpace );
    }
}
