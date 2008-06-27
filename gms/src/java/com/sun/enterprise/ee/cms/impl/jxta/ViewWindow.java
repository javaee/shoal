/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.enterprise.ee.cms.impl.jxta;

import com.sun.enterprise.ee.cms.core.*;
import com.sun.enterprise.ee.cms.impl.common.*;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import com.sun.enterprise.jxtamgmt.ClusterView;
import com.sun.enterprise.jxtamgmt.ClusterViewEvents;
import com.sun.enterprise.jxtamgmt.SystemAdvertisement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Shreedhar Ganapathy
 *         Date: Jun 26, 2006
 * @version $Revision$
 */
class ViewWindow implements com.sun.enterprise.ee.cms.impl.common.ViewWindow, Runnable {
    private GMSContext ctx;
    private Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    private int size = 100;  // 100 is some default.
    private final List<ArrayList<GMSMember>> views = new Vector<ArrayList<GMSMember>>();
    private List<Signal> signals = new Vector<Signal>();
    private final List<String> currentCoreMembers = new ArrayList<String>();
    private final List<String> allCurrentMembers = new ArrayList<String>();
    private static final String CORETYPE = "CORE";
    //This is for DSC cache syncups so that member details are locally available
    //to GMS clients when they ask for it with the Join signals.
    private static final int SYNCWAITMILLIS = 3000;
    private static final String REC_PROGRESS_STATE = GroupManagementService.RECOVERY_STATE.RECOVERY_IN_PROGRESS.toString();
    private static final String REC_APPOINTED_STATE = GroupManagementService.RECOVERY_STATE.RECOVERY_SERVER_APPOINTED.toString();
    private final ArrayBlockingQueue<EventPacket> viewQueue;
    private final String groupName;

    ViewWindow(final String groupName, final ArrayBlockingQueue<EventPacket> viewQueue) {
        this.groupName = groupName;
        this.viewQueue = viewQueue;
    }

    private GMSContext getGMSContext() {
        if (ctx == null) {
            ctx = (GMSContext) GMSContextFactory.getGMSContext(groupName);
        }
        return ctx;
    }

    public void run() {

        while (!getGMSContext().isShuttingDown()) {
            final EventPacket packet;
            try {
                packet = viewQueue.take();
                logger.log(Level.FINE, "ViewWindow : processing a received view " + packet.getClusterViewEvent());
                if (packet != null) {
                    newViewObserved(packet);
                }
            } catch (InterruptedException e) {
                logger.log(Level.FINEST, e.getLocalizedMessage());
            }
        }
    }

    private void newViewObserved(final EventPacket packet) {
        synchronized (views) {
            views.add(getMemberTokens(packet));
            if (views.size() > size) {
                views.remove(0);
            }
            logger.log(Level.INFO, "membership.snapshot.analysis", new Object[]{packet.getClusterViewEvent().toString()});
            Signal[] activeSignals = analyzeViewChange(packet);

            if (activeSignals.length != 0) {
                getGMSContext().getRouter().queueSignals(new SignalPacket(activeSignals));
            }
        }
    }

    private ArrayList<GMSMember> getMemberTokens(final EventPacket packet) {
        synchronized (currentCoreMembers) {
            currentCoreMembers.clear();
        }
        synchronized (allCurrentMembers) {
            allCurrentMembers.clear();
        }
        final List<GMSMember> tokens = new ArrayList<GMSMember>(); // contain list of GMSMember objects.
        ClusterView view = packet.getClusterView();
        GMSMember member;
        SystemAdvertisement advert;
        int count = 0;
        final StringBuffer sb =
                new StringBuffer("GMS View Change Received for group ").append(groupName).append(" : Members in view for ").append("(before change analysis) are :\n");
        for (SystemAdvertisement systemAdvertisement : view.getView()) {
            advert = systemAdvertisement;
            member = getGMSMember(advert);
            member.setSnapShotId(view.getClusterViewId());
            sb.append(++count)
                    .append(": MemberId: ")
                    .append(member.getMemberToken())
                    .append(", MemberType: ")
                    .append(member.getMemberType())
                    .append(", Address: ")
                    .append(advert.getID().toString()).append('\n');
            if (member.getMemberType().equals(CORETYPE)) {
                synchronized (currentCoreMembers) {
                    currentCoreMembers.add(
                            new StringBuffer(member.getMemberToken())
                                    .append("::")
                                    .append(member.getStartTime()).toString());
                }
            }
            tokens.add(member);
            synchronized (allCurrentMembers) {
                allCurrentMembers.add(new StringBuffer()
                        .append(member.getMemberToken())
                        .append("::")
                        .append(member.getStartTime()).toString());
            }
        }
        logger.log(Level.INFO, sb.toString());
        return (ArrayList<GMSMember>) tokens;
    }

