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

package org.shoal.ha.cache.impl.command;

import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.interceptor.ExecutionInterceptor;
import org.shoal.ha.cache.impl.command.ReplicationCommandOpcode;

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
        trans.initialize(instanceName, getDataStoreContext());
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
            if (rft == null) {
                rft = new ReplicationCommandTransmitter<K, V>();
                rft.initialize(target, getDataStoreContext());
                transmitters.put(target, rft);
            }
            rft.addCommand(cmd);
        } else {
            super.onTransmit(cmd);
        }
    }
    
}