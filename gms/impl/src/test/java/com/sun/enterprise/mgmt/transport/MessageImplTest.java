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


import junit.framework.TestCase;
/**
 *
 * @author sdimilla
 */
public class MessageImplTest extends TestCase {

    Message message = null;
    Message emptyMessage = null;
    static final String key1 = "test_key1";
    static final String value1 = new String("test message1");
    static final String key2 = "test_key2";
    static final String value2 = new String("test message2");

    public MessageImplTest( String testName )
    {
        super( testName );
    }

    private void mySetUp() {
        emptyMessage = new MessageImpl();
        message = new MessageImpl(Message.TYPE_CLUSTER_MANAGER_MESSAGE);
        message.addMessageElement(key1, value1);
        message.addMessageElement(key2, value2);
        message.addMessageElement(Message.SOURCE_PEER_ID_TAG, "fromMember");
        message.addMessageElement(Message.TARGET_PEER_ID_TAG, "targetMember");
    }

    /**
     * Test of initial message setup
     */

    public void testInitialMessage() {
        mySetUp();
        System.out.println("initialized message = " + message);
        assertNotNull(message.getMessageElement(key1));
        assertNotNull(message.getMessageElement(key2));
        assertNotNull(message.getMessageElement(message.SOURCE_PEER_ID_TAG));
        assertNotNull(message.getMessageElement(message.TARGET_PEER_ID_TAG));
        assertEquals(message.getType(), Message.TYPE_CLUSTER_MANAGER_MESSAGE);
        assertEquals((String) message.getMessageElement(key1), value1);
        assertEquals((String) message.getMessageElement(key2), value2);
        assertEquals((String) message.getMessageElement(message.SOURCE_PEER_ID_TAG), "fromMember");
        assertEquals((String) message.getMessageElement(message.TARGET_PEER_ID_TAG), "targetMember");

    }


    public void testGetVersionFromEmptyMessage() {
                mySetUp();

        assertEquals(emptyMessage.getVersion(), 0);
    }


    public void testGetTypeFromEmptyMessage() {
                mySetUp();

        assertEquals(emptyMessage.getType(), 0);
    }

   
    public void testGetVersion() {
                mySetUp();

        assertEquals(message.getVersion(), 1);
    }

    
    public void testGetType() {
                mySetUp();

        // tested in testInitialMessage() above
    }

 
    public void testGetStringType() {
                mySetUp();

        assertEquals(MessageImpl.getStringType(Message.TYPE_CLUSTER_MANAGER_MESSAGE), "CLUSTER_MANAGER_MESSAGE");
        assertEquals(MessageImpl.getStringType(Message.TYPE_HEALTH_MONITOR_MESSAGE), "HEALTH_MONITOR_MESSAGE");
        assertEquals(MessageImpl.getStringType(Message.TYPE_MASTER_NODE_MESSAGE), "MASTER_NODE_MESSAGE");
        assertEquals(MessageImpl.getStringType(Message.TYPE_MCAST_MESSAGE), "MCAST_MESSAGE");
        assertEquals(MessageImpl.getStringType(Message.TYPE_PING_MESSAGE), "PING_MESSAGE");
        assertEquals(MessageImpl.getStringType(Message.TYPE_PONG_MESSAGE), "PONG_MESSAGE");
        assertEquals(MessageImpl.getStringType(999), "UNKNOWN_MESSAGE(999)");
    }

    /**
     * Test of getPlainBytes()
     */
   
    public void testGetPlainBytes() {
                mySetUp();

        Message message2 = new MessageImpl();
        try {
            byte[] plainBytes = message.getPlainBytes();
            int messageLen = message2.parseHeader(plainBytes, 0);
            message2.parseMessage(plainBytes, MessageImpl.HEADER_LENGTH, messageLen);
            assertNotNull(message2.getMessageElement(key1));
            assertNotNull(message2.getMessageElement(key2));
            assertNotNull(message2.getMessageElement(message.SOURCE_PEER_ID_TAG));
            assertNotNull(message2.getMessageElement(message.TARGET_PEER_ID_TAG));
            assertEquals((String) message2.getMessageElement(key1), value1);
            assertEquals((String) message2.getMessageElement(key2), value2);
            assertEquals((String) message.getMessageElement(message2.SOURCE_PEER_ID_TAG), "fromMember");
            assertEquals((String) message.getMessageElement(message2.TARGET_PEER_ID_TAG), "targetMember");
            System.out.println("message from bytes = " + message2);
        } catch (Exception ex) {
            fail("Exception occurred: " + ex);
        }
    }

    /**
     * Test of remove message from bytes()
     */
    
    public void testRemoveFromBytes() {
                mySetUp();

        message.removeMessageElement(key2);
        assertNull(message.getMessageElement(key2));
        Message message3 = new MessageImpl();
        try {
            byte[] plainBytes = message.getPlainBytes();
            int messageLen = message3.parseHeader(plainBytes, 0);
            message3.parseMessage(plainBytes, MessageImpl.HEADER_LENGTH, messageLen);
            System.out.println("removed message from bytes = " + message3);
        } catch (Exception ex) {
            fail("Exception occurred: " + ex);
        }
    }

    /**
     * Test of remove message from bytes()
     */
    
    public void testMessageFromByteBuffer() {
                mySetUp();

        try {
            byte[] plainByteBuffer = message.getPlainByteBuffer().array();
            Message message4 = new MessageImpl();
            int messageLen = message4.parseHeader(plainByteBuffer, 0);
            message4.parseMessage(plainByteBuffer, MessageImpl.HEADER_LENGTH, messageLen);
            assertNotNull(message4.getMessageElement(key1));
            assertNotNull(message4.getMessageElement(message.SOURCE_PEER_ID_TAG));
            assertNotNull(message4.getMessageElement(message.TARGET_PEER_ID_TAG));
            assertEquals((String) message4.getMessageElement(key1), value1);
            assertEquals((String) message4.getMessageElement(key2), value2);
            assertEquals((String) message.getMessageElement(message4.SOURCE_PEER_ID_TAG), "fromMember");
            assertEquals((String) message.getMessageElement(message4.TARGET_PEER_ID_TAG), "targetMember");
            System.out.println("message from byte buffer = " + message4);
        } catch (Exception ex) {
            fail("Exception occurred: " + ex);
        }
    }

    /**
     * Test of remove message from bytes()
     */
   
    public void testRemoveFromByteBuffer() {
                mySetUp();

        message.removeMessageElement(key1);
        assertNull(message.getMessageElement(key1));
        Message message3 = new MessageImpl();
        try {
            byte[] plainByteBuffer = message.getPlainByteBuffer().array();
            Message message5 = new MessageImpl();
            int messageLen = message5.parseHeader(plainByteBuffer, 0);
            message5.parseMessage(plainByteBuffer, MessageImpl.HEADER_LENGTH, messageLen);
            System.out.println("removed message from byte buffer = " + message5);
        } catch (Exception ex) {
            fail("Exception occurred: " + ex);
        }
    }

    /**
     * Test of remove message from bytes()
     */
   
    public void testAddLargeAppMessage() {
                mySetUp();

        message.addMessageElement("APPMESSAGE", new byte[MessageImpl.getMaxMessageLength()+1]);
        try {
            byte[] plainByteBuffer = message.getPlainByteBuffer().array();
            Message message6 = new MessageImpl();
            int messageLen = message6.parseHeader(plainByteBuffer, 0);
            message6.parseMessage(plainByteBuffer, MessageImpl.HEADER_LENGTH, messageLen);
            fail("No Exception thrown when adding a message too big");
        } catch (Exception ex) {
        }
    }
}
