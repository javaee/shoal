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

package org.shoal.adapter.store.commands;

import org.shoal.ha.cache.api.DataStoreEntry;
import org.shoal.ha.cache.api.ShoalCacheLoggerConstants;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.command.ReplicationCommandOpcode;
import org.shoal.ha.cache.impl.util.ReplicationInputStream;
import org.shoal.ha.cache.impl.util.ReplicationOutputStream;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public class StoreableRemoveCommand<K, V>
        extends AcknowledgedCommand<K, V> {

    protected static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_REMOVE_COMMAND);

    private K key;

    private String target;

    public StoreableRemoveCommand() {
        super(ReplicationCommandOpcode.STOREABLE_REMOVE);
    }

    public StoreableRemoveCommand(K key) {
        this();
        this.key = key;
    }

    @Override
    protected StoreableRemoveCommand<K, V> createNewInstance() {
        return new StoreableRemoveCommand<K, V>();
    }


    public void setTarget(String t) {
        this.target = t;
    }

    @Override
    public void writeCommandPayload(ReplicationOutputStream ros) throws IOException {
        //super.selectReplicaInstance( key);
        if (!dsc.isDoASyncReplication()) {
            super.writeAcknowledgementId(ros);
        }
        super.setTargetName(target);
        dsc.getDataStoreKeyHelper().writeKey(ros, key);
    }

    @Override
    public void readCommandPayload(ReplicationInputStream ris)
            throws IOException {
        if (!dsc.isDoASyncReplication()) {
            super.readAcknowledgementId(ris);
        }
        key = dsc.getDataStoreKeyHelper().readKey(ris);
    }

    @Override
    public void execute(String initiator) {
        dsc.getReplicaStore().remove(key);
        
        if (!dsc.isDoASyncReplication()) {
            super.sendAcknowledgement();
        }
    }

    public String toString() {
        return getName() + "(" + key + ")";
    }

}