#!/bin/sh +x

#
# Copyright 2010 Sun Microsystems, Inc.  All rights reserved.
# Use is subject to license terms.
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


PUBLISH_HOME=./dist
LIB_HOME=./lib

usage () {
    echo "usage:"
    echo "--------------------------------------------------------------------------------------------"
    echo "single machine:"
    echo "  [-h] [-t transport] [-g groupname] [-noo num] [-nom num] [-ms num] [-ts port] [-te port] "
    echo "  [-ma address] [-mp port] [-tll level] [-sll level] [-l logdir] nodename membertype numberofmembers"
    echo "     -t :  Transport type grizzly|jxta|jxtanew (default is grizzly)"
    echo "     -g :  group name  (default is habuddygroup)"
    echo "     -noo :  Number of Objects(default is 10)"
    echo "     -nom :  Number of messages Per Object (default is 500)"
    echo "     -ms :  Message size (default is 4096)"
    echo "     -ts :  TCP start port (default is 4096)"
    echo "     -te :  Message size (default is 4096)"
    echo "     -ma :  Multicast address (default is 229.9.1.2)"
    echo "     -mp :  Multicast port (default is 2299)"
    echo "     -tll :  Test log level (default is INFO)"
    echo "     -sll :  ShoalGMS log level (default is INFO)"
    echo "     -l :  location where output is saved (default is LOGS/hamessagebuddyreplicasimulator"
    echo "     nodename :  name used by the member to join cluster"
    echo "     membertype :  can be either CORE, SPECTATOR, or WATCHDOG"
    echo "     numberofmembers :  number of CORE members in cluster (default is 10)"
    echo "--------------------------------------------------------------------------------------------"
    echo "distributed environment manditory args:"
    echo "  -d  -g groupname -l logdir"
    echo "     -d :  Indicates this is test is run distributed"
    echo "     -g :  group name  (default is habuddygroup)"
    echo "     -l :  location where output is saved (default is LOGS/hamessagebuddyreplicasimulator"
    echo "--------------------------------------------------------------------------------------------"
    echo "special distributed environment args:"
    echo "  -wait numberofmembers -g groupname -l logdir"
    echo "     -wait :  waits until Testing Complete is found in the server.log"
    echo "     -g :  group name  (default is habuddygroup)"
    echo "     -l :  location where output is being saved for the master (default is LOGS/hamessagebuddyreplicasimulator"
    echo "--------------------------------------------------------------------------------------------"
    exit 0
}


MAINCLASS="com.sun.enterprise.shoal.messagesenderreceivertest.HAMessageBuddyReplicationSimulator"

GRIZZLY_JARS=${PUBLISH_HOME}/shoal-gms-tests.jar:${PUBLISH_HOME}/shoal-gms.jar:${LIB_HOME}/grizzly-framework.jar:${LIB_HOME}/grizzly-utils.jar
JXTA_JARS=${PUBLISH_HOME}/shoal-gms-tests.jar:${PUBLISH_HOME}/shoal-gms.jar:${LIB_HOME}/grizzly-framework.jar:${LIB_HOME}/grizzly-utils.jar:${LIB_HOME}/jxta.jar

GROUPNAME=habuddygroup

TCPSTARTPORT=""
TCPENDPORT=""
MULTICASTADDRESS="-DMULTICASTADDRESS=229.9.1.2"
MULTICASTPORT="-DMULTICASTPORT=2299"
TRANSPORT=grizzly

SHOALGMS_LOG_LEVEL=INFO
TEST_LOG_LEVEL=INFO

NUMOFOBJECTS=10
NUMOFMSGSPEROBJECT=500
MSGSIZE=4096

