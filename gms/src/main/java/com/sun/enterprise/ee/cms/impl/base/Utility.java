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

package com.sun.enterprise.ee.cms.impl.base;

import com.sun.enterprise.ee.cms.core.GMSMember;
import com.sun.enterprise.ee.cms.core.GroupManagementService;
import com.sun.enterprise.ee.cms.logging.GMSLogDomain;
import com.sun.enterprise.ee.cms.logging.NiceLogFormatter;

import java.util.logging.*;
import java.util.Map;

/**
 * Utility class that can be used by any calling code to do common routines
 *
 * @author shreedhar ganapathy, Bongjae Chang
 */
public class Utility {

    private static Logger logger = GMSLogDomain.getLogger( GMSLogDomain.GMS_LOGGER );

    public static void setLogger(Logger theLogger) {
        logger = theLogger;
    }


    public static GMSMember getGMSMember( final SystemAdvertisement systemAdvertisement ) {
        GMSMember member;
        String memberType = getCustomTagValue(systemAdvertisement, CustomTagNames.MEMBER_TYPE.toString());
        String groupName = getCustomTagValue(systemAdvertisement, CustomTagNames.GROUP_NAME.toString());
        String startTimeString = getCustomTagValue(systemAdvertisement, CustomTagNames.START_TIME.toString());
        long startTime = 0L;
        try {
            startTime = Long.valueOf(startTimeString);
        } catch (NumberFormatException nfe) {
            logger.log(Level.WARNING, "Misformed SystemAdvertisement.  Missing StartingTime for member:" + systemAdvertisement.getName() + " systemAdverisement=" + systemAdvertisement.toString());
        }
        member = new GMSMember( systemAdvertisement.getName(), memberType, groupName, startTime);
        return member;
    }

    private static String getCustomTagValue(SystemAdvertisement sa, String tagName) {
        String result = null;
        try {
            result = sa.getCustomTagValue(tagName);
        } catch (NoSuchFieldException e) {
            logger.log( Level.WARNING,
                        new StringBuffer("Missing expected tag name:" + tagName + " in SystemAdvertisement for member:" + sa.getName() + ". Defaulting value to null.\n")
                                .append( e.getLocalizedMessage() ).toString(), e);
        }
        return result;
    }

    public static boolean isWatchDog( SystemAdvertisement sysAdv ) {
        GMSMember member = getGMSMember( sysAdv );
        return GroupManagementService.MemberType.WATCHDOG.toString().equalsIgnoreCase( member.getMemberType() );
    }

    public static String getStringProperty( String propertyName, String defaultValue, Map props ) {
        try {
            String value = null;
            if( props != null ) {
                Object obj = props.get( propertyName );
                if( obj instanceof String )
                    value = (String)obj;
            }
            if( value == null )
                value = System.getProperty( propertyName );
            if( value == null )
                return defaultValue;
            else
                return value;
        }
        catch( Exception e ) {
            return defaultValue;
        }
    }

    public static int getIntProperty( String propertyName, int defaultValue, Map props ) {
        try {
            String value = null;
            if( props != null ) {
                Object obj = props.get( propertyName );
                if( obj instanceof String )
                    value = (String)obj;
                else if( obj instanceof Integer )
                    return (Integer)obj;
            }
            if( value == null )
                value = System.getProperty( propertyName );
            if( value == null )
                return defaultValue;
            else
                return Integer.parseInt( value );
        }
        catch( Exception e ) {
            return defaultValue;
        }
    }

    public static long getLongProperty( String propertyName, long defaultValue, Map props ) {
        try {
            String value = null;
            if( props != null ) {
                Object obj = props.get( propertyName );
                if( obj instanceof String )
                    value = (String)obj;
                else if( obj instanceof Long )
                    return (Long)obj;
            }
            if( value == null )
                value = System.getProperty( propertyName );
            if( value == null )
                return defaultValue;
            else
                return Long.parseLong( value );
        }
        catch( Exception ex ) {
            return defaultValue;
        }
    }

    public static boolean getBooleanProperty( String propertyName, boolean defaultValue, Map props ) {
        try {
            String value = null;
            if( props != null ) {
                Object obj = props.get( propertyName );
                if( obj instanceof String )
                    value = (String)obj;
                else if( obj instanceof Boolean )
                    return (Boolean)obj;
            }
            if( value == null )
                value = System.getProperty( propertyName );
            if( value == null )
                return defaultValue;
            else
                return Boolean.parseBoolean( value );
        }
        catch( Exception e ) {
            return defaultValue;
        }
    }

    public static void setupLogHandler() {
        final ConsoleHandler consoleHandler = new ConsoleHandler();
        try {
            consoleHandler.setLevel(Level.ALL);
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
        logger.addHandler(consoleHandler);
        logger.setUseParentHandlers(false);
        final String level = System.getProperty("LOG_LEVEL", "FINEST");
        logger.setLevel(Level.parse(level));
    }

}
