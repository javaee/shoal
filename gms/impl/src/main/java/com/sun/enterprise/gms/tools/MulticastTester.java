/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.enterprise.gms.tools;

/**
 * A work in progress.
 */
public class MulticastTester {

    static final StringManager sm = StringManager.getInstance();

    static final String DASH = "--";
    static final String HELP_OPTION = DASH + sm.get("help.option");
    static final String PORT_OPTION = DASH + sm.get("port.option");
    static final String ADDRESS_OPTION = DASH + sm.get("address.option");
    static final String BIND_OPTION = DASH + sm.get("bind.int.option");
    static final String WAIT_PERIOD_OPTION = DASH + sm.get("period.option");
    static final String TIMEOUT_OPTION = DASH + sm.get("timeout.option");
    static final String DEBUG_OPTION = DASH + sm.get("debug.option");

    int mcPort = 2048;
    String mcAddress = "228.9.3.1";
    String bindInterface = null;
    long msgPeriodInMillis = 2000;
    boolean debug = false;

    // this is more useful for development, but there is a param for it
    long testerTimeoutInSeconds = 20;

    private void run(String [] args) {
        parseArgs(args);

        StringBuilder out = new StringBuilder();
        out.append(sm.get("port.set", mcPort)).append("\n");
        out.append(sm.get("address.set", mcAddress)).append("\n");
        out.append(sm.get("bind.int.set", bindInterface)).append("\n");
        out.append(sm.get("period.set", msgPeriodInMillis)).append("\n");
        System.out.println(out.toString());

        MultiCastReceiverThread receiver = new MultiCastReceiverThread(
            mcPort, mcAddress, bindInterface, debug);
        MulticastSenderThread sender = new MulticastSenderThread(
            mcPort, mcAddress, bindInterface, msgPeriodInMillis, debug);
        receiver.start();
        sender.start();

        try {
            Thread.sleep(1000 * testerTimeoutInSeconds);

            log("interrupting sender thread");
            sender.interrupt();
            log("joining sender thread");
            sender.join(1000);
            if (sender.isAlive()) {
                log("could not join sender thread. exiting");
            } else {
                log("joined sender thread");
            }

            log("interrupting receiver thread");
            receiver.interrupt();
            log("joining receiver thread");
            receiver.join(1000);
            if (receiver.isAlive()) {
                log("could not join receiver thread. exiting");
            } else {
                log("joined receiver thread");
            }
        } catch (InterruptedException ie) {
            System.err.println(sm.get("whoops", ie.getMessage()));
            ie.printStackTrace();
        }

        System.out.println(sm.get("timeout.exit", testerTimeoutInSeconds,
            TIMEOUT_OPTION));
        if (!receiver.receivedAnything.get()) {
            System.out.println(sm.get("no.data.for.you"));
        }
        printAndExit(null, 0);
    }

    /*
     * Can't catch every random input the user can throw
     * at us, but let's at least make an attempt to correct
     * honest mistakes.
     */
    private void parseArgs(String [] args) {
        String arg;
        try {
            for (int i=0; i<args.length; i++) {
                arg = args[i];
                if (HELP_OPTION.equals(arg)) {
                    doHelpAndExit(0);
                } else if (PORT_OPTION.equals(arg)) {
                    try {
                        arg = args[++i];
                        mcPort = Integer.parseInt(arg);
                    } catch (NumberFormatException nfe) {
                        printAndExit(sm.get("bad.num.param",
                            arg, PORT_OPTION), 1);
                    }
                } else if (ADDRESS_OPTION.equals(arg)) {
                    mcAddress = args[++i];
                } else if (BIND_OPTION.equals(arg)) {
                    bindInterface = args[++i];
                } else if (WAIT_PERIOD_OPTION.equals(arg)) {
                    try {
                        arg = args[++i];
                        msgPeriodInMillis = Long.parseLong(arg);
                    } catch (NumberFormatException nfe) {
                        printAndExit(sm.get("bad.num.param",
                            arg, WAIT_PERIOD_OPTION), 1);
                    }
                } else if (TIMEOUT_OPTION.equals(arg)) {
                    try {
                        arg = args[++i];
                        testerTimeoutInSeconds = Long.parseLong(arg);
                    } catch (NumberFormatException nfe) {
                        printAndExit(sm.get("bad.num.param",
                            arg, TIMEOUT_OPTION), 1);
                    }
                    System.out.println(sm.get("timeout.set",
                        testerTimeoutInSeconds));
                } else if (DEBUG_OPTION.equals(arg)) {
                    System.err.println(sm.get("debug.set"));
                    debug = true;
                } else {
                    printAndExit(sm.get("unknown.option", arg, HELP_OPTION), 1);
                }
            }
        } catch (ArrayIndexOutOfBoundsException badUser) {
            System.err.println(sm.get("bad.user.param"));
            doHelpAndExit(1);
        }
    }

    private void doHelpAndExit(int status) {
        StringBuilder sb = new StringBuilder();
        sb.append(sm.get("help.message")).append("\n");
        sb.append(HELP_OPTION).append("\n");
        sb.append(PORT_OPTION).append("\n");
        sb.append(ADDRESS_OPTION).append("\n");
        sb.append(BIND_OPTION).append("\n");
        sb.append(WAIT_PERIOD_OPTION).append("\n");
        sb.append(DEBUG_OPTION).append("\n");
        printAndExit(sb.toString(), status);
    }

    private void printAndExit(String msg, int status) {
        if (msg != null) {
            if (status == 0) {
                System.out.println(msg);
            } else {
                System.err.println(msg);
            }
        }
        System.exit(status);
    }

    private void log(String msg) {
        if (debug) {
            System.err.println(msg);
        }
    }

    public static void main(String[] args) {
        MulticastTester tester = new MulticastTester();
        tester.run(args) ;
    }
}
