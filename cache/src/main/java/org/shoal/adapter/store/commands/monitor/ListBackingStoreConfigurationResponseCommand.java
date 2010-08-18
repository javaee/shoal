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

package org.shoal.adapter.store.commands.monitor;

import org.shoal.ha.cache.api.ShoalCacheLoggerConstants;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.command.ReplicationCommandOpcode;
import org.shoal.ha.cache.impl.util.CommandResponse;
import org.shoal.ha.cache.impl.util.ReplicationInputStream;
import org.shoal.ha.cache.impl.util.ReplicationOutputStream;
import org.shoal.ha.cache.impl.util.ResponseMediator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public class ListBackingStoreConfigurationResponseCommand<K, V>
        extends Command<K, V> {

    private static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_MONITOR);

    ArrayList<String> confList;

    private long tokenId;

    private String originatingInstance;

    public ListBackingStoreConfigurationResponseCommand() {
        super(ReplicationCommandOpcode.MONITOR_LIST_BACKING_STORE_CONF_RESPONSE);
    }

    public ListBackingStoreConfigurationResponseCommand(String originatingInstance, long tokenId, ArrayList<String> confList) {
        this();

        this.originatingInstance = originatingInstance;
        this.tokenId = tokenId;
        this.confList = confList;
    }

    @Override
    protected ListBackingStoreConfigurationResponseCommand<K, V> createNewInstance() {
        return new ListBackingStoreConfigurationResponseCommand<K, V>();
    }

    @Override
    protected void writeCommandPayload(ReplicationOutputStream ros)
        throws IOException {
        setTargetName(originatingInstance);

        ros.writeLong(tokenId);
        ros.writeLengthPrefixedString(originatingInstance);
        ros.writeInt(confList.size());
        for (String str : confList) {
            ros.writeLengthPrefixedString(str);
        }
    }



    @Override
    public void readCommandPayload(ReplicationInputStream ris)
        throws IOException {

        tokenId = ris.readLong();
        originatingInstance = ris.readLengthPrefixedString();
        int sz = ris.readInt();
        confList = new ArrayList<String>();
        for (int i=0; i<sz; i++) {
            confList.add(ris.readLengthPrefixedString());
        }
    }

    @Override
    public void execute(String initiator) {

        ResponseMediator respMed = getDataStoreContext().getResponseMediator();
        CommandResponse resp = respMed.getCommandResponse(tokenId);
        if (resp != null) {
            resp.setResult(confList);
        }
    }


}