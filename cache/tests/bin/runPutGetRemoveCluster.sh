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
