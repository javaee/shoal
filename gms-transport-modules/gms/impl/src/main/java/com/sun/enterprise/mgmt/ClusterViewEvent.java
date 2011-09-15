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

package com.sun.enterprise.mgmt;

import com.sun.enterprise.ee.cms.impl.base.SystemAdvertisement;

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
    static final long serialVersionUID = 4125228994646649851L;

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

