/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.ee.cms.logging;


import java.util.logging.Logger;

/**
 * GMS logger that abstracts out application specific loggers. One can
 * plug in any logger here - even potentially set custom log handlers through
 * this abstraction.
 *
 * @author Shreedhar Ganapathy
 *         Date: Apr 1, 2004
 * @version $Revision$
 */
public class GMSLogDomain  {

    public static final String GMS_LOGGER = "ShoalLogger";

    private static final String LOG_STRINGS =
        "com.sun.enterprise.ee.cms.logging.LogStrings";

    private static final String GMS_MONITOR_LOGGER = GMS_LOGGER + ".monitor";
    private static final String GMS_HANDLER_LOGGER = GMS_LOGGER + ".handler";
    private static final String MCAST_LOGGER_NAME = GMS_LOGGER + ".mcast";
    private static final String MASTER_LOGGER_NAME = GMS_LOGGER + ".MasterNode";
    private static final String GMS_SEND = GMS_LOGGER + ".send";
    private static final String GMS_DSC = GMS_LOGGER + ".dsc";
    private static final String GMS_NOMCAST = GMS_LOGGER + ".nomcast";

    private GMSLogDomain() { /* you can't have me */}

    public static Logger getLogger(final String loggerName){
        return Logger.getLogger(loggerName,  LOG_STRINGS);
    }

    public static Logger getMonitorLogger() {
        return Logger.getLogger(GMS_MONITOR_LOGGER, LOG_STRINGS);
    }

    public static Logger getMcastLogger() {
        return Logger.getLogger(MCAST_LOGGER_NAME, LOG_STRINGS);
    }

    public static Logger getMasterNodeLogger() {
        return Logger.getLogger(MASTER_LOGGER_NAME, LOG_STRINGS);
    }
    
    public static Logger getSendLogger() {
        return Logger.getLogger(GMS_SEND, LOG_STRINGS);
    }

    public static Logger getHandlerLogger() {
        return Logger.getLogger(GMS_HANDLER_LOGGER, LOG_STRINGS);
    }

    public static Logger getDSCLogger() {
        return Logger.getLogger(GMS_DSC, LOG_STRINGS);
    }

    public static Logger getNoMCastLogger() {
        return Logger.getLogger(GMS_NOMCAST, LOG_STRINGS);
    }
}