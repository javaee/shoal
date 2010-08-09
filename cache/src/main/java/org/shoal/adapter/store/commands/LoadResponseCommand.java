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

import org.shoal.ha.cache.api.ShoalCacheLoggerConstants;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.command.ReplicationCommandOpcode;
import org.shoal.ha.cache.impl.util.*;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public class LoadResponseCommand<K, V>
        extends Command<K, V> {

    private static final Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_TOUCH_COMMAND);

    private K key;

    private V v;

    private long tokenId;

    private String originatingInstance;

    public LoadResponseCommand() {
        super(ReplicationCommandOpcode.LOAD_RESPONSE);
    }

    public LoadResponseCommand(K key, V v, long tokenId) {
        super(ReplicationCommandOpcode.LOAD_RESPONSE);
        this.key = key;
        this.v = v;
        this.tokenId = tokenId;
    }

    public void setOriginatingInstance(String originatingInstance) {
        this.originatingInstance = originatingInstance;
    }

    @Override
    protected LoadResponseCommand<K, V> createNewInstance() {
        return new LoadResponseCommand<K, V>();
    }

    @Override
    protected void writeCommandPayload(ReplicationOutputStream ros)
        throws IOException {
        setTargetName(originatingInstance);

        ros.writeLong(tokenId);
        ros.writeLengthPrefixedString(originatingInstance);
        dsc.getDataStoreKeyHelper().writeKey(ros, key);
        ros.writeBoolean(v != null);
        if (v != null) {
            dsc.getDataStoreEntryHelper().writeObject(ros, v);
        }
        if (_logger.isLoggable(Level.INFO)) {
            _logger.log(Level.INFO, dsc.getInstanceName() + " sending load_response " + key + " to " + originatingInstance);
            _logger.log(Level.INFO, dsc.getInstanceName() + " RESULT load_response " + key + " => " + v + ":" + originatingInstance);
        }
    }



    @Override
    public void readCommandPayload(ReplicationInputStream ris)
        throws IOException {

        tokenId = ris.readLong();
        originatingInstance = ris.readLengthPrefixedString();
        key = dsc.getDataStoreKeyHelper().readKey(ris);
        boolean notNull = ris.readBoolean();
        if (notNull) {
            v = (V) dsc.getDataStoreEntryHelper().readObject(ris);
        }
    }

    @Override
    public void execute(String initaitor) {

        if (_logger.isLoggable(Level.INFO)) {
            _logger.log(Level.INFO, dsc.getInstanceName() + " received load_response " + key + " from " + initaitor);
        }

        ResponseMediator respMed = getDataStoreContext().getResponseMediator();
        CommandResponse resp = respMed.getCommandResponse(tokenId);
        if (resp != null) {
            if (_logger.isLoggable(Level.INFO)) {
                _logger.log(Level.INFO, dsc.getInstanceName() + " executed load_response " + key + " value " + v);
            }
            resp.setResult(v);
        }
    }


}