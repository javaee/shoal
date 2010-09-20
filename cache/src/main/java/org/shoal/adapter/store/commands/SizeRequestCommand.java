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

import org.shoal.ha.cache.api.DataStoreEntry;
import org.shoal.ha.cache.api.DataStoreException;
import org.shoal.ha.cache.api.ShoalCacheLoggerConstants;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.command.ReplicationCommandOpcode;
import org.shoal.ha.cache.impl.util.CommandResponse;
import org.shoal.ha.cache.impl.util.ReplicationInputStream;
import org.shoal.ha.cache.impl.util.ReplicationOutputStream;
import org.shoal.ha.cache.impl.util.ResponseMediator;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public class SizeRequestCommand<K, V>
        extends Command<K, V> {

    private static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_SIZE_REQUEST_COMMAND);

    private long tokenId;

    private String targetInstanceName;

    private Future future;

    public SizeRequestCommand() {
        super(ReplicationCommandOpcode.SIZE_REQUEST);
    }

    public SizeRequestCommand(String targetInstanceName) {
        this();

        this.targetInstanceName = targetInstanceName;
    }

    @Override
    protected SizeRequestCommand<K, V> createNewInstance() {
        return new SizeRequestCommand<K, V>();
    }
    
    @Override
    protected void writeCommandPayload(ReplicationOutputStream ros)
        throws IOException {
        ResponseMediator respMed = dsc.getResponseMediator();
        CommandResponse resp = respMed.createCommandResponse();

        future = resp.getFuture();
        setTargetName(targetInstanceName);
        
        ros.writeLengthPrefixedString(dsc.getInstanceName());
        ros.writeLengthPrefixedString(targetInstanceName);
        ros.writeLong(resp.getTokenId());
    }

    @Override
    public void readCommandPayload(ReplicationInputStream ris)
        throws IOException {

        targetInstanceName = ris.readLengthPrefixedString();
        String myName = ris.readLengthPrefixedString();
        tokenId = ris.readLong();
    }

    @Override
    public void execute(String initiator)
        throws DataStoreException {

        int size = dsc.getReplicaStore().size();
        SizeResponseCommand<K, V> srCmd = new SizeResponseCommand<K, V>(targetInstanceName, tokenId, size);
        dsc.getCommandManager().execute(srCmd);
    }

    public String toString() {
        return getName() + "; tokenId=" + tokenId;
    }

    public int getResult() {
        int result = 0;
        try {
            result = (Integer) future.get(3, TimeUnit.SECONDS);
        } catch (Exception dse) {
           //TODO
        }

        return result;
    }
}
