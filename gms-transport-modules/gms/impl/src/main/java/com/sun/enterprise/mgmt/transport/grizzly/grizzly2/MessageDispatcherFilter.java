/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.mgmt.transport.grizzly.grizzly2;

import com.sun.enterprise.mgmt.transport.Message;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

/**
 * Message dispatcher Filter.
 * 
 * @author Alexey Stashok
 */
public class MessageDispatcherFilter extends BaseFilter {
    private final Attribute<Map<String, Connection>> piggyBackAttribute =
            Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute(
            MessageDispatcherFilter.class.getName() + ".piggyBack");

    private final GrizzlyNetworkManager2 networkManager;

    public MessageDispatcherFilter( GrizzlyNetworkManager2 networkManager ) {
        this.networkManager = networkManager;
    }

    @Override
    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        final Connection connection = ctx.getConnection();
        final Message message = ctx.getMessage();

        Map<String, Connection> piggyBack = piggyBackAttribute.get(connection);
        if (piggyBack == null) {
            piggyBack = new HashMap<String, Connection>();
            piggyBack.put(GrizzlyNetworkManager2.MESSAGE_CONNECTION_TAG, connection);
            piggyBackAttribute.set(connection, piggyBack);
        }
        
        networkManager.receiveMessage(message, piggyBack);
        
        return ctx.getInvokeAction();
    }
}