NUMOFMEMBERS=10
LOGS_DIR=LOGS/hamessagebuddyreplicasimulator

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
         NUMOFMEMBERS=`echo "${1}" | egrep "^[0-9]+$" `
         shift
         if [ "${NUMOFMEMBERS}" != "" ]; then
            if [ ${NUMOFMEMBERS} -le 0 ];then
               echo "ERROR: Invalid number of members specified"
               usage
            fi
         else
               echo "ERROR: Invalid number of members specified"
               usage
         fi
       WAIT=true
       ;;
       -t)
         shift
         TRANSPORT="${1}"
         shift
         if [ ! -z "${TRANSPORT}" ] ;then
            if [ "${TRANSPORT}" != "grizzly" -a "${TRANSPORT}" != "jxta" -a "${TRANSPORT}" != "jxtanew" ]; then
               echo "ERROR: Invalid transport specified"
               usage
            fi
         else
            echo "ERROR: Missing or invalid argument for transport"
            usage
         fi
       ;;
       -g)
         shift
         GROUPNAME="${1}"
         shift
         if [ -z "${GROUPNAME}" ]; then
            echo "ERROR: Missing group name value"
            usage
         fi
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
       TCPSTARTPORT=`echo "${1}" | egrep "^[0-9]+$" `
       if [ "${TCPSTARTPORT}" != "" ]; then
            if [ ${TCPSTARTPORT} -le 0 ];then
               echo "ERROR: Invalid TCP start port specified [${TCPSTARTPORT}]"
               usage
            fi
       else
            echo "ERROR: Missing or invalid argument for TCP start port"
            usage
       fi
       TCPSTARTPORT="-DTCPSTARTPORT=${TCPSTARTPORT}"
       shift
       ;;
       -te)
       shift
       TCPENDPORT=`echo "${1}" | egrep "^[0-9]+$" `
       if [ "${TCPENDPORT}" != "" ]; then
            if [ ${TCPENDPORT} -le 0 ];then
               echo "ERROR: Invalid TCP end port specified [${TCPENDPORT}]"
               usage
            fi
       else
            echo "ERROR: Missing or invalid argument for TCP end point"
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
       MULTICASTPORT=`echo "${1}" | egrep "^[0-9]+$" `
       if [ "${MULTICASTPORT}" != "" ]; then
            if [ ${MULTICASTPORT} -le 0 ];then
               echo "ERROR: Invalid Multicast port specified [${MULTICASTPORT}]"
               usage
            fi
       else
            echo "ERROR: Missing or invalid argument for Multicast port"
            usage
       fi
       MULTICASTPORT="-DMULTICASTPORT=${MULTICASTPORT}"
       shift
       ;;
       -noo)
         shift
         NUMOFOBJECTS=`echo "${1}" | egrep "^[0-9]+$" `
         shift
         if [ "${NUMOFOBJECTS}" != "" ]; then
            if [ ${NUMOFOBJECTS} -le 0 ];then
               echo "ERROR: Invalid number of objects specified"
               usage
            fi
         else
            echo "ERROR: Missing or invalid argument for number of objects"

            usage
         fi
       ;;
       -nom)
         shift
         NUMOFMSGSPEROBJECT=`echo "${1}" | egrep "^[0-9]+$" `
         shift
         if [ "${NUMOFMSGSPEROBJECT}" != "" ]; then
            if [ ${NUMOFMSGSPEROBJECT} -le 0 ];then
               echo "ERROR: Invalid number of messages specified"
               usage
            fi
         else
            echo "ERROR: Missing or invalid argument for number of messages"
            usage
         fi
       ;;
       -ms)
         shift
         MSGSIZE=`echo "${1}" | egrep "^[0-9]+$" `
         shift
         if [ "${MSGSIZE}" != "" ]; then
            if [ ${MSGSIZE} -le 0 ];then
               echo "ERROR: Invalid messages size specified"
               usage
            fi
         else
            echo "ERROR: Missing or invalid argument for messages size"
            usage
         fi
       ;;
       *)
       if [ ${DONEREQUIRED} = false ]; then
           NODENAME="${1}"
           shift
           MEMBERTYPE="${1}"
           shift
           NUMOFMEMBERS=`echo "${1}" | egrep "^[0-9]+$" `
           if [ "${NUMOFMEMBERS}" != "" ]; then
               if [ ${NUMOFMEMBERS} -le 0 ];then
                  echo "ERROR: Invalid number of members specified"
                  usage
               fi
           else
            echo "ERROR: Missing or invalid argument for number of members"
               usage
           fi
           shift
           DONEREQUIRED=true
       else
          echo "ERROR: ignoring invalid argument $1"
          shift
       fi
       ;;
     esac
