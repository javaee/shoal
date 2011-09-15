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

package org.shoal.test.maptest;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.shoal.ha.mapper.DefaultKeyMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit test for simple App.
 */
public class KeyMapperTest
        extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public KeyMapperTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(KeyMapperTest.class);
    }


    private static void mapStringKeyTest(DefaultKeyMapper km) {
        String[] keys = new String[]{"Key0", "Key1", "Key2"};

        System.out.print("{");
        String delim = "";
        for (String key : keys) {
            System.out.print(delim + key + " => " + km.getMappedInstance("g1", key));
            delim = ", ";
        }

        System.out.println("}");
    }

    public void testRegisterOnly() {
        DefaultKeyMapper km = new DefaultKeyMapper("n0", "g1");

        String[] aliveInstances = new String[] {"n0", "n1"};
        String[] previousView = new String[] {};
        km.onViewChange("n0", Arrays.asList(aliveInstances),
                Arrays.asList(previousView), true);

        assert (true);
    }

    public void testUnregisterOnly() {
        DefaultKeyMapper km = new DefaultKeyMapper("n0", "g1");
        String[] aliveInstances = new String[] {};
        String[] previousView = new String[] {"n0", "n1"};
        km.onViewChange("n0", Arrays.asList(aliveInstances),
                Arrays.asList(previousView), false);
        
        assert (true);
    }

    public void testRegisterAndUnregister() {
        DefaultKeyMapper km = new DefaultKeyMapper("n0", "g1");

        String[] aliveInstances = new String[] {"n0", "n1"};
        String[] previousView = new String[] {};
        km.onViewChange("n0", Arrays.asList(aliveInstances),
                Arrays.asList(previousView), true);
        aliveInstances = new String[] {};
        previousView = new String[] {"n0", "n1"};
        km.onViewChange("n1", Arrays.asList(aliveInstances),
                Arrays.asList(previousView), false);

        assert (true);
    }

    
    public void testEmptyMapTest() {
        DefaultKeyMapper km = new DefaultKeyMapper("n0", "g1");
        String mappedInstance = km.getMappedInstance("g1", "Key1");
        String[] replicaInstances = (km.findReplicaInstance("g1", "Key1", null));
        
        assert (mappedInstance == null);
        assert (replicaInstances.length == 1);

        System.out.println("* Test[testEmptyMapTest] => " +
                mappedInstance + "; " + replicaInstances.length + "; ");
    }

    public void testRegisterAndTest() {
        DefaultKeyMapper km = new DefaultKeyMapper("n0", "g1");

        String[] aliveInstances = new String[] {"n0", "n1"};
        String[] previousView = new String[] {};
        km.onViewChange("n0", Arrays.asList(aliveInstances),
                Arrays.asList(previousView), true);
        mapStringKeyTest(km);

        aliveInstances = new String[] {"n0", "n1", "n2", "inst1", "instance2", "someInstance"};
        previousView = new String[] {};
        km.onViewChange("**NON-EXISTENT-INSTANCE", Arrays.asList(aliveInstances),
                Arrays.asList(previousView), true);
        mapStringKeyTest(km);

        assert (true);
    }

    public void testMappedToMyself() {
        DefaultKeyMapper km = new DefaultKeyMapper("inst0", "g1");

        String[] aliveInstances = new String[10];
        for (int i=0; i<10; i++) {aliveInstances[i] = "inst"+i;}

        String[] previousView = new String[] {};
        km.onViewChange("inst0", Arrays.asList(aliveInstances),
                Arrays.asList(previousView), true);

        Integer[] keys = new Integer[14];
        for (int i=0; i<14; i++) {keys[i] = i;}

        boolean result = true;
        for (int i = 0; i < keys.length; i++) {
            if (km.getMappedInstance("g1", keys[i]).equals("inst0")) {
                result = false;
                System.err.println("For key: " + keys[i] + " was mapped to me!!");
            }
        }
        System.out.println("* Test[testMappedToMyself] => " + result);
        assert (result);
    }

    public void testReplicaUponFailure() {
        DefaultKeyMapper km1 = new DefaultKeyMapper("inst2", "g1");

        String[] aliveInstances = new String[10];
        for (int i=0; i<10; i++) {aliveInstances[i] = "inst"+i;}

        String[] previousView = new String[] {};
        km1.onViewChange("inst2", Arrays.asList(aliveInstances),
                Arrays.asList(previousView), true);

        String[] keys = new String[16];
        String[] replicaInstanceNames = new String[16];

        int count = keys.length;

        km1.printMemberStates("Before MAP");
        for (int i = 0; i < count; i++) {
            keys[i] = "Key-" + Math.random();
            replicaInstanceNames[i] = km1.getMappedInstance("g1", keys[i]);
        }


        DefaultKeyMapper km4 = new DefaultKeyMapper("inst4", "g1");
        List<String> currentMembers = new ArrayList();
        currentMembers.addAll(Arrays.asList(aliveInstances));
        currentMembers.remove("inst2");
        km4.onViewChange("inst2", currentMembers, Arrays.asList(aliveInstances), false);

        km4.printMemberStates("!@#$#$#$%^%^%^%%&");
        boolean result = true;
        for (int i = 0; i < keys.length; i++) {
            String mappedInstanceName = km4.findReplicaInstance("g1", keys[i], null)[0];
            if (!mappedInstanceName.equals(replicaInstanceNames[i])) {
                result = false;
                System.err.println("For key: " + keys[i] + " exptected Replica was: " + replicaInstanceNames[i] +
                        " but got mapped to: " + mappedInstanceName);
            } else {
                System.out.println("**KeyMapperTest:testReplicaUponFailure; expected: "
                        + replicaInstanceNames[i] + " and got: " + mappedInstanceName);
            }
        }
        System.out.println("* Test[testReplicaUponFailure] => " + result);
        assert (result);
    }

    /*
    public void testReplicaUponFailureFromAllOtherNodes() {

        String[] aliveInstances = new String[10];
        for (int i=0; i<10; i++) {aliveInstances[i] = "inst"+i;}

        String[] previousView = new String[] {};

        int sz = 10;
        DefaultKeyMapper<String>[] mappers = new DefaultKeyMapper[sz];
        for (int i = 0; i < sz; i++) {
            mappers[i] = new DefaultKeyMapper("n"+i, "g1");
            mappers[i].onViewChange(Arrays.asList(aliveInstances),
                Arrays.asList(previousView), true);
        }

        String[] keys = new String[16];
        String[] replicaInstanceNames = new String[16];

        int count = keys.length;

        for (int i = 0; i < count; i++) {
            keys[i] = "Key-" + Math.random();
            replicaInstanceNames[i] = mappers[0].getMappedInstance("g1", keys[i]);
        }

        for (int i = 0; i < sz; i++) {
            List<String> currentMembers = new ArrayList();
            currentMembers.addAll(Arrays.asList(aliveInstances));
            currentMembers.remove("inst5");
            mappers[i].onViewChange(currentMembers, Arrays.asList(aliveInstances), false);
        }

        boolean result = true;
        for (int id = 1; id < sz; id++) {
            for (int i = 0; i < keys.length; i++) {
                String mappedInstanceName = mappers[id].findReplicaInstance("g1", keys[i]);
                if (!mappedInstanceName.equals(replicaInstanceNames[i])) {
                    result = false;
                    System.err.println("For key: " + keys[i] + " exptected Replica was: " + replicaInstanceNames[i] +
                            " but got mapped to: " + mappedInstanceName);
                } else {
//                    System.out.println("**KeyMapperTest:testReplicaUponFailure; Mapper[" + id + "]: expected: "
//                            + replicaInstanceNames[i] + " and got: " + mappedInstanceName);
                }
            }
        }
        System.out.println("* Test[testReplicaUponFailureFromAllOtherNodes] => " + result);
        assert (result);
    }
    */

    /*
    public void testReplicaUponRestart() {
        Integer[] keys = new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
        String[] expectedReplica = new String[]{
                "n0", "n1", "n2", "n4", "n4", "n5",
                "n0", "n1", "n2", "n5", "n4", "n5",
                "n0", "n1", "n2"};

        boolean result = true;




        DefaultKeyMapper mapper = new DefaultKeyMapper("n3", "g1");
        mapper.registerInstance("n0");
        mapper.registerInstance("n1");
        mapper.registerInstance("n2");
        mapper.registerInstance("n4");
        mapper.registerInstance("n5");
        mapper.removeInstance("n3");
        mapper.registerInstance("n3");
        for (int i = 0; i < keys.length; i++) {
            if (!mapper.findReplicaInstance("g1", keys[i]).equals(expectedReplica[i])) {
                result = false;
                System.err.println("testReplicaUponRestart:: For key: " + keys[i] + " exptected Replica was: " + expectedReplica[i] +
                        " but got mapped to: " + mapper.findReplicaInstance("g1", keys[i]));
            }
        }


        System.out.println("* Test[testReplicaUponRestart] => " + result);
        assert (result);
    }
    */

}
