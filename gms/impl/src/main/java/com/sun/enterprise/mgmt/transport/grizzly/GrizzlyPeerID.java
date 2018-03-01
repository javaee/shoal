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

import java.io.Serializable;

/**
 * @author Bongjae Chang
 */
public class GrizzlyPeerID implements Serializable, Comparable<GrizzlyPeerID> {

    // TODO: rework this peerID so its serialized form is only a name based java.util.UUID and the mapping of the id
    //       to its network protocol is managed outside this class.
    //

    static final long serialVersionUID = 9093067296675025106L;

    //  For latest release, both the ip address (host) and tcpPort are not considered part of the identity of the
    //  Grizzly peerid.
    public final String host;  // due to Grizzly transport hack, this host is used to send a message to a member.
                               // this value is not considered part of data structure for identity.
    public final int tcpPort;  // due to Grizzly transport hack, this tcpport is used to send a message to a member.
                               // so this value must stay in the datastructure BUT is not considered part of it for
                               // comparison's sake.
    public final String multicastAddress;
    public final int multicastPort;
    transient private String toStringValue = null;

    public GrizzlyPeerID( String host, int tcpPort, String multicastAddress, int multicastPort ) {
        this.host = host;
        this.multicastAddress = multicastAddress;
        this.tcpPort = tcpPort;
        this.multicastPort = multicastPort;
    }

    public String getHost() {
        return host;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public int getMulticastPort() {
        return multicastPort;
    }

    public String getMulticastAddress() {
        return multicastAddress;
    }

    // NOTE: no longer include tcpport in this calculation nor the hash calculation.
    //       instance should be able to use a port within a range and still be considered same instance.
    @Override
    public boolean equals( Object other ) {
        if( other instanceof GrizzlyPeerID ) {
            GrizzlyPeerID otherPeerID = (GrizzlyPeerID)other;
            boolean multicastAddressCompare;
            if (multicastAddress == null)  {
                multicastAddressCompare = (multicastAddress == otherPeerID.multicastAddress);
            } else {
                multicastAddressCompare = multicastAddress.equals(otherPeerID.multicastAddress);
            }
            return multicastPort == otherPeerID.multicastPort  && multicastAddressCompare && host.equals(otherPeerID.getHost());
        } else {
            return false;
        }
    }

    // DO NOT INCLUDE HOST or TCP PORT in this calculation.
    //
    @Override
    public int hashCode() {
        int result = 17;
        if (multicastAddress != null) {
            result = 37 * result + multicastAddress.hashCode();
        }
        result = 37 * result + multicastPort;
        result = 37 * result + host.hashCode();
        return result;
    }

    @Override
    public String toString() {
       if (toStringValue == null) {
            toStringValue = host + ":" + tcpPort + ":" + multicastAddress + ":" + multicastPort;
        }
        return toStringValue;
    }

    @Override
    public int compareTo(GrizzlyPeerID other) {
        if (this == other) {
            return 0;
        }
        if (other == null) {
            return 1;
        }
        int result = multicastPort - other.multicastPort;
        if (result != 0) {
            return result;
        } else {
            return multicastAddress.compareTo(other.multicastAddress);
        }
    }
}
