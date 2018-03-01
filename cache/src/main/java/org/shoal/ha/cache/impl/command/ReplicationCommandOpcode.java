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

package org.shoal.ha.cache.impl.command;

/**
 * @author Mahesh Kannan
 */
public class ReplicationCommandOpcode {
    

    public static final byte REPLICATION_FRAME_PAYLOAD = 1;

    public static final byte SIMPLE_ACK_COMMAND = 2;

    public static final byte SAVE = 33;

    public static final byte LOAD_REQUEST = 35;

    public static final byte REMOVE = 36;

    public static final byte LOAD_RESPONSE = 37;

    public static final byte TOUCH = 38;

    public static final byte REMOVE_EXPIRED = 39;

    public static final byte REMOVE_EXPIRED_RESULT = 44;

    public static final byte STALE_REMOVE = 40;

    public static final byte SIZE_REQUEST = 51;

    public static final byte SIZE_RESPONSE = 52;


    public static final byte STOREABLE_SAVE = 68;

    public static final byte STOREABLE_UNICAST_LOAD_REQUEST = 69;

    public static final byte STOREABLE_REMOVE = 71;

    public static final byte STOREABLE_LOAD_RESPONSE = 72;

    public static final byte STOREABLE_TOUCH = 73;

    public static final byte STOREABLE_FULL_SAVE_COMMAND = 76;

    
    public static final byte NOOP_COMMAND = 102;
}
