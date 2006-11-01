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
package com.sun.enterprise.ee.cms.core;

/**
 * Raises exceptions occuring while releasing signals.
 *
 * This will occur on rare conditions, for example, when it is impossible
 * to stop fencing a previously failed server.
 *
 * @author Shreedhar Ganapathy
 * Date: Jan 8, 2004
 * @version $Revision$
 */
public class SignalReleaseException extends Exception{
    public SignalReleaseException(){
        super();
    }

    public SignalReleaseException(final String message){
        super(message);
    }

    public SignalReleaseException(final Exception e) {
        super(e);
    }
}
