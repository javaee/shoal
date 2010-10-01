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
import java.util.Comparator;

/**
 * @author Bongjae Chang
 */
public class GrizzlyPeerID implements Serializable, Comparable<GrizzlyPeerID> {

    static final long serialVersionUID = 9093067296675025106L;

    public final String host;
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
            if( multicastPort == otherPeerID.multicastPort  &&  multicastAddress.equals(otherPeerID.multicastAddress)) {
                if( host == otherPeerID.host )
                    return true;
                if( host != null && host.equals( otherPeerID.host ) )
                    return true;
                return false;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    // DO NOT INCLUDE TCP PORT in this calculation.
    //
    @Override
    public int hashCode() {
        int result = 17;
        if( host != null )
            result = 37 * result + host.hashCode();
        result = 37 * result + multicastAddress.hashCode();
        result = 37 * result + multicastPort;
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
        int result = 0;
        if (this == other) {
            return 0;
        }
        if (other == null) {
            return 1;
        }
        if (host != null && other.host != null) {
            result = host.compareTo(other.host);
            if (result != 0) {
                return result;
            }
        }
        result = multicastPort - other.getMulticastPort();
        if (result != 0) {
            return result;
        }
        result = multicastAddress.compareTo(other.getMulticastAddress());
        //assert (result == 0 ? hashCode() == other.hashCode() : true);
        //assert (result == 0 ? equals(other) : !this.equals(other));
        return result;
    }
}
