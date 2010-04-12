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

package com.sun.enterprise.ee.cms.tests;

import com.sun.enterprise.ee.cms.core.*;
import com.sun.enterprise.ee.cms.impl.client.*;
import com.sun.enterprise.ee.cms.impl.common.GroupManagementServiceImpl;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GMSAdminAgent implements CallBack {

    private static final Level DEBUG_LEVEL = Level.FINE;
    private Logger gmsLogger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    private GroupManagementService gms;
    private String groupName;
    private String memberName;
    private long lifeTime;
    private boolean isAdmin = false;
    private static AtomicInteger NotifiedOfStateChange = new AtomicInteger(GMSAdminConstants.RUN);
    private boolean SHUTDOWNINITIATED = false;
    List<String> activeMembers;
    private GMSConstants.shutdownType shutdownType = GMSConstants.shutdownType.INSTANCE_SHUTDOWN;
    private static AtomicLong timeReceivedLastJoinJoinedAndReady = new AtomicLong(System.currentTimeMillis());
    private static AtomicInteger numJoinAndReadyReceived = new AtomicInteger(0);
    private static AtomicLong diffTime = new AtomicLong(0);
    private static AtomicBoolean startupComplete = new AtomicBoolean(false);
    private static AtomicBoolean startupInitiated = new AtomicBoolean(false);

    public GMSAdminAgent(final GroupManagementService gms,
            final String groupName,
            final String memberName,
            final long lifeTime) {
        this.gms = gms;
        this.groupName = groupName;
        this.memberName = memberName.toLowerCase();
        this.lifeTime = lifeTime;

        // if the member name is server then it is assumed that that member is
        // the admin
        if (this.memberName.equals(GMSAdminConstants.ADMINNAME)) {
            this.isAdmin = true;
        }

        gms.addActionFactory(new PlannedShutdownActionFactoryImpl(this));
        gms.addActionFactory(new MessageActionFactoryImpl(this), GMSAdminConstants.ADMINAGENT);
        gms.addActionFactory(new JoinNotificationActionFactoryImpl(this));
        gms.addActionFactory(new JoinedAndReadyNotificationActionFactoryImpl(this));
    }

    // returns true if shutdown was successful, false if shutdown was a result of a timeout
    public int waitTillNotified() {
        gmsLogger.fine("GMSAdminAgent: entering waitTillNotified");

        if (isAdmin) {

            if (lifeTime == 0) {
                // only wait on startup if we AREN"T using specific times
                if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                    gmsLogger.log(DEBUG_LEVEL, "Waiting for startup to begin");
                }
                synchronized (startupInitiated) {
                    try {
                        startupInitiated.wait(0);
                    } catch (InterruptedException ie) {
                    }
                }
                if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                    gmsLogger.log(DEBUG_LEVEL, "Startup has begun");
                }
                while (!startupComplete.get()) {
                    // using this mechanism requires the registering of JoinNotification
                    // and JoinedAndReadyNotification
                    long currentTime = System.currentTimeMillis();
                    long diff = currentTime - timeReceivedLastJoinJoinedAndReady.get();
                    // if there are not outstanding messages and the delta time is greater than 5 seconds
                    int outstanding = ((GroupManagementServiceImpl) gms).outstandingNotifications();
                    if ((outstanding == 0) && (diff >= 5000)) {
                        startupComplete.set(true);
                        synchronized (startupComplete) {
                            startupComplete.notifyAll();
                        }
                    } else {
                        if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                            gmsLogger.log(DEBUG_LEVEL, "Waiting to complete startup - outstanding=" + outstanding + ", diff:" + diff);
                        }
                        sleep(1);
                    }
                }

            }
            startupComplete.set(true);
            if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                gmsLogger.log(DEBUG_LEVEL, "Startup Complete");
            }

            // wait until the Admin receives a shutdown message from the gmsadmincli
            synchronized (NotifiedOfStateChange) {
                try {
                    NotifiedOfStateChange.wait(lifeTime);
                } catch (InterruptedException ie) {
                }
            }
            if (NotifiedOfStateChange.get() == GMSAdminConstants.SHUTDOWNCLUSTER) {

                activeMembers = gms.getGroupHandle().getCurrentCoreMembers();
                if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                    gmsLogger.log(DEBUG_LEVEL, "numberOfCoreMembers=" + activeMembers.size());
                }

                // tell the cluster, shutdown has started
                if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                    gmsLogger.log(DEBUG_LEVEL, "Sending GroupShutdown Initiated");
                }
                gms.announceGroupShutdown(groupName, GMSConstants.shutdownState.INITIATED);


                synchronized (activeMembers) {
                    try {
                        activeMembers.wait(15000); // wait till all activeMembers shutdown OR fifteen seconds
                    } catch (InterruptedException ie) {
                    }
                }
                List<String> dup = null;
                synchronized (activeMembers) {
                    // tell the cluster,  shutdown has completed
                    if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                        gmsLogger.log(DEBUG_LEVEL, "activeMembers=|" + activeMembers.toString() + "|");
                    }
                    if (activeMembers.size() > 0) {
                        // not all instances reported shutdown, so now individually
                        //   shutdown each one that is still active.
                        dup = new ArrayList<String>(activeMembers);
                    }
                }

                if (dup != null && dup.size() > 0) {
                    for (String member : dup) {
                        try {
                            gms.getGroupHandle().sendMessage(member, "adminagent", GMSAdminConstants.STOPINSTANCE.getBytes());
                        } catch (GMSException e) {
                            gmsLogger.log(Level.SEVERE, "Exception occurred while sending stopinstance message:" + e, e);
                        }
                    }
                }
                synchronized (activeMembers) {
                    try {
                        if (activeMembers.size() > 0) {
                            activeMembers.wait(15000); // wait till all remaining activeMembers have shutdown OR fifteen seconds
                        }
                        if (activeMembers.size() > 0) {
                            gmsLogger.warning("Not all instances were successfully shutdown within 15 seconds: " + activeMembers.toString());
                        }
                    } catch (InterruptedException ie) {
                    }
                }

                if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                    gmsLogger.log(DEBUG_LEVEL, "Sending GroupShutdown Completed");
                }
                gms.announceGroupShutdown(groupName, GMSConstants.shutdownState.COMPLETED);
                if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                    gmsLogger.log(DEBUG_LEVEL, "GMSAdminAgent: leaving waitTillNotified");
                }
            } else if (NotifiedOfStateChange.get() == GMSAdminConstants.KILL) {

                // this is used to inject a fatal failure
                if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                    gmsLogger.log(DEBUG_LEVEL, "Killing ourselves as instructed to do so");
                }
                Runtime.getRuntime().halt(0);
            }

        } else {

            // is an instance of the cluster
            synchronized (NotifiedOfStateChange) {
                try {
                    NotifiedOfStateChange.wait(lifeTime);
                } catch (InterruptedException ie) {
                }

                if (NotifiedOfStateChange.get() == GMSAdminConstants.KILL) {

                    // this is used to inject a fatal failure
                    if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                        gmsLogger.log(DEBUG_LEVEL, "Killing ourselves as instructed to do so");
                    }
                    Runtime.getRuntime().halt(0);
                }
            }
        }
        if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
            gmsLogger.log(DEBUG_LEVEL, "GMSAdminAgent: exiting waitTillNotified");
        }

        return NotifiedOfStateChange.get();

    }

    public GMSConstants.shutdownType getShutdownType() {
        return shutdownType;
    }

    public static void sleep(int i) {
        try {
            Thread.sleep(i * 1000);

        } catch (InterruptedException ex) {
        }
    }

    public synchronized void processNotification(final Signal notification) {
        final String from = notification.getMemberToken();

        if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
            gmsLogger.log(DEBUG_LEVEL, "Received a NOTIFICATION from member: " + from);
        }
        // PLANNEDSHUTDOWN  HANDLING
        if (notification instanceof PlannedShutdownSignal) {
            // don't processess gmsadmincli shutdown messages
            if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                gmsLogger.log(DEBUG_LEVEL, "Received PlannedShutdownNotification from member " + from);
            }
            if (isAdmin) {
                if (!from.equals(GMSAdminConstants.APPLICATIONADMIN)) {
                    synchronized (activeMembers) {
                        if (!activeMembers.remove(from)) {
                            gmsLogger.severe("Received more than one plannedshutdown from:" + from);
                        }
                        if (activeMembers.size() == 0) {
                            activeMembers.notifyAll();
                        }
                    }
                }
            } else {
                PlannedShutdownSignal psSignal = (PlannedShutdownSignal) notification;
                if (psSignal.getEventSubType().equals(GMSConstants.shutdownType.GROUP_SHUTDOWN)) {
                    shutdownType = GMSConstants.shutdownType.GROUP_SHUTDOWN;
                    NotifiedOfStateChange.set(GMSAdminConstants.SHUTDOWNCLUSTER);

                    synchronized (NotifiedOfStateChange) {
                        NotifiedOfStateChange.notifyAll();
                    }
                }
            }
            // }
            // MESSAGE HANDLING
        } else if (notification instanceof MessageSignal) {
            MessageSignal messageSignal = (MessageSignal) notification;
            String msgString = new String(messageSignal.getMessage());
            if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                gmsLogger.log(DEBUG_LEVEL, "Message received was:" + msgString);
            }
            if (msgString.equals(GMSAdminConstants.STOPCLUSTER)) {
                // only allow admin to stop cluster
                if (isAdmin) {
                    if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                        gmsLogger.log(DEBUG_LEVEL, "Received stopcluster from member " + from);
                    }
                    try {
                        if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                            gmsLogger.log(DEBUG_LEVEL, "Sending stop cluster reply to member " + from);
                        }
                        gms.getGroupHandle().sendMessage(from, GMSAdminConstants.APPLICATIONADMIN, GMSAdminConstants.STOPCLUSTERREPLY.getBytes());
                        if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                            gmsLogger.log(DEBUG_LEVEL, "Done sending stopcluster reply to member " + from);
                        }

                    } catch (GMSException ge1) {
                        gmsLogger.log(Level.SEVERE, "Exception occurred while sending reply message: " + GMSAdminConstants.STOPCLUSTERREPLY + ge1, ge1);
                    }
                    NotifiedOfStateChange.set(GMSAdminConstants.SHUTDOWNCLUSTER);
                    synchronized (NotifiedOfStateChange) {
                        NotifiedOfStateChange.notifyAll();                    //}
                    }
                } else {
                    if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                        gmsLogger.log(DEBUG_LEVEL, "Ignoring " + GMSAdminConstants.STOPCLUSTER + " since we are not the admin");
                    }
                }
            } else if (msgString.equals(GMSAdminConstants.STOPINSTANCE)) {
                if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                    gmsLogger.log(DEBUG_LEVEL, "Received instance stop from member " + from);
                }
                try {
                    if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                        gmsLogger.log(DEBUG_LEVEL, "Sending stop instance reply to member " + from);
                    }
                    gms.getGroupHandle().sendMessage(from, GMSAdminConstants.APPLICATIONADMIN, GMSAdminConstants.STOPINSTANCEREPLY.getBytes());
                    if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                        gmsLogger.log(DEBUG_LEVEL, "Done sending stop instance reply to member " + from);
                    }

                } catch (GMSException ge1) {
                    gmsLogger.log(Level.SEVERE, "Exception occurred while sending reply message: " + GMSAdminConstants.STOPINSTANCEREPLY + ge1, ge1);
                }
                NotifiedOfStateChange.set(GMSAdminConstants.STOP);

                synchronized (NotifiedOfStateChange) {
                    NotifiedOfStateChange.notifyAll();                    //}
                }
            } else if (msgString.equals(GMSAdminConstants.KILLINSTANCE)) {
                if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                    gmsLogger.log(DEBUG_LEVEL, "Received kill instance from member " + from);
                }
                try {
                    if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                        gmsLogger.log(DEBUG_LEVEL, "Sending kill instance reply to member " + from);
                    }
                    gms.getGroupHandle().sendMessage(from, GMSAdminConstants.APPLICATIONADMIN, GMSAdminConstants.KILLINSTANCEREPLY.getBytes());
                    if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                        gmsLogger.log(DEBUG_LEVEL, "Done sending kill instance reply to member " + from);
                    }

                } catch (GMSException ge1) {
                    gmsLogger.log(Level.SEVERE, "Exception occurred while sending reply message: " + GMSAdminConstants.KILLINSTANCEREPLY + ge1, ge1);
                }
                NotifiedOfStateChange.set(GMSAdminConstants.KILL);

                synchronized (NotifiedOfStateChange) {
                    NotifiedOfStateChange.notifyAll();                    //}
                }
            } else if (msgString.equals(GMSAdminConstants.ISSTARTUPCOMPLETE)) {
                // this message is only for master
                if (isAdmin) {
                    if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                        gmsLogger.log(DEBUG_LEVEL, "Received isstartupcomplete from member " + from);
                    }
                    if (startupComplete.get() == false) {
                        if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                            gmsLogger.log(DEBUG_LEVEL, "Waiting for startup to complete before sending back message");
                        }
                        synchronized (startupComplete) {
                            try {
                                startupComplete.wait(0);
                            } catch (InterruptedException ie) {
                            }
                        }
                    }
                    if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                        gmsLogger.log(DEBUG_LEVEL, "Startup Complete detected, ok to reply");
                    }
                    try {
                        if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                            gmsLogger.log(DEBUG_LEVEL, "Sending isstartupcomplete reply to member " + from);
                        }
                        gms.getGroupHandle().sendMessage(from, GMSAdminConstants.APPLICATIONADMIN, GMSAdminConstants.ISSTARTUPCOMPLETEREPLY.getBytes());
                        if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                            gmsLogger.log(DEBUG_LEVEL, "Done sending isstartupcomplete reply to member " + from);
                        }

                    } catch (GMSException ge1) {
                        gmsLogger.log(Level.SEVERE, "Exception occurred while sending reply message: " + GMSAdminConstants.ISSTARTUPCOMPLETEREPLY + ge1, ge1);
                    }
                } else {
                    if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                        gmsLogger.log(DEBUG_LEVEL, "Ignoring " + GMSAdminConstants.ISSTARTUPCOMPLETE + " since we are not the admin");
                    }
                }
            } else {
                if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                    gmsLogger.log(DEBUG_LEVEL, "Ignoring message:" + msgString);
                }
            }
        } else if (notification instanceof JoinNotificationSignal) {
            if (isAdmin) {

                // if we are the master and we've received an add from someone else but not the application admin
                if (!from.equals(GMSAdminConstants.ADMINNAME) && !from.equals(GMSAdminConstants.APPLICATIONADMIN)) {
                    if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                        gmsLogger.log(DEBUG_LEVEL, "JOIN NOTIFICATION was received ");
                        gmsLogger.log(DEBUG_LEVEL, "STARTUP INITIATED ");
                    }
                    if (startupInitiated.get() == false) {
                        startupInitiated.set(true);
                        synchronized (startupInitiated) {
                            startupInitiated.notifyAll();
                        }
                    }
                }
                timeReceivedLastJoinJoinedAndReady.set(System.currentTimeMillis());
            }
        } else if (notification instanceof JoinedAndReadyNotificationSignal) {

            if (isAdmin) {
                // if we are the master and we've received an add from someone else but not the application admin
                if (!from.equals(GMSAdminConstants.ADMINNAME) && !from.equals(GMSAdminConstants.APPLICATIONADMIN)) {
                    if (gmsLogger.isLoggable(DEBUG_LEVEL)) {
                        gmsLogger.log(DEBUG_LEVEL, "JOINANDREADY NOTIFICATION was received");
                    }
                    timeReceivedLastJoinJoinedAndReady.set(System.currentTimeMillis());
                }
            }

        }
    }
}
