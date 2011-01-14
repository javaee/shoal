#!/bin/sh +x
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

PUBLISH_HOME=./dist
LIB_HOME=./lib

usage () {
    cat << USAGE 
Usage: $0 <parameters...> 
The required parameters are :
 <instance_id_token> <groupname> <membertype{CORE|SPECTATOR}> <Life In Milliseconds> <log level> <transport>{grizzly,jxta} <-l logdir> <-ts tcpstartport> <-tp tcpendport> <-ma multicastaddress> <-mp multicastport> <-bia bindinginterfaceaddress>

Life in milliseconds should be either 0 or at least 60000 to demo failure fencing.

<-l fullpathtologdir> <-ts tcpstartport>, <-te tcpendport>, <-ma multicastaddress>, <-mp multicastport> <-bia bindinginterfaceaddress> are optional parameters.
Grizzly and jxta transports have different defaults.
USAGE
   exit 0
}

MAINCLASS=com.sun.enterprise.ee.cms.tests.ApplicationServer

GRIZZLY_JARS=${PUBLISH_HOME}/shoal-gms-tests.jar:${PUBLISH_HOME}/shoal-gms.jar:${LIB_HOME}/grizzly-framework.jar:${LIB_HOME}/grizzly-utils.jar
JXTA_JARS=${PUBLISH_HOME}/shoal-gms-tests.jar:${PUBLISH_HOME}/shoal-gms.jar:${LIB_HOME}/grizzly-framework.jar:${LIB_HOME}/grizzly-utils.jar:${LIB_HOME}/jxta.jar
DEBUGARGS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005 -DjxtaMulticastPoolsize=25"
NONDEBUGARGS="-Dcom.sun.management.jmxremote"

DEBUG=false
TCPSTARTPORT=""
TCPENDPORT=""
MULTICASTADDRESS="-DMULTICASTADDRESS=229.9.1.2"
MULTICASTPORT="-DMULTICASTPORT=2299"

TRANSPORT=grizzly
BINDINGINTERFACEADDRESS=""
MAXMISSEDHEARTBEATS=""
TEST_LOG_LEVEL=WARNING
LOG_LEVEL=INFO

#bump next log level to FINE for deeper analysis
MONITOR_LOG_LEVEL=INFO

JARS=${GRIZZLY_JARS}
DONEREQUIRED=false
while [ $# -ne 0 ]
do
     case ${1} in
       -h)
       usage
       exit 1
       ;;
       -debug)
       shift
       DEBUG=true
       ;;
       -bia)
       shift
       BINDINGINTERFACEADDRESS="${1}"
       shift
       ;;
       -mmh)
       shift
       MAXMISSEDHEARTBEATS="${1}"
       shift
       ;;
       -l)
       shift
       LOGS_DIR="${1}"
       shift
       ;;
       -tl)
       shift
       TEST_LOG_LEVEL="${1}"
       shift
       ;;
       -ts)
       shift
       TCPSTARTPORT="-DTCPSTARTPORT=${1}"
       shift
       ;;
       -te)
       shift
       TCPENDPORT="-DTCPENDPORT=${1}"
       shift
       ;;
       -ma)
       shift
       MULTICASTADDRESS="-DMULTICASTADDRESS=${1}"
       shift
       ;;
       -mp)
       shift
       MULTICASTPORT="-DMULTICASTPORT=${1}"
       shift
       ;;
       *)
       if [ ${DONEREQUIRED} = false ]; then
           INSTANCEID=$1
           shift
           CLUSTERNAME=$1
           shift
           MEMBERTYPE=$1
           shift
           LIFEINMILLIS=$1
           shift
           LOGLEVEL=$1
           shift
           TRANSPORT=$1
           shift
           DONEREQUIRED=true
       else
          echo "ERRROR: ignoring invalid argument $1"
          shift
       fi
       ;;
     esac
done

if [ -z "${INSTANCEID}" -o -z "${CLUSTERNAME}" -o -z "${MEMBERTYPE}" -o -z "${LIFEINMILLIS}" -o -z "${LOGLEVEL}" -o -z "${TRANSPORT}" ]; then
    echo "ERROR: Missing a required argument"
    usage;
fi

if [ "${MEMBERTYPE}" != "CORE" -a "${MEMBERTYPE}" != "SPECTATOR" -a "${MEMBERTYPE}" != "WATCHDOG" ]; then
    echo "ERROR: Invalid membertype specified"
    usage;
fi
if [ "${TRANSPORT}" != "grizzly" -a "${TRANSPORT}" != "jxta" ]; then
    echo "ERROR: Invalid transport specified"
    usage;
fi

if [ $TRANSPORT != "grizzly" ]; then
    JARS=${JXTA_JARS}
fi

if [ ${DEBUG} = false ]; then
    OTHERARGS=${NONDEBUGARGS}
else
    OTHERARGS=${DEBUGARGS}
fi

if [ ! -z ${BINDINGINTERFACEADDRESS} ]; then
    BINDINGINTERFACEADDRESS="-DBIND_INTERFACE_ADDRESS=${BINDINGINTERFACEADDRESS}"
fi
if [ ! -z ${MAXMISSEDHEARTBEATS} ]; then
    MAXMISSEDHEARTBEATS="-DMAX_MISSED_HEARTBEATS=${MAXMISSEDHEARTBEATS}"
fi

#  If you run shoal over grizzly on JDK7, NIO.2 multicast channel used. Otherwise, blocking multicast server used
CMD="java ${OTHERARGS} -DMEMBERTYPE=${MEMBERTYPE} -DINSTANCEID=${INSTANCEID} -DCLUSTERNAME=${CLUSTERNAME} -DMESSAGING_MODE=true -DLIFEINMILLIS=${LIFEINMILLIS} -DLOG_LEVEL=${LOGLEVEL} -DTEST_LOG_LEVEL=${TEST_LOG_LEVEL} -DMONITOR_LOG_LEVEL=${MONITOR_LOG_LEVEL} -cp ${JARS} ${TCPSTARTPORT} ${TCPENDPORT} -DSHOAL_GROUP_COMMUNICATION_PROVIDER=${TRANSPORT} ${MULTICASTADDRESS} ${MULTICASTPORT} ${BINDINGINTERFACEADDRESS} ${MAXMISSEDHEARTBEATS} -Djava.util.logging.config.file=gmsdemo_logging.properties ${MAINCLASS}"

if [ -z "${LOGS_DIR}" ]; then
   echo "Running using Shoal with transport ${TRANSPORT}"
   echo "=========================="
   echo ${CMD}
   echo "=========================="
   ${CMD} &
else
   if [ ! -d ${LOGS_DIR} ];then
      mkdir -p ${LOGS_DIR}
   fi
   #echo "LOGS_DIR=${LOGS_DIR}"
   echo "Running using Shoal with transport ${TRANSPORT}" >> ${LOGS_DIR}/${INSTANCEID}.log
   echo "==========================" >> ${LOGS_DIR}/${INSTANCEID}.log
   echo ${CMD} >> ${LOGS_DIR}/${INSTANCEID}.log
   echo "==========================" >> ${LOGS_DIR}/${INSTANCEID}.log
   ${CMD}  >> ${LOGS_DIR}/${INSTANCEID}.log 2>&1 &
fi

