#!/bin/sh +x
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

GROUPNAME=ha_msg_repl_sim_group

ECHO=`which echo`

usage () {
    ${ECHO} "usage:"
    ${ECHO} "--------------------------------------------------------------------------------------------"
    ${ECHO} "single machine:"
    ${ECHO} "  [-h] [-r type] [-t transport] [-g groupname] [-noo num] [-mpo num] [-ms num] [-ts port] [-te port] "
    ${ECHO} "  [-ma address] [-mp port] [-bia address] [-tll level] [-sll level] [-l logdir] nodename membertype numberofmembers"
    ${ECHO} "     -t :  Transport type grizzly|jxta(default is grizzly)"
    ${ECHO} "     -g :  group name  (default is ${GROUPNAME})"
    ${ECHO} "     -noo :  Number of Objects(default is 10)"
    ${ECHO} "     -mpo :  Messages Per Object (default is 500)"
    ${ECHO} "     -ms :  Message size (default is 4096)"
    ${ECHO} "     -ts :  TCP start port (default is 4096)"
    ${ECHO} "     -te :  Message size (default is 4096)"
    ${ECHO} "     -ma :  Multicast address (default is 229.9.1.2)"
    ${ECHO} "     -mp :  Multicast port (default is 2299)"
    ${ECHO} "     -bia :  Bind Interface Address, used on a multihome machine"
    ${ECHO} "     -tll :  Test log level (default is INFO)"
    ${ECHO} "     -sll :  ShoalGMS log level (default is INFO)"
    ${ECHO} "     -l :  location where output is saved (default is LOGS/hamessagebuddyreplicasimulator"
    ${ECHO} "     -r :  The type of replication to use buddy or chash (consistent hash) (default is buddy)"
    ${ECHO} "     nodename :  name used by the member to join cluster"
    ${ECHO} "     membertype :  can be either CORE, SPECTATOR, or WATCHDOG"
    ${ECHO} "     numberofmembers :  number of CORE members in cluster (default is 10)"
    ${ECHO} "--------------------------------------------------------------------------------------------"
    ${ECHO} "distributed environment manditory args:"
    ${ECHO} "  -d  -g groupname -l logdir"
    ${ECHO} "     -d :  Indicates this is test is run distributed"
    ${ECHO} "     -g :  group name  (default is ${GROUPNAME})"
    ${ECHO} "     -l :  location where output is saved (default is LOGS/hamessagebuddyreplicasimulator"
    ${ECHO} "--------------------------------------------------------------------------------------------"
    ${ECHO} "special distributed environment args:"
    ${ECHO} "  -wait numberofmembers -g groupname -l logdir"
    ${ECHO} "     -wait :  waits until Testing Complete is found in the server.log"
    ${ECHO} "     -g :  group name  (default is habuddygroup)"
    ${ECHO} "     -l :  location where output is being saved for the master (default is LOGS/hamessagebuddyreplicasimulator"
    ${ECHO} "--------------------------------------------------------------------------------------------"
    exit 0
}

MAINCLASS="com.sun.enterprise.shoal.messagesenderreceivertest.HAMessageReplicationSimulator"


GRIZZLY_JARS=${PUBLISH_HOME}/shoal-gms-tests.jar:${PUBLISH_HOME}/shoal-gms.jar:${LIB_HOME}/grizzly2-framework.jar:${LIB_HOME}/grizzly-framework.jar:${LIB_HOME}/grizzly-utils.jar
JXTA_JARS=${PUBLISH_HOME}/shoal-gms-tests.jar:${PUBLISH_HOME}/shoal-gms.jar:${LIB_HOME}/grizzly2-framework.jar:${LIB_HOME}/grizzly-framework.jar:${LIB_HOME}/grizzly-utils.jar:${LIB_HOME}/jxta.jar

TCPSTARTPORT="-DTCPSTARTPORT=9130"
TCPENDPORT="-DTCPENDPORT=9160"
GMSMAXMSGLENGTH="-DMAX_MESSAGE_LENGTH=4396000"
BIND_INTERFACE_ADDRESS=""

