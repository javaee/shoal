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
import com.sun.grizzly.filter.*;
import com.sun.enterprise.mgmt.transport.Message;
import com.sun.enterprise.mgmt.transport.MessageImpl;

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

    private SSLConfig sslConfig;   // TBD.

    private Message message;
    private ByteBuffer savedBuffer;
    private boolean partial;              // is there only a partial message left in the buffer
    private int nextMsgStartPos;
    private boolean error;
    private int messageLength;
    private int originalLimit;

    static private final Level DEBUG_LEVEL = Level.FINEST;  // set this to Level.INFO to trace parsing.
    static private boolean DEBUG_ENABLED = false;           // must be set to TRUE to enable debugging.

    protected GrizzlyMessageProtocolParser() {
    }

    protected GrizzlyMessageProtocolParser( SSLConfig sslConfig ) {
        this.sslConfig = sslConfig;
        if (sslConfig != null) {
            throw new UnsupportedOperationException("GrizzlyMesageProtocolParser: sslConfig is not yet supported");
        }
    }

    public static ParserProtocolFilter createParserProtocolFilter( final SSLConfig sslConfig ) {
        return new ParserProtocolFilter() {
            public ProtocolParser newProtocolParser() {
                return new GrizzlyMessageProtocolParser( sslConfig );
            }
        };
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
        if( DEBUG_ENABLED && LOG.isLoggable(DEBUG_LEVEL ) ) {
            LOG.log( DEBUG_LEVEL, logState( "isExpectingMoreData() return partial=" + partial));
        }
        return partial;
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
        if( DEBUG_ENABLED && LOG.isLoggable(DEBUG_LEVEL ) ) {
            LOG.log( DEBUG_LEVEL, logState(
                    "hasMoreBytesToParse() return nextMsgStartPos[" + nextMsgStartPos +
                    "] < originalLimit[" + originalLimit));
        }
        return nextMsgStartPos < originalLimit;
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
        if (error || partial) {
            nextMsgStartPos = originalLimit;  // after error,  drop entire buffer.
        } else {
            nextMsg= message;

            // now that next Message has been read, increment to the nextMsgStartPos.
            nextMsgStartPos += MessageImpl.HEADER_LENGTH + messageLength;
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
        String partialReason = "";
        partial = false;
        int savedBufferRemainingBytes = originalLimit - nextMsgStartPos;
        if (DEBUG_ENABLED && LOG.isLoggable(DEBUG_LEVEL)) {
            LOG.log(DEBUG_LEVEL,  logState("hasNextMessage() - enter"));
        }
        try {
            if( message == null ) {
                savedBuffer.position(nextMsgStartPos);
                savedBuffer.limit(originalLimit);
                if( savedBufferRemainingBytes < MessageImpl.HEADER_LENGTH ) {
                    partial = true;
                    message = null;
                    messageLength = 0;
                    if (DEBUG_ENABLED && LOG.isLoggable(DEBUG_LEVEL)) {
                        partialReason = "isPartialMsg:not enough buffer for msg header";
                    }
                    nextMsgStartPos = originalLimit;  // so hasNoMoreBytesToParse returns true and releaseBuffer will compact partial data.
                } else {
                    Message incomingMessage = new MessageImpl();
                    messageLength = incomingMessage.parseHeader(savedBuffer, nextMsgStartPos );
                    message = incomingMessage;
                }
            }
            if( !partial && messageLength > 0 ) {
                if( messageLength + MessageImpl.HEADER_LENGTH > MessageImpl.MAX_TOTAL_MESSAGE_LENGTH ){
                    throw new Exception( "too large message" );
                }
                int totalMsgLength = MessageImpl.HEADER_LENGTH + messageLength;
                savedBuffer.position(nextMsgStartPos);
                savedBuffer.limit(originalLimit);
                if( savedBufferRemainingBytes <  totalMsgLength) {
                    message = null;
                    partial = true;
                    if (DEBUG_ENABLED && LOG.isLoggable(DEBUG_LEVEL)){
                        partialReason = "isPartialMsg:not enough buffer for msg length";
                    }
                    nextMsgStartPos = originalLimit;  // so hasMoreBytesToParse() returns true and releaseBuffer will get called and compact partial data.
                } else {
                    savedBuffer.limit(nextMsgStartPos + totalMsgLength);
                    message.parseMessage(savedBuffer, nextMsgStartPos + MessageImpl.HEADER_LENGTH, messageLength );

                    // ensure when getNextMessage() is called, that position is set to start of data corresponding to 'message' and limit is
                    // to end of data for that message.
                    savedBuffer.position(nextMsgStartPos + MessageImpl.HEADER_LENGTH);
                }
            }
        } catch( Throwable t ) {
            if( DEBUG_ENABLED && LOG.isLoggable( Level.WARNING ) ) {
                LOG.log( Level.WARNING, logState( "hasNextMessage() - exit with error" ), t );
            }
            partial = false;
            error = true;
            nextMsgStartPos = originalLimit;   // so no more bytes to parse.
        } finally {
            if ( !error && DEBUG_ENABLED && LOG.isLoggable(DEBUG_LEVEL)) {
                LOG.log(DEBUG_LEVEL,  logState("hasNextMessage() - exit" + partialReason));
            }
        }
        return !partial;
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
        message = null;
        error = false;
        messageLength = 0;
        savedBuffer = bb;
        savedBuffer.flip();
        partial = false;
        originalLimit = savedBuffer.limit();
        nextMsgStartPos = savedBuffer.position();

        // future optimization possibility:
        // if (savedBuffer.hasArray) {
        //      data = savedBuffer.array();
        //      position = savedBuffer.position() + savedBuffer.arrayOffset();
        //        limit = savedBuffer.limit() + savedBuffer.arrayOffset();
        //    } else ...maybe copy out the data, or use put/get when parsing...
        if (DEBUG_ENABLED && LOG.isLoggable(DEBUG_LEVEL)) {
            LOG.log(DEBUG_LEVEL, this.logState("startBuffer"));
        }

        // constraint check remaining from initial revision of this ProtocolParser.
        if( savedBuffer.capacity() < MessageImpl.MAX_TOTAL_MESSAGE_LENGTH ) {
            LOG.log( Level.WARNING, "byte buffer capacity is too small: capacity = " + savedBuffer.capacity() +
                                    " max total message length=" + MessageImpl.MAX_TOTAL_MESSAGE_LENGTH);
           
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
        if(!isExpectingMoreData()) {
            nextMsgStartPos = 0;
            messageLength = 0;
            originalLimit = 0;
            error = false;
            savedBuffer.clear();

        } else {
            // partial message remains in buffer.  release buffer so rest of message can be written into buffer.
            // crucial that savedBuffer position was set to correct position when partial true.  nextMsgStartPos
            // was altered to trigger hasNoMoreBytesToParse().
            savedBuffer.limit(originalLimit);
            savedBuffer.compact();
            nextMsgStartPos = 0;
            originalLimit = 0;
        }
        if (DEBUG_ENABLED && LOG.isLoggable(DEBUG_LEVEL)) {
            LOG.log(DEBUG_LEVEL, logState("releaseBuffer - exit"));
        }
        return partial;
    }

    private String logState(String where) {
        StringBuffer result = new StringBuffer(40);
        result.append(where);
        result.append(" Thread:" + Thread.currentThread().getName());
        if (savedBuffer != null) {
            result.append(",position:" + savedBuffer.position());
            result.append(",limit:" + savedBuffer.limit());
            result.append(",capacity:" + savedBuffer.capacity());
            result.append(",isDirect:" + savedBuffer.isDirect());
        }
        result.append(",nextMsgStartPos:" + nextMsgStartPos);
        result.append(",originalLimit: " + originalLimit);
        result.append(",hasMoreBytesToParse:" + (boolean)(nextMsgStartPos < originalLimit));
        result.append(",partialMsg:" + partial);
        result.append(",error:" + error);
        result.append(",msg size:" + messageLength);
        result.append(",message: " + message);

        return result.toString();
    }
}
