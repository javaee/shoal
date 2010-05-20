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

EXECUTE_REMOTE_CONNECT=rsh

ADMINCLI_LOG_LEVEL=WARNING
ADMINCLI_SHOALGMS_LOG_LEVEL=WARNING

TEST_LOG_LEVEL=INFO
SHOALGMS_LOG_LEVEL=INFO

CLUSTER_CONFIGS="./configs/clusters"
if [ -f "./configs/clusters" ]; then
   echo "ERROR: the configs/clusters directory is missing"
   exit 1
fi

LOGS_DIR=LOGS/hamessagebuddyreplicasimulator
COLLECT_LOGS_DIR=""

TRANSPORT=grizzly
CMD=normal
NUMOFMEMBERS=10

NUMOFOBJECTS=10
NUMOFMSGSPEROBJECT=100
MSGSIZE=4096

GROUPNAME=habuddygroup
MULTICASTADDRESS=229.9.1.`./randomNumber.sh`
MULTICASTPORT=2299

# in milliseconds
THINKTIME=10

PUBLISH_HOME=./dist
LIB_HOME=./lib

MAINCLASS="com.sun.enterprise.shoal.messagesenderreceivertest.HAMessageBuddyReplicationSimulator"

GRIZZLY_JARS=${PUBLISH_HOME}/shoal-gms-tests.jar:${PUBLISH_HOME}/shoal-gms.jar:${LIB_HOME}/grizzly-framework.jar:${LIB_HOME}/grizzly-utils.jar
JXTA_JARS=${PUBLISH_HOME}/shoal-gms-tests.jar:${PUBLISH_HOME}/shoal-gms.jar:${LIB_HOME}/grizzly-framework.jar:${LIB_HOME}/grizzly-utils.jar:${LIB_HOME}/jxta.jar
JARS=${GRIZZLY_JARS}


DIST=false

usage () {
 echo "usage:"
 echo "--------------------------------------------------------------------------------------------"
 echo "single machine:"
 echo "  [-h] [-t transport] [-g groupname] [-noo num] [-nom num] [-ms num] [-bia address] [-n numberOfMembers] [-tll level] [-sll level] [-tt num]"
 echo "     -t :  Transport type grizzly|jxta|jxtanew (default is grizzly)"
 echo "     -g :  group name  (default is habuddygroup)"
 echo "     -noo :  Number of Objects(default is 10)"
 echo "     -nom :  Number of messages Per Object (default is 100)"
 echo "     -ms :  Message size (default is 4096)"
 echo "     -n : Number of CORE members in the group (default is 10)"
 echo "     -tll :  Test log level (default is INFO)"
 echo "     -sll :  ShoalGMS log level (default is INFO)"
 echo "     -bia :  Bind Interface Address, used on a multihome machine"
 echo "     -tt :  Think time during sending in milliseconds"

 echo "--------------------------------------------------------------------------------------------"
 echo "   distributed environment manditory args:"
 echo "  -d  <-g groupname> <-cl collectlogdir>"
 echo "     -d :  Indicates this is test is run distributed"
 echo "     -g :  group name  (default is habuddygroup)"
 echo "     -cl :  log directory where logs are copied to and analyzed"
 echo "--------------------------------------------------------------------------------------------"
 echo " "
 echo " Examples:"
 echo "     runHAMessageBuddyReplicationSimulator.sh"
 echo "     runHAMessageBuddyReplicationSimulator.sh -noo 5 -mnom 256 -ms 1024 -n 5 "
 echo "     runHAMessageBuddyReplicationSimulator.sh -d -g testgroup -l /net/machine1/test"
 echo "     runHAMessageBuddyReplicationSimulator.sh -d -g testgroup -l /net/machine1/test -noo 5 -mnom 256 -ms 1024"
 exit 1
}



