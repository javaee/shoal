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

package org.shoal.ha.cache.impl.command;

import org.shoal.ha.cache.api.*;
import org.shoal.ha.cache.impl.interceptor.CommandHandlerInterceptor;
import org.shoal.ha.cache.impl.interceptor.ReplicationCommandTransmitterManager;
import org.shoal.ha.cache.impl.interceptor.ReplicationFramePayloadCommand;
import org.shoal.ha.cache.impl.interceptor.TransmitInterceptor;
import org.shoal.ha.cache.impl.util.MessageReceiver;
import org.shoal.ha.cache.impl.util.ReplicationInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicInteger;
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

    private static Logger _statsLogger = Logger.getLogger(ShoalCacheLoggerConstants.CACHE_STATS);

    public CommandManager() {}

    public void initialize(DataStoreContext<K, V> dsc) {
        this.dsc = dsc;
        this.myName = dsc.getInstanceName();


        head = new CommandHandlerInterceptor<K, V>();
        head.initialize(dsc);

        tail = new TransmitInterceptor<K, V>();
        tail.initialize(dsc);
        
        head.setNext(tail);
        tail.setPrev(head);
    }

    public void registerCommand(Command command) {
        commands[command.getOpcode()] = command;
        command.initialize(dsc);
    }

    public synchronized void registerExecutionInterceptor(AbstractCommandInterceptor<K, V> interceptor) {
        interceptor.initialize(dsc);

        interceptor.setPrev(tail.getPrev());
        tail.getPrev().setNext(interceptor);

        interceptor.setNext(tail);
        tail.setPrev(interceptor);
    }

    public void execute(Command<K, V> cmd)
        throws DataStoreException {
        executeCommand(cmd, true, myName);
    }

    public final void executeCommand(Command<K, V> cmd, boolean forward, String initiator)
        throws DataStoreException {
        cmd.initialize(dsc);
        if (forward) {
            try {
                head.onTransmit(cmd, initiator);
                cmd.onSuccess();
            } catch (DataStoreException dseEx) {
                cmd.onFailure();
            }
        } else {
            tail.onReceive(cmd, initiator);
        }
    }

    @Override
    protected void handleMessage(String sourceMemberName, String token, byte[] messageData) {

        ObjectInputStream ois = null;
        ByteArrayInputStream bis = null;
        try {
            bis = new ByteArrayInputStream(messageData);
            ois = (dsc.getKeyTransformer() == null)
                ? new ObjectInputStreamWithLoader(bis, dsc.getClassLoader())
                : new ObjectInputStream(bis);
            Command<K, V> cmd = (Command<K, V>) ois.readObject();
            if (_logger.isLoggable(Level.FINER)) {
                _logger.log(Level.FINER, dsc.getServiceName() + " RECEIVED " + cmd);
            }
            cmd.initialize(dsc);


            int receivedCount = dsc.getDataStoreMBean().incrementBatchReceivedCount();
            if (_statsLogger.isLoggable(Level.FINE)) {
                _statsLogger.log(Level.FINE, "Received message#  " + receivedCount + "  from " + sourceMemberName);
            }

            
            this.executeCommand(cmd, false, sourceMemberName);
        } catch (IOException dse) {
            _logger.log(Level.WARNING, "Error during parsing command: opcode: " + messageData[0], dse);
        } catch (Throwable th) {
            _logger.log(Level.WARNING, "Error[2] during parsing command: opcode: " + messageData[0], th);
        } finally {
           try {bis.close();} catch (Exception ex) {}
           try {ois.close();} catch (Exception ex) {}
        }
    }
    
}
