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
import junit.framework.TestCase;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.utils.ChunkingFilter;

/**
 * Set of Grizzly 2.0 tests
 *
 * @author Alexey Stashok
 */
public class GrizzlyParserTest extends TestCase {
    public static final String NUMBER_ELEMENT_KEY = "Number";
    public static final String RESULT_ELEMENT = "RESULT";

    public static final int PORT = 8998;

    private static final Logger LOGGER = Grizzly.logger(GrizzlyParserTest.class);
    
    private TCPNIOTransport transport;
    private ServerEchoFilter serverEchoFilter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        transport = initializeServer();
    }

    @Override
    protected void tearDown() throws Exception {
        transport.stop();

        super.tearDown();
    }

    public void testSimpleMessage() throws Exception {
        final Message request = createMessage(1, 1);
        final Message response = sendRequest(request);

        final String result = (String) response.getMessageElement(RESULT_ELEMENT);
        assertNotNull(result);
        assertEquals("OK", result);
    }

    public void test10Messages() throws Exception {
        for (int i = 0; i < 10; i++) {
            final Message request = createMessage(i + 1, i * 512);
            final Message response = sendRequest(request);

            final String result = (String) response.getMessageElement(RESULT_ELEMENT);
            assertNotNull(result);
            assertEquals("OK", result);
        }
    }

    public Message sendRequest(final Message request) throws Exception {
        final FutureImpl<Message> future = SafeFutureImpl.<Message>create();
        
        final FilterChain clientFilterChain = FilterChainBuilder.stateless()
                .add(new TransportFilter())
                .add(new ChunkingFilter(1))
                .add(new MessageFilter())
                .add(new ClientResultFilter(future))
                .build();
        final TCPNIOTransport clientTransport =
                TCPNIOTransportBuilder.newInstance().build();
        clientTransport.setProcessor(clientFilterChain);

        Connection connection = null;
        try {
            clientTransport.start();

            final Future<Connection> connectFuture =
                    clientTransport.connect("localhost", PORT);

            connection = connectFuture.get(10, TimeUnit.SECONDS);

            connection.write(request);

            return future.get(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error on msg#" + request.getMessageElement(NUMBER_ELEMENT_KEY)
                    + " server processed " + serverEchoFilter.getCount() + " requests", e);
            throw e;
        } finally {
            if (connection != null) {
                connection.close();
            }
            try {
                clientTransport.stop();
            } catch (IOException ignored) {
            }
        }
    }

    private static Message createMessage(final int num,
            final int objectsCount) throws IOException {
        final MessageImpl message = new MessageImpl(100);
        
        message.addMessageElement(NUMBER_ELEMENT_KEY, num);
        for (int i = 0; i < objectsCount; i++) {
            message.addMessageElement("Param #" + i, "Value #" + i);
        }

        return message;
    }

    private static class ClientResultFilter extends BaseFilter {
        private final FutureImpl<Message> future;

        private ClientResultFilter(FutureImpl<Message> future) {
            this.future = future;
        }

        @Override
        public NextAction handleRead(final FilterChainContext ctx) throws IOException {
            final Message message = ctx.getMessage();
            future.result(message);

            return ctx.getStopAction();
        }
    
    }

    private static class ServerEchoFilter extends BaseFilter {
        private final AtomicInteger count = new AtomicInteger();

        @Override
        public NextAction handleRead(final FilterChainContext ctx) throws IOException {
            final Message message = ctx.getMessage();

            count.incrementAndGet();
            
            final Message outputMessage = new MessageImpl(100);
            outputMessage.addMessageElement(RESULT_ELEMENT, message != null ? "OK" : "FAILED");

            ctx.write(outputMessage);
            
            return ctx.getInvokeAction();
        }

        private int getCount() {
            return count.get();
        }
    }

    private TCPNIOTransport initializeServer() throws IOException {
        serverEchoFilter = new ServerEchoFilter();

        final FilterChain filterChain = FilterChainBuilder.stateless()
                .add(new TransportFilter())
                .add(new ChunkingFilter(1))
                .add(new MessageFilter())
                .add(serverEchoFilter)
                .build();

        TCPNIOTransport newTransport = TCPNIOTransportBuilder.newInstance().build();
        newTransport.setProcessor(filterChain);
        newTransport.bind(PORT);
        newTransport.start();
        return newTransport;
    }
}