analyzeLogs(){
    echo  "The following logs contain failures:"
    echo  "==============="
    grep -a "FAILED" ${LOGS_DIR}/instance*log
    echo  "==============="
    echo  "The following are the time results for SENDING messages:" | tee -a ${LOGS_DIR}/timingdata.out
    grep -a "Sending Messages Time data" ${LOGS_DIR}/instance*log | tee -a ${LOGS_DIR}/timingdata.out
    echo  "---------------"
    echo "          Time Delta          MsgsPerSec  BytesPerSecond        Messagesize    numofobjects  Number of messages Per Object"
    #Delta
    DELTAMIN=`grep "Sending Messages Time data" ${LOGS_DIR}/timingdata.out | grep Delta | sed -e 's/^.*Delta//' | sed -e 's/sec.*$//' | sed -e 's/\[//' | sed -e 's/]//' | sort -n | head -n 1 | sed -e 's/ //' `
    DELTAMAX=`grep "Sending Messages Time data" ${LOGS_DIR}/timingdata.out | grep Delta | sed -e 's/^.*Delta//' | sed -e 's/sec.*$//' | sed -e 's/\[//' | sed -e 's/]//' | sort -n | tail -n 1 | sed -e 's/ //' `
    #MsgsPerSec
    MPSMIN=`grep "Sending Messages Time data" ${LOGS_DIR}/timingdata.out | grep MsgsPerSec | sed -e 's/^.*MsgsPerSec//' | sed -e 's/,.*$//' | sed -e 's/\[//' | sed -e 's/]//' | sort -n | head -n 1 `
    MPSMAX=`grep "Sending Messages Time data" ${LOGS_DIR}/timingdata.out | grep MsgsPerSec | sed -e 's/^.*MsgsPerSec//' | sed -e 's/,.*$//' | sed -e 's/\[//' | sed -e 's/]//' | sort -n | tail -n 1 `
    #BytesPerSecond
    BPSMIN=`grep "Sending Messages Time data" ${LOGS_DIR}/timingdata.out | grep BytesPerSecond | sed -e 's/^.*BytesPerSecond//' | sed -e 's/,.*$//' | sed -e 's/\[//' | sed -e 's/]//' | sort -n | head -n 1 `
    BPSMAX=`grep "Sending Messages Time data" ${LOGS_DIR}/timingdata.out | grep BytesPerSecond | sed -e 's/^.*BytesPerSecond//' | sed -e 's/,.*$//' | sed -e 's/\[//' | sed -e 's/]//' | sort -n | tail -n 1 `
    #MsgSize
    MSGSIZE=`grep "Sending Messages Time data" ${LOGS_DIR}/timingdata.out | grep MsgSize | sed -e 's/^.*MsgSize//' | sed -e 's/,.*$//' | sed -e 's/\[//' | sed -e 's/]//' | sort -n | head -n 1 `
    echo "send     ${DELTAMIN}-${DELTAMAX}     ${MPSMIN}-${MPSMAX}      ${BPSMIN}-${BPSMAX}      ${MSGSIZE}         ${NUMOFOBJECTS}           ${NUMOFMSGSPEROBJECT}"

    echo  "==============="
    echo  "The following are the time results for RECEIVING messages:" | tee -a ${LOGS_DIR}/timingdata.out
    grep -a "Receiving Messages Time data" ${LOGS_DIR}/instance*log | tee -a ${LOGS_DIR}/timingdata.out
    echo  "---------------"
    echo "          Time Delta          MsgsPerSec  BytesPerSecond        Messagesize    numofobjects  Number of messages Per Object"
    #Delta
    DELTAMIN=`grep "Receiving Messages Time data" ${LOGS_DIR}/timingdata.out | grep Delta | sed -e 's/^.*Delta//' | sed -e 's/sec.*$//' | sed -e 's/\[//' | sed -e 's/]//' | sort -n | head -n 1 | sed -e 's/ //' `
    DELTAMAX=`grep "Receiving Messages Time data" ${LOGS_DIR}/timingdata.out | grep Delta | sed -e 's/^.*Delta//' | sed -e 's/sec.*$//' | sed -e 's/\[//' | sed -e 's/]//' | sort -n | tail -n 1 | sed -e 's/ //' `
    #MsgsPerSec
    MPSMIN=`grep "Receiving Messages Time data" ${LOGS_DIR}/timingdata.out | grep MsgsPerSec | sed -e 's/^.*MsgsPerSec//' | sed -e 's/,.*$//' | sed -e 's/\[//' | sed -e 's/]//' | sort -n | head -n 1 `
    MPSMAX=`grep "Receiving Messages Time data" ${LOGS_DIR}/timingdata.out | grep MsgsPerSec | sed -e 's/^.*MsgsPerSec//' | sed -e 's/,.*$//' | sed -e 's/\[//' | sed -e 's/]//' | sort -n | tail -n 1 `
    #BytesPerSecond
    BPSMIN=`grep "Receiving Messages Time data" ${LOGS_DIR}/timingdata.out | grep BytesPerSecond | sed -e 's/^.*BytesPerSecond//' | sed -e 's/,.*$//' | sed -e 's/\[//' | sed -e 's/]//' | sort -n | head -n 1 `
    BPSMAX=`grep "Receiving Messages Time data" ${LOGS_DIR}/timingdata.out | grep BytesPerSecond | sed -e 's/^.*BytesPerSecond//' | sed -e 's/,.*$//' | sed -e 's/\[//' | sed -e 's/]//' | sort -n | tail -n 1 `
    #MsgSize
    MSGSIZE=`grep "Receiving Messages Time data" ${LOGS_DIR}/timingdata.out | grep MsgSize | sed -e 's/^.*MsgSize//' | sed -e 's/,.*$//' | sed -e 's/\[//' | sed -e 's/]//' | sort -n | head -n 1 `
    echo "receive  ${DELTAMIN}-${DELTAMAX}     ${MPSMIN}-${MPSMAX}      ${BPSMIN}-${BPSMAX}      ${MSGSIZE}         ${NUMOFOBJECTS}           ${NUMOFMSGSPEROBJECT}"
    echo  "==============="
    echo  "The following are EXCEPTIONS found in the logs:"
    echo  "==============="
    grep -a "Exception" ${LOGS_DIR}/instance*log
    grep -a "Exception" ${LOGS_DIR}/server.log
    echo  "==============="
    echo  "The following are SEVERE messages found in the logs:"
    echo  "==============="
    grep -a "SEVERE" ${LOGS_DIR}/instance*log
    grep -a "SEVERE" ${LOGS_DIR}/server.log
    echo  "==============="
}



