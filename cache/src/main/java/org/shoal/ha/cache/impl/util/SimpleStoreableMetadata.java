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

package org.shoal.ha.cache.impl.util;

import org.glassfish.ha.store.api.Storeable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * @author Mahesh Kannan
 * 
 */
public class SimpleStoreableMetadata
    implements Storeable {

    private long version;

    private long lastAccessTime;

    private long maxIdleTime;

    private boolean isNew;

    private byte[] state;

    private static final String[] _attrNames = new String[] {"state"};

    private static final boolean[] _dirtyStates = new boolean[] {true};

    public SimpleStoreableMetadata() {

    }

    public SimpleStoreableMetadata(long version, long lastAccessTime, long maxIdleTime, boolean aNew,
                                   byte[] state) {
        this.version = version;
        this.lastAccessTime = lastAccessTime;
        this.maxIdleTime = maxIdleTime;
        isNew = aNew;
        this.state = state;
    }

    public byte[] getState() {
        return state;
    }

    @Override
    public String toString() {
        return "SimpleStoreableMetadata{" +
                "version=" + version +
                ", lastAccessTime=" + lastAccessTime +
                ", maxIdleTime=" + maxIdleTime +
                ", isNew=" + isNew +
                ", state=" + Arrays.toString(state) +
                '}';
    }

    @Override
    public long _storeable_getVersion() {
        return version;
    }

    @Override
    public void _storeable_setVersion(long l) {
        version = l;
    }

    @Override
    public long _storeable_getLastAccessTime() {
        return lastAccessTime;
    }

    @Override
    public void _storeable_setLastAccessTime(long l) {
        lastAccessTime = l;
    }

    @Override
    public long _storeable_getMaxIdleTime() {
        return maxIdleTime;
    }

    @Override
    public void _storeable_setMaxIdleTime(long l) {
        maxIdleTime = l;
    }

    @Override
    public String[] _storeable_getAttributeNames() {
        return _attrNames;
    }

    @Override
    public boolean[] _storeable_getDirtyStatus() {
        return _dirtyStates;
    }

    @Override
    public void _storeable_writeState(OutputStream outputStream) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void _storeable_readState(InputStream inputStream) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
