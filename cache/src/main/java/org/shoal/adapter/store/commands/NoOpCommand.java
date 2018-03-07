/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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
import org.shoal.ha.cache.impl.command.ReplicationCommandOpcode;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author Mahesh Kannan
 */
public class NoOpCommand<K, V>
    extends Command {

    private transient static final byte[] rawReadState = new byte[] {
            (byte) ReplicationCommandOpcode.NOOP_COMMAND,
            (byte) 123};

    private transient static final NoOpCommand _noopCommand = new NoOpCommand();

    public NoOpCommand() {
        super(ReplicationCommandOpcode.NOOP_COMMAND);
        super.setKey("Noop" + System.identityHashCode(this));
    }

    public boolean beforeTransmit() {
        return true;
    }

    public Object getCommandKey() {
        return "Noop" + System.identityHashCode(this);
    }
    
    @Override
    public void execute(String initiator)
        throws DataStoreException {
    }

    public String toString() {
        return getName();
    }

    @Override
    public String getKeyMappingInfo() {
        return null;
    }

    @Override
    protected boolean isArtificialKey() {
        return true;
    }
}
