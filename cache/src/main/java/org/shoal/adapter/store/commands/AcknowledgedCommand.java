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

import org.shoal.ha.cache.api.DataStoreException;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.util.CommandResponse;
import org.shoal.ha.cache.impl.util.ResponseMediator;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Mahesh Kannan
 */
public abstract class AcknowledgedCommand<K, V>
        extends Command<K, V> {

    private transient CommandResponse resp;

    private transient Future future;

    private long tokenId;

    private String originatingInstance;

    protected AcknowledgedCommand(byte opCode) {
        super(opCode);
    }

    protected boolean beforeTransmit() {
        if (dsc.isDoSynchronousReplication()) {
            originatingInstance = dsc.getInstanceName();
            ResponseMediator respMed = dsc.getResponseMediator();
            resp = respMed.createCommandResponse();
            tokenId = resp.getTokenId();
            future = resp.getFuture();
        }

        return true;
    }

    protected void sendAcknowledgement() {
        try {
            dsc.getCommandManager().execute(
                    new SimpleAckCommand<K, V>(originatingInstance, tokenId));
        } catch (DataStoreException dse) {
            //TODO: But can safely ignore
        }
    }

    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        out.writeBoolean(dsc.isDoSynchronousReplication());

        if (dsc.isDoSynchronousReplication()) {
            out.writeLong(tokenId);
            out.writeUTF(originatingInstance);
        }
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {

        boolean doSync = in.readBoolean();
        if (doSync) {
            tokenId = in.readLong();
            originatingInstance = in.readUTF();
        }
    }

    @Override
    public final void onSuccess() {
        if (dsc.isDoSynchronousReplication()) {
            try {
                waitForAck();
            } catch (Exception ex) {
                System.out.println("** Got exception: " + ex);
            }
        }
    }

    @Override
    public final void onFailure() {
        if (dsc.isDoSynchronousReplication()) {
            ResponseMediator respMed = dsc.getResponseMediator();
            respMed.removeCommandResponse(tokenId);
        }
    }

    private void waitForAck()
        throws DataStoreException, TimeoutException {
        try {
            future.get(3, TimeUnit.SECONDS);
        } catch (TimeoutException tEx) {
            throw tEx;
        } catch (Exception inEx) {
            throw new DataStoreException(inEx);
        } finally {
            ResponseMediator respMed = dsc.getResponseMediator();
            respMed.removeCommandResponse(tokenId);
        }
    }

}
