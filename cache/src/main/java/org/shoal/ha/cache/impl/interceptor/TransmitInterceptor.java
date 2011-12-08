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

package org.shoal.ha.cache.impl.interceptor;

import org.shoal.ha.cache.api.AbstractCommandInterceptor;
import org.shoal.ha.cache.api.DataStoreContext;
import org.shoal.ha.cache.api.DataStoreException;
import org.shoal.ha.cache.api.ShoalCacheLoggerConstants;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.util.ReplicationOutputStream;
import org.shoal.ha.group.GroupService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Mahesh Kannan
 *
 */
public final class TransmitInterceptor<K, V>
    extends AbstractCommandInterceptor<K, V> {

    private static final Logger _logger =
            Logger.getLogger(ShoalCacheLoggerConstants.CACHE_TRANSMIT_INTERCEPTOR);

    @Override
    public void onTransmit(Command<K, V> cmd, String initiator)
        throws DataStoreException {
        DataStoreContext<K, V> ctx = getDataStoreContext();
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        boolean transmitted = false;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(cmd);
            oos.close();
            byte[] data = bos.toByteArray();

            GroupService gs = ctx.getGroupService();
            gs.sendMessage(cmd.getTargetName(),
                    ctx.getServiceName(), data);
            dsc.getDataStoreMBean().incrementGmsSendCount();
            dsc.getDataStoreMBean().incrementGmsSendBytesCount(data.length);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, storeName + ": TransmitInterceptor." + ctx.getServiceName()
                        + ":onTransmit() Sent " + cmd + " to "
                        + (cmd.getTargetName() == null ? " ALL MEMBERS " : cmd.getTargetName())
                        + "; size: " + data.length);
            }
            cmd.onSuccess();
            transmitted = true;
        } catch (IOException ioEx) {
            throw new DataStoreException("Error DURING transmit...", ioEx);
        } finally {
            if (! transmitted) {
                cmd.onFailure();   
            }
            try {oos.close();} catch (Exception ex) {_logger.log(Level.FINEST, "Ignorable error while closing ObjectOutputStream");}
            try {bos.close();} catch (Exception ex) {_logger.log(Level.FINEST, "Ignorable error while closing ByteArrayOutputStream");}
        }
    }

}
