/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.mgmt.transport.grizzly.grizzly2;

import com.sun.enterprise.mgmt.transport.Message;
import com.sun.enterprise.mgmt.transport.MessageImpl;
import java.io.IOException;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.attributes.NullaryFunction;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.memory.MemoryManager;

/**
 * Filter, responsible for {@link Buffer} <-> {@link Message} transformation.
 * 
 * Message protocol format is:
 *
 * Message Header is MessageImpl.HEADER_LENGTH and composed of following fields.
 *      magicNumber    integer     {@link MessageImpl#MAGIC_NUMBER}
 *      version        integer     {@link MessageImpl#VERSION}
 *      type           integer     {@link Message#getType} for possible values
 *      messageLength  integer     {@link MessageImpl#maxTotalMessageLength}
 *
 * Message Body is composed of following fields.
 *      payload        byte[messageLen]
 *
 * MessageHeader  {@link Message#parseHeader(com.sun.enterprise.mgmt.transport.Buffer, int)}
 * MessageBody    {@link Message#parseMessage(com.sun.enterprise.mgmt.transport.Buffer, int, int)}
 *
 * @author Bongjae Chang
 * @author Joe Fialli
 * @author Alexey Stashok
 */
public class MessageFilter extends BaseFilter {

    private final Attribute<MessageParsingState> preparsedMessageAttr =
            Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute(
            MessageFilter.class + ".preparsedMessageAttr",
            new NullaryFunction<MessageParsingState>() {

                @Override
                public MessageParsingState evaluate() {
                    return new MessageParsingState();
                }
            });

    @Override
    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        final Connection connection = ctx.getConnection();
        final Buffer buffer = ctx.getMessage();

        final MessageParsingState parsingState =
                preparsedMessageAttr.get(connection);

        if (!parsingState.isHeaderParsed) {
            // Header was not parsed yet
            if (buffer.remaining() < MessageImpl.HEADER_LENGTH) {
                // not enough data to parse the header
                return ctx.getStopAction(buffer);
            }


            final MessageImpl message = new MessageImpl();

            final GMSBufferWrapper gmsBuffer =
                    parsingState.gmsBufferWrapper.wrap(buffer);

            final int messageLength =
                    message.parseHeader(gmsBuffer, gmsBuffer.position());

            gmsBuffer.recycle();

            if (messageLength + MessageImpl.HEADER_LENGTH > MessageImpl.getMaxMessageLength()) {
                throw new IllegalStateException("too large message."
                        + " request-size=" + (messageLength + MessageImpl.HEADER_LENGTH)
                        + " max-size=" + MessageImpl.getMaxMessageLength());
            }

            parsingState.isHeaderParsed = true;
            parsingState.message = message;
            parsingState.messageLength = messageLength;
        }

        final int totalMsgLength = MessageImpl.HEADER_LENGTH +
                parsingState.messageLength;

        if (buffer.remaining() <  totalMsgLength) {
            // We don't have entire message
            return ctx.getStopAction(buffer);
        }

        final int pos = buffer.position();

        final GMSBufferWrapper gmsBuffer =
                parsingState.gmsBufferWrapper.wrap(buffer);

        parsingState.message.parseMessage(gmsBuffer,
                pos + MessageImpl.HEADER_LENGTH,
                parsingState.messageLength);

        ctx.setMessage(parsingState.message);

        gmsBuffer.recycle();

        // Go to the next message
        final Buffer remainder =
                buffer.split(pos + totalMsgLength);

        parsingState.reset();
        
        return ctx.getInvokeAction(remainder.hasRemaining() ? remainder : null);
    }

    @Override
    public NextAction handleWrite(final FilterChainContext ctx) throws IOException {
        final Message message = ctx.getMessage();

        final MemoryManager mm = ctx.getConnection().getTransport().getMemoryManager();

        com.sun.enterprise.mgmt.transport.buffers.Buffer buffer =
                message.getPlainBuffer(
                Grizzly2ExpandableBufferWriter.createFactory(mm));

        ctx.setMessage(buffer.underlying());

        return ctx.getInvokeAction();
    }

    static final class MessageParsingState {
        final GMSBufferWrapper gmsBufferWrapper = new GMSBufferWrapper();
        boolean isHeaderParsed;
        int messageLength;
        MessageImpl message;

        void reset() {
            isHeaderParsed = false;
            message = null;
            messageLength = 0;
            gmsBufferWrapper.recycle();
        }
    }
}
