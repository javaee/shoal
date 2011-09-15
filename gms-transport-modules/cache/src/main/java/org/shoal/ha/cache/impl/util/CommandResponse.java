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

import org.shoal.ha.cache.impl.util.ResponseMediator;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicLong;


/**
 * @author Mahesh Kannan
 *
 */
public class CommandResponse
    implements Callable {

    private static final AtomicLong tokenCounter = new AtomicLong(0);

    private long tokenId;

    private String respondingInstanceName;

    protected Object result;

    protected int expectedUpdateCount;

    private FutureTask future;

    private ResponseMediator mediator;

    public CommandResponse(ResponseMediator mediator) {
        this.mediator = mediator;
        this.tokenId = tokenCounter.incrementAndGet();
        this.future = new FutureTask(this);
    }

    public void setExpectedUpdateCount(int value) {
        this.expectedUpdateCount = value;
    }

    public int getExpectedUpdateCount() {
        return expectedUpdateCount;
    }

    public int decrementAndGetExpectedUpdateCount() {
        return --expectedUpdateCount;
    }

    public long getTokenId() {
        return tokenId;
    }

    public FutureTask getFuture() {
        return future;
    }

    public void setResult(Object v) {
        this.result = v;
        mediator.removeCommandResponse(tokenId);
        future.run(); //Which calls our call()
    }

    public Object getTransientResult() {
        return result;
    }

    public void setTransientResult(Object temp) {
        result = temp;
    }

    public void setException(Exception ex) {
        setResult(ex);
    }

    public String getRespondingInstanceName() {
        return respondingInstanceName;
    }

    public void setRespondingInstanceName(String respondingInstanceName) {
        this.respondingInstanceName = respondingInstanceName;
    }

    public Object call() {
        return result;
    }
}
