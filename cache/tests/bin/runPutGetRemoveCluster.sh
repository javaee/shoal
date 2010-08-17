#!/bin/sh +x

#
# Copyright 2010 Sun Microsystems, Inc.  All rights reserved.
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


NUMOFOBJECTS=100
MSGSIZE=1024
GROUPNAME="putgetremovetestgroup"
TEST_LOG_LEVEL=INFO
SHOALGMS_LOG_LEVEL=INFO




CACHE_WORKSPACE_HOME=`pwd`/../..
CACHE_TESTS_DIR=`pwd`/..
LOGS_DIR=$CACHE_TESTS_DIR/LOGS/putgetremove
SHOAL_WORKSPACE_HOME=${CACHE_WORKSPACE_HOME}/../gms


usage () {
 echo "usage: [-h] [numberOfMembers(10 is default)]"
exit 1
}

if [ "$1" == "-h" ]; then
    usage
fi

NUMOFMEMBERS=10
if [ ! -z "${1}" ]; then
    NUMOFMEMBERS=`echo "${1}" | egrep "^[0-9]+$" `
    if [ "${NUMOFMEMBERS}" != "" ]; then
       if [ ${NUMOFMEMBERS} -le 0 ];then
          echo "ERROR: Invalid number of members specified"
          usage
       fi
    else
       echo "ERROR: Invalid number of members specified"
       usage
    fi
    shift
fi



MULTICASTPORT=2299
MULTICASTADDRESS=229.9.1.`${SHOAL_WORKSPACE_HOME}/randomNumber.sh`
echo ${MULTICASTADDRESS} > ./currentMulticastAddress.txt

mkdir -p ${LOGS_DIR}
echo "Removing old logs"
rm -f ${LOGS_DIR}/server.log ${LOGS_DIR}/instance??.log ./currentMulticastAddress.txt


echo "Starting admin... "
./runPutGetRemove.sh server ${GROUPNAME} -tl ${TEST_LOG_LEVEL} -sl ${SHOALGMS_LOG_LEVEL} -ts 9130 -te 9160 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/server.log 2>&1 &
sleep 5

echo "Starting ${NUMOFMEMBERS} members"

instanceNum=1
sdtcp=9161
edtcp=`expr ${sdtcp} + 30`
count=1
while [ $count -le ${NUMOFMEMBERS} ]
do
   if [  ${instanceNum} -lt 10 ]; then
       iNum="0${instanceNum}"
    else
       iNum=${instanceNum}
    fi
    ./runPutGetRemove.sh instance${iNum} ${GROUPNAME}  ${NUMOFOBJECTS} ${MSGSIZE} -tl ${TEST_LOG_LEVEL} -sl ${SHOALGMS_LOG_LEVEL} -ts ${sdtcp} -te ${edtcp} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/instance${iNum}.log 2>&1 &
    instanceNum=`expr ${instanceNum} + 1`
    sdtcp=`expr ${edtcp} + 1`
    edtcp=`expr ${sdtcp} + 30`
    count=`expr ${count} + 1`
done


PWD=`pwd`
cd ${SHOAL_WORKSPACE_HOME}
echo "Waiting for group [${GROUPNAME}] to complete startup"
./gms_admin.sh waits ${GROUPNAME} -tl ${TEST_LOG_LEVEL} -sl ${SHOALGMS_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT}

echo "Group startup has completed, execute testing and wait for it to complete"
./gms_admin.sh test ${GROUPNAME} -tl ${TEST_LOG_LEVEL} -sl ${SHOALGMS_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT}

echo "Group testing has completed, shutting down group [${GROUPNAME}]"
./gms_admin.sh stopc ${GROUPNAME} -tl ${TEST_LOG_LEVEL} -sl ${SHOALGMS_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT}
cd ${PWD}

FAILURES=`grep -a "SEVERE" ${LOGS_DIR}/* `

NUMFAILED=`grep -a "SEVERE" ${LOGS_DIR}/* | wc -l | tr -d ' ' `
echo "FAILURES:${NUMFAILED}"
echo "========================="
echo "${FAILURES}"
echo "========================="