MULTICASTADDRESS="-DMULTICASTADDRESS=229.9.1.2"
MULTICASTPORT="-DMULTICASTPORT=2299"
TRANSPORT=grizzly

SHOALGMS_LOG_LEVEL=CONFIG
TEST_LOG_LEVEL=INFO

NUMOFOBJECTS=10
MSGSPEROBJECT=100
MSGSIZE=4096

# in milliseconds
THINKTIME=10

REPLICATIONTYPE=buddy

NUMOFMEMBERS=10
LOGS_DIR=LOGS/${GROUPNAME}

JARS=${GRIZZLY_JARS}
DONEREQUIRED=false
WAIT=false
while [ $# -ne 0 ]
do
     case ${1} in
       -h)
       usage
       exit 1
       ;;
       -l)
       shift
       LOGS_DIR="${1}"
       shift
       ;;
       -wait)
       shift
         NUMOFMEMBERS=`${ECHO} "${1}" | egrep "^[0-9]+$" `
         shift
         if [ "${NUMOFMEMBERS}" != "" ]; then
            if [ ${NUMOFMEMBERS} -le 0 ];then
               ${ECHO} "ERROR: Invalid number of members specified"
               usage
            fi
         else
               ${ECHO} "ERROR: Invalid number of members specified"
               usage
         fi
       WAIT=true
       ;;
       -t)
         shift
         TRANSPORT="${1}"
         shift
         if [ ! -z "${TRANSPORT}" ] ;then
            if [ "${TRANSPORT}" != "grizzly" -a "${TRANSPORT}" != "jxta" ]; then
               ${ECHO} "ERROR: Invalid transport specified"
               usage
            fi
         else
            ${ECHO} "ERROR: Missing or invalid argument for transport"
            usage
         fi
       ;;
       -r)
         shift
         REPLICATIONTYPE="${1}"
         shift
         if [ ! -z "${REPLICATIONTYPE}" ] ;then
            if [ "${REPLICATIONTYPE}" != "buddy" -a "${REPLICATIONTYPE}" != "chash" ]; then
               ${ECHO} "ERROR: Invalid replication type specified"
               usage
            fi
         else
            ${ECHO} "ERROR: Missing or invalid argument for replication type"
            usage
         fi
       ;;
       -g)
         shift
         GROUPNAME="${1}"
         shift
         if [ -z "${GROUPNAME}" ]; then
            ${ECHO} "ERROR: Missing group name value"
            usage
         fi
       ;;
       -bia)
         shift
         BIND_INTERFACE_ADDRESS="${1}"
         shift
         if [ -z "${BIND_INTERFACE_ADDRESS}" ]; then
            ${ECHO} "ERROR: Missing bind interface address value"
            usage
         fi
         BIND_INTERFACE_ADDRESS="-DBIND_INTERFACE_ADDRESS=${BIND_INTERFACE_ADDRESS}"
       ;;
       -tll)
       shift
       TEST_LOG_LEVEL="${1}"
       shift
       ;;
       -sll)
       shift
       SHOALGMS_LOG_LEVEL="${1}"
       shift
       ;;
       -ts)
       shift
       TCPSTARTPORT=`${ECHO} "${1}" | egrep "^[0-9]+$" `
       if [ "${TCPSTARTPORT}" != "" ]; then
            if [ ${TCPSTARTPORT} -le 0 ];then
               ${ECHO} "ERROR: Invalid TCP start port specified [${TCPSTARTPORT}]"
               usage
            fi
       else
            ${ECHO} "ERROR: Missing or invalid argument for TCP start port"
            usage
       fi
       TCPSTARTPORT="-DTCPSTARTPORT=${TCPSTARTPORT}"
       shift
       ;;
       -te)
       shift
       TCPENDPORT=`${ECHO} "${1}" | egrep "^[0-9]+$" `
       if [ "${TCPENDPORT}" != "" ]; then
            if [ ${TCPENDPORT} -le 0 ];then
               ${ECHO} "ERROR: Invalid TCP end port specified [${TCPENDPORT}]"
               usage
            fi
       else
            ${ECHO} "ERROR: Missing or invalid argument for TCP end point"
            usage
       fi
       TCPENDPORT="-DTCPENDPORT=${TCPENDPORT}"
       shift
       ;;
       -ma)
       shift
       MULTICASTADDRESS="-DMULTICASTADDRESS=${1}"
       shift
       ;;
       -mp)
       shift
       MULTICASTPORT=`${ECHO} "${1}" | egrep "^[0-9]+$" `
       if [ "${MULTICASTPORT}" != "" ]; then
            if [ ${MULTICASTPORT} -le 0 ];then
               ${ECHO} "ERROR: Invalid Multicast port specified [${MULTICASTPORT}]"
               usage
            fi
       else
            ${ECHO} "ERROR: Missing or invalid argument for Multicast port"
            usage
       fi
       MULTICASTPORT="-DMULTICASTPORT=${MULTICASTPORT}"
       shift
       ;;
       -noo)
         shift
         NUMOFOBJECTS=`${ECHO} "${1}" | egrep "^[0-9]+$" `
         shift
         if [ "${NUMOFOBJECTS}" != "" ]; then
            if [ ${NUMOFOBJECTS} -le 0 ];then
               ${ECHO} "ERROR: Invalid number of objects specified"
               usage
            fi
         else
            ${ECHO} "ERROR: Missing or invalid argument for number of objects"

            usage
         fi
       ;;
       -mpo)
         shift
         MSGSPEROBJECT=`${ECHO} "${1}" | egrep "^[0-9]+$" `
         shift
         if [ "${MSGSPEROBJECT}" != "" ]; then
            if [ ${MSGSPEROBJECT} -le 0 ];then
               ${ECHO} "ERROR: Invalid messages per object specified"
               usage
            fi
         else
            ${ECHO} "ERROR: Missing or invalid argument for messages per object"
            usage
         fi
       ;;
       -ms)
         shift
         MSGSIZE=`${ECHO} "${1}" | egrep "^[0-9]+$" `
         shift
         if [ "${MSGSIZE}" != "" ]; then
            if [ ${MSGSIZE} -le 0 ];then
               ${ECHO} "ERROR: Invalid messages size specified"
               usage
            fi
         else
            ${ECHO} "ERROR: Missing or invalid argument for messages size"
            usage
         fi
       ;;
       -tt)
         shift
         THINKTIME=`${ECHO} "${1}" | egrep "^[0-9]+$" `
         shift
         if [ "${THINKTIME}" != "" ]; then
            if [ ${THINKTIME} -le 0 ];then
               ${ECHO} "ERROR: Invalid think time specified"
               usage
            fi
         else
            ${ECHO} "ERROR: Invalid think time specified"
            usage
         fi
       ;;
       *)
       if [ ${DONEREQUIRED} = false ]; then
           NODENAME="${1}"
           shift
           MEMBERTYPE="${1}"
           shift
           NUMOFMEMBERS=`${ECHO} "${1}" | egrep "^[0-9]+$" `
           if [ "${NUMOFMEMBERS}" != "" ]; then
               if [ ${NUMOFMEMBERS} -le 0 ];then
                  ${ECHO} "ERROR: Invalid number of members specified"
                  usage
               fi
           else
            ${ECHO} "ERROR: Missing or invalid argument for number of members"
               usage
           fi
           shift
           DONEREQUIRED=true
       else
          ${ECHO} "ERROR: ignoring invalid argument $1"
          shift
       fi
       ;;
     esac
