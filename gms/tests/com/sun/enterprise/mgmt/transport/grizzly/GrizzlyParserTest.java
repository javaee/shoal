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

import com.sun.grizzly.Context;
import com.sun.grizzly.Controller;
import com.sun.grizzly.DefaultProtocolChain;
import com.sun.grizzly.DefaultProtocolChainInstanceHandler;
import com.sun.grizzly.ProtocolChain;
import com.sun.grizzly.ProtocolFilter;
import com.sun.grizzly.ProtocolParser;
import com.sun.grizzly.TCPSelectorHandler;
import com.sun.grizzly.filter.ParserProtocolFilter;
import com.sun.grizzly.util.OutputWriter;
import com.sun.grizzly.util.WorkerThreadImpl;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.util.Arrays;
import java.util.logging.Level;

/**
 * Set of Grizzly tests
 * 
 * @author Alexey Stashok
 */
public class GrizzlyParserTest {
    public static final int PORT = 8999;

    private static final int MAGIC_NUMBER = 770303;
    private static final int VERSION = 1;

    private static final int THREAD_LOCAL_BUFFER_SIZE = WorkerThreadImpl.DEFAULT_BYTE_BUFFER_SIZE;

    public static void main(String[] args) throws IOException {
        enableDebug(false);

        Controller controller = initializeServer();

        try {
            System.out.println("testSimpleMessage: " + testSimpleMessage());
            System.out.println("testChunkedMessage: " + testChunkedMessage());
            System.out.println("testOneAndHalfMessage: " + testOneAndHalfMessage());
            System.out.println("testBigMessage: " + testBigMessage());
            System.out.println("testOneAndHalfBigMessage: " + testOneAndHalfBigMessage());
            System.out.println("testTinyRemainder: " + testTinyRemainder());
        } finally {
            controller.stop();
        }
    }

    private static Controller initializeServer() {
        final ProtocolFilter resultFilter = new ResultFilter();
        final ParserProtocolFilter parserProtocolFilter = createParserProtocolFilter();
        TCPSelectorHandler selectorHandler = new TCPSelectorHandler();
        selectorHandler.setPort(PORT);

        final Controller controller = new Controller();

        controller.setSelectorHandler(selectorHandler);

        controller.setProtocolChainInstanceHandler(
                new DefaultProtocolChainInstanceHandler() {

                    @Override
                    public ProtocolChain poll() {
                        ProtocolChain protocolChain = protocolChains.poll();
                        if (protocolChain == null) {
                            protocolChain = new DefaultProtocolChain();
                            protocolChain.addFilter(parserProtocolFilter);
                            protocolChain.addFilter(resultFilter);
                        }
                        return protocolChain;
                    }
                });

        ControllerUtils.startController(controller);
        
        return controller;
    }

    private static boolean testSimpleMessage() throws IOException {
        Socket s = new Socket("localhost", PORT);
        s.setSoTimeout(5000);
        OutputStream os = s.getOutputStream();

        byte[] message = createMessage(1);
        os.write(message);
        os.flush();

        InputStream is = s.getInputStream();
        int result = is.read();

        s.close();
        
        if (result == 1) {
            return true;
        } else if (result == 0) {
            return false;
        }

        throw new IllegalStateException("Unexpected result: " + result);
    }

    private static boolean testChunkedMessage() throws IOException {
        Socket s = new Socket("localhost", PORT);
        s.setSoTimeout(5000);
        OutputStream os = s.getOutputStream();

        byte[] message = createMessage(3);

        os.write(message, 0, 4);  // MAGIC
        sleep(os, 1000);

        os.write(message, 4, 4);  // VERSION
        sleep(os, 1000);

        os.write(message, 8, 4);  // TYPE
        sleep(os, 1000);

        os.write(message, 12, 4);  // BODY-SIZE
        sleep(os, 2000);

        os.write(message, 16, 4);  // PARAMS-COUNT
        sleep(os, 1000);
        
        int messageHalf = (message.length - 20) / 2;
        os.write(message, 20, messageHalf);  // BODY#1
        sleep(os, 2000);

        os.write(message, 20 + messageHalf, message.length - messageHalf - 20);  // BODY #2
        os.flush();
        
        InputStream is = s.getInputStream();
        int result = is.read();

        s.close();
        
        if (result == 1) {
            return true;
        } else if (result == 0) {
            return false;
        }

        throw new IllegalStateException("Unexpected result: " + result);
    }

    private static boolean testOneAndHalfMessage() throws IOException {
        Socket s = new Socket("localhost", PORT);
        s.setSoTimeout(5000);
        OutputStream os = s.getOutputStream();

        byte[] message1 = createMessage(3);
        byte[] message2 = createMessage(5);

        byte[] totalMessage = Arrays.copyOf(message1, message1.length + message2.length);
        System.arraycopy(message2, 0, totalMessage, message1.length, message2.length);

        int oneAndHalf = message1.length + message2.length / 2;
        
        os.write(totalMessage, 0, oneAndHalf);  // 2/3 MESSAGE
        sleep(os, 4000);

        os.write(totalMessage, oneAndHalf, totalMessage.length - oneAndHalf);  // 1/3 MESSAGE

        os.flush();

        InputStream is = s.getInputStream();
        int result1 = is.read();
        int result2 = is.read();

        s.close();

        if (result1 == 1 && result2 == 1) {
            return true;
        } else if (result1 == 0 || result2 == 0) {
            return false;
        }

        throw new IllegalStateException("Unexpected result1: " + result1 + " result2: " + result2);
    }

