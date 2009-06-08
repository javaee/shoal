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

import java.nio.ByteBuffer;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * @author Bongjae Chang
 */
public interface Message extends Serializable {

    static final long serialVersionUID = -8835127468511258700L;

    public static final int TYPE_CLUSTER_MANAGER_MESSAGE = 1;

    public static final int TYPE_HEALTH_MONITOR_MESSAGE = 2;

    public static final int TYPE_MASTER_NODE_MESSAGE = 3;

    public static final int TYPE_MCAST_MESSAGE = 4;

    public static final int TYPE_PING_MESSAGE = 5;

    public static final int TYPE_PONG_MESSAGE = 6;

    public static final String SOURCE_PEER_ID_TAG = "sourcePeerId";
    
    public static final String TARGET_PEER_ID_TAG = "targetPeerId";

    public void initialize( final int type, final Map<String, Serializable> messages ) throws IllegalArgumentException;

    public int parseHeader( final byte[] bytes, final int offset ) throws IllegalArgumentException;

    public int parseHeader( final ByteBuffer byteBuffer, final int offset ) throws IllegalArgumentException;

    public void parseMessage( final byte[] bytes, final int offset, final int length ) throws IllegalArgumentException, MessageIOException;

    public void parseMessage( final ByteBuffer byteBuffer, final int offset, final int length ) throws IllegalArgumentException, MessageIOException;

    public int getVersion();

    public int getType();

    public Object addMessageElement( final String key, final Serializable value );

    public Object getMessageElement( final String key );

    public Object removeMessageElement( final String key );

    public Set<Map.Entry<String, Serializable>> getMessageElements();

    public ByteBuffer getPlainByteBuffer() throws MessageIOException;

    public byte[] getPlainBytes() throws MessageIOException;
}