done

if [ $WAIT = false ];then
     if [ -z "${NODENAME}" -o -z "${MEMBERTYPE}" ]; then
         ${ECHO} "ERROR: Missing a required argument"
         usage;
     fi

     if [ "${MEMBERTYPE}" != "CORE" -a "${MEMBERTYPE}" != "SPECTATOR" -a "${MEMBERTYPE}" != "WATCHDOG" ]; then
         ${ECHO} "ERROR: Invalid membertype specified [${MEMBERTYPE}]"
         usage;
     fi

     if [ $TRANSPORT != "grizzly" ]; then
         JARS=${JXTA_JARS}
     fi

     CMD=""
     if [ "${NODENAME}" = "server" ] ; then
        CMD="java -Dcom.sun.management.jmxremote -DSHOAL_GROUP_COMMUNICATION_PROVIDER=${TRANSPORT} -DLOG_LEVEL=${SHOALGMS_LOG_LEVEL} -DTEST_LOG_LEVEL=${TEST_LOG_LEVEL} ${TCPSTARTPORT} ${TCPENDPORT} ${GMSMAXMSGLENGTH}  ${MULTICASTADDRESS} ${MULTICASTPORT} ${BIND_INTERFACE_ADDRESS} -cp ${JARS} ${MAINCLASS} ${NODENAME} ${GROUPNAME} ${NUMOFMEMBERS} ${REPLICATIONTYPE}"
     else
        CMD="java -Dcom.sun.management.jmxremote -DSHOAL_GROUP_COMMUNICATION_PROVIDER=${TRANSPORT} -DLOG_LEVEL=${SHOALGMS_LOG_LEVEL} -DTEST_LOG_LEVEL=${TEST_LOG_LEVEL} ${TCPSTARTPORT} ${TCPENDPORT} ${GMSMAXMSGLENGTH} ${MULTICASTADDRESS} ${MULTICASTPORT} ${BIND_INTERFACE_ADDRESS} -cp ${JARS} ${MAINCLASS} ${NODENAME} ${GROUPNAME} ${NUMOFMEMBERS} ${NUMOFOBJECTS} ${MSGSPEROBJECT} ${MSGSIZE} ${THINKTIME} ${REPLICATIONTYPE}"
     fi


     if [ -z "${LOGS_DIR}" ]; then
        ${ECHO} "Running using Shoal with transport ${TRANSPORT}"
        ${ECHO} "=========================="
        echo ${CMD}
        ${ECHO} "=========================="
        ${CMD} &
     else
        if [ ! -d ${LOGS_DIR} ];then
           mkdir -p ${LOGS_DIR}
        fi
        ${ECHO} "Running using Shoal with transport ${TRANSPORT}" >> ${LOGS_DIR}/${NODENAME}.log
        ${ECHO} "==========================" >> ${LOGS_DIR}/${NODENAME}.log
        echo ${CMD} >> ${LOGS_DIR}/${NODENAME}.log
        ${ECHO} "==========================" >> ${LOGS_DIR}/${NODENAME}.log
        ${CMD}  >> ${LOGS_DIR}/${NODENAME}.log 2>&1 &
     fi