while [ $# -ne 0 ]
do
     case ${1} in
       -h)
         usage
       ;;
       -d)
         shift
         DIST=true
       ;;
       -cl)
         shift
         COLLECT_LOGS_DIR="${1}"
         shift
         if [ ! -d ${COLLECT_LOGS_DIR} ];then
            echo "ERROR: Collect Log directory does not exist"
            usage
         fi
       ;;
       -g)
         shift
         GROUPNAME=${1}
         shift
         if [ -z "${GROUPNAME}" ] ;then
            echo "ERROR: Missing group name value"
            usage
         fi
       ;;
       -bia)
         shift
         BIND_INTERFACE_ADDRESS="${1}"
         shift
         if [ -z "${BIND_INTERFACE_ADDRESS}" ]; then
            echo "ERROR: Missing bind interface address value"
            usage
         fi
         BIND_INTERFACE_ADDRESS="-bia ${BIND_INTERFACE_ADDRESS}"
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
            echo "ERROR: Invalid number of objects specified"
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
            echo "ERROR: Invalid number of messages specified"
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
            echo "ERROR: Invalid messages size specified"
            usage
         fi
       ;;
       -t)
         shift
         TRANSPORT=${1}
         shift
         if [ ! -z "${TRANSPORT}" ] ;then
            if [ "${TRANSPORT}" != "grizzly" -a "${TRANSPORT}" != "jxta" -a "${TRANSPORT}" != "jxtanew" ]; then
               echo "ERROR: Invalid transport specified"
               usage
            fi
         else
            echo "ERROR: Missing transport value"
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
       -n)
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
       ;;
       -tt)
         shift
         THINKTIME=`echo "${1}" | egrep "^[0-9]+$" `
         shift
         if [ "${THINKTIME}" != "" ]; then
            if [ ${THINKTIME} -le 0 ];then
               echo "ERROR: Invalid think time specified"
               usage
            fi
         else
            echo "ERROR: Invalid think time specified"
            usage
         fi
       ;;
       *)
         echo "ERROR: Invalid argument specified [${1}]"
         usage
       ;;
     esac
done


echo ${MULTICASTADDRESS} > ./currentMulticastAddress.txt

echo Transport: ${TRANSPORT}
if [ $TRANSPORT != "grizzly" ]; then
    JARS=${JXTA_JARS}
fi

if [ $DIST = true ]; then
    NUMOFMEMBERS=`find ${CLUSTER_CONFIGS}/${GROUPNAME} -name "*.properties" | grep -v server.properties | sort | wc -w`
    if [ -z "${COLLECT_LOGS_DIR}" ];then
       echo "ERROR: When using distributed mode, you must specified the -cl option so logs can be saved and analyzed"
       usage
    fi
    touch ${COLLECT_LOGS_DIR}/test
    if [ ! -f "${COLLECT_LOGS_DIR}/test" ];then
       echo "ERROR: Unable to write to the directory specified by -l [${COLLECT_LOGS_DIR}]"
       usage
    fi
    rm -rf ${COLLECT_LOGS_DIR}/test
