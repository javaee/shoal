#!/bin/sh
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2004-2010 Oracle and/or its affiliates. All rights reserved.
#
# The contents of this file are subject to the terms of either the GNU
# General Public License Version 2 only ("GPL") or the Common Development
# and Distribution License("CDDL") (collectively, the "License").  You
# may not use this file except in compliance with the License.  You can
# obtain a copy of the License at
# https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
# or packager/legal/LICENSE.txt.  See the License for the specific
# language governing permissions and limitations under the License.
#
# When distributing the software, include this License Header Notice in each
# file and include the License file at packager/legal/LICENSE.txt.
#
# GPL Classpath Exception:
# Oracle designates this particular file as subject to the "Classpath"
# exception as provided by Oracle in the GPL Version 2 section of the License
# file that accompanied this code.
#
# Modifications:
# If applicable, add the following below the License Header, with the fields
# enclosed by brackets [] replaced by your own identifying information:
# "Portions Copyright [year] [name of copyright owner]"
#
# Contributor(s):
# If you wish your version of this file to be governed by only the CDDL or
# only the GPL Version 2, indicate your decision by adding "[Contributor]
# elects to include this software in this distribution under the [CDDL or GPL
# Version 2] license."  If you don't indicate a single choice of license, a
# recipient has the option to distribute your version of this file under
# either the CDDL, the GPL Version 2 or to extend the choice of license to
# its licensees as provided above.  However, if you add GPL Version 2 code
# and therefore, elected the GPL Version 2 license, then the option applies
# only if the new code is made subject to such option by the copyright
# holder.
#

publish_home=./dist
lib_home=./lib

usage () {
    cat << USAGE 
Usage: $0 <parameters...> 
The required parameters are :
 <instance_id_token> <groupname> <membertype{CORE|SPECTATOR}> <Life In Milliseconds> <log level> IS_INITIAL_HOST=[true|false] INITIAL_HOST_LIST=[comma separated list of tcp addresses in the form tcp://ipaddress:port]
Life in milliseconds should be at least 60000 to demo failure fencing.
USAGE
   exit 0
}

if [ $# -lt 3 ]; then
    usage;
fi

if [ -n $5 ]; then 
    if [ $5 = "-debug" ]; then
	java -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005 -DMEMBERTYPE=$3 -DINSTANCEID=$1 -DCLUSTERNAME=$2 -DMESSAGING_MODE=true -DLIFEINMILLIS=$4 -DLOG_LEVEL=INFO -cp ${publish_home}/shoal-gms-tests.jar:${publish_home}/shoal-gms.jar:${lib_home}/jxta.jar:${lib_home}/bcprov-jdk14.jar com.sun.enterprise.ee.cms.tests.ApplicationServer;
    else
 echo java -Dcom.sun.management.jmxremote -DMEMBERTYPE=$3 -DINSTANCEID=$1 -DCLUSTERNAME=$2 -DMESSAGING_MODE=true -DLIFEINMILLIS=$4 -DLOG_LEVEL=$5 -D$6 -D$7   -cp ${publish_home}/shoal-gms-tests.jar:${publish_home}/shoal-gms.jar:${lib_home}/jxta.jar:${lib_home}/bcprov-jdk14.jar com.sun.enterprise.ee.cms.tests.ApplicationServer;
	java -Dcom.sun.management.jmxremote -DMEMBERTYPE=$3 -DINSTANCEID=$1 -DCLUSTERNAME=$2 -DMESSAGING_MODE=true -DLIFEINMILLIS=$4 -DLOG_LEVEL=$5 -D$6 -D$7 -cp ${publish_home}/shoal-gms-tests.jar:${publish_home}/shoal-gms.jar:${lib_home}/jxta.jar:${lib_home}/bcprov-jdk14.jar com.sun.enterprise.ee.cms.tests.ApplicationServer;
    fi
fi

