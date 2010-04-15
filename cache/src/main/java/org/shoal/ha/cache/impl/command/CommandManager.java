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
import org.shoal.ha.cache.impl.interceptor.ExecutionInterceptor;
import org.shoal.ha.cache.impl.util.MessageReceiver;
import org.shoal.ha.cache.impl.util.ResponseMediator;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.logging.Logger;


/**
 * @author Mahesh Kannan
 */
public class CommandManager<K, V>
        extends MessageReceiver {

    private final static Logger logger = Logger.getLogger("ReplicationLogger");

    private String myName;

    private String groupName;

    private DataStoreContext<K, V> dsc;

    private Command<K, V>[] commands = (Command<K, V>[]) Array.newInstance(Command.class, 256);

    private int interceptorSz = 0;

    private volatile ExecutionInterceptor<K, V> head;

    private volatile ExecutionInterceptor<K, V> tail;

    public CommandManager(DataStoreContext<K, V> dsc) {
        super(dsc.getServiceName());
        this.dsc = dsc;
        this.myName = dsc.getInstanceName();
        this.groupName = dsc.getGroupName();

        dsc.getGroupService().registerGroupMessageReceiver(dsc.getServiceName(), this);
    }

    public void registerCommand(Command<K, V> command) {
        commands[command.getOpcode()] = command;
        command.initialize(dsc);
    }

    public void unregisterCommand(byte opcode) {
        commands[opcode] = null;
    }

    public synchronized void registerExecutionInterceptor(ExecutionInterceptor<K, V> interceptor) {
        interceptor.initialize(dsc);
        if (head == null) {
            head = interceptor;
        } else {
            tail.setNext(interceptor);
        }
        interceptor.setPrev(tail);
        interceptor.setNext(null);
        tail = interceptor;
        interceptorSz++;
    }

    public Command getCommand(byte opcode) {
        return commands[opcode];
    }

    //Initiated to transmit

    public void execute(Command<K, V> cmd) {
        execute(cmd, true, myName);
    }

    //Initiated to transmit

    public void execute(Command<K, V> cmd, boolean forward, String initiator) {
        cmd.initialize(dsc);
        if (head != null) {
            if (forward) {
                cmd.prepareToTransmit(dsc);
                if (! cmd.getTargetName().equals(myName)) {
                head.onTransmit(cmd);
                } else {
                    cmd.execute(dsc);
                }
            } else {
                tail.onReceive(cmd);
            }
        }
    }

    public Command<K, V> createNewInstance(byte opcode, byte[] data, int offset)
            throws IOException {
        Command<K, V> cmd2 = commands[opcode];
        Command<K, V> cmd = null;
        if (cmd2 != null) {
            cmd = cmd2.createNewInstance();
            cmd.initialize(dsc);
            cmd.readCommandState(data, offset);
        }

        return cmd;
    }

    public void transmit(Command<K, V> cmd) {

    }

    public ResponseMediator getResponseMediator() {
        return dsc.getResponseMediator();
    }

    @Override
    protected void handleMessage(String sourceMemberName, String token, byte[] frameData) {

        byte opCode = frameData[0];
        Command<K, V> cmd2 = commands[opCode];
        if (cmd2 != null) {
            Command<K, V> cmd = cmd2.createNewInstance();
//            System.out.println("RECEIVED MEEESSAGE FOR: " + cmd.getClass().getName());
            cmd.initialize(dsc);
            try {
                cmd.readCommandState(frameData, 0);
                execute(cmd, false, sourceMemberName);
            } catch (IOException dse) {
                //TODO
            }
        }
    }
}