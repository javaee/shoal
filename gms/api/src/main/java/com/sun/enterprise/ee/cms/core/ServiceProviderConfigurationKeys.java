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
    /**
     * unreserved valid multicast address in the range 224.0.0.0 through
     * 239.255.255.255
     * See http://www.iana.org/assignments/multicast-addresses for more details
     * on valid addresses.
     * If not using multicast, do not specify this property
     */
    MULTICASTADDRESS,
    /**
     * A valid port. If not using multicast, do not specify this property
     */
    MULTICASTPORT,
    /**
     * The timeout in milliseconds which will be used to send out periodic
     * heartbeats. This is also the period that will be used to check for
     * update to the health state of each member process.
     */
    FAILURE_DETECTION_TIMEOUT,
    /**
     * Number of periodic heartbeats than can be missed in order to be 
     * determined a suspected failure. Once the retries have been exhausted,
     * a FailureSuspectedNotificationSignal is sent out to all GMS clients who
     * have registered for this event.
     */
    FAILURE_DETECTION_RETRIES,
    /**
     * The timeout in milliseconds which will be used to wait and verify that
     * the suspected member has indeed failed.
     * Once confirmed failed, a FailureNotificationSignal is sent out to all GMS
     * clients who have registered for this event.
     */
    FAILURE_VERIFICATION_TIMEOUT,
    /**
     * The timeout in milliseconds that each member would wait to discover a
     * group leader. If no group leader was found within this timeout, the member
     * announces itself as the assumed and assigned group leader.
     */
    DISCOVERY_TIMEOUT,
    /**
     * Setting the value of this key to true, would make all application level
     * messages sent by this member to also be received by this member in
     * addition to the target members to whom the message was sent.
     */
    LOOPBACK,
    /**
     * Represents a key whose value is set to true if this node will be a bootstrapping
     * host for other members to use for discovery purposes. This is particularly useful
     * when multicast traffic is not supported in the network or cluster members are located
     * outside a multicast supported network area.
     * Setting the value of this to true requires specifying a URI corresponding to this
     * member process's IP address and port with tcp protocol, as a value in the
     * VIRTUAL_MULTICAST_URI_LIST property.
     * See below for the VIRTUAL_MULTICAST_URI_LIST property
     */
    IS_BOOTSTRAPPING_NODE,

   /**
    * This enum represents a key the value of which is a comma separated list of
    * initial bootstrapping tcp addresses. This address list must be specified on
    * all members of the cluster through this property.
    * <p>Typically an address uri would be specified as tcp://ipaddress:port</p>
    * The port here could be any available unoccupied port.<br> 
    * Specifying this list is helpful particularly when cluster members are located
    * beyond one subnet or multicast traffic is disabled.
    * Note: The implementation in Shoal at the moment only uses the first
    * address in this list and ignores the rest. This is an enhancement that is not
    * yet completed.
    */
    VIRTUAL_MULTICAST_URI_LIST,

   /**
    * If you wish to specify a particular network interface that should be used
    * for all group communication messages, use this key and specify an interface
    * address.
    * This is the address which Shoal would pass down to a service provider such as
    * Jxta to bind to for communication.
    */
    BIND_INTERFACE_ADDRESS,
    /**
     * Maximum time that the health monitoring protocol would wait for a reachability
     * query to block for a response. After this time expires, the health monitoring
     * protocol would report a failure based on the fact that an endpoint was unreachable
     * for this length of time. <br>
     * Specifying this property is typically helpful in determining hardware and network failures
     * within a shorter time period than the OS/System configured TCP retransmission timeout.
     * On many OSs, the TCP retransmission timeout is about 10 minutes.
     * <p>The default timeout for this property is set to 30 seconds.</p>
     * <p> As an example, let's take the case of 2 machines A and B hosting
     * instances X and Y, respectively. Machine B goes down due to a power outage
     * or a hardware failure.</p>
     * <p>Under normal circumstances, Instance X on machine A would not know of the unavailability of
     * Instance X on Machine B until the TCP retransmission timeout (typically 10 minutes) has passed.
     * By setting this property's value to some lower time threshold, instance X would
     * determine Y's failure due to Machine B's failure, a lot earlier. </p>
     * <p>Related to this key is the FAILURE_DETECTION_TCP_RETRANSMIT_PORT. See below</p>
     */
    FAILURE_DETECTION_TCP_RETRANSMIT_TIMEOUT,
    /**
     * <p> This value of this key is a port common to all cluster members where a socket will be
     * attempted to be created when a particular instance's configured periodic heartbeats
     * have been missed for the max retry times. The port number specified should be an available
     * unoccupied port on all machines involved in the cluster. If the socket creation attempt
     * blocks for the above-mentioned FAILURE_DETECTION_TCP_RETRANSMIT_TIMEOUT, then the health
     * monitoring protocol would return a failure event. </p>
     */
    FAILURE_DETECTION_TCP_RETRANSMIT_PORT,
    /**
     * <p>OPTIONAL: not a meaningful option for all implementations.
     * Specify the max number of threads allocated to run handlers for incoming multicast messages.
     */
    MULTICAST_POOLSIZE,

    /**
     * <p>Enable setting how large fixed incoming message queue is.
     */
    INCOMING_MESSAGE_QUEUE_SIZE ,

    /**
     * Max message length.  This length is not just application payload but includes message overhead (such as headers)
     * that is implementation and transport dependent.
     */
    MAX_MESSAGE_LENGTH,

    /**
     * Configure number of threads for incoming message processing.
     */
    INCOMING_MESSAGE_THREAD_POOL_SIZE,

    /**
     * Set MONITORING frequency in seconds.
     */
    MONITORING
}
