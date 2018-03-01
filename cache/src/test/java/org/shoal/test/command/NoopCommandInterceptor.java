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

package org.shoal.test.command;

import org.shoal.ha.cache.api.DataStoreException;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.api.AbstractCommandInterceptor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mahesh Kannan
 */
public class NoopCommandInterceptor<K, V>
    extends AbstractCommandInterceptor<K, V> {

    private AtomicInteger totalTransCount = new AtomicInteger();

    private AtomicInteger noopTranscount = new AtomicInteger();

    private AtomicInteger noopRecvCount = new AtomicInteger();

    @Override
    public void onTransmit(Command cmd, String initiator)
        throws DataStoreException {
        totalTransCount.incrementAndGet();
        System.out.println("**** NoopCommandInterceptor.onTransmit() got: " + cmd.getClass().getName());
        if (cmd instanceof NoopCommand) {
            noopTranscount.incrementAndGet();
            getDataStoreContext().getCommandManager().execute(new BatchedNoopCommand());
        } else {
            super.onTransmit(cmd, initiator);

        }
    }

    @Override
    public void onReceive(Command cmd, String initiator)
        throws DataStoreException {
        if (cmd instanceof NoopCommand) {
            noopRecvCount.incrementAndGet();
            super.onReceive(cmd, initiator);
        }
    }

    public int getTotalTransCount() {
        return totalTransCount.get();
    }

    public int getNoopTransCount() {
        return noopTranscount.get();
    }

    public int getReceiveCount() {
        return noopRecvCount.get();
    }
}