    private GMSMember getGMSMember(final SystemAdvertisement systemAdvertisement) {
        GMSMember member;
        try {
            member = new GMSMember(systemAdvertisement.getName(),
                    systemAdvertisement.getCustomTagValue(
                            CustomTagNames.MEMBER_TYPE.toString()),
                    systemAdvertisement.getCustomTagValue(
                            CustomTagNames.GROUP_NAME.toString()),
                    Long.valueOf(systemAdvertisement.getCustomTagValue(CustomTagNames.START_TIME.toString())));
        } catch (NoSuchFieldException e) {
            logger.log(Level.WARNING,
                    new StringBuffer("SystemAdvertisement did not contain one of the ")
                            .append("specified tag values:")
                            .append(e.getLocalizedMessage()).toString());
            member = new GMSMember(systemAdvertisement.getName(), null, null, null);
        }
        return member;
    }

    private Signal[] analyzeViewChange(final EventPacket packet) {
        ((Vector) signals).removeAllElements();
        final ClusterViewEvents events = packet.getClusterViewEvent();
        switch (events) {
            case ADD_EVENT:
                addNewMemberJoins(packet);
                break;
            case CLUSTER_STOP_EVENT:
                addPlannedShutdownSignals(packet);
                break;
            case FAILURE_EVENT:
                addFailureSignals(packet);
                break;
            case IN_DOUBT_EVENT:
                addInDoubtMemberSignals(packet);
                break;
            case JOINED_AND_READY_EVENT:
                addReadyMembers(packet);
                break;
            case MASTER_CHANGE_EVENT:
                analyzeMasterChangeView(packet);
                break;
            case NO_LONGER_INDOUBT_EVENT:
                addNewMemberJoins(packet);
                break;
            case PEER_STOP_EVENT:
                addPlannedShutdownSignals(packet);
                break;
        }
 
        final Signal[] s = new Signal[signals.size()];
        return signals.toArray(s);
    }

    private void analyzeMasterChangeView(final EventPacket packet) {
        if(views.size() == 1){ //views list only contains 1 view which is assumed to be the 1st view.
            addNewMemberJoins(packet);
        }
        if (views.size() > 1 &&
                packet.getClusterView().getSize() !=
                        views.get(views.size() - 2).size()) {
            determineAndAddNewMemberJoins();
        }
    }

    private void determineAndAddNewMemberJoins() {
        final List<GMSMember> newMembership = views.get(views.size() - 1);
        String token;
        if (views.size() == 1) {
            if (newMembership.size() > 1) {
                for (GMSMember member : newMembership) {
                    token = member.getMemberToken();
                    if (!token.equals(getGMSContext().getServerIdentityToken())) {
                        syncDSC(token);
                    }
                    if (member.getMemberType().equalsIgnoreCase(CORETYPE)) {
                        addJoinNotificationSignal(token, member.getGroupName(),
                                member.getStartTime());
                    }
                }
            }
        } else if (views.size() > 1) {
            final List<String> oldMembers = getTokens(views.get(views.size() - 2));
            for (GMSMember member : newMembership) {
                token = member.getMemberToken();
                if (!oldMembers.contains(token)) {
                    syncDSC(token);
                    if (member.getMemberType().equalsIgnoreCase(CORETYPE)) {
                        addJoinNotificationSignal(token, member.getGroupName(),
                                member.getStartTime());
                    }
                }
            }
        }
    }

    private List<String> getTokens(final List<GMSMember> oldMembers) {
        final List<String> tokens = new ArrayList<String>();
        for (GMSMember member : oldMembers) {
            tokens.add(member.getMemberToken());
        }
        return tokens;
    }

