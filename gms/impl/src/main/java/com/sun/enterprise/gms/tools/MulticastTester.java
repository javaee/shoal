/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.gms.tools;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * A work in progress.
 */
public class MulticastTester {

    static final StringManager sm = StringManager.getInstance();

    static final String DASH = "--";

    // these are public so they can be used in external programs
    public static final String HELP_OPTION = DASH + sm.get("help.option");
    public static final String PORT_OPTION = DASH + sm.get("port.option");
    public static final String ADDRESS_OPTION = DASH + sm.get("address.option");
    public static final String BIND_OPTION = DASH + sm.get("bind.int.option");
    public static final String TTL_OPTION = DASH + sm.get("ttl.option");
    public static final String WAIT_PERIOD_OPTION = DASH + sm.get("period.option");
    public static final String TIMEOUT_OPTION = DASH + sm.get("timeout.option");
    public static final String DEBUG_OPTION = DASH + sm.get("debug.option");

    int mcPort = 2048;
    String mcAddress = "228.9.3.1";
    String bindInterface = null;
    int ttl = -1; // will only set if specified as command line param
    long msgPeriodInMillis = 2000;
    boolean debug = false;

    // this is more useful for development, but there is a param for it
    long testerTimeoutInSeconds = 20;

    /*
     * Called by main or external tool wrapper (such as asadmin
     * in GlassFish). Returns the exit value.
     */
    public int run(String [] args) {
        if (!parseArgs(args)) {
            return 1;
        }

        StringBuilder out = new StringBuilder();
        out.append(sm.get("port.set", Integer.toString(mcPort))).append("\n");
        out.append(sm.get("address.set", mcAddress)).append("\n");
        out.append(sm.get("bind.int.set", bindInterface)).append("\n");
        out.append(sm.get("period.set", msgPeriodInMillis)).append("\n");
        if (ttl != -1) {
            out.append(sm.get("ttl.set", ttl)).append("\n");
        }
        System.out.println(out.toString());

        String dataString;

        try {
            InetAddress localHost = InetAddress.getLocalHost();
            dataString = localHost.getHostName() + "|" +
                UUID.randomUUID().toString();
        } catch (UnknownHostException uhe) {
            System.err.println(sm.get("whoops", uhe.getMessage()));
            return 1;
        }

        MultiCastReceiverThread receiver = new MultiCastReceiverThread(
            mcPort, mcAddress, bindInterface, debug, dataString);
        MulticastSenderThread sender = new MulticastSenderThread(mcPort,
            mcAddress, bindInterface, ttl,
            msgPeriodInMillis, debug, dataString);
        receiver.start();
        sender.start();

        try{

            Thread.sleep(1000 * testerTimeoutInSeconds);

            receiver.done = true;
            sender.done = true;

            log("joining receiver thread");
            receiver.interrupt();
            receiver.join(500);
            if (receiver.isAlive()) {
                log("could not join receiver thread (expected)");
            } else {
                log("joined receiver thread");
            }

            log("interrupting sender thread");
            sender.interrupt();
            log("joining sender thread");
            sender.join(500);
            if (sender.isAlive()) {
                log("could not join sender thread");
            } else {
                log("joined sender thread");
            }

        } catch (InterruptedException ie) {
            System.err.println(sm.get("whoops", ie.getMessage()));
            ie.printStackTrace();
        }

        System.out.println(sm.get("timeout.exit", testerTimeoutInSeconds,
            TIMEOUT_OPTION));

        if (!receiver.receivedAnything.get()) {
            System.out.println(sm.get("no.data.for.you"));
            return 1;
        }

        return 0;
    }

    /*
     * Can't catch every random input the user can throw
     * at us, but let's at least make an attempt to correct
     * honest mistakes.
     *
     * Return true if we should keep processing.
     */
    private boolean parseArgs(String [] args) {
        String arg;
        try {
            for (int i=0; i<args.length; i++) {
                arg = args[i];
                if (HELP_OPTION.equals(arg)) {
                    // yes, this will return a non-zero exit code
                    printHelp();
                    return false;
                } else if (PORT_OPTION.equals(arg)) {
                    try {
                        arg = args[++i];
                        mcPort = Integer.parseInt(arg);
                    } catch (NumberFormatException nfe) {
                        System.err.println(sm.get("bad.num.param",
                            arg, PORT_OPTION));
                        return false;
                    }
                } else if (ADDRESS_OPTION.equals(arg)) {
                    mcAddress = args[++i];
                } else if (BIND_OPTION.equals(arg)) {
                    bindInterface = args[++i];
                } else if (TTL_OPTION.equals(arg)) {
                    try {
                        arg = args[++i];
                        ttl = Integer.parseInt(arg);
                    } catch (NumberFormatException nfe) {
                        System.err.println(sm.get("bad.num.param",
                            arg, TTL_OPTION));
                        return false;
                    }
                } else if (WAIT_PERIOD_OPTION.equals(arg)) {
                    try {
                        arg = args[++i];
                        msgPeriodInMillis = Long.parseLong(arg);
                    } catch (NumberFormatException nfe) {
                        System.err.println(sm.get("bad.num.param",
                            arg, WAIT_PERIOD_OPTION));
                        return false;
                    }
                } else if (TIMEOUT_OPTION.equals(arg)) {
                    try {
                        arg = args[++i];
                        testerTimeoutInSeconds = Long.parseLong(arg);
                    } catch (NumberFormatException nfe) {
                        System.err.println(sm.get("bad.num.param",
                            arg, TIMEOUT_OPTION));
                        return false;
                    }
                    System.out.println(sm.get("timeout.set",
                        testerTimeoutInSeconds));
                } else if (DEBUG_OPTION.equals(arg)) {
                    System.err.println(sm.get("debug.set"));
                    debug = true;
                } else {
                    System.err.println(sm.get(
                        "unknown.option", arg, HELP_OPTION));
                    return false;
                }
            }
        } catch (ArrayIndexOutOfBoundsException badUser) {
            System.err.println(sm.get("bad.user.param"));
            printHelp();
            return false;
        }
        return true;
    }

    private void printHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append(sm.get("help.message")).append("\n");
        sb.append(HELP_OPTION).append("\n");
        sb.append(PORT_OPTION).append("\n");
        sb.append(ADDRESS_OPTION).append("\n");
        sb.append(BIND_OPTION).append("\n");
        sb.append(TTL_OPTION).append("\n");
        sb.append(WAIT_PERIOD_OPTION).append("\n");
        sb.append(TIMEOUT_OPTION).append("\n");
        sb.append(DEBUG_OPTION).append("\n");
        System.out.println(sb.toString());
    }

    private void log(String msg) {
        if (debug) {
            System.err.println(msg);
        }
    }

    public static void main(String[] args) {
        MulticastTester tester = new MulticastTester();
        System.exit(tester.run(args));
    }

    // make the output a little more readable
    static String trimDataString(String s) {
        return s.substring(0, s.indexOf("|")); 
    }
}