fi
echo "NumberOfMembers: ${NUMOFMEMBERS}"
echo "LOGS_DIRS=${LOGS_DIR}"
echo "Killing any existing members and cleaning out old logs"
if [ $DIST = false ]; then
   killmembers.sh
   rm -rf ${LOGS_DIR}/*.log
else
    if [ -f ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties ]; then
       MEMBERS=`find ${CLUSTER_CONFIGS}/${GROUPNAME} -name "*.properties"  `
       for member in ${MEMBERS}
       do
          TMP=`egrep "^MACHINE_NAME" ${member}`
          MACHINE_NAME=`echo $TMP | awk -F= '{print $2}' `
          TMP=`egrep "^INSTANCE_NAME" ${member}`
          INSTANCE_NAME=`echo $TMP | awk -F= '{print $2}' `
          TMP=`egrep "^WORKSPACE_HOME" ${member}`
          WORKSPACE_HOME=`echo $TMP | awk -F= '{print $2}' `
          echo "killing ${INSTANCE_NAME} on ${MACHINE_NAME}"
          ${EXECUTE_REMOTE_CONNECT} ${MACHINE_NAME} "cd ${WORKSPACE_HOME};killmembers.sh;rm -rf ${LOGS_DIR}/*.log"
       done
    else
       echo "ERROR: Could not find ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties"
       exit 1
    fi

fi

#--------------------------
# STARTING OF THE MASTER
if [ $DIST = false ]; then
    mkdir -p ${LOGS_DIR}
    echo "Removing old logs"
    rm -f ${LOGS_DIR}/*.log
    echo "Starting server"
    _HAMessageBuddyReplicationSimulator.sh server SPECTATOR ${NUMOFMEMBERS} -g ${GROUPNAME} -tll ${TEST_LOG_LEVEL} -sll ${SHOALGMS_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} ${BIND_INTERFACE_ADDRESS} >& ${LOGS_DIR}/server.log &
else
    if [ -f ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties ]; then
       TMP=`egrep "^MACHINE_NAME" ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties`
       MACHINE_NAME=`echo $TMP | awk -F= '{print $2}' `
       TMP=`egrep "^WORKSPACE_HOME" ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties`
       WORKSPACE_HOME=`echo $TMP | awk -F= '{print $2}' `
       TMP=`egrep "^BIND_INTERFACE_ADDRESS" ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties`
       BIND_INTERFACE_ADDRESS=`echo $TMP | awk -F= '{print $2}' `
       if [ ! -z ${BIND_INTERFACE_ADDRESS} ];then
          BIND_INTERFACE_ADDRESS="-bia ${BIND_INTERFACE_ADDRESS}"
       fi
       echo "Starting server on ${MACHINE_NAME}"
       ${EXECUTE_REMOTE_CONNECT} ${MACHINE_NAME} "cd ${WORKSPACE_HOME}; mkdir -p ${LOGS_DIR};./_HAMessageBuddyReplicationSimulator.sh server SPECTATOR ${NUMOFMEMBERS} -g ${GROUPNAME} -tll ${TEST_LOG_LEVEL} -sll ${SHOALGMS_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} ${BIND_INTERFACE_ADDRESS} -l ${WORKSPACE_HOME}/${LOGS_DIR}"
    else
       echo "ERROR: Could not find ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties"
       exit 1
    fi
fi

#--------------------------
# Give time for the server to startup before starting the other members
sleep 5

sdtcp=9161
edtcp=`expr ${sdtcp} + 30`
if [ $DIST = false ]; then
    # single machine startup
    echo "Starting ${NUMOFMEMBERS} CORE members"
    count=1
    while [ $count -le ${NUMOFMEMBERS} ]
    do
        INSTANCE_NAME="instance`expr ${count} + 100 `"
        echo "Starting ${INSTANCE_NAME}"
        MEMBERSTARTCMD="./_HAMessageBuddyReplicationSimulator.sh ${INSTANCE_NAME} CORE ${NUMOFMEMBERS} -noo ${NUMOFOBJECTS} -nom ${NUMOFMSGSPEROBJECT} -ms ${MSGSIZE} -g ${GROUPNAME} -tll ${TEST_LOG_LEVEL} -sll ${SHOALGMS_LOG_LEVEL} -ts ${sdtcp} -te ${edtcp} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} ${BIND_INTERFACE_ADDRESS} -l ${LOGS_DIR} -tt ${THINKTIME}"
        ${MEMBERSTARTCMD}

        sdtcp=`expr ${edtcp} + 1`
        edtcp=`expr ${sdtcp} + 30`
        count=`expr ${count} + 1`
    done
else 
   # distributed environment startup
   echo "Starting CORE members in the distributed environment"

   MEMBERS=`find ${CLUSTER_CONFIGS}/${GROUPNAME} -name "*.properties" | grep -v server.properties  `
   for member in ${MEMBERS}
   do
      TMP=`egrep "^MACHINE_NAME" ${member}`
      MACHINE_NAME=`echo $TMP | awk -F= '{print $2}' `
      TMP=`egrep "^INSTANCE_NAME" ${member}`
      INSTANCE_NAME=`echo $TMP | awk -F= '{print $2}' `
      TMP=`egrep "^WORKSPACE_HOME" ${member}`
      WORKSPACE_HOME=`echo $TMP | awk -F= '{print $2}' `
      TMP=`egrep "^BIND_INTERFACE_ADDRESS" ${member}`
      BIND_INTERFACE_ADDRESS=`echo $TMP | awk -F= '{print $2}' `
      if [ ! -z ${BIND_INTERFACE_ADDRESS} ];then
          BIND_INTERFACE_ADDRESS="-bia ${BIND_INTERFACE_ADDRESS}"
      fi
      echo "Starting ${INSTANCE_NAME} on ${MACHINE_NAME}"

      MEMBERSTARTCMD="./_HAMessageBuddyReplicationSimulator.sh ${INSTANCE_NAME} CORE ${NUMOFMEMBERS} -noo ${NUMOFOBJECTS} -nom ${NUMOFMSGSPEROBJECT} -ms ${MSGSIZE} -g ${GROUPNAME} -tll ${TEST_LOG_LEVEL} -sll ${SHOALGMS_LOG_LEVEL} -ts ${sdtcp} -te ${edtcp} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} ${BIND_INTERFACE_ADDRESS} -l ${WORKSPACE_HOME}/${LOGS_DIR} -tt ${THINKTIME}"
      ${EXECUTE_REMOTE_CONNECT} ${MACHINE_NAME} "cd ${WORKSPACE_HOME}; mkdir -p ${LOGS_DIR}; ${MEMBERSTARTCMD}"
   done
fi

echo "Waiting for testing to complete"
if [ $DIST = false ]; then
    _HAMessageBuddyReplicationSimulator.sh -wait ${NUMOFMEMBERS} -g ${GROUPNAME} -l ${LOGS_DIR}
    analyzeLogs
else
    # we are running in a dist mode and we want to wait until all the instances are done and the server.log contains Testing Complete
    if [ -f ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties ]; then
       TMP=`egrep "^MACHINE_NAME" ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties`
       MACHINE_NAME=`echo $TMP | awk -F= '{print $2}' `
       TMP=`egrep "^WORKSPACE_HOME" ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties`
       WORKSPACE_HOME=`echo $TMP | awk -F= '{print $2}' `
       echo "Waiting for server on ${MACHINE_NAME} to complete testing"
       ${EXECUTE_REMOTE_CONNECT} ${MACHINE_NAME} "cd ${WORKSPACE_HOME};_HAMessageBuddyReplicationSimulator.sh -wait ${NUMOFMEMBERS} -g ${GROUPNAME} -l ${WORKSPACE_HOME}/${LOGS_DIR}"

       echo "Collecting logs from all machines"
       # remove any existing files before doing copy
       rm -rf ${COLLECT_LOGS_DIR}/*

       MEMBERS=`find ${CLUSTER_CONFIGS}/${GROUPNAME} -name "*.properties"  `
       for member in ${MEMBERS}
       do
          TMP=`egrep "^MACHINE_NAME" ${member}`
          MACHINE_NAME=`echo $TMP | awk -F= '{print $2}' `
          TMP=`egrep "^INSTANCE_NAME" ${member}`
          INSTANCE_NAME=`echo $TMP | awk -F= '{print $2}' `
          TMP=`egrep "^WORKSPACE_HOME" ${member}`
          WORKSPACE_HOME=`echo $TMP | awk -F= '{print $2}' `
          echo "Collecting logs from ${INSTANCE_NAME} on ${MACHINE_NAME}"

         ${EXECUTE_REMOTE_CONNECT} ${MACHINE_NAME} "cd ${WORKSPACE_HOME}; cp -r ${LOGS_DIR}/* ${COLLECT_LOGS_DIR}"
      done
      LOGS_DIR=${COLLECT_LOGS_DIR}
      analyzeLogs
    else
       echo "ERROR: Could not find ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties"
       exit 1
    fi
fi
echo "Testing Complete"
