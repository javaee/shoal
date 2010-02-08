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

package com.sun.enterprise.jxtamgmt;

import java.util.HashSet;
import java.util.logging.Filter;
import java.util.logging.LogRecord;

/**
 * A simple logging filter to allow multi per class logging
 */
class SelectiveLogFilter implements Filter {
    private HashSet<String> classLogSet = new HashSet<String>();
    private HashSet<String> methodLogSet = new HashSet<String>();

    /**
     * Adds a name to the set of loggable classes
     *
     * @param className canonical class name e.g. App.class.getName()
     */
    public void add(String className) {
        classLogSet.add(className);
    }

    /**
     * Removes a name from the set loggable classes
     *
     * @param className canonical class name e.g. App.class.getName()
     */
    public void remove(String className) {
        classLogSet.remove(className);
    }

    /**
     * Adds a method name to the set of loggable classes
     *
     * @param methodName canonical method name e.g. App.class.getName()+"methodName"
     */
    public void addMethod(String methodName) {
        methodLogSet.add(methodName);
    }

    /**
     * Removes a name from the set loggable classes
     *
     * @param methodName canonical method name e.g. App.class.getName()+"methodName"
     */
    public void removeMethod(String methodName) {
        methodLogSet.remove(methodName);
    }

    /**
     * Clears the loggable Class Set (effectively turns off logging)
     */
    public void reset() {
        classLogSet.clear();
    }

    /**
     * Clears the loggable Method Set
     */
    public void resetMethodSet() {
        methodLogSet.clear();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLoggable(LogRecord record) {
        return classLogSet.contains(record.getSourceClassName()) || methodLogSet.contains(record.getSourceMethodName());
    }
}

