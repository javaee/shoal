/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.enterprise.mgmt.transport;

import java.nio.ByteBuffer;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

/**
 *
 * @author sdimilla
 */
public class MessageImplTest {

    Message message = null;
    static final String key1 = "test_key1";
    static final String value1 = new String("test message1");
    static final String key2 = "test_key2";
    static final String value2 = new String("test message2");

    public MessageImplTest() {
    }

    @Before
    public void setUp() throws Exception {
        message = new MessageImpl(Message.TYPE_CLUSTER_MANAGER_MESSAGE);
        message.addMessageElement(key1, value1);
        message.addMessageElement(key2, value2);
        message.addMessageElement(Message.SOURCE_PEER_ID_TAG, "fromMember");
        message.addMessageElement(Message.TARGET_PEER_ID_TAG, "targetMember");
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test of initial message setup
     */
    @Test
    public void testInitialMessage() {
        System.out.println("initialized message = " + message);
        assertNotNull(message.getMessageElement(key1));
        assertNotNull(message.getMessageElement(key2));
        assertNotNull(message.getMessageElement(message.SOURCE_PEER_ID_TAG));
        assertNotNull(message.getMessageElement(message.TARGET_PEER_ID_TAG));
        assertEquals((String) message.getMessageElement(key1), value1);
        assertEquals((String) message.getMessageElement(key2), value2);
        assertEquals((String) message.getMessageElement(message.SOURCE_PEER_ID_TAG), "fromMember");
        assertEquals((String) message.getMessageElement(message.TARGET_PEER_ID_TAG), "targetMember");

    }

    /**
     * Test of getPlainBytes()
     */
    @Test
    public void testGetPlainBytes() {
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
    @Test
    public void testRemoveFromBytes() {
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
    @Test
    public void testMessageFromByteBuffer() {
        try {
            ByteBuffer plainByteBuffer = message.getPlainByteBuffer();
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
    @Test
    public void testRemoveFromByteBuffer() {
        message.removeMessageElement(key1);
        assertNull(message.getMessageElement(key1));
        Message message3 = new MessageImpl();
        try {
            ByteBuffer plainByteBuffer = message.getPlainByteBuffer();
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
    @Test
    public void testAddLargeAppMessage() {
        message.addMessageElement("APPMESSAGE", new byte[8192]);
        try {
            ByteBuffer plainByteBuffer = message.getPlainByteBuffer();
            Message message6 = new MessageImpl();
            int messageLen = message6.parseHeader(plainByteBuffer, 0);
            message6.parseMessage(plainByteBuffer, MessageImpl.HEADER_LENGTH, messageLen);
            fail("No Exception thrown when adding a message too big");
        } catch (Exception ex) {
        }
    }
}
