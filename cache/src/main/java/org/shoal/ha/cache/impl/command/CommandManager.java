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

package org.shoal.ha.cache.impl.command;

import org.shoal.ha.cache.api.DataStoreContext;
import org.shoal.ha.cache.api.DataStoreException;
import org.shoal.ha.cache.api.ShoalCacheLoggerConstants;
import org.shoal.ha.cache.impl.interceptor.AbstractCommandInterceptor;
import org.shoal.ha.cache.impl.interceptor.CommandHandlerInterceptor;
import org.shoal.ha.cache.impl.interceptor.TransmitInterceptor;
import org.shoal.ha.cache.impl.util.MessageReceiver;
import org.shoal.ha.cache.impl.util.ReplicationInputStream;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Mahesh Kannan
 */
public class CommandManager<K, V>
        extends MessageReceiver {

    private String myName;

    private DataStoreContext<K, V> dsc;

    private Command<K, V>[] commands = (Command<K, V>[]) Array.newInstance(Command.class, 256);

    private volatile AbstractCommandInterceptor<K, V> head;

    private volatile AbstractCommandInterceptor<K, V> tail;

    private static Logger _logger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_COMMAND);

    public CommandManager(DataStoreContext<K, V> dsc) {
        super(dsc.getServiceName());
        this.dsc = dsc;
        this.myName = dsc.getInstanceName();

        registerExecutionInterceptor(new CommandHandlerInterceptor<K, V>());
        registerExecutionInterceptor(new TransmitInterceptor<K, V>());

        //dsc.getGroupService().registerGroupMessageReceiver(dsc.getServiceName(), this);
    }

    public void registerCommand(Command<K, V> command) {
        commands[command.getOpcode()] = command;
        command.initialize(dsc);
    }

    public synchronized void registerExecutionInterceptor(AbstractCommandInterceptor<K, V> interceptor) {
        interceptor.initialize(dsc);
        if (head == null) {
            head = interceptor;
        } else {
            tail.setNext(interceptor);
        }
        interceptor.setPrev(tail);
        interceptor.setNext(null);
        tail = interceptor;
    }

    //Initiated to transmit

    public void execute(Command<K, V> cmd)
        throws DataStoreException {
        executeCommand(cmd, true, myName);
    }

    //Initiated to transmit
    public final void executeCommand(Command<K, V> cmd, boolean forward, String initiator)
        throws DataStoreException {
        cmd.initialize(dsc);
        if (forward) {
            head.onTransmit(cmd, initiator);
        } else {
            tail.onReceive(cmd, initiator);
        }
    }

    public Command<K, V> createNewInstance(byte opcode)
            throws IOException {
        Command<K, V> cmd2 = commands[opcode];
        Command<K, V> cmd = null;
        if (cmd2 != null) {
            cmd = cmd2.createNewInstance();
            cmd.initialize(dsc);
        } else {
            throw new IOException("Illegal opcode: " + opcode);
        }

        return cmd;
    }

    @Override
    protected void handleMessage(String sourceMemberName, String token, byte[] messageData) {

        ReplicationInputStream ris = null;
        try {
            byte opCode = messageData[0];
            _logger.log(Level.INFO, "RECEIVED GMS MESSAGE from instance: " + sourceMemberName
             + "; opcode: " + opCode + "; size: " + messageData.length);

            Command<K, V> cmd = createNewInstance(opCode);
            ris = new ReplicationInputStream(messageData);
            cmd.prepareToExecute(ris);
            this.executeCommand(cmd, false, sourceMemberName);
        } catch (IOException dse) {
            _logger.log(Level.INFO, "Error during parsing command", dse);
        } finally {
           try {ris.close();} catch (Exception ex) {}
        }
    }
    
}