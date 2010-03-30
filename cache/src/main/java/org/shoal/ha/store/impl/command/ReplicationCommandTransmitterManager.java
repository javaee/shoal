package org.shoal.ha.store.impl.command;

import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import org.shoal.ha.store.impl.command.Command;
import org.shoal.ha.store.impl.interceptor.ExecutionInterceptor;
import org.shoal.ha.store.impl.command.ReplicationCommandOpcode;

import java.util.Arrays;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 *
 */
public class ReplicationCommandTransmitterManager<K, V>
        extends ExecutionInterceptor<K, V> {

    private static final Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);

    private ConcurrentHashMap<String, ReplicationCommandTransmitter<K, V>> transmitters
            = new ConcurrentHashMap<String, ReplicationCommandTransmitter<K, V>>();

    private AtomicInteger indexCounter = new AtomicInteger();

    private ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<String, Integer>();

    private volatile String[] instances = new String[0];

    public void memberReady(String instanceName, String groupName) {
        logger.info("**=> ReplicationCommandTransmitterManager::memberReady(" + instanceName + ", "
                 + groupName + ")");

        ReplicationCommandTransmitter<K, V> trans = new ReplicationCommandTransmitter<K, V>();
        trans.initialize(instanceName, getReplicationService());
        TreeSet<String> set = new TreeSet<String>(Arrays.asList(instances));
        set.add(instanceName);
        instances = set.toArray(new String[0]);

        
        transmitters.put(instanceName, trans);
    }

    public void memberLeft(String instanceName, String groupName) {
                map.remove(instanceName);
        TreeSet<String> set = new TreeSet<String>(Arrays.asList(instances));
        set.remove(instanceName);
        instances = set.toArray(new String[0]);

        logger.info(" ReplicationServiceImpl.memberLeft() ==> " + instanceName);
        transmitters.remove(instanceName);
    }

    public void groupShutdown(String groupName) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getMappedInstance(String key) {
        int index = Math.abs(key.hashCode()) % instances.length;
        return instances[index];
    }

    public String[] getMappedInstances(String key) {
        int index = Math.abs(key.hashCode()) % instances.length;
        return new String[] {instances[index]};
    }

    public void onTransmit(Command<K, V> cmd) {
        if (cmd.getOpcode() != ReplicationCommandOpcode.REPLICATION_FRAME_PAYLOAD) {
            String target = cmd.getTargetName();
            System.out.println("** ReplicationCommandTransmitterManager: "
                    + "About to transmit to " + target + "; cmd: " + cmd);
            ReplicationCommandTransmitter<K, V> rft = transmitters.get(target);
            rft.send(target, cmd);
        } else {
            super.onTransmit(cmd);
        }
    }
    
}