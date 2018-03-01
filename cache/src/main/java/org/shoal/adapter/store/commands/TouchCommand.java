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

package org.shoal.adapter.store.commands;

import org.shoal.ha.cache.impl.store.DataStoreEntry;
import org.shoal.ha.cache.api.DataStoreException;
import org.shoal.ha.cache.api.ShoalCacheLoggerConstants;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.command.ReplicationCommandOpcode;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public class TouchCommand<K, V>
    extends AbstractSaveCommand<K, V> {

    public TouchCommand() {
        super(ReplicationCommandOpcode.TOUCH);
    }

    public TouchCommand(K k, long version, long accessTime, long maxIdleTime) {
        super(ReplicationCommandOpcode.TOUCH, k, version, accessTime, maxIdleTime);
    }

    @Override
    public void execute(String initiator)
        throws DataStoreException {

        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, dsc.getServiceName() + getName() + " received touch_command for key = " + getKey() + " from " + initiator);
        }

        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,  dsc.getServiceName() + getName()
                    + " received touch_command for key = " + getKey() + " from " + initiator
                    + "; version = " + getVersion() + "; " + dsc.getDataStoreEntryUpdater().getClass().getCanonicalName());
        }

        DataStoreEntry<K, V> entry = dsc.getReplicaStore().getOrCreateEntry(getKey());
        synchronized (entry) {
            dsc.getDataStoreEntryUpdater().executeTouch(entry, this);
        }

        if (dsc.isDoSynchronousReplication()) {
            _logger.log(Level.FINE, "TouchCommand Sending SIMPLE_ACK");
            super.sendAcknowledgement();
        }
    }
}
