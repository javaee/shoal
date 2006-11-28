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

