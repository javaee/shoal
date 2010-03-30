package org.shoal.ha.mapper;

/**
 * @author Mahesh Kannan
 * 
 */
public interface KeyMapper<K> {

    public String getMappedInstance(String groupName, K key);

    //public String[] getMappedInstances(String groupName, K key, int count);

}