else
  # wait= true
    if [ -f ${LOGS_DIR}/server.log ];then
         GREP_STRING="Received DONE_Receiving message from:instance"
         count=`grep -a "${GREP_STRING}" ${LOGS_DIR}/server.log | wc -l | sed -e 's/ //g' `
         ${ECHO} "Waiting for done messages from each instances"
         ${ECHO}  -n "$count"
         while [ $count -lt $NUMOFMEMBERS ]
         do
             ${ECHO}  -n ",$count"
             count=`grep -a "${GREP_STRING}" ${LOGS_DIR}/server.log | wc -l | sed -e 's/ //g' `
             if [ $count -eq $NUMOFMEMBERS ];then
                   continue
             fi
             count2=`grep -a "Testing Complete" ${LOGS_DIR}/server.log | wc -l | sed -e 's/ //g' `
             if [ ${count2} -gt 0 ];then
                ${ECHO} " "
                ${ECHO} "ERROR: Testing Complete was detected before all instances have completed"
                break
             fi
             sleep 5
         done
         ${ECHO} ", $count"
         count=`grep -a "Testing Complete" ${LOGS_DIR}/server.log | wc -l | sed -e 's/ //g' `
         ${ECHO} "Waiting for the MASTER to complete testing"
         ${ECHO}  -n "$count"
         while [ $count -eq 0 ]
         do
            ${ECHO}  -n ",$count"
            count=`grep -a "Testing Complete" ${LOGS_DIR}/server.log | wc -l | sed -e 's/ //g' `
            sleep 5
         done
         echo  ", $count"
     else
         ${ECHO} "ERROR: Could not locate ${LOGS_DIR}/server.log"
     fi
fi


