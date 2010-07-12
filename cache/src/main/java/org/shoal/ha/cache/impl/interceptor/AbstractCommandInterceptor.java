/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
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

package org.shoal.ha.cache.impl.interceptor;

import org.shoal.ha.cache.api.DataStoreContext;
import org.shoal.ha.cache.api.DataStoreException;
import org.shoal.ha.cache.impl.command.Command;
import org.shoal.ha.cache.impl.command.CommandManager;


/**
 * @author Mahesh Kannan
 *
 */
public abstract class AbstractCommandInterceptor<K, V> {

    private DataStoreContext<K, V> dsc;
    
    private CommandManager<K, V> cm;
    
    private AbstractCommandInterceptor<K, V> next;
  
    private AbstractCommandInterceptor<K, V> prev;
    
    public final void initialize(DataStoreContext<K, V> dsc) {
        this.dsc = dsc;
        this.cm = dsc.getCommandManager();
    }

    public final DataStoreContext<K, V> getDataStoreContext() {
        return dsc;
    }

    public CommandManager getCommandManager() {
        return cm;
    }

    public final void setNext(AbstractCommandInterceptor<K, V> next) {
        this.next = next;
    }

    public final void setPrev(AbstractCommandInterceptor<K, V> prev) {
        this.prev = prev;
    }

    public final AbstractCommandInterceptor<K, V> getNext() {
        return next;    
    }
    
    public final AbstractCommandInterceptor<K, V> getPrev() {
        return prev;
    }

    public void onTransmit(Command<K, V> cmd)
        throws DataStoreException {
        AbstractCommandInterceptor n = getNext();
        if (n != null) {
            n.onTransmit(cmd);
        }
    }

    public void onReceive(Command<K, V> cmd)
        throws DataStoreException {
        AbstractCommandInterceptor<K, V> p = getPrev();
        if (p != null) {
            p.onReceive(cmd);
        }  else {
            cmd.execute(dsc);
        }
    }

}
