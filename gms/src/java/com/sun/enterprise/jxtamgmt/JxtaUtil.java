/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://shoal.dev.java.net/public/CDDLv1.0.html
 *
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 */

package com.sun.enterprise.jxtamgmt;

import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.WireFormatMessage;
import net.jxta.endpoint.WireFormatMessageFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class that can be used by any calling code to do common routines
 *
 * @author shreedhar ganapathy
 */

public class JxtaUtil {
    private static Logger LOG = Logger.getLogger(
            System.getProperty("JXTA_MGMT_LOGGER", "JxtaMgmt"));

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
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex.toString());
        }
    }

    public static Object getObjectFromByteArray(MessageElement element) {
        if (element == null) {
            return null;
        }
        try {
            ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(element.getStream()));
            return in.readObject();
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
            consoleHandler.setLevel(Level.ALL);
            consoleHandler.setFormatter(new NiceLogFormatter());
            SelectiveLogFilter filter = new SelectiveLogFilter();
            //filter.add(HealthMonitor.class.getName());
            //filter.add(MasterNode.class.getName());
            //filter.add(ClusterView.class.getName());
            //filter.add("com.sun.enterprise.ee.cms.impl.jxta.ViewWindow");
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

    public static NetworkManagerProxy getNetworkManagerProxy(final String groupName) throws IllegalArgumentException {
        final NetworkManagerProxy manager = NetworkManagerRegistry.getNetworkManagerProxy(groupName);
        if (manager != null) {
            return manager;
        } else {
            throw new IllegalArgumentException("Network Manager Proxy for GroupName " + groupName + "could not be located."
                    + "Check if group has been created or enabled");
        }
    }
}

