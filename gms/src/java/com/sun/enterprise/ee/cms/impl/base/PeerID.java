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

package com.sun.enterprise.ee.cms.impl.base;

import java.io.Serializable;

/**
 * @author Bongjae Chang
 */
public class PeerID<T extends Serializable> implements Serializable, Comparable<PeerID> {

    static final long serialVersionUID = 2618091647571033721L;

    public static final PeerID<Serializable> NULL_PEER_ID = new PeerID<Serializable>( null, null, null );

    private final T uniqueID;
    private final String groupName;
    private final String instanceName;

    public PeerID( T uniqueID, String groupName, String instanceName ) {
        this.uniqueID = uniqueID;
        this.groupName = groupName;
        this.instanceName = instanceName;
    }

    public T getUniqueID() {
        return uniqueID;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public boolean equals( Object other ) {
        if( other instanceof PeerID ) {
            boolean equal = true;
            PeerID otherPeerID = (PeerID)other;
            if( uniqueID != null && uniqueID.equals( otherPeerID.getUniqueID() ) ) {
                if( groupName != null )
                    equal = groupName.equals( otherPeerID.getGroupName() );
                if( !equal )
                    return false;
                if( instanceName != null )
                    equal = instanceName.equals( otherPeerID.getInstanceName() );
                if( !equal )
                    return false;
            } else {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    public int hashCode() {
        int result = 17;
        if( uniqueID != null )
            result = 37 * result + uniqueID.hashCode();
        if( groupName != null )
            result = 37 * result + groupName.hashCode();
        if( instanceName != null )
            result = 37 * result + instanceName.hashCode();
        return result;
    }

    public String toString() {
        String uniqueIDString = ( ( uniqueID == null ) ? "null" : uniqueID.toString() );
        return uniqueIDString + ":" + groupName + ":" + instanceName;
    }

    public int compareTo( PeerID other ) {
        if( this == other )
            return 0;
        if( other == null )
            return 1;
        return toString().compareTo( other.toString() );
    }
}
