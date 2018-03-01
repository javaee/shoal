/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.util.ResourceBundle;
import java.util.HashMap;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.MessageFormat;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
 * NiceLogFormatter conforms to the logging format defined by the
 * Log Working Group in Java Webservices Org.
 * The specified format is
 * "[#|DATETIME|LOG_LEVEL|PRODUCT_ID|LOGGER NAME|OPTIONAL KEY VALUE PAIRS|
 * MESSAGE|#]\n"
 *
 * @author Shreedhar Ganapathy
 *         Date: Jun 6, 2006
 * @version $Revision$
 */
public class NiceLogFormatter extends Formatter {

    // loggerResourceBundleTable caches references to all the ResourceBundle
    // and can be searched using the LoggerName as the key

    private HashMap<String, ResourceBundle> loggerResourceBundleTable;
    // A Dummy Container Date Object is used to format the date
    private Date date = new Date();
    private static boolean LOG_SOURCE_IN_KEY_VALUE = true;

    private static boolean RECORD_NUMBER_IN_KEY_VALUE = false;

    static {
        String logSource = System.getProperty(
                "com.sun.aas.logging.keyvalue.logsource");
        if ((logSource != null)
                && (logSource.equals("true"))) {
            LOG_SOURCE_IN_KEY_VALUE = true;
        }

        String recordCount = System.getProperty(
                "com.sun.aas.logging.keyvalue.recordnumber");
        if ((recordCount != null)
                && (recordCount.equals("true"))) {
            RECORD_NUMBER_IN_KEY_VALUE = true;
        }
    }

    private long recordNumber = 0;

    @SuppressWarnings("unchecked")
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private static final String RECORD_BEGIN_MARKER = "[#|";
    private static final String RECORD_END_MARKER = "|#]" + LINE_SEPARATOR +
                                                    LINE_SEPARATOR;
    private static final char FIELD_SEPARATOR = '|';
    private static final char NVPAIR_SEPARATOR = ';';
    private static final char NV_SEPARATOR = '=';

    private static final String RFC_3339_DATE_FORMAT =
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private final SimpleDateFormat dateFormatter =
            new SimpleDateFormat( RFC_3339_DATE_FORMAT );
    private static final String PRODUCT_VERSION = "Shoal";

    public NiceLogFormatter() {
        super();
        loggerResourceBundleTable = new HashMap<String, ResourceBundle>();
    }


    public String format( LogRecord record) {
        return uniformLogFormat(record);
    }

    public String formatMessage(LogRecord record) {
        return uniformLogFormat(record);
    }


    /**
     * Sun One AppServer SE/EE can override to specify their product version
     *
     * @return product ID
     */
    protected String getProductId() {
        return PRODUCT_VERSION;
    }


    /**
     * Note: This method is not synchronized, we are assuming that the
     * synchronization will happen at the Log Handler.publish( ) method.
     *
     * @param record log record
     * @return the log message
     */
    private String uniformLogFormat(LogRecord record) {

        try {

            StringBuilder recordBuffer = new StringBuilder( RECORD_BEGIN_MARKER );
            // The following operations are to format the date and time in a
            // human readable  format.
            // _REVISIT_: Use HiResolution timer to analyze the number of
            // Microseconds spent on formatting date object
            date.setTime(record.getMillis());
            recordBuffer.append( dateFormatter.format(date));
            recordBuffer.append( FIELD_SEPARATOR );

            recordBuffer.append(record.getLevel()).append( FIELD_SEPARATOR );
            recordBuffer.append(getProductId()).append( FIELD_SEPARATOR );
            recordBuffer.append(record.getLoggerName()).append( FIELD_SEPARATOR );

            recordBuffer.append("_ThreadID").append( NV_SEPARATOR );
            recordBuffer.append(record.getThreadID()).append( NVPAIR_SEPARATOR );

            recordBuffer.append("_ThreadName").append( NV_SEPARATOR );
            recordBuffer.append(Thread.currentThread().getName());
            recordBuffer.append( NVPAIR_SEPARATOR );

            // See 6316018. ClassName and MethodName information should be
            // included for FINER and FINEST log levels.
            Level level = record.getLevel();
            String className = record.getSourceClassName();
            className = className.substring(className.lastIndexOf(".") + 1, className.length());
            if ( LOG_SOURCE_IN_KEY_VALUE ||
                    (level.intValue() <= Level.FINE.intValue())) {
                recordBuffer.append("ClassName").append( NV_SEPARATOR );
                recordBuffer.append(className);
                recordBuffer.append( NVPAIR_SEPARATOR );
                recordBuffer.append("MethodName").append( NV_SEPARATOR );
                recordBuffer.append(record.getSourceMethodName());
                recordBuffer.append( NVPAIR_SEPARATOR );
            }

            if ( RECORD_NUMBER_IN_KEY_VALUE ) {
                recordBuffer.append("RecordNumber").append( NV_SEPARATOR );
                recordBuffer.append(recordNumber++).append( NVPAIR_SEPARATOR );
            }
            recordBuffer.append( FIELD_SEPARATOR );

            String logMessage = record.getMessage();
            if (logMessage == null) {
                logMessage = "The log message is null.";
            }
            if (logMessage.indexOf("{0}") >= 0) {
                // If we find {0} or {1} etc., in the message, then it's most
                // likely finer level messages for Method Entry, Exit etc.,
                logMessage = java.text.MessageFormat.format(
                        logMessage, record.getParameters());
            } else {
                ResourceBundle rb = getResourceBundle(record.getLoggerName());
                if (rb != null) {
                    try {
                        logMessage = MessageFormat.format(
                                rb.getString(logMessage),
                                record.getParameters());
                    } catch (java.util.MissingResourceException e) {
                        // If we don't find an entry, then we are covered
                        // because the logMessage is intialized already
                    }
                }
            }
            recordBuffer.append(logMessage);

            if (record.getThrown() != null) {
                recordBuffer.append( LINE_SEPARATOR );
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                recordBuffer.append(sw.toString());
            }

            recordBuffer.append( RECORD_END_MARKER );
            return recordBuffer.toString();

        } catch (Exception ex) {
            new ErrorManager().error(
                    "Error in formatting Logrecord", ex,
                    ErrorManager.FORMAT_FAILURE);
            // We've already notified the exception, the following
            // return is to keep javac happy
            return new String("");
        }
    }

    private synchronized ResourceBundle getResourceBundle(String loggerName) {
        if (loggerName == null) {
            return null;
        }
        ResourceBundle rb = loggerResourceBundleTable.get(
                loggerName);

        if (rb == null) {
            rb = LogManager.getLogManager().getLogger(loggerName).getResourceBundle();
            loggerResourceBundleTable.put(loggerName, rb);
        }
        return rb;
    }
}
