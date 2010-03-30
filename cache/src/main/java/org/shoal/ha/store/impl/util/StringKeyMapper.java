package org.shoal.ha.store.impl.util;

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


    private String groupName;

    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    private ReentrantReadWriteLock.ReadLock rLock;

    private ReentrantReadWriteLock.WriteLock wLock;

    private volatile TreeSet<String> currentMemeberMap = new TreeSet<String>();

    private volatile String[] members;


    public StringKeyMapper(String groupName) {
        rLock = rwLock.readLock();
        wLock = rwLock.writeLock();
    }

    @Override
    public String getMappedInstance(String groupName, K key1) {
        rLock.lock();
        try {
        int hc = Math.abs(getDigestHashCode(key1.toString()));
        //System.out.println("Key: " + key1 + " => " + hc + ";   " + hc%(members.length));
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

    private static int getDigestHashCode(String name) {
        int hc = 0;
        try {
            MessageDigest dig = MessageDigest.getInstance("MD5");
            dig.update(name.getBytes());
            BigInteger bi = new BigInteger(dig.digest());
            hc = bi.intValue();
        } catch (NoSuchAlgorithmException nsaEx) {
            hc = name.hashCode();
        }

        return hc;
    }

    public void registerInstance(String inst) {
        wLock.lock();
        try {
            if (!currentMemeberMap.contains(inst)) {
                currentMemeberMap.add(inst);
            }

            members = currentMemeberMap.toArray(new String[0]);

        } finally {
            wLock.unlock();
        }
    }

    public synchronized void removeInstance(String inst) {
        wLock.lock();
        try {
            currentMemeberMap.remove(inst);
            members = currentMemeberMap.toArray(new String[0]);
        } finally {
            wLock.unlock();
        }
    }

    public void printMemberStates() {
        System.out.print("Members[");
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
        String[] keys = new String[] {"Key0", "Key1", "Key2"};

        for (String key : keys) {
            System.out.println("\t" + key + " => " + km.getMappedInstance("g1", key));
        }

        System.out.println();
    }

    public static void main(String[] args) {
        StringKeyMapper km = new StringKeyMapper("g1");

        km.registerInstance("n0");
        km.registerInstance("n1");
        mapTest(km);
        km.registerInstance("DAS");
        km.registerInstance("instance0");
        km.registerInstance("instance1");
        km.registerInstance("instancen0");
        km.registerInstance("instancen1");
        mapTest(km);

        km.printMemberStates();
    }

}
