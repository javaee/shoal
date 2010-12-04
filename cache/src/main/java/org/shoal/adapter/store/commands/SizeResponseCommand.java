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

import org.shoal.ha.cache.api.ShoalCacheLoggerConstants;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.command.ReplicationCommandOpcode;
import org.shoal.ha.cache.impl.util.CommandResponse;
import org.shoal.ha.cache.impl.util.ReplicationInputStream;
import org.shoal.ha.cache.impl.util.ReplicationOutputStream;
import org.shoal.ha.cache.impl.util.ResponseMediator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public class SizeResponseCommand<K, V>
        extends Command {

    private static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_SIZE_RESPONSE_COMMAND);

    private long tokenId;

    private int size;

    private String originatingInstance;

    private String respondingInstanceName;

    public SizeResponseCommand() {
        super(ReplicationCommandOpcode.SIZE_RESPONSE);
        super.setKey("SizeResp:" + tokenId);
    }

    public SizeResponseCommand(String originatingInstance, long tokenId, int size) {
        this();
        this.originatingInstance = originatingInstance;
        this.tokenId = tokenId;
        this.size = size;
    }


    protected boolean beforeTransmit() {
        setTargetName(originatingInstance);
        return originatingInstance != null;
    }

    public Object getCommandKey() {
        return "SizeResp:" + tokenId;
    }

    private void writeObject(ObjectOutputStream ros)
        throws IOException {

        ros.writeLong(tokenId);
        ros.writeInt(size);
        ros.writeUTF(dsc.getInstanceName());
    }



    private void readObject(ObjectInputStream ris)
        throws IOException {

        tokenId = ris.readLong();
        size = ris.readInt();
        respondingInstanceName = ris.readUTF();
    }

    @Override
    public void execute(String initiator) {

        ResponseMediator respMed = getDataStoreContext().getResponseMediator();
        CommandResponse resp = respMed.getCommandResponse(tokenId);
        if (resp != null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, dsc.getInstanceName() + " executed size_response tokenId=" + tokenId + " value " + size);
            }
            
            resp.setRespondingInstanceName(respondingInstanceName);
            resp.setResult(size);
        }
    }

    @Override
    protected boolean isArtificialKey() {
        return true;
    }
       
    public String toString() {
        return getName() + "(" + tokenId + ")";
    }
}
