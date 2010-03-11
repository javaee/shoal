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
 <instance_id_token> <groupname> <membertype{CORE|SPECTATOR}> <Life In Milliseconds> <log level> <transport>{grizzly,jxtanew,jxta} <tcpstartport> <tcpendport>
Life in milliseconds should be at least 60000 to demo failure fencing.
<tcpstartport> and <tcpendport> are optional.  Grizzly and jxta transports have different defaults.
USAGE
   exit 0
}

if [ $# -lt 3 ]; then
    usage;
fi

if [ $# -gt 5 ]; then 
    if [ $5 = "-debug" ]; then
	java -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005 -DMEMBERTYPE=$3 -DINSTANCEID=$1 -DCLUSTERNAME=$2 -DMESSAGING_MODE=true -DLIFEINMILLIS=$4 -DLOG_LEVEL=INFO -cp ${publish_home}/shoal-gms-tests.jar:${publish_home}/shoal-gms.jar:${lib_home}/jxta.jar:${lib_home}/bcprov-jdk14.jar -DjxtaMulticastPoolsize=25 com.sun.enterprise.ee.cms.tests.ApplicationServer;
    elif [ $6 = "grizzly" ]; then
        echo Running using Shoal with transport grizzly
#  If you run shoal over grizzly on JDK7, NIO.2 multicast channel used. Otherwise, blocking multicast server used
       java -Dcom.sun.management.jmxremote -DMEMBERTYPE=$3 -DINSTANCEID=$1 -DCLUSTERNAME=$2 -DMESSAGING_MODE=true -DLIFEINMILLIS=$4 -DLOG_LEVEL=$5 -cp ${publish_home}/shoal-gms-tests.jar:${publish_home}/shoal-gms.jar:${lib_home}/grizzly-framework.jar:${lib_home}/grizzly-utils.jar -DTCPSTARTPORT=$7 -DTCPENDPORT=$8 -DMULTICASTADDRESS="229.9.1.2" -DSHOAL_GROUP_COMMUNICATION_PROVIDER=grizzly com.sun.enterprise.ee.cms.tests.ApplicationServer;
  else
       echo Running using Shoal with transport $6
       java -Dcom.sun.management.jmxremote -DMEMBERTYPE=$3 -DINSTANCEID=$1 -DCLUSTERNAME=$2 -DMESSAGING_MODE=true -DLIFEINMILLIS=$4 -DLOG_LEVEL=$5 -cp ${publish_home}/shoal-gms-tests.jar:${publish_home}/shoal-gms.jar:${lib_home}/grizzly-framework.jar:${lib_home}/grizzly-utils.jar:${lib_home}/jxta.jar -DTCPSTARTPORT=9090 -DTCPENDPORT=9120 -DSHOAL_GROUP_COMMUNICATION_PROVIDER=$6 -DMULTICASTADDRESS="229.9.1.2"  com.sun.enterprise.ee.cms.tests.ApplicationServer;
    fi 
fi

