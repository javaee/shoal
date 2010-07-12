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

import org.shoal.ha.cache.api.HashableKey;
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
public class DefaultKeyMapper<K>
        implements KeyMapper<K>, GroupMemberEventListener {

    private String myName;

    private String groupName;

    private ReentrantReadWriteLock.ReadLock rLock;

    private ReentrantReadWriteLock.WriteLock wLock;

    private volatile String[] members = new String[0];

    private volatile String[] previuousAliveAndReadyMembers = new String[0];


    public DefaultKeyMapper(String myName, String groupName) {
        this.myName = myName;
        this.groupName = groupName;
        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        
        rLock = rwLock.readLock();
        wLock = rwLock.writeLock();

        previuousAliveAndReadyMembers = new String[] {myName};
    }

    @Override
    public String getMappedInstance(String groupName, K key1) {
        int hc = key1.hashCode();
        if (key1 instanceof HashableKey) {
            HashableKey k = (HashableKey) key1;
            hc = k.getHashKey() == null ? hc : k.getHashKey().hashCode();
        }
        hc = Math.abs(hc);

        try {
            rLock.lock();
            return members.length == 0
                    ? null
                    : members[hc % (members.length)];
        } finally {
            rLock.unlock();
        }
    }

    @Override
    public String findReplicaInstance(String groupName, K key1) {
        int hc = key1.hashCode();
        if (key1 instanceof HashableKey) {
            HashableKey k = (HashableKey) key1;
            hc = k.getHashKey() == null ? hc : k.getHashKey().hashCode();
        }
        hc = Math.abs(hc);

        try {
            rLock.lock();
            return previuousAliveAndReadyMembers.length == 0
                    ? null
                    : previuousAliveAndReadyMembers[hc % (previuousAliveAndReadyMembers.length)];
        } finally {
            rLock.unlock();
        }
    }

    @Override
    public void onViewChange(String memberName,
                             Collection<String> readOnlyCurrentAliveAndReadyMembers,
                             Collection<String> readOnlyPreviousAliveAndReadyMembers,
                             boolean isJoinEvent) {
        try {
            wLock.lock();


            TreeSet<String> currentMemberSet = new TreeSet<String>();
            currentMemberSet.addAll(readOnlyCurrentAliveAndReadyMembers);
            currentMemberSet.remove(myName);
            members = currentMemberSet.toArray(new String[0]);


            TreeSet<String> previousView = new TreeSet<String>();
            previousView.addAll(readOnlyPreviousAliveAndReadyMembers);
            if (! isJoinEvent) {
                previousView.remove(memberName);
            }
            previuousAliveAndReadyMembers = previousView.toArray(new String[0]);

            printMemberStates("onViewChange");
        } finally {
            wLock.unlock();
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

    public void printMemberStates(String message) {
        System.out.print(message + " DefaultKeyMapper<" + myName + "> [");
        for (String st : members) {
            System.out.print("<" + st + "> ");
        }
        System.out.println("]");

        System.out.print("\t MembersIncludingMe[");
        for (String st : previuousAliveAndReadyMembers) {
            System.out.print("<" + st + "> ");
        }
        System.out.println("]");
    }

}