done

if [ $WAIT = false ];then
     if [ -z "${NODENAME}" -o -z "${MEMBERTYPE}" ]; then
         echo "ERROR: Missing a required argument"
         usage;
     fi

     if [ "${MEMBERTYPE}" != "CORE" -a "${MEMBERTYPE}" != "SPECTATOR" -a "${MEMBERTYPE}" != "WATCHDOG" ]; then
         echo "ERROR: Invalid membertype specified [${MEMBERTYPE}]"
         usage;
     fi

     if [ $TRANSPORT != "grizzly" ]; then
         JARS=${JXTA_JARS}
     fi

     CMD=""
     if [ "${NODENAME}" = "server" ] ; then
        CMD="java -Dcom.sun.management.jmxremote -DSHOAL_GROUP_COMMUNICATION_PROVIDER=${TRANSPORT} -DTCPSTARTPORT=9130 -DTCPENDPORT=9160 -DLOG_LEVEL=${SHOALGMS_LOG_LEVEL} -DTEST_LOG_LEVEL=${TEST_LOG_LEVEL} ${TCPSTARTPORT} ${TCPENDPORT} ${MULTICASTADDRESS} ${MULTICASTPORT} -cp ${JARS} ${MAINCLASS} ${NODENAME} ${GROUPNAME} ${NUMOFMEMBERS}"
     else
        CMD="java -Dcom.sun.management.jmxremote -DSHOAL_GROUP_COMMUNICATION_PROVIDER=${TRANSPORT} -DTCPSTARTPORT=9130 -DTCPENDPORT=9160 -DLOG_LEVEL=${SHOALGMS_LOG_LEVEL} -DTEST_LOG_LEVEL=${TEST_LOG_LEVEL} ${TCPSTARTPORT} ${TCPENDPORT} ${MULTICASTADDRESS} ${MULTICASTPORT} -cp ${JARS} ${MAINCLASS} ${NODENAME} ${GROUPNAME} ${NUMOFMEMBERS} ${NUMOFOBJECTS} ${NUMOFMSGSPEROBJECT} ${MSGSIZE}"
     fi


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
        echo "Running using Shoal with transport ${TRANSPORT}" >> ${LOGS_DIR}/${NODENAME}.log
        echo "==========================" >> ${LOGS_DIR}/${NODENAME}.log
        echo ${CMD} >> ${LOGS_DIR}/${NODENAME}.log
        echo "==========================" >> ${LOGS_DIR}/${NODENAME}.log
        ${CMD}  >> ${LOGS_DIR}/${NODENAME}.log 2>&1 &
     fi
else
  # wait= true
    if [ -f ${LOGS_DIR}/server.log ];then
         count=`grep -a "Received DONE message:DONE:instance" ${LOGS_DIR}/server.log | wc -l | sed -e 's/ //g' `
         echo "Waiting for (${NUMOFMEMBERS}) instances to complete testing"
         echo -n "$count"
         while [ $count -lt $NUMOFMEMBERS ]
         do
             echo -n ",$count"
             count=`grep -a "Received DONE message:DONE:instance" ${LOGS_DIR}/server.log | wc -l | sed -e 's/ //g' `
             if [ $count -eq $NUMOFMEMBERS ];then
                 continue
             fi
             count2=`grep -a "Testing Complete" ${LOGS_DIR}/server.log | wc -l | sed -e 's/ //g' `
             if [ ${count2} -gt 0 ];then
                echo " "
                echo "ERROR: Testing Complete was detected before all instances have completed"
                break
             fi
             sleep 5
         done
         echo ", $count"
         count=`grep -a "Testing Complete" ${LOGS_DIR}/server.log | wc -l | sed -e 's/ //g' `
         echo "Waiting for the MASTER to complete testing"
         echo -n "$count"
         while [ $count -lt 1 ]
         do
            echo -n ",$count"
            count=`grep -a "Testing Complete" ${LOGS_DIR}/server.log | wc -l | sed -e 's/ //g' `
            sleep 5
         done
         echo  ", $count"
     else
         echo "ERROR: Could not locate ${LOGS_DIR}/server.log"
     fi
fi


