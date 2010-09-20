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

import com.sun.enterprise.ee.cms.impl.base.PeerID;

import java.util.EventObject;

/**
 * This class represents a received message event
 *
 * Management modules will use this message event in order to process a received network packet internally 
 *
 * @author Bongjae Chang
 */
public class MessageEvent extends EventObject {

    /**
     * The received {@link Message}
     */
    private final Message message;

    /**
     * The received message's source {@link PeerID}
     */
    private final PeerID sourcePeerID;

    /**
     * The received message's destination {@link PeerID}
     */
    private final PeerID targetPeerID;

    /**
     * Creates a new event
     *
     * @param source  The object on which the message was received.
     * @param message The message object
     * @param sourcePeerID source peer id
     * @param targetPeerID target peer id
     */
    public MessageEvent( Object source, Message message, PeerID sourcePeerID, PeerID targetPeerID ) {
        super( source );
        this.message = message;
        this.sourcePeerID = (PeerID)sourcePeerID;
        this.targetPeerID = (PeerID)targetPeerID;
    }

    /**
     * Returns the message associated with the event
     *
     * @return message
     */
    public Message getMessage() {
        return message;
    }

    /**
     * Returns the source peer id from which this message is sent
     *
     * @return peer id
     */
    public PeerID getSourcePeerID() {
        return sourcePeerID;
    }

    /**
     * Returns the target peer id to which this message is sent 
     *
     * @return peer id
     */
    public PeerID getTargetPeerID() {
        return targetPeerID;
    }
}
