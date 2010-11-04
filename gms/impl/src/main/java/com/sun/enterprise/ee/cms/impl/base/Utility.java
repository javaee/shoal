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

    private static Logger logger =
        GMSLogDomain.getLogger(GMSLogDomain.GMS_LOGGER);

    public static final long NO_SUCH_TIME = -1L;

    public static void setLogger(Logger theLogger) {
        logger = theLogger;
    }

    public static GMSMember getGMSMember( final SystemAdvertisement systemAdvertisement ) {
        GMSMember member;
        String memberType = getCustomTagValue(systemAdvertisement, CustomTagNames.MEMBER_TYPE.toString());
        String groupName = getGroupName(systemAdvertisement);
        long startTime = getStartTime(systemAdvertisement);
        member = new GMSMember( systemAdvertisement.getName(), memberType, groupName, startTime);
        return member;
    }

    private static String getCustomTagValue(SystemAdvertisement sa, String tagName) {
        String result = null;
        try {
            result = sa.getCustomTagValue(tagName);
        } catch (NoSuchFieldException e) {
            logger.log( Level.WARNING,"util.sysadv.missing.custom.tag", new Object[]{tagName, sa.getName()});
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
                if( obj instanceof String ) {
                    value = (String)obj;
                } else if( obj instanceof Long ) {
                    return (Long)obj;
                } else if (obj instanceof Integer) {
                    return ((Integer)obj).longValue();
                }
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
        final String level = System.getProperty("LOG_LEVEL", "INFO");
        try {
            logger.setLevel(Level.parse(level));
        } catch (IllegalArgumentException iae) {}
    }

    public static long getStartTime(SystemAdvertisement advert) {
        try {
            return Long.valueOf(advert.getCustomTagValue(
                CustomTagNames.START_TIME.toString()));
        } catch (NoSuchFieldException nsfe) {
            // logged at FINER since most calling methods already
            // log result at FINE or higher
            if (logger.isLoggable(Level.FINER)) {
                logger.finer(String.format(
                    "NoSuchFieldException caught in Utility#getStartTime. Returning %s",
                    NO_SUCH_TIME));
            }
            return NO_SUCH_TIME;
        }
    }

    public static String getGroupName(SystemAdvertisement advert) {
        try {
            return advert.getCustomTagValue(CustomTagNames.GROUP_NAME.toString());
        } catch (NoSuchFieldException nsfe) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer(
                    "NoSuchFieldException caught in Utility#getGroupName. Returning null");
            }
            return null;
        }
    }
}
