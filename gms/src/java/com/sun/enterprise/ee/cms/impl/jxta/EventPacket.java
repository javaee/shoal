/*
* The contents of this file are subject to the terms
* of the Common Development and Distribution License
* (the License).  You may not use this file except in
* compliance with the License.
*
* You can obtain a copy of the license at
* https://shoal.dev.java.net/public/CDDLv1.0.html
*
* See the License for the specific language governing
* permissions and limitations under the License.
*
* When distributing Covered Code, include this CDDL
* Header Notice in each file and include the License file
* at
* If applicable, add the following below the CDDL Header,
* with the fields enclosed by brackets [] replaced by
* you own identifying information:
* "Portions Copyrighted [year] [name of copyright owner]"
*
* Copyright 2006 Sun Microsystems, Inc. All rights reserved.
*/
package com.sun.enterprise.ee.cms.impl.jxta;

import com.sun.enterprise.jxtamgmt.ClusterView;
import com.sun.enterprise.jxtamgmt.ClusterViewEvents;
import com.sun.enterprise.jxtamgmt.SystemAdvertisement;

/**
 * @author Shreedhar Ganapathy
 *         Date: Jun 27, 2006
 * @version $Revision$
 */
public class EventPacket {
    private final ClusterViewEvents clusterViewEvent;
    private final SystemAdvertisement systemAdvertisement;
    private final ClusterView clusterView;

    public EventPacket(
            final ClusterViewEvents clusterViewEvent,
            final SystemAdvertisement systemAdvertisement,
            final ClusterView clusterView) {
        this.clusterViewEvent = clusterViewEvent;
        this.systemAdvertisement = systemAdvertisement;
        this.clusterView = clusterView;
    }

    public ClusterViewEvents getClusterViewEvent() {
        return clusterViewEvent;
    }

    public SystemAdvertisement getSystemAdvertisement() {
        return systemAdvertisement;
    }

    public ClusterView getClusterView() {
        return clusterView;
    }
}