    private void addPlannedShutdownSignals(final EventPacket packet) {
        final SystemAdvertisement advert = packet.getSystemAdvertisement();
        final String token = advert.getName();
        final DistributedStateCache dsc = getGMSContext().getDistributedStateCache();
        try {
            final GMSConstants.shutdownType shutdownType;
            if (packet.getClusterViewEvent().equals(ClusterViewEvents.CLUSTER_STOP_EVENT)) {
                shutdownType = GMSConstants.shutdownType.GROUP_SHUTDOWN;
            } else {
                shutdownType = GMSConstants.shutdownType.INSTANCE_SHUTDOWN;
                if (dsc != null) {
                    dsc.removeAllForMember(token);
                }
            }
            //logger.log(Level.INFO, "gms.plannedShutdownEventReceived", token);
            logger.log(Level.INFO, "plannedshutdownevent.announcement", new Object[]{token, shutdownType});
            signals.add(new PlannedShutdownSignalImpl(token,
                         advert.getCustomTagValue(CustomTagNames.GROUP_NAME.toString()),
                         Long.valueOf(advert.getCustomTagValue(CustomTagNames.START_TIME.toString())), shutdownType));
        } catch (NoSuchFieldException e) {
            logger.log(Level.WARNING, "systemadv.not.contain.customtag", new Object[]{e.getLocalizedMessage()});
        }
    }

    private void addInDoubtMemberSignals(final EventPacket packet) {
        final SystemAdvertisement advert = packet.getSystemAdvertisement();
        final String token = advert.getName();
        getGMSContext().addToSuspectList(token);
        try {
            logger.log(Level.INFO, "gms.failureSuspectedEventReceived", token);
            signals.add(new FailureSuspectedSignalImpl(token,
                          advert.getCustomTagValue(CustomTagNames.GROUP_NAME.toString()),
                          Long.valueOf(advert.getCustomTagValue(CustomTagNames.START_TIME.toString()))));
        } catch (NoSuchFieldException e) {
            logger.log(Level.WARNING, "systemadv.not.contain.customtag", new Object[]{e.getLocalizedMessage()});
        }
    }

    private void addFailureSignals(final EventPacket packet) {
        final SystemAdvertisement advert = packet.getSystemAdvertisement();
        final String token = advert.getName();
        try {
            final String type = advert.getCustomTagValue(CustomTagNames.MEMBER_TYPE.toString());
            if (type.equalsIgnoreCase(CORETYPE)) {
                logger.log(Level.INFO, "member.failed", new Object[]{token});
                generateFailureRecoverySignals(views.get(views.size() - 2),
                        token,
                        advert.getCustomTagValue(CustomTagNames.GROUP_NAME.toString()),
                        Long.valueOf(advert.getCustomTagValue(CustomTagNames.START_TIME.toString())));

                if (getGMSContext().getRouter().isFailureNotificationAFRegistered()) {
                    signals.add(new FailureNotificationSignalImpl(token,
                                  advert.getCustomTagValue(CustomTagNames.GROUP_NAME.toString()),
                                  Long.valueOf(advert.getCustomTagValue(CustomTagNames.START_TIME.toString()))));
                }

                logger.fine("removing newly added node from the suspected list..." + token);
                getGMSContext().removeFromSuspectList(token);
            }
        } catch (NoSuchFieldException e) {
            logger.log(Level.WARNING, "systemadv.not.contain.customtag", new Object[]{e.getLocalizedMessage()});
        }
    }

    private void generateFailureRecoverySignals(final List<GMSMember> oldMembership,
                                                final String token,
                                                final String groupName,
                                                final Long startTime) {

        final Router router = getGMSContext().getRouter();
        //if Recovery notification is registered then
        if (router.isFailureRecoveryAFRegistered()) {
            logger.log(Level.FINE, "Determining the recovery server..");
            //determine if we are recovery server
            if (RecoveryTargetSelector.resolveRecoveryTarget(null, oldMembership, token, getGMSContext())) {
                //this is a list containing failed members who were in the
                //process of being recovered.i.e. state was RECOVERY_IN_PROGRESS
                final List<String> recInProgressMembers = getRecoveriesInProgressByFailedMember(token);
                //this is a list of failed members (who are still dead)
                // for whom the failed member here was appointed as recovery
                // server.
                final List<String> recApptsHeldByFailedMember = getRecApptsHeldByFailedMember(token);
                for (final String comp : router.getFailureRecoveryComponents()) {
                    logger.log(Level.FINE, new StringBuffer("adding failure recovery signal for component=").append(comp).toString());
                    signals.add(new FailureRecoverySignalImpl(comp, token, groupName, startTime));
                    if (!recInProgressMembers.isEmpty()) {
                        for (final String fToken : recInProgressMembers) {
                            signals.add(new FailureRecoverySignalImpl(comp, fToken, groupName, 0));
                        }
                    }
                    if (!recApptsHeldByFailedMember.isEmpty()) {
                        for (final String fToken : recApptsHeldByFailedMember) {
                            signals.add(new FailureRecoverySignalImpl(comp, fToken, groupName, 0));
                        }
                    }
                }
            }
        }
    }

