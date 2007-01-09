/*
 * Copyright 2004-2005 Sun Microsystems, Inc.  All rights reserved.
 * Use is subject to license terms.
 */
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
package com.sun.enterprise.ee.cms.impl.common;

import com.sun.enterprise.ee.cms.core.*;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Routes signals to appropriate destinations
 * @author Shreedhar Ganapathy
 * Date: Jan 16, 2004
 * @version $Revision$
 */
public class Router{
    private final Vector<FailureNotificationActionFactory>
        failureNotificationAF = new Vector<FailureNotificationActionFactory>();

    private final Hashtable<String, FailureRecoveryActionFactory> failureRecoveryAF =
            new Hashtable<String, FailureRecoveryActionFactory>();

    private final  Hashtable<String,MessageActionFactory> messageAF =
            new Hashtable<String, MessageActionFactory>();

    private final Vector<PlannedShutdownActionFactory> plannedShutdownAF =
            new Vector<PlannedShutdownActionFactory>();

    private final Vector<JoinNotificationActionFactory> joinNotificationAF =
            new Vector<JoinNotificationActionFactory>();
    private Vector<FailureSuspectedActionFactory> failureSuspectedAF =
            new Vector<FailureSuspectedActionFactory>();

    private final QueueHelper queHelper;
    private final Logger logger = GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);
    private final ExecutorService actionPool;
    private long startupTime;
    private static final int GROUP_WARMUP_TIME = 30000;

    public Router(){
        queHelper = new QueueHelper();
        final SignalHandler signalHandler = new SignalHandler(queHelper, this);
        new Thread(signalHandler, this.getClass().getCanonicalName()+" Thread").start();        
        actionPool = Executors.newCachedThreadPool();
        startupTime = System.currentTimeMillis();
    }

    /**
     * adds a FailureNotificationActionFactory as a destination.
     * Collects this actionfactory in a Collection of same type.
     * @param failureNotificationActionFactory
     */
    void addDestination(
      final FailureNotificationActionFactory failureNotificationActionFactory)
    {
        failureNotificationAF.add(failureNotificationActionFactory);
    }

    /**
     * adds a FailureRecoveryActionFactory as a destination.
     * Collects this actionfactory in a Collection of same type.
     * @param  componentName
     * @param failureRecoveryActionFactory
     */
    void addDestination( final String componentName,
            final FailureRecoveryActionFactory failureRecoveryActionFactory)
    {
        failureRecoveryAF.put(componentName, failureRecoveryActionFactory);
    }

    /**
     * adds a JoinNotificationActionFactory as a destination.
     * Collects this actionfactory in a Collection of same type.
     * @param joinNotificationActionFactory
     */
    void addDestination(
            final JoinNotificationActionFactory joinNotificationActionFactory)
    {
        joinNotificationAF.add(joinNotificationActionFactory);
    }

    /**
     * adds a PlannedShutdownActionFactory as a destination.
     * Collects this actionfactory in a Collection of same type.
     * @param plannedShutdownActionFactory
     */
    void addDestination(
            final PlannedShutdownActionFactory plannedShutdownActionFactory)
    {
        plannedShutdownAF.add(plannedShutdownActionFactory);
    }

    /**
     * adds a FailureSuspectedActionFactory as a destination.
     * Collects this actionfactory in a Collection of same type.
     * @param failureSuspectedActionFactory
     */
    void addDestination(
            final FailureSuspectedActionFactory failureSuspectedActionFactory)
    {
        failureSuspectedAF.add(failureSuspectedActionFactory);
    }

    /**
     * adds a MessageActionFactory as a destination for a given component name.
     */
     void addDestination(final MessageActionFactory messageActionFactory,
                         final String componentName)
    {
        messageAF.put(componentName, messageActionFactory);
    }

    /**
     * removes a FailureNotificationActionFactory destination.
     * @param failureNotificationActionFactory
     */
    void removeDestination(
     final FailureNotificationActionFactory failureNotificationActionFactory)
    {
        failureNotificationAF.remove(failureNotificationActionFactory);
    }

    /**
     * removes a JoinNotificationActionFactory destination.
     * @param joinNotificationActionFactory
     */
    void removeDestination(
            final JoinNotificationActionFactory joinNotificationActionFactory) {
        joinNotificationAF.remove(joinNotificationActionFactory);
    }

    /**
     * removes a PlannedShutdownActionFactory destination.
     * @param plannedShutdownActionFactory
     */
    void removeDestination(
            final PlannedShutdownActionFactory plannedShutdownActionFactory) {
        plannedShutdownAF.remove(plannedShutdownActionFactory);
    }

    /**
     * removes a PlannedShutdownActionFactory destination.
     * @param failureSuspectedActionFactory
     */
    void removeDestination(
            final FailureSuspectedActionFactory failureSuspectedActionFactory) {
        failureSuspectedAF.remove(failureSuspectedActionFactory);
    }

    /**
     * removes a MessageActionFactory instance belonging to a specified
     * component
     * @param componentName
     */
    public void removeMessageAFDestination(final String componentName)
    {
        messageAF.remove(componentName);
    }

    /**
     * removes a FailureRecoveryActionFactory instance belonging to a specified
     * component
     * @param componentName
     */
    public void removeFailureRecoveryAFDestination(final String componentName)
    {
        messageAF.remove(componentName);
    }

    /**
     * Queues signals.  Expects an array of signals which are handed off
     * to working threads that will determine their corresponding actions
     * to call their consumeSignal method.
     * @param signalPacket
     */
    public void queueSignals(final SignalPacket signalPacket){
        queHelper.put(signalPacket);
    }

    /**
     * Adds a single signal to the queue.
     * @param signalPacket
     */
    public void queueSignal(final SignalPacket signalPacket) {
        queHelper.put(signalPacket);
    }

    void undocketAllDestinations() {
        synchronized(failureRecoveryAF){
            failureRecoveryAF.clear();
        }
        synchronized(failureNotificationAF){
            failureNotificationAF.removeAllElements();
        }
        synchronized(plannedShutdownAF){
            plannedShutdownAF.removeAllElements();
        }
        synchronized(joinNotificationAF){
            joinNotificationAF.removeAllElements();
        }
        synchronized(messageAF){
            messageAF.clear();
        }
        synchronized( failureSuspectedAF){
            failureSuspectedAF.removeAllElements();
        }
    }

    void notifyFailureNotificationAction(final FailureNotificationSignal signal)
    {
        FailureNotificationAction a;
        FailureNotificationSignal fns;
        logger.log(Level.INFO,"Sending FailureNotificationSignals to register" +
                "ed Actions. Member: "+signal.getMemberToken()+"...");
        synchronized(failureNotificationAF){
            for(FailureNotificationActionFactory fnaf: failureNotificationAF)
            {
                a = (FailureNotificationAction) fnaf.produceAction();
                fns = new FailureNotificationSignalImpl(signal);
                callAction(a, fns);
            }
        }
    }

    void notifyFailureRecoveryAction(final FailureRecoverySignal signal) {
        final FailureRecoveryAction a;
        final FailureRecoverySignal frs;
        logger.log(Level.INFO,
               "Sending FailureRecoveryNotification to component "+
               signal.getComponentName());
        synchronized(failureRecoveryAF){
                final FailureRecoveryActionFactory fraf =
                        failureRecoveryAF.get( signal.getComponentName());
                a = (FailureRecoveryAction) fraf.produceAction();
                frs = new FailureRecoverySignalImpl(signal);
                callAction(a, frs);
        }
    }

    void notifyFailureSuspectedAction(final FailureSuspectedSignal signal)
    {
        FailureSuspectedAction a;
        FailureSuspectedSignal fss;
        logger.log(Level.INFO,"Sending FailureSuspectedSignals to register" +
                "ed Actions. Member:"+signal.getMemberToken()+"...");
        synchronized(failureSuspectedAF){
            for(FailureSuspectedActionFactory fsaf: failureSuspectedAF)
            {
                a = (FailureSuspectedAction) fsaf.produceAction();
                fss = new FailureSuspectedSignalImpl(signal);
                callAction(a, fss);
            }
        }
    }

    void notifyMessageAction(final MessageSignal signal) {
        MessageAction a;
        MessageActionFactory maf;
        String target;
        synchronized(messageAF){
            for(Enumeration<String> i=messageAF.keys();
                i.hasMoreElements();)
            {
                target = i.nextElement();
                if(target.equals(signal.getTargetComponent())) {
                    maf = messageAF.get( target );
                    a = (MessageAction) maf.produceAction();
                    try{
                        //due to message ordering requirements, 
                        //this call is not delegated to a the thread pool
                        a.consumeSignal(signal);
                    }
                    catch(ActionException e){
                        logger.log(Level.WARNING, 
                                new StringBuffer("ActionException:")
                                .append(e.getLocalizedMessage())
                                .toString());
                    }
                }
            }
        }
    }

    void notifyJoinNotificationAction(final JoinNotificationSignal signal) {
        JoinNotificationAction a;
        JoinNotificationSignal jns;
        //todo: NEED to be able to predetermine the number of GMS clients 
        //that would register for join notifications.
        if(isJoinNotificationAFRegistered()){
            logger.log(Level.FINE,
                  "Sending JoinNotificationSignals to registered Actions, Member"
                          +signal.getMemberToken()+"...");
            synchronized(joinNotificationAF){
                for(JoinNotificationActionFactory jnaf : joinNotificationAF) {
                    a = (JoinNotificationAction) jnaf.produceAction();
                    jns = new JoinNotificationSignalImpl(signal);
                    callAction(a, jns);
                }
            }
        }
        else if(System.currentTimeMillis() - startupTime < GROUP_WARMUP_TIME){
            // put it back to the queue if it is less than
            // 30 secs since start time. we give 30 secs for join notif
            // registrations to happen until which time, the signals are
            // available in queue. 
            queHelper.put(new SignalPacket(signal));
        }
    }

    void notifyPlannedShutdownAction(final PlannedShutdownSignal signal) {
        PlannedShutdownAction a;
        PlannedShutdownSignal pss;
        logger.log(Level.INFO,
                "Sending PlannedShutdownSignals to registered Actions "+
                "for shutdownType "+signal.getEventSubType()+" Member: "+
                        signal.getMemberToken()+"...");
        synchronized(plannedShutdownAF){
            for(PlannedShutdownActionFactory psaf : plannedShutdownAF) {
                a = (PlannedShutdownAction) psaf.produceAction();
                pss = new PlannedShutdownSignalImpl(signal); 
                callAction(a,  pss);
            }
        }
    }

    private void callAction(final Action a, final Signal signal) {
        try {
            final CallableAction task = new CallableAction(a, signal);
            actionPool.submit(task);
        } catch (RejectedExecutionException e) {
            logger.log(Level.WARNING, e.getMessage());
        }
    }

    public boolean isFailureNotificationAFRegistered() {
        boolean retval=true;
        synchronized(failureNotificationAF){
            if(failureNotificationAF.isEmpty())
                retval = false;
        }
        return retval;
    }

    public boolean isFailureRecoveryAFRegistered(){
        boolean retval = true;
        synchronized(failureRecoveryAF){
            if(failureRecoveryAF.isEmpty())
                retval= false;
        }
        return retval;
    }

    public boolean isMessageAFRegistered(){
        boolean retval = true;
        synchronized(messageAF){
            if(messageAF.isEmpty())
                retval =false;
        }
        return retval;
    }

    public boolean isPlannedShutdownAFRegistered() {
        boolean retval = true;
        synchronized(plannedShutdownAF){
            if(plannedShutdownAF.isEmpty())
                retval= false;
        }
        return retval;
    }

    public boolean isJoinNotificationAFRegistered() {
        boolean retval = true;
        synchronized(joinNotificationAF){
            if(joinNotificationAF.isEmpty())
                retval= false;
        }
        return retval;
    }

    public boolean isFailureSuspectedAFRegistered () {
        boolean retval = true;
        synchronized( failureSuspectedAF ) {
            if(failureSuspectedAF.isEmpty()){
                retval = false;
            }
        }
        return retval;
    }

    Hashtable<String, FailureRecoveryActionFactory>
            getFailureRecoveryAFRegistrations()
    {
        return
        new Hashtable<String, FailureRecoveryActionFactory>(failureRecoveryAF);
    }

    public Set<String> getFailureRecoveryComponents () {
        logger.log(Level.FINEST, "Router Returning failure rec comps="+
                                 failureRecoveryAF.keySet());
        return failureRecoveryAF.keySet();
    }

    /**
     * implements Callable. Used for handing off the job of calling the Action's
     * consumeSignal() method to a ThreadPool.
     */
    private static class CallableAction implements Callable<Object> {
        private Action action;
        private Signal signal;
        CallableAction(final Action action, final Signal signal ){
            this.action = action;
            this.signal = signal;
        }
        public Object call() throws ActionException {
            action.consumeSignal(signal);
            return null;
        }        
    }
}
