/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.ee.cms.core.GMSException;
import com.sun.enterprise.ee.cms.core.GroupManagementService;
import java.util.HashMap;
import java.util.Map;
import com.sun.enterprise.ee.cms.impl.base.CustomTagNames;
import com.sun.enterprise.ee.cms.impl.base.SystemAdvertisement;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author sdimilla
 */
public class ClusterManagerTest extends TestCase  {

    private ClusterManager manager = null;
    private ClusterManager watchdogManager = null;
    static final String name = "junit_instanceName";
    static final String groupName = "junit_groupName";
    static final String watchdogGroupName = "junit_watchdog_groupName";
    Map props = null;
    Map<String, String> idMap = null;
    List<ClusterMessageListener> mListeners = null;
    List<ClusterViewEventListener> vListeners = null;


        public ClusterManagerTest( String testName )
    {
        super( testName );
    }

    private static Map<String, String> getIdMap(String memberType, String groupName) {
        final Map<String, String> idMap = new HashMap<String, String>();
        idMap.put(CustomTagNames.MEMBER_TYPE.toString(), memberType);
        idMap.put(CustomTagNames.GROUP_NAME.toString(), groupName);
        idMap.put(CustomTagNames.START_TIME.toString(), Long.valueOf(System.currentTimeMillis()).toString());
        return idMap;
    }

    //TODO: NOT YET IMPLEMENTED
    private static Map getPropsForTest() {
        return new HashMap();
    }


    public void mySetUp() throws GMSException {
        props = getPropsForTest();
        idMap = getIdMap(name, groupName);
        vListeners =
                new ArrayList<ClusterViewEventListener>();
        mListeners =
                new ArrayList<ClusterMessageListener>();
        vListeners.add(
                new ClusterViewEventListener() {

                    public void clusterViewEvent(
                            final ClusterViewEvent event,
                            final ClusterView view) {
                        System.out.println("clusterViewEvent:event.getEvent=" + event.getEvent().toString());
                        System.out.println("clusterViewEvent:event.getAdvertisement()=" + event.getAdvertisement().toString());
                        System.out.println("clusterViewEvent:view.getPeerNamesInView=" + view.getPeerNamesInView().toString());
                    }
                });
        mListeners.add(
                new ClusterMessageListener() {

                    public void handleClusterMessage(
                            SystemAdvertisement id, final Object message) {
                        System.out.println("ClusterMessageListener:id.getName=" + id.getName());
                        System.out.println("ClusterMessageListener:message.toString=" + message.toString());
                    }
                });
        manager = new ClusterManager(groupName,
                name,
                idMap,
                props,
                vListeners,
                mListeners);
    }

    /**
     * 
     */

    public void testStartRunStopClusterManager() throws GMSException {
        mySetUp();

        assertFalse(manager.isGroupStartup());
        manager.start();
        assertEquals(manager.getGroupName(),groupName);
        assertEquals(manager.getID(name).getInstanceName(),name);
        assertEquals(manager.getInstanceName(),name);
        assertEquals(manager.getPeerID().getInstanceName(),name);
        try {
            Thread.sleep(10000); // long enough to announce being master.
        } catch (InterruptedException ie) {
        }
        manager.stop(false);
        assertTrue(manager.isStopping());
        manager.isGroupStartup();
    }

    /**
     *
     */

    public void testisWatchdogFalse() throws GMSException  {
                mySetUp();

        assertFalse(manager.isWatchdog());
    }

    /**
     *
     */

    public void testisWatchdogTrue() throws GMSException {
                mySetUp();

        watchdogManager = new ClusterManager(groupName,
                GroupManagementService.MemberType.WATCHDOG.toString(),
                getIdMap(GroupManagementService.MemberType.WATCHDOG.toString(), groupName),
                null,
                null,
                null);
        assertTrue(watchdogManager.isWatchdog());
    }
}