    private List<String> getRecApptsHeldByFailedMember(final String token) {
        final List<String> tokens = new ArrayList<String>();
        final DistributedStateCache dsc = getGMSContext().getDistributedStateCache();
        final Map<GMSCacheable, Object> entries = dsc.getFromCache(token);
        for (GMSCacheable gmsCacheable : entries.keySet()) {
            //if this failed member was appointed for recovering someone else
            if (token.equals(gmsCacheable.getMemberTokenId()) && !token.equals(gmsCacheable.getKey())) {
                final Object entry = entries.get(gmsCacheable);
                if (entry instanceof String) {
                    if (((String) entry).startsWith(REC_APPOINTED_STATE) && !currentCoreMembers.contains(gmsCacheable.getKey())) {
                        //if the target member is already up dont include that
                        logger.log(Level.FINER, new StringBuffer("Failed Member ")
                                .append(token)
                                .append(" was appointed for recovery of ")
                                .append(gmsCacheable.getKey())
                                .append(" when ").append(token)
                                .append(" failed. ")
                                .append("Adding to recovery-appointed list...")
                                .toString());
                        tokens.add((String) gmsCacheable.getKey());
                        try {
                            dsc.removeFromCache(gmsCacheable.getComponentName(),
                                    gmsCacheable.getMemberTokenId(),
                                    (Serializable) gmsCacheable.getKey());
                            RecoveryTargetSelector.setRecoverySelectionState(
                                    getGMSContext().getServerIdentityToken(), (String) gmsCacheable.getKey(),
                                    getGMSContext().getGroupName());
                        } catch (GMSException e) {
                            logger.log(Level.WARNING, e.getLocalizedMessage());
                        }
                    }
                }
            }
        }
        return tokens;
    }

    private List<String> getRecoveriesInProgressByFailedMember(final String token) {
        final List<String> tokens = new ArrayList<String>();
        final DistributedStateCache dsc = getGMSContext().getDistributedStateCache();
        final Map<GMSCacheable, Object> entries = dsc.getFromCache(token);

        for (GMSCacheable gmsCacheable : entries.keySet()) {
            //if this member is recovering someone else
            if (token.equals(gmsCacheable.getMemberTokenId()) && !token.equals(gmsCacheable.getKey())) {
                final Object entry = entries.get(gmsCacheable);
                if (entry instanceof String) {
                    if (((String) entry).startsWith(REC_PROGRESS_STATE)) {
                        logger.log(Level.FINER, new StringBuffer("Failed Member ").append(token)
                                .append(" had recovery-in-progress for ")
                                .append(gmsCacheable.getKey()).append(" when ")
                                .append(token).append(" failed. ").toString());
                        tokens.add((String) gmsCacheable.getKey());
                        RecoveryTargetSelector.setRecoverySelectionState(
                                getGMSContext().getServerIdentityToken(),
                                (String) gmsCacheable.getKey(), getGMSContext().getGroupName());
                    }
                }
            }
        }
        return tokens;
    }

    private void addNewMemberJoins(final EventPacket packet) {
        final SystemAdvertisement advert = packet.getSystemAdvertisement();
        final String token = advert.getName();
        if (packet.getClusterView().getSize() > 1) {
            // TODO: Figure out a better way to sync
            syncDSC(token);
        }
        try {
            if (advert.getCustomTagValue(
                    CustomTagNames.MEMBER_TYPE.toString()).equalsIgnoreCase(CORETYPE)) {
                addJoinNotificationSignal(token,
                        advert.getCustomTagValue(
                                CustomTagNames.GROUP_NAME.toString()),
                        Long.valueOf(advert.getCustomTagValue(
                                CustomTagNames.START_TIME.toString())));
            }
        } catch (NoSuchFieldException e) {
            logger.log(Level.WARNING,
                    new StringBuffer("The SystemAdvertisement did ")
                            .append("not contain the ").append(
                            e.getLocalizedMessage())
                            .append(" custom tag value:").toString());
        }
    }

