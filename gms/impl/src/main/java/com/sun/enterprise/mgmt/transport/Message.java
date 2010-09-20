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

import java.nio.ByteBuffer;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * This represents internal message interface which is sent and received on the top of Shoal management module
 *
 * Currently, six message types exist.
 * <code>TYPE_CLUSTER_MANAGER_MESSAGE</code>, <code>TYPE_HEALTH_MONITOR_MESSAGE</code>, <code>TYPE_MASTER_NODE_MESSAGE</code> and <code>TYPE_MCAST_MESSAGE</code> are required.
 * <code>TYPE_PING_MESSAGE</code>, <code>TYPE_PONG_MESSAGE</code> are optional. i.g. JXTA transport layer doesn't need it.
 *
 * This message can contain various elements with key-value pair.
 * i.g. For the purpose of storing a source's {@link com.sun.enterprise.ee.cms.impl.base.PeerID} from which the message is sent,
 * <code>SOURCE_PEER_ID_TAG</code> key is used.
 *
 * @author Bongjae Chang
 */
public interface Message extends Serializable {

    static final long serialVersionUID = -8835127468511258700L;

    /**
     * The type of the {@link com.sun.enterprise.mgmt.ClusterManager} message
     */
    public static final int TYPE_CLUSTER_MANAGER_MESSAGE = 1;

    /**
     * The type of {@link com.sun.enterprise.mgmt.HealthMonitor}'s message
     *
     * Currently, {@link com.sun.enterprise.mgmt.HealthMessage} will be used for message elements
     */
    public static final int TYPE_HEALTH_MONITOR_MESSAGE = 2;

    /**
     * The type of the {@link com.sun.enterprise.mgmt.MasterNode} message
     */
    public static final int TYPE_MASTER_NODE_MESSAGE = 3;

    /**
     * The type of the {@link com.sun.enterprise.mgmt.LWRMulticast} message
     */
    public static final int TYPE_MCAST_MESSAGE = 4;

    /**
     * Currently, this type is used in only Grizzly transport layer
     * when {@link NetworkManager#isConnected(com.sun.enterprise.ee.cms.impl.base.PeerID)} send the ping message to suspicious member
     */
    public static final int TYPE_PING_MESSAGE = 5;

    /**
     * Currently, this type is used in only Grizzly transport layer in order to send own liveness to the sender when a ping message is received
     */
    public static final int TYPE_PONG_MESSAGE = 6;

    /**
     * The element's key for storing and getting source's {@link com.sun.enterprise.ee.cms.impl.base.PeerID}
     */
    public static final String SOURCE_PEER_ID_TAG = "sourcePeerId";

    /**
     * The element's key for storing and getting destination's {@link com.sun.enterprise.ee.cms.impl.base.PeerID}
     */
    public static final String TARGET_PEER_ID_TAG = "targetPeerId";

    /**
     * Initializes this message with initial type and key-value pair's map
     *
     * @param type message type
     * @param messages key-value pair's message
     * @throws IllegalArgumentException if the argument is not valid
     */
    public void initialize( final int type, final Map<String, Serializable> messages ) throws IllegalArgumentException;

    /**
     * Parses the message's header from given byte array
     *
     * @param bytes byte array which should be parsed
     * @param offset offset from which the message should be parsed
     * @return the message's length(body length) which this message contains
     * @throws IllegalArgumentException if the argument is not valid or an unexpected error occurs
     */
    public int parseHeader( final byte[] bytes, final int offset ) throws IllegalArgumentException;

    /**
     * Parses the message's header from given ByteBuffer
     *
     * @param byteBuffer ByteBuffer which should be parsed
     * @param offset offset from which the message should be parsed
     * @return the message's length(body length) which this message contains
     * @throws IllegalArgumentException if the argument is not valid or an unexpected error occurs
     */
    public int parseHeader( final Buffer buffer, final int offset ) throws IllegalArgumentException;

    /**
     * Parses the message's body from given byte array
     *
     * @param bytes byte array which should be parsed
     * @param offset offset from which the message should be parsed
     * @param length the message's length(body length)
     * @throws IllegalArgumentException if the argument is not valid or an unexpected error occurs
     * @throws MessageIOException if an I/O error occurs
     */
    public void parseMessage( final byte[] bytes, final int offset, final int length ) throws IllegalArgumentException, MessageIOException;

    /**
     * Parses the message's body from given ByteBuffer
     *
     * @param byteBuffer ByteBuffer which should be parsed
     * @param offset offset from which the message should be parsed
     * @param length the message's length(body length)
     * @throws IllegalArgumentException if the argument is not valid or an unexpected error occurs
     * @throws MessageIOException if an I/O error occurs
     */
    public void parseMessage( final Buffer buffer, final int offset, final int length ) throws IllegalArgumentException, MessageIOException;

    /**
     * Returns the message's version
     *
     * @return message version
     */
    public int getVersion();

    /**
     * Returns the message's type
     * i.g. <code>TYPE_CLUSTER_MANAGER_MESSAGE</code> or <code>TYPE_HEALTH_MONITOR_MESSAGE</code>'s integer value or etc...
     *
     * @return message type
     */
    public int getType();

    /**
     * Adds a special element to this message with key-value pair
     *
     * @param key key with which the specified value is to be associated
     * @param value serializable value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> if there was no mapping for <tt>key</tt>
     */
    public Object addMessageElement( final String key, final Serializable value );

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if this message contains no mapping for the key
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or {@code null} if this message contains no mapping for the key
     */
    public Object getMessageElement( final String key );

    /**
     * Removes the mapping for a key from this message if it is present
     * 
     * @param key key whose mapping is to be removed from the message
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> if there was no mapping for <tt>key</tt>
     */
    public Object removeMessageElement( final String key );

    /**
     * Returns a {@link Set} element view of the mappings contained in this message
     *
     * @return a set element view of the mappings contained in this message
     */
    public Set<Map.Entry<String, Serializable>> getMessageElements();

    /**
     * Returns a {@link ByteBuffer} of this message
     *
     * @return a ByteBuffer
     * @throws MessageIOException if an I/O error occurs
     */
    public ByteBuffer getPlainByteBuffer() throws MessageIOException;

    /**
     * Returns a byte array of this message
     *
     * @return byte array
     * @throws MessageIOException if an I/O error occurs
     */
    public byte[] getPlainBytes() throws MessageIOException;
}
