/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.shoal.ha.cache.api;

/**
 * @author Mahesh Kannan
 */
public class ShoalCacheLoggerConstants {

    public static final String CACHE = "org.shoal.ha.cache";

    public static final String CACHE_STATS = "org.shoal.ha.cache.stats";

    public static final String CACHE_CONFIG = "org.shoal.ha.cache.config";

    public static final String CACHE_KEY_MAPPER = "org.shoal.ha.cache.mapper";

    public static final String CACHE_COMMAND = "org.shoal.ha.cache.command";

    public static final String CACHE_SAVE_COMMAND = "org.shoal.ha.cache.command.save";

    public static final String CACHE_REPLICATION_FRAME_COMMAND = "org.shoal.ha.cache.command.frame";

    public static final String CACHE_REMOVE_COMMAND = "org.shoal.ha.cache.command.remove";

    public static final String CACHE_STALE_REMOVE_COMMAND = "org.shoal.ha.cache.command.remove";

    public static final String CACHE_LOAD_REQUEST_COMMAND = "org.shoal.ha.cache.command.load_request";

    public static final String CACHE_LOAD_RESPONSE_COMMAND = "org.shoal.ha.cache.command.load_response";

    public static final String CACHE_TOUCH_COMMAND = "org.shoal.ha.cache.command.touch";

    public static final String CACHE_TRANSMIT_INTERCEPTOR = "org.shoal.ha.cache.interceptor.transmit";

    public static final String CACHE_REVEIVE_INTERCEPTOR = "org.shoal.ha.cache.interceptor.receive";

    public static final String CACHE_DATA_STORE = "org.shoal.ha.cache.store";

    public static final String CACHE_MONITOR = "org.shoal.ha.monitor";

    public static final String CACHE_SIZE_REQUEST_COMMAND = "org.shoal.ha.cache.command.size";

    public static final String CACHE_SIZE_RESPONSE_COMMAND = "org.shoal.ha.cache.command.size";

    public static final String CACHE_REMOVE_EXPIRED_COMMAND = "org.shoal.ha.cache.command.remove_expired";
    
}