    private void addReadyMembers(final EventPacket packet) {
        final SystemAdvertisement advert = packet.getSystemAdvertisement();
        final String token = advert.getName();
        logger.log(Level.FINE, "Adding Joined And Ready member : " + token);
        try {
            if (advert.getCustomTagValue(
                    CustomTagNames.MEMBER_TYPE.toString()).equalsIgnoreCase(CORETYPE)) {
                addJoinedAndReadyNotificationSignal(token,
                        advert.getCustomTagValue(
                                CustomTagNames.GROUP_NAME.toString()),
                        Long.valueOf(advert.getCustomTagValue(
                                CustomTagNames.START_TIME.toString())));
            }
        } catch (NoSuchFieldException e) {
            logger.log(Level.WARNING,
                    new StringBuffer("The SystemAdvertisement did ")
                            .append("not contain the ").append(
                            e.getLocalizedMessage())
                            .append(" custom tag value:").toString());
        }
    }

    private void addJoinedAndReadyNotificationSignal(final String token,
                                    final String groupName,
                                    final long startTime) {
        logger.log(Level.FINE, "adding join and ready signal");
        signals.add(new JoinedAndReadyNotificationSignalImpl(token,
                getCurrentCoreMembers(),
                getAllCurrentMembers(),
                groupName,
                startTime));
    }

    private void addJoinNotificationSignal(final String token,
                                           final String groupName,
                                           final long startTime) {

        logger.log(Level.FINE, "adding join signal");
        signals.add(new JoinNotificationSignalImpl(token,
                getCurrentCoreMembers(),
                getAllCurrentMembers(),
                groupName,
                startTime));
        logger.log(Level.FINE, "gms.newMemberAdded", token);
    }

    private void syncDSC(final String token) {
        final DistributedStateCacheImpl dsc;
        // if coordinator, call dsc to sync with this member
        if (isCoordinator()) {
            logger.log(Level.FINE, "I am coordinator, performing sync ops on " + token);
            try {
                dsc = (DistributedStateCacheImpl) getGMSContext().getDistributedStateCache();
                logger.log(Level.FINER, "got DSC ref " + dsc.toString());

                // this sleep() gives the new remote member some time to receive
                // this same view change before we ask it to sync with us.
                Thread.sleep(SYNCWAITMILLIS);
                logger.log(Level.FINER, "Syncing...");
                dsc.syncCache(token, true);
                logger.log(Level.FINER, "Sync request sent..");
            } catch (GMSException e) {
                logger.log(Level.WARNING, "GMSException during DSC sync" +
                        e.getLocalizedMessage());
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, e.getLocalizedMessage());
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception during DSC sync:" + e);
                e.printStackTrace();
            }
        }
    }

    public boolean isCoordinator() {
        return getGMSContext().getGroupCommunicationProvider().isGroupLeader();
    }

    public List getPreviousView() {
        return views.get(views.size() - 2);
    }

    public List getCurrentView() {
        return views.get(views.size() - 1);
    }

    public List<String> getCurrentCoreMembers() {
        final List<String> retVal = new ArrayList<String>();
        synchronized (currentCoreMembers) {
            for (String member : currentCoreMembers) {
                member = member.substring(0, member.indexOf("::"));
                retVal.add(member);
            }
        }
        return retVal;
    }

    public List<String> getAllCurrentMembers() {
        final List<String> retVal = new ArrayList<String>();
        synchronized (allCurrentMembers) {
            for (String member : allCurrentMembers) {
                member = member.substring(0, member.indexOf("::"));
                retVal.add(member);
            }
        }
        return retVal;
    }

    public List<String> getCurrentCoreMembersWithStartTimes() {
        List<String> ret = new ArrayList<String>();
        synchronized (currentCoreMembers) {
            ret.addAll(currentCoreMembers);
        }
        return ret;
    }

    public List<String> getAllCurrentMembersWithStartTimes() {
        List<String> ret = new ArrayList<String>();
        synchronized (allCurrentMembers) {
            ret.addAll(allCurrentMembers);
        }
        return ret;
    }
}
