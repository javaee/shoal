package com.sun.enterprise.ee.cms.core;

/**
 * Provides the keys that correspond to properties within group
 * communication providers that can be configured. Note that this is not
 * exhaustive enough to cover all possible configurations in different
 * group communication libraries.
 *
 * @author Shreedhar Ganapathy
 */
public enum ServiceProviderConfigurationKeys {
    MULTICASTADDRESS,
    MULTICASTPORT,
    FAILURE_DETECTION_TIMEOUT,
    FAILURE_DETECTION_RETRIES,
    FAILURE_VERIFICATION_TIMEOUT,
    DISCOVERY_TIMEOUT,
    LOOPBACK,
    VIRTUAL_MULTICAST_ENABLED,//true, if the underlying network does not support multicast, and the group comm lib supports simulated multicast through other protocols
    VIRTUAL_MULTICAST_URI // typically an interface address of the initial rendezvous peer that is known to all joining members. 
}
