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

package com.sun.enterprise.mgmt.transport.grizzly;

import com.sun.enterprise.mgmt.transport.BufferUtils;
import com.sun.enterprise.mgmt.transport.ByteBuffersBuffer;
import com.sun.grizzly.ProtocolParser;
import com.sun.grizzly.SSLConfig;
import com.sun.enterprise.mgmt.transport.Message;
import com.sun.enterprise.mgmt.transport.MessageImpl;
import com.sun.grizzly.filter.ParserProtocolFilter;
import com.sun.grizzly.util.WorkerThread;

import java.nio.ByteBuffer;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Message protocol format is:
 *
 * Message Header is MessageImpl.HEADER_LENGTH and composed of following fields.
 *      magicNumber    integer     {@link MessageImpl#MAGIC_NUMBER}
 *      version        integer     {@link MessageImpl#VERSION}
 *      type           integer     {@link Message#getType} for possible values
 *      messageLength  integer     {@link MessageImpl#MAX_TOTAL_MESSAGE_LENGTH}
 *
 * Message Body is composed of following fields.
 *      payload        byte[messageLen]
 *
 * MessageHeader  {@link Message#parseHeader(ByteBuffer, int)}
 * MessageBody    {@link Message#parseMessage(ByteBuffer, int, int)}
 *
 * @author Bongjae Chang
 * @author Joe FIalli
 */
public class GrizzlyMessageProtocolParser implements ProtocolParser<Message> {

    private static final Logger LOG = GrizzlyUtil.getLogger();

    private static final int MIN_BUFFER_FREE_SPACE = 1024;

    private SSLConfig sslConfig;   // TBD.

    private final ByteBuffersBuffer workBuffer = new ByteBuffersBuffer();
    
    private Message message;
    private ByteBuffer lastBuffer;
    private boolean error;
    private int messageLength;
    private boolean justParsedMessage;

    static volatile Level DEBUG_LEVEL = Level.FINE;          // set this to Level.INFO to trace parsing.
    static volatile boolean DEBUG_ENABLED = false;           // must be set to TRUE to enable debugging.

    protected GrizzlyMessageProtocolParser() {
    }

    protected GrizzlyMessageProtocolParser( SSLConfig sslConfig ) {
        this.sslConfig = sslConfig;
        if (sslConfig != null) {
            throw new UnsupportedOperationException("GrizzlyMessageProtocolParser: sslConfig is not yet supported");
        }
    }

    public static ParserFilter createParserProtocolFilter( final SSLConfig sslConfig ) {
        return new ParserFilter() {
            public ProtocolParser newProtocolParser() {
                return new GrizzlyMessageProtocolParser( sslConfig );
            }
        };
    }

    public boolean isError() {
        return error;
    }

    /**
     * Is this ProtocolParser expecting more data ?
     *
     * This method is typically called after a call to
     * {@link ProtocolParser#hasNextMessage()} to determine if the
     * {@link ByteBuffer} which has been parsed contains a partial message
     *
     * @return - <tt>true</tt> if more bytes are needed to construct a
     *           message;  <tt>false</tt>, if no
     *           additional bytes remain to be parsed into a protocol data unit.
     *	 	 Note that if no partial message exists, this method should
     *		 return false.
     */
    public boolean isExpectingMoreData() {
        final boolean isExpectingMoreData = workBuffer.hasRemaining() && !justParsedMessage && !error;

        if( DEBUG_ENABLED && LOG.isLoggable(DEBUG_LEVEL ) ) {
            LOG.log( DEBUG_LEVEL, logState( "isExpectingMoreData() return " + isExpectingMoreData));
        }
        return isExpectingMoreData;
    }

    /**
     * Are there more bytes to be parsed in the {@link ByteBuffer} given
     * to this ProtocolParser's {@link ProtocolParser#startBuffer(ByteBuffer)} ?
     *
     * This method is typically called after processing the protocol message,
     * to determine if the {@link ByteBuffer} has more bytes which
     * need to parsed into a next message.
     *
     * @return <tt>true</tt> if there are more bytes to be parsed.
     *         Otherwise <tt>false</tt>.
     */
    public boolean hasMoreBytesToParse() {
        final boolean hasMoreBytesToParse = workBuffer.hasRemaining() && justParsedMessage && !error;
        if( DEBUG_ENABLED && LOG.isLoggable(DEBUG_LEVEL ) ) {
            LOG.log( DEBUG_LEVEL, logState(
                    "hasMoreBytesToParse() return " + hasMoreBytesToParse));
        }
        return hasMoreBytesToParse;
    }

    /**
     * Get the next complete message from the buffer, which can then be
     * processed by the next filter in the protocol chain. Because not all
     * filters will understand protocol messages, this method should also
     * set the position and limit of the buffer at the start and end
     * boundaries of the message. Filters in the protocol chain can
     * retrieve this message via context.getAttribute(MESSAGE)
     *
     * @return The next message in the buffer. If there isn't such a message,
     *	return <tt>null</tt>.
     *
     */
    public Message getNextMessage() {
        if( DEBUG_ENABLED && LOG.isLoggable(DEBUG_LEVEL ) ) {
            LOG.log( DEBUG_LEVEL, logState( "getNextMessage() - enter"));
        }

        Message nextMsg = null;
        if (error) {
            workBuffer.dispose();  // after error,  drop entire buffer.
        } else {
            nextMsg= message;
//            workBuffer.trimLeft();
//            if (!workBuffer.hasRemaining()) {
//                lastBuffer = null;
//            }
            justParsedMessage = true;
        }

        // reset cached values from last hasNextMessage() call.
        message = null;
        messageLength = 0;

        if( DEBUG_ENABLED && LOG.isLoggable(DEBUG_LEVEL ) ) {
            LOG.log( DEBUG_LEVEL, logState( "getNextMessage() - exit"));
        }
        return nextMsg;
    }

    /**
     * Indicates whether the buffer has a complete message that can be
     * returned from {@link ProtocolParser#getNextMessage()}. Smart
     * implementations of this will set up all the information so that an
     * actual call to {@link ProtocolParser#getNextMessage()} doesn't need to
     * re-parse the data.
     */
    public boolean hasNextMessage() {
        justParsedMessage = false;
        String partialReason = "";
        boolean hasMessage = false;
        if (DEBUG_ENABLED && LOG.isLoggable(DEBUG_LEVEL)) {
            LOG.log(DEBUG_LEVEL,  logState("hasNextMessage() - enter"));
        }
        try {
            if( message == null ) {
                if( workBuffer.remaining() < MessageImpl.HEADER_LENGTH ) {
                    messageLength = 0;
                    if (DEBUG_ENABLED && LOG.isLoggable(DEBUG_LEVEL)) {
                        partialReason = "isPartialMsg:not enough buffer for msg header";
                    }
                } else {
                    Message incomingMessage = new MessageImpl();
                    messageLength = incomingMessage.parseHeader(workBuffer, workBuffer.position());
                    message = incomingMessage;
                }
            }
            if(message != null && messageLength > 0 ) {
                if( messageLength + MessageImpl.HEADER_LENGTH > MessageImpl.getMaxMessageLength() ){
                    throw new Exception( "too large message."
                            + " request-size=" + (messageLength + MessageImpl.HEADER_LENGTH) +
                            " max-size=" +  MessageImpl.getMaxMessageLength());
                }
                int totalMsgLength = MessageImpl.HEADER_LENGTH + messageLength;
                if( workBuffer.remaining() <  totalMsgLength) {
                    if (DEBUG_ENABLED && LOG.isLoggable(DEBUG_LEVEL)){
                        partialReason = "isPartialMsg:not enough buffer for msg length";
                    }
                } else {
                    int pos = workBuffer.position();
                    message.parseMessage(workBuffer, workBuffer.position() + MessageImpl.HEADER_LENGTH, messageLength );

                    // Go to the next message
                    workBuffer.position(pos + totalMsgLength);
                    hasMessage = true;
                }
            }
        } catch( Throwable t ) {
            if( DEBUG_ENABLED && LOG.isLoggable( Level.WARNING ) ) {
                LOG.log( Level.WARNING, logState( "hasNextMessage() - exit with error" ), t );
            }
            error = true;
        } finally {
            if ( !error && DEBUG_ENABLED && LOG.isLoggable(DEBUG_LEVEL)) {
                LOG.log(DEBUG_LEVEL,  logState("hasNextMessage() - exit" + partialReason));
            }
        }
        return hasMessage;
    }

    /**
     * Set the buffer to be parsed. This method should store the buffer and
     * its state so that subsequent calls to
     * {@link ProtocolParser#getNextMessage()} will return distinct messages,
     * and the buffer can be restored after parsing when the
     * {@link ProtocolParser#releaseBuffer()} method is called.
     */
    public void startBuffer( ByteBuffer bb ) {
        // We begin with a buffer containing data. Save the initial buffer
        // state information. The best thing here is to get the backing store
        // so that the bytes can be parsed directly. We also need to save the
        // original limit so that we can place the buffer in the correct state at the
        // end of parsing
        justParsedMessage  = false;
        error = false;
        bb.flip();
        if (bb == lastBuffer) {
            // If coming bb is already in the workBuffer
            // recalc workBuffer capacity
            workBuffer.calcCapacity();
            workBuffer.limit(workBuffer.capacity());
        } else {
            // If coming bb is not in the workBuffer - add it
            lastBuffer = bb;
            workBuffer.append(lastBuffer);
        }
        if (DEBUG_ENABLED && LOG.isLoggable(DEBUG_LEVEL)) {
            LOG.log(DEBUG_LEVEL, this.logState("startBuffer"));
        }
    }

    /**
     * No more parsing will be done on the buffer passed to
     * {@link ProtocolParser#startBuffer(ByteBuffer)}.
     * Set up the buffer so that its position is the first byte that was
     * not part of a full message, and its limit is the original limit of
     * the buffer.
     *
     * @return -- true if the parser has saved some state (e.g. information
     * data in the buffer that hasn't been returned in a full message);
     * otherwise false. If this method returns true, the framework will
     * make sure that the same parser is used to process the buffer after
     * more data has been read.
     */
    public boolean releaseBuffer() {
        if (DEBUG_ENABLED && LOG.isLoggable(DEBUG_LEVEL)){
            LOG.log(DEBUG_LEVEL, logState("releaseBuffer - enter"));
        }

        final boolean hasRemaining = workBuffer.hasRemaining();
        if(!hasRemaining || error) {
            if (lastBuffer != null) {
                lastBuffer.clear();
            }
            workBuffer.dispose();
            messageLength = 0;
            lastBuffer = null;
            error = false;
        } else {
            compactBuffers();
        }

        if (DEBUG_ENABLED && LOG.isLoggable(DEBUG_LEVEL)) {
            LOG.log(DEBUG_LEVEL, logState("releaseBuffer - exit"));
        }
        return hasRemaining;
    }

    private void compactBuffers() {
        workBuffer.trimLeft();
        final int workerBufferRemaining = workBuffer.remaining();
        final int lastBufferRemaining = lastBuffer.remaining();

        final int lastBufferFreeSpace = lastBuffer.capacity() - lastBufferRemaining;

        if (lastBufferFreeSpace < MIN_BUFFER_FREE_SPACE) {
            // if lastBuffer has less than min buffer size available for the
            // next read operation - make Grizzly to forget about it and
            // allocate a new buffer
            ((WorkerThread) Thread.currentThread()).setByteBuffer(null);
        } else {
            if (workerBufferRemaining < lastBufferRemaining) {
            // We've parsed a message, and part of the part of the next message
            // is ready in the workBuffer.
            // But next message, which workerBuffer refers, starts in the
            // midle of lastBuffer. So in order to prepare last buffer for the
            // next read - we need to compact it

                lastBuffer.position(lastBuffer.limit() - workerBufferRemaining);
            lastBuffer.compact();
            lastBuffer.flip();
                BufferUtils.setPositionLimit(workBuffer, 0, workerBufferRemaining);
                workBuffer.calcCapacity();

//            workBuffer.append(lastBuffer);
            }
            // prepare lastBuffer for the next read operation
            lastBuffer.position(lastBuffer.limit());
            lastBuffer.limit(lastBuffer.capacity());
        }
    }

    private String logState(String where) {
        StringBuilder result = new StringBuilder(40);
        result.append(where);
        result.append(" Thread:" + Thread.currentThread().getName());
        if (workBuffer != null) {
            result.append(",workerBuffer:" + workBuffer);
            result.append(",lastBuffer:" + lastBuffer);
        }
        result.append(",justParsedMessage:" + justParsedMessage);
        result.append(",error:" + error);
        result.append(",msg size:" + messageLength);
        result.append(",message: " + message);

        return result.toString();
    }
}
