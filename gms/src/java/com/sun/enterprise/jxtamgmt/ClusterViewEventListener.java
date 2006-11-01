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

import java.util.EventListener;

/**
 * The listener interface for receiving ClusterViewManager events.
 * <p/>
 * The following example illustrates how to implement a {@link com.sun.enterprise.jxtamgmt.ClusterViewEventListener}:
 * <pre><tt>
 * ClusterViewEventListener myListener = new ClusterViewEventListener() {
 * <p/>
 *   public void clusterViewEvent(int event, , SystemAdvertisement advertisement) {
 *        if (event == ClusterViewManager.ADD_EVENT) {
 *          .....
 *        }
 *   }
 * }
 * <p/>
 * </tt></pre>
 */

public interface ClusterViewEventListener extends EventListener {

    /**
     * Called when a cluster view event occurs.
     *
     * @param event       The event that occurred.
     * @param clusterView the current membership snapshot after the event.
     */
    void clusterViewEvent(ClusterViewEvent event,
                          ClusterView clusterView);
}
