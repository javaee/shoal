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

package com.sun.enterprise.mgmt.transport.jxta;

import net.jxta.protocol.PipeAdvertisement;
import net.jxta.protocol.RouteAdvertisement;
import net.jxta.pipe.PipeService;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.peer.PeerID;
import net.jxta.impl.pipe.BlockingWireOutputPipe;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class manages Jxta's InputPipe and OutputPipe and sends Jxta's message through JxtaUtility
 *
 * This stores and caches Jxta's OutputPipe according to Jxta's PeerID
 *
 * @author Bongjae Chang
 */
public class JxtaPipeManager {

    private static final Logger LOG = JxtaUtil.getLogger();

    private final JxtaNetworkManager networkManager;

    private final PipeService pipeService;
    private final PipeAdvertisement pipeAdv;
    private final PipeMsgListener pipeMsgListener;
    private InputPipe inputPipe;
    private OutputPipe outputPipe;
    private Map<net.jxta.peer.PeerID, OutputPipe> pipeCache = new ConcurrentHashMap<net.jxta.peer.PeerID, OutputPipe>();

    // Time duration in milliseconds to wait for a successful pipe resolution
    private final long pipeResolutionTimeout = 100; // ms

    public JxtaPipeManager( JxtaNetworkManager networkManager, PipeService pipeService, PipeAdvertisement pipeAdv, PipeMsgListener pipeMsgListener ) {
        this.networkManager = networkManager;
        this.pipeService = pipeService;
        this.pipeAdv = pipeAdv;
        this.pipeMsgListener = pipeMsgListener;
    }

    public void start() {
        try {
            outputPipe = pipeService.createOutputPipe( pipeAdv, pipeResolutionTimeout );
        } catch( IOException io ) {
            LOG.log( Level.FINE, "Failed to create master outputPipe", io );
        }
        try {
            inputPipe = pipeService.createInputPipe( pipeAdv, pipeMsgListener );
        } catch( IOException ioe ) {
            LOG.log( Level.SEVERE, "Failed to create service input pipe: " + ioe );
        }
    }

    public void stop() {
        if( outputPipe != null )
            outputPipe.close();
        if( inputPipe != null )
            inputPipe.close();
        pipeCache.clear();
    }

    /**
     * in the event of a failure or planned shutdown, remove the
     * pipe from the pipeCache
     *
     * @param peerid peerID
     */
    public void removePipeFromCache( net.jxta.peer.PeerID peerid ) {
        pipeCache.remove( peerid );
    }

    public void clearPipeCache() {
        pipeCache.clear();
    }

    public boolean send( net.jxta.peer.PeerID peerid, net.jxta.endpoint.Message message ) throws IOException {
        OutputPipe output = pipeCache.get( peerid );
        RouteAdvertisement route = null;
        final int MAX_RETRIES = 2;
        IOException lastOne = null;
        if( output != null && output.isClosed() )
            output = null;
        for( int createOutputPipeAttempts = 0; output == null && createOutputPipeAttempts < MAX_RETRIES; createOutputPipeAttempts++ )
        {
            route = networkManager.getCachedRoute( (PeerID)peerid );
            if( route != null ) {
                try {
                    output = new BlockingWireOutputPipe( networkManager.getNetPeerGroup(), pipeAdv, (PeerID)peerid, route );
                } catch( IOException ioe ) {
                    lastOne = ioe;
                }
            }
            if( output == null ) {
                // Unicast datagram
                // create a op pipe to the destination peer
                try {
                    output = pipeService.createOutputPipe( pipeAdv, Collections.singleton( peerid ), 1 );
                    if( LOG.isLoggable( Level.FINE ) && output != null ) {
                        LOG.fine( "ClusterManager.send : adding output to cache without route creation : " + peerid );
                    }
                } catch( IOException ioe ) {
                    lastOne = ioe;
                }
            }
        }
        if( output != null ) {
            pipeCache.put( peerid, output );
            return JxtaUtil.send( output, message );
        } else {
            LOG.log( Level.WARNING, "ClusterManager.send : sending of message " + message + " failed. Unable to create an OutputPipe for " + peerid +
                                    " route = " + route, lastOne );
            return false;
        }
    }

    public boolean broadcast( net.jxta.endpoint.Message message ) throws IOException {
        return JxtaUtil.send( outputPipe, message );
    }
}
