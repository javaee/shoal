/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.ha.store.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Set;


/**
 * A Storeable is an interface that must be implemented by objects that
 * are to be presisted in the backing store.
 *
 * @author Mahesh.Kannan@Sun.Com
 */
public interface Storeable
        extends Serializable {

    /**
     * Get the version of this entry. -1 means that this entry has no version
     *
     * @return The version or null if this entry has no version
     */
    public long _storeable_getVersion();

    public void _storeable_setVersion(long version);

    public long _storeable_getLastAccessTime();

    public void _storeable_setLastAccessTime(long version);

    public long _storeable_getMaxIdleTime();

    public void _storeable_setMaxIdleTime(long version);

    /**
     * Providers can cache this
     * @return an array of attribute names
     */
    public String[] _storeable_getAttributeNames();

    /**
     * Providers can cache this
     * @return  A boolean array each representing the dirty status of the attribute whose name
     *  can be found at the same index in the array returned by _getAttributeNames()
     */
    public boolean[] _storeable_getDirtyStatus();

    public void _storeable_writeState(OutputStream os)
        throws IOException;

    public void _storeable_readState(InputStream is)
        throws IOException;

}
