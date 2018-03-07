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

package com.sun.enterprise.mgmt.transport.jxta;

import net.jxta.logging.Logging;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.WireFormatMessage;
import net.jxta.endpoint.WireFormatMessageFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.Element;
import net.jxta.pipe.OutputPipe;

import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import com.sun.enterprise.ee.cms.logging.NiceLogFormatter;

/**
 * Utility class that can be used by any calling code to do common routines
 *
 * @author shreedhar ganapathy
 */
public class JxtaUtil {
    private static Logger LOG = Logger.getLogger(
            System.getProperty("JXTA_MGMT_LOGGER", "JxtaMgmt"));

    private static String jxtaLoggingPropertyValue = System.getProperty( Logging.JXTA_LOGGING_PROPERTY);

    private JxtaUtil() {
    }

    public static byte[] createByteArrayFromObject(Object object) {
        if (object == null)
            return null;
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            GZIPOutputStream gos = new GZIPOutputStream(outStream);
            ObjectOutputStream out = new ObjectOutputStream(gos);
            out.writeObject(object);
            gos.finish();
            gos.close();
            return outStream.toByteArray();
        } catch ( IOException ex) {
            throw new IllegalArgumentException(ex.toString());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getObjectFromByteArray( MessageElement element) {
        if (element == null) {
            return null;
        }
        try {
            ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(element.getStream()));
            return (T) in.readObject();
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.toString());
        }
    }

    static Logger getLogger() {
        return LOG;
    }

    static Logger getLogger(String name) {
        //return Logger.getLogger(name);
        return getLogger();
    }


    public static void setLogger(Logger logger) {
        LOG = logger;
    }

    public static void setupLogHandler() {
        final ConsoleHandler consoleHandler = new ConsoleHandler();
        try {
            consoleHandler.setLevel( Level.ALL);
            consoleHandler.setFormatter(new NiceLogFormatter());
            //SelectiveLogFilter filter = new SelectiveLogFilter();
            //filter.add(HealthMonitor.class.getName());
            //filter.add(MasterNode.class.getName());
            //filter.add(ClusterView.class.getName());
            //filter.add(NetworkManager.class.getName());
            //filter.add(net.jxta.impl.rendezvous.RendezVousServiceImpl.class.getName());
            //consoleHandler.setFilter(filter);
        } catch (SecurityException e) {
            new ErrorManager().error(
                    "Exception caught in setting up ConsoleHandler ",
                    e, ErrorManager.GENERIC_FAILURE);
        }
        LOG.addHandler(consoleHandler);
        LOG.setUseParentHandlers(false);
        final String level = System.getProperty("LOG_LEVEL", "FINEST");
        LOG.setLevel(Level.parse(level));
        // Remove all existing handlers for jxta logging
        Logger jxtaLogger = Logger.getLogger("net.jxta");
        for( Handler aHandler : jxtaLogger.getHandlers() ) {
            jxtaLogger.removeHandler(aHandler);
    }

        String jxtaLoggingPropertyValue = System.getProperty(Logging.JXTA_LOGGING_PROPERTY);
        if (jxtaLoggingPropertyValue == null) {
            // Only disable jxta logging when jxta logging has not already been explicitly enabled.
            System.setProperty(Logging.JXTA_LOGGING_PROPERTY, Level.SEVERE.toString());
        }

        jxtaLogger.addHandler(consoleHandler);
    }

    /**
     * Prints message element names and content and some stats
     *
     * @param msg     message to print
     * @param verbose indicates whether to print elment content
     */
    public static void printMessageStats(final Message msg,
                                         final boolean verbose) {
        try {
            final Message.ElementIterator it = msg.getMessageElements();
            LOG.log(Level.FINER, "------------------Begin Message---------------------");
            final WireFormatMessage serialed = WireFormatMessageFactory.toWire(
                    msg,
                    new MimeMediaType("application/x-jxta-msg"), null);
            LOG.log(Level.FINER, "Message Size :" + serialed.getByteLength());
            while (it.hasNext()) {
                final MessageElement el = (MessageElement) it.next();
                final String eName = el.getElementName();
                LOG.log(Level.FINER, "Element " + eName);
                if (verbose) {
                    LOG.log(Level.FINER, "[" + el + "]");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public static JxtaNetworkManagerProxy getNetworkManagerProxy(final String groupName) throws IllegalArgumentException {
        final JxtaNetworkManagerProxy manager = JxtaNetworkManagerRegistry.getNetworkManagerProxy(groupName);
        if (manager != null) {
            return manager;
        } else {
            throw new IllegalArgumentException("Network Manager Proxy for GroupName " + groupName + "could not be located."
                    + "Check if group has been created or enabled");
        }
    }

    @SuppressWarnings("unchecked")
    static public void appendChild( StructuredDocument adv, Element child) {
        adv.appendChild(child);
    }

    public static void configureJxtaLogging() {
        if ( jxtaLoggingPropertyValue == null) {

            // Only default jxta logging to SEVERE when jxta logging has not already been explicitly enabled.
            jxtaLoggingPropertyValue = Level.SEVERE.toString();
            System.setProperty(Logging.JXTA_LOGGING_PROPERTY, jxtaLoggingPropertyValue );
            if ( LOG.isLoggable(Level.CONFIG)) {
                LOG.config("gms configureJxtaLogging: set jxta logging to default of " + jxtaLoggingPropertyValue );
            }
        } else {
            if ( LOG.isLoggable(Level.CONFIG)) {
                LOG.config("gms configureJxtaLogging: found jxta system property " + Logging.JXTA_LOGGING_PROPERTY + " is already configured to "
                           + jxtaLoggingPropertyValue );
            }
        }
    }

    static final public int MAX_SEND_RETRIES = 4;

    /**
     * Send <code>msg</code> over <code>pipe</code>.
     *
     * @param pipe the output pipe
     * @param msg the message
     * @return boolean <code>true</code> if the message has been sent otherwise
     * <code>false</code>. <code>false</code>. is commonly returned for
     * non-error related congestion, meaning that you should be able to send
     * the message after waiting some amount of time.
     * @throws java.io.IOException the exception
     */
    public static boolean send( OutputPipe pipe, Message msg) throws IOException {

        boolean sent = false;
        for (int i = 0; i < MAX_SEND_RETRIES && !sent; i++) {
            // Short term fix is to introduce a synchronized on pipe
            // since msg send is failing when too many threads try to send on same
            // pipe at same time.  (i.e. MultiThreadMessageSender sending msg using 100 threads)
            //TBD:   Future optimization is to introduce a pipe pool where each pipe is
            //       only used by one thread at a time.
            synchronized(pipe) {
                sent = pipe.send(msg);
            }
        }
        return sent;
    }
}
