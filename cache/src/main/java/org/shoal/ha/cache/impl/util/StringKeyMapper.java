/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
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

package org.shoal.ha.cache.impl.util;

import org.shoal.ha.group.GroupMemberEventListener;
import org.shoal.ha.mapper.KeyMapper;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Mahesh Kannan
 */
public class StringKeyMapper<K>
        implements KeyMapper<K>, GroupMemberEventListener {

    private String myName;

    private String groupName;

    private boolean includeMe;

    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    private ReentrantReadWriteLock.ReadLock rLock;

    private ReentrantReadWriteLock.WriteLock wLock;

    private volatile TreeSet<String> currentMemberSet = new TreeSet<String>();

    private volatile String[] members;

    private volatile String[] otherMembers;


    public StringKeyMapper(String myName, String groupName) {
        this.myName = myName;
        this.groupName = groupName;

        rLock = rwLock.readLock();
        wLock = rwLock.writeLock();
    }

    @Override
    public String getMappedInstance(String groupName, K key1) {
        rLock.lock();
        try {
            int hc = Math.abs(getDigestHashCode(key1.toString()));
            System.out.println("Mapping Key: " + key1 + " => " + hc + ";   " + members[hc % (members.length)]);
            return members[hc % (members.length)];
        } finally {
            rLock.unlock();
        }
    }

//    @Override
//    public String[] getMappedInstances(String groupName, K key, int count) {
//        return new String[]{getMappedInstance(groupName, key)};
//    }

    @Override
    public void memberReady(String instanceName, String groupName) {
        if (this.groupName.equals(groupName)) {
            registerInstance(instanceName);
        }
    }

    @Override
    public void memberLeft(String instanceName, String groupName, boolean isShutdown) {
        if (this.groupName.equals(groupName)) {
            removeInstance(instanceName);
        }
    }

    private static int getDigestHashCode(String val) {
        int hc = val.hashCode();
        try {
            String hcStr = "_" + val.hashCode() + "_";
            MessageDigest dig = MessageDigest.getInstance("MD5");
            dig.update(hcStr.getBytes());
            dig.update(val.getBytes());
            dig.update(hcStr.getBytes());
            BigInteger bi = new BigInteger(dig.digest());
            hc = bi.intValue();
            return hc;
        } catch (NoSuchAlgorithmException nsaEx) {
            hc = val.hashCode();
        }

        return hc;
    }

    public void registerInstance(String inst) {
        wLock.lock();
        try {
            if (!currentMemberSet.contains(inst)) {
                currentMemberSet.add(inst);
            }
            members = currentMemberSet.toArray(new String[0]);
            printMemberStates();
        } finally {
            wLock.unlock();
        }
    }

    public synchronized void removeInstance(String inst) {
        wLock.lock();
        try {
            currentMemberSet.remove(inst);
            members = currentMemberSet.toArray(new String[0]);
        } finally {
            wLock.unlock();
        }
    }

    public void printMemberStates() {
        System.out.print("StringKeyMapper:: Members[");
        for (String st : members) {
            System.out.print("<" + st + "> ");
        }
        System.out.println("]");
    }

    private static class ReplicaState
            implements Comparable<ReplicaState> {
        String name;
        boolean active;

        ReplicaState(String name, boolean b) {
            this.name = name;
            active = b;
        }

        @Override
        public int compareTo(ReplicaState s) {
            return name.compareTo(s.name);
        }

        public int hashCode() {
            return name.hashCode();
        }

        public boolean equals(Object other) {
            ReplicaState st = (ReplicaState) other;
            return st.name.equals(name);
        }

        public String toString() {
            return "<" + name + ":" + active + ">";
        }
    }

    private static void mapTest(StringKeyMapper km) {
        String[] keys = new String[]{"Key0", "Key1", "Key2"};

        for (String key : keys) {
            System.out.println("\t" + key + " => " + km.getMappedInstance("g1", key));
        }

        System.out.println();
    }

    public static void main(String[] args) {
        StringKeyMapper km = new StringKeyMapper("n0", "g1");

        km.registerInstance("n0");
        km.registerInstance("n1");
        mapTest(km);

        km.registerInstance("inst0");
        km.registerInstance("inst1");
        km.registerInstance("instancen0");
        km.registerInstance("instancen1");
        mapTest(km);

        km.printMemberStates();
    }

}
