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

package com.sun.enterprise.jxtamgmt;

import java.io.Serializable;

/**
 * Denotes a Cluster View Change Event. Provides the event and the system
 * advertisement in question.
 *
 * @author Shreedhar Ganapathy
 *         Date: Jun 29, 2006
 * @version $Revision$
 */
public class ClusterViewEvent implements Serializable {
    private final ClusterViewEvents event;
    private final SystemAdvertisement advertisement;

    /**
     * Constructor for the ClusterViewEvent object
     *
     * @param event         the Event
     * @param advertisement The system advertisement associated with the event
     */
    ClusterViewEvent(final ClusterViewEvents event,
                     final SystemAdvertisement advertisement) {

        if (event == null) {
            throw new IllegalArgumentException("Null event not allowed");
        }
        if (advertisement == null) {
            throw new IllegalArgumentException("Null advertisement not allowed");
        }
        this.event = event;
        this.advertisement = advertisement;
    }

    /**
     * Gets the event attribute of the ClusterViewEvent object
     *
     * @return The event value
     */
    public ClusterViewEvents getEvent() {
        return event;
    }

    /**
     * Gets the advertisement attribute of the ClusterViewEvent object
     *
     * @return The advertisement value
     */
    public SystemAdvertisement getAdvertisement() {
        return advertisement;
    }
}

