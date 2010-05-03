package org.shoal.ha.mapper;

/**
 * @author Mahesh Kannan
 * 
 */
public interface KeyMapper {

    public String getMappedInstance(String groupName, Object key);

    public String findReplicaInstance(String groupName, Object key);

    //public String[] getMappedInstances(String groupName, K key, int count);

}