    private static boolean testBigMessage() throws IOException {
        Socket s = new Socket("localhost", PORT);
        s.setSoTimeout(5000);
        OutputStream os = s.getOutputStream();

        byte[] message = createMessage(2048);
        System.out.println("[DEBUG] messageSize=" + message.length);
        os.write(message);
        os.flush();

        InputStream is = s.getInputStream();
        int result = is.read();

        s.close();

        if (result == 1) {
            return true;
        } else if (result == 0) {
            return false;
        }

        throw new IllegalStateException("Unexpected result: " + result);
    }
    
    private static boolean testOneAndHalfBigMessage() throws IOException {
        Socket s = new Socket("localhost", PORT);
        s.setSoTimeout(5000);
        OutputStream os = s.getOutputStream();

        byte[] message1 = createMessage(2048);
        byte[] message2 = createMessage(2048);

        byte[] totalMessage = Arrays.copyOf(message1, message1.length + message2.length);
        System.arraycopy(message2, 0, totalMessage, message1.length, message2.length);

        int oneAndHalf = message1.length + message2.length / 2;

        os.write(totalMessage, 0, oneAndHalf);  // 2/3 MESSAGE
        sleep(os, 4000);

        os.write(totalMessage, oneAndHalf, totalMessage.length - oneAndHalf);  // 1/3 MESSAGE

        os.flush();

        InputStream is = s.getInputStream();
        int result1 = is.read();
        int result2 = is.read();

        s.close();

        if (result1 == 1 && result2 == 1) {
            return true;
        } else if (result1 == 0 || result2 == 0) {
            return false;
        }

        throw new IllegalStateException("Unexpected result1: " + result1 + " result2: " + result2);
    }

    private static boolean testTinyRemainder() throws IOException {
        Socket s = new Socket("localhost", PORT);
        s.setSoTimeout(5000);
        OutputStream os = s.getOutputStream();

        byte[] message1 = createMessage(2048);
        byte[] message2 = createMessage(2048);

        byte[] totalMessage = Arrays.copyOf(message1, message1.length + message2.length);
        System.arraycopy(message2, 0, totalMessage, message1.length, message2.length);

        int offset = 0;

        while (offset < totalMessage.length) {
            int sendSize = Math.min(THREAD_LOCAL_BUFFER_SIZE - 100, totalMessage.length - offset);
            os.write(totalMessage, offset, sendSize); // send chunk
            sleep(os, 2000);

            offset += sendSize;
        }

        InputStream is = s.getInputStream();
        int result1 = is.read();
        int result2 = is.read();

        s.close();

        if (result1 == 1 && result2 == 1) {
            return true;
        } else if (result1 == 0 || result2 == 0) {
            return false;
        }

        throw new IllegalStateException("Unexpected result1: " + result1 + " result2: " + result2);
    }

    private static byte[] createMessage(int count) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        byte[] body = createBody(count);

        dos.writeInt(MAGIC_NUMBER);
        dos.writeInt(VERSION);
        dos.writeInt(3);
        dos.writeInt(body.length + 4);

        dos.writeInt(count);
        dos.write(body);
        dos.flush();

        byte[] message = baos.toByteArray();
        dos.close();
        return message;
    }
    
    private static byte[] createBody(int count) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        for (int i = 0; i < count; i++) {
            oos.writeObject("Param #" + i);
            oos.writeObject("Value #" + i);
        }

        oos.flush();

        byte[] body = baos.toByteArray();
        oos.close();

        return body;
    }

    private static ParserProtocolFilter createParserProtocolFilter() {
        return new ParserProtocolFilter() {

            @Override
            public ProtocolParser newProtocolParser() {
                return new GrizzlyMessageProtocolParser();
            }

        };
    }

    private static class ResultFilter implements ProtocolFilter {

        @Override
        public boolean execute(Context ctx) throws IOException {
            Object message = ctx.getAttribute(ProtocolParser.MESSAGE);
            byte[] b = new byte[1];
            b[0] = (byte) (message != null ? 1 : 0);
            ByteBuffer bb = ByteBuffer.wrap(b);
            SelectableChannel channel = ctx.getSelectionKey().channel();
            OutputWriter.flushChannel(channel, bb);
            return false;
        }

        @Override
        public boolean postExecute(Context ctx) throws IOException {
            return true;
        }
    }

    private static void sleep(OutputStream dos, int i) throws IOException {
        dos.flush();
        System.out.println("[DEBUG] Sleeping for " + i + " millis");
        try {
            Thread.sleep(i);
        } catch (InterruptedException e) {
        }
    }

    private static void enableDebug(boolean b) {
        if (b) {
            GrizzlyMessageProtocolParser.DEBUG_ENABLED = false;
            GrizzlyMessageProtocolParser.DEBUG_LEVEL = Level.INFO;
        }
    }
}
