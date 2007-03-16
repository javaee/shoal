#!/bin/sh 

#
# Copyright 2004-2005 Sun Microsystems, Inc.  All rights reserved.
# Use is subject to license terms.
#
 #
 # The contents of this file are subject to the terms
 # of the Common Development and Distribution License
 # (the License).  You may not use this file except in
 # compliance with the License.
 #
 # You can obtain a copy of the license at
 # https://shoal.dev.java.net/public/CDDLv1.0.html
 #
 # See the License for the specific language governing
 # permissions and limitations under the License.
 #
 # When distributing Covered Code, include this CDDL
 # Header Notice in each file and include the License file
 # at
 # If applicable, add the following below the CDDL Header,
 # with the fields enclosed by brackets [] replaced by
 # you own identifying information:
 # "Portions Copyrighted [year] [name of copyright owner]"
 #
 # Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 #
publish_home=./dist
lib_home=./lib

usage () {
    cat << USAGE 
Usage: $0 <parameters...> 
The required parameters are :
 <instance_id_token> <groupname> <membertype{CORE|SPECTATOR}> <Life In Milliseconds> <log level>
Life in milliseconds should be at least 60000 to demo failure fencing.
USAGE
   exit 0
}

if [ $# -lt 3 ]; then
    usage;
fi

if [ -n $5 ]; then 
    if [ $5 = "-debug" ]; then
	java -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005 -DMEMBERTYPE=$3 -DINSTANCEID=$1 -DCLUSTERNAME=$2 -DMESSAGING_MODE=true -DLIFEINMILLIS=$4 -DLOG_LEVEL=INFO -cp ${publish_home}/shoal-gms.jar:${lib_home}/jxta.jar:${lib_home}/bcprov-jdk14.jar com.sun.enterprise.ee.cms.tests.ApplicationServer;    
    else
	java -Dcom.sun.management.jmxremote -DMEMBERTYPE=$3 -DINSTANCEID=$1 -DCLUSTERNAME=$2 -DMESSAGING_MODE=true -DLIFEINMILLIS=$4 -DLOG_LEVEL=$5 -cp ${publish_home}/shoal-gms.jar:${lib_home}/jxta.jar:${lib_home}/bcprov-jdk14.jar com.sun.enterprise.ee.cms.tests.ApplicationServer;
    fi
fi

