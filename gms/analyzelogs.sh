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

usage () {
 echo "usage: [-h] [-l logdir] [add|stop|kill|rejoin|default]"
 exit 1
}
LOGS_DIR=LOGS

# default and "" are the same value
CMD=default

while [ $# -ne 0 ]
do
     case ${1} in
       -h)
         usage
       ;;
       add|stop|kill|rejoin|default|"")
         # default and "" are the same value
         CMD=${1}
         shift
       ;;
       -l)
         shift
         LOGS_DIR=${1}
         shift
         if [ ! -z "${LOGS_DIR}" ] ;then
             if [ ! -d "${LOGS_DIR}" ] ;then
                echo "ERROR: The log dir specified does not exist"
             fi
         else
            echo "ERROR: Missing log dir value"
            usage
         fi
       ;;
       *)
         echo "ERROR: Invalid argument [${1}]"
         usage         
       ;;
     esac
done
if [ "${CMD}" = "add" ]; then
   LOGS_DIR=${LOGS_DIR}/simulateCluster_add
elif [ "${CMD}" = "stop" ]; then
   LOGS_DIR=${LOGS_DIR}/simulateCluster_stop
elif [ "${CMD}" = "kill" ]; then
   LOGS_DIR=${LOGS_DIR}/simulateCluster_kill
elif [ "${CMD}" = "rejoin" ]; then
   LOGS_DIR=${LOGS_DIR}/simulateCluster_rejoin
else
   LOGS_DIR=${LOGS_DIR}/simulateCluster
fi

echo "Logs_Dir:${LOGS_DIR}"


APPLICATIONADMIN=admincli

SERVERLOG=${LOGS_DIR}/server.log
ALLLOGS=`ls ${LOGS_DIR}/*log | egrep "^.*\/(instance[0-9][0-9]|server).log"`
NUMOFMEMBERS=`ls ${LOGS_DIR}/*log | egrep "^.*\/(instance[0-9][0-9]|server).log" | wc -l`
NUMOFINSTANCES=`ls ${LOGS_DIR}/inst*log | egrep "^.*\/instance[0-9][0-9].log" | wc -l`

PASS_TOTAL=0
FAIL_TOTAL=0

if [ ${NUMOFMEMBERS} -le 0 ]; then
   echo "ERROR: No logs were found"
   usage
fi


echo "Report for simulation "
grep "Running using Shoal with transport" ${SERVERLOG}
echo

TMP=`grep "Adding Join member:"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN} | wc -l`
if [ "${CMD}" = "stop" ]; then
   EXPECTED=`expr ${NUMOFINSTANCES} + 1`
elif [ "${CMD}" = "kill" ]; then
   EXPECTED=`expr ${NUMOFINSTANCES} + 1`
elif [ "${CMD}" = "rejoin" ]; then
   EXPECTED=`expr ${NUMOFINSTANCES} + 1`
elif [ "${CMD}" = "add" ]; then
   EXPECTED=${NUMOFINSTANCES}
else
   EXPECTED=${NUMOFINSTANCES}
fi
if [ ${TMP} -eq ${EXPECTED} ];then
   echo "Check for JOIN in DAS log. Expect: ${EXPECTED},  Found: ${TMP} [PASSED]"
   PASS_TOTAL=`expr ${PASS_TOTAL} + 1 `
else
   echo "Check for JOIN in DAS log. Expect: ${EXPECTED},  Found: ${TMP} [FAILED]"
   echo "-----------------"
   grep "Adding Join member:"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN}
   echo "-----------------"
   FAIL_TOTAL=`expr ${FAIL_TOTAL} + 1 `
fi

echo
TMP=`grep "JOINED_AND_READY_EVENT for member:"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN} | wc -l`
if [ "${CMD}" = "stop" ]; then
   EXPECTED=`expr ${NUMOFMEMBERS} + 1`
elif [ "${CMD}" = "kill" ]; then
   EXPECTED=`expr ${NUMOFMEMBERS} + 1`
elif [ "${CMD}" = "rejoin" ]; then
   EXPECTED=`expr ${NUMOFMEMBERS} + 1`
elif [ "${CMD}" = "add" ]; then
   EXPECTED=`expr ${NUMOFMEMBERS}`
else
   EXPECTED=`expr ${NUMOFMEMBERS}`
fi
if [ ${TMP} -eq ${EXPECTED} ];then
   echo "Check for JOINED_AND_READY_EVENT in DAS log. Expect: ${EXPECTED},  Found: ${TMP}  [PASSED]"
   PASS_TOTAL=`expr ${PASS_TOTAL} + 1 `
else
   echo "Check for JOINED_AND_READY_EVENT in DAS log. Expect: ${EXPECTED},  Found: ${TMP} [FAILED]"
   echo "-----------------"
   grep "JOINED_AND_READY_EVENT for member:"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN}
   echo "-----------------"
   FAIL_TOTAL=`expr ${FAIL_TOTAL} + 1 `
fi

echo
TMP=`grep "rejoining: missed reporting past" ${SERVERLOG} | grep "addJoinNotificationSignal" | grep -v ${APPLICATIONADMIN} | wc -l`
if [ "${CMD}" = "stop" ]; then
   EXPECTED=0
elif [ "${CMD}" = "kill" ]; then
   EXPECTED=0
elif [ "${CMD}" = "rejoin" ]; then
   EXPECTED=1
elif [ "${CMD}" = "add" ]; then
   EXPECTED=0
else
   EXPECTED=0
fi
if [ ${TMP} -eq ${EXPECTED} ];then
     echo "Check for REJOIN(addJoinNotificationSignal) in DAS log. Expect: ${EXPECTED},  Found: ${TMP}  [PASSED]"
     PASS_TOTAL=`expr ${PASS_TOTAL} + 1 `
else
     echo "Check for REJOIN(addJoinNotificationSignal) in DAS log. Expect: ${EXPECTED},  Found: ${TMP} [FAILED]"
     echo "-----------------"
     grep "rejoining: missed reporting past" ${SERVERLOG} | grep "addJoinNotificationSignal" | grep -v ${APPLICATIONADMIN}
     echo "-----------------"
     FAIL_TOTAL=`expr ${FAIL_TOTAL} + 1 `
fi

echo
TMP=`grep "rejoining: missed reporting past" ${SERVERLOG} | grep "addJoinedAndReadyNotificationSignal" | grep -v ${APPLICATIONADMIN} | wc -l`
if [ "${CMD}" = "stop" ]; then
   EXPECTED=0
elif [ "${CMD}" = "kill" ]; then
   EXPECTED=0
elif [ "${CMD}" = "rejoin" ]; then
   EXPECTED=1
elif [ "${CMD}" = "add" ]; then
   EXPECTED=0
else
   EXPECTED=0
fi
if [ ${TMP} -eq ${EXPECTED} ];then
     echo "Check for REJOIN(addJoinedAndReadyNotificationSignal) in DAS log. Expect: ${EXPECTED},  Found: ${TMP}  [PASSED]"
     PASS_TOTAL=`expr ${PASS_TOTAL} + 1 `
else
     echo "Check for REJOIN(addJoinedAndReadyNotificationSignal) in DAS log. Expect: ${EXPECTED},  Found: ${TMP} [FAILED]"
     echo "-----------------"
     grep "rejoining: missed reporting past" ${SERVERLOG} | grep "addJoinedAndReadyNotificationSignal" | grep -v ${APPLICATIONADMIN}
     echo "-----------------"
     FAIL_TOTAL=`expr ${FAIL_TOTAL} + 1 `
fi

echo
TMP=`grep "Received PlannedShutdownEvent"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN} | wc -l`
if [ "${CMD}" = "stop" ]; then
   EXPECTED=`expr ${NUMOFINSTANCES} + 1`
elif [ "${CMD}" = "kill" ]; then
   EXPECTED=`expr ${NUMOFINSTANCES}`
elif [ "${CMD}" = "rejoin" ]; then
   EXPECTED=${NUMOFINSTANCES}
elif [ "${CMD}" = "add" ]; then
   EXPECTED=${NUMOFINSTANCES}
else
   EXPECTED=${NUMOFINSTANCES}
fi
if [ ${TMP} -eq ${EXPECTED} ];then
   echo "PlannedShutdownEvent in DAS log. Expect: ${EXPECTED},   Found: ${TMP}  [PASSED]"
   PASS_TOTAL=`expr ${PASS_TOTAL} + 1 `
else
   echo "PlannedShutdownEvent in DAS log. Expect: ${EXPECTED},   Found: ${TMP} [FAILED]"
   echo "-----------------"
   grep "Received PlannedShutdownEvent"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN}
   echo "-----------------"
   FAIL_TOTAL=`expr ${FAIL_TOTAL} + 1 `
fi

echo
TMP=`grep "adding GroupLeadershipNotification"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN} | wc -l`
if [ "${CMD}" = "stop" ]; then
   EXPECTED=2
elif [ "${CMD}" = "kill" ]; then
   EXPECTED=2
elif [ "${CMD}" = "rejoin" ]; then
   EXPECTED=2
elif [ "${CMD}" = "add" ]; then
   EXPECTED=2
else
   EXPECTED=2
fi
if [ ${TMP} -eq ${EXPECTED} ];then
   echo "Check for GroupLeadershipNotifications in DAS log. Expect: ${EXPECTED},   from server. Found: ${TMP}  [PASSED]"
   PASS_TOTAL=`expr ${PASS_TOTAL} + 1 `
else
   echo "Check for GroupLeadershipNotifications in DAS log. Expect: ${EXPECTED},   from server. Found: ${TMP} [FAILED]"
   echo "-----------------"
   grep "adding GroupLeadershipNotification"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN}
   echo "-----------------"
   FAIL_TOTAL=`expr ${FAIL_TOTAL} + 1 `
fi

echo "Check for issues in any members sending a GroupLeadershipNotification to server for this scenario"
grep "adding GroupLeadershipNotification"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN} | grep -v server
echo
echo "*****************************************"

if [ "${CMD}" = "kill" ]; then
   echo
   TMP=`grep "has failed"  ${ALLLOGS} | grep -v ${APPLICATIONADMIN} | wc -l`
   EXPECTED=`expr ${NUMOFINSTANCES}`

   if [ ${TMP} -eq ${EXPECTED} ];then
       echo "Check for failure of killed instance over all logs.  Expect: ${EXPECTED},   Found: ${TMP} [PASSED]"
       PASS_TOTAL=`expr ${PASS_TOTAL} + 1 `
   else
       echo "Check for failure of killed instance over all logs.  Expect: ${EXPECTED},   Found: ${TMP} [FAILED]"
       echo "-----------------"
       grep "has failed"  ${ALLLOGS} | grep -v ${APPLICATIONADMIN}
       echo "-----------------"
       FAIL_TOTAL=`expr ${FAIL_TOTAL} + 1 `
   fi
fi
echo
echo "*****************************************"

echo
TMP=`grep "Adding Join member:"  ${ALLLOGS} | grep -v ${APPLICATIONADMIN} | wc -l`
# format of the expected   instance counts  +  das counts  + test count adjustments
if [ "${CMD}" = "stop" ]; then
   EXPECTED=`expr  ${NUMOFMEMBERS} \* ${NUMOFMEMBERS} `
elif [ "${CMD}" = "kill" ]; then
   EXPECTED=`expr \( ${NUMOFMEMBERS} \* ${NUMOFMEMBERS} \) + 2`
elif [ "${CMD}" = "rejoin" ]; then
   EXPECTED=`expr \( ${NUMOFMEMBERS} \* ${NUMOFMEMBERS} \) + 2`
elif [ "${CMD}" = "add" ]; then
   EXPECTED=`expr ${NUMOFMEMBERS} \* ${NUMOFINSTANCES} `
else
   EXPECTED=`expr ${NUMOFMEMBERS} \* ${NUMOFINSTANCES} `
fi
echo "Check for Join members over all logs.  Expect: ${EXPECTED},   Found: ${TMP}"
TMP=`grep "Adding Join member:"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN}  | wc -l`
echo "Join in server    : ${TMP}"
count=1
num=0
while [ $count -le ${NUMOFINSTANCES} ]
do
    if [ ${count} -lt 10 ]; then
       num="0${count}"
    else
       num=${count}
    fi
    TMP=`grep "Adding Join member:"  ${LOGS_DIR}/instance${num}.log | grep -v ${APPLICATIONADMIN}  | wc -l`
    echo "Join in instance${num}: ${TMP}"
    count=`expr ${count} + 1`
done

echo
TMP=`grep "JOINED_AND_READY_EVENT for member:"  ${ALLLOGS} | grep -v ${APPLICATIONADMIN} | wc -l`
if [ "${CMD}" = "stop" ]; then
   EXPECTED=`expr \( ${NUMOFMEMBERS} \* ${NUMOFMEMBERS} \) + 1`
elif [ "${CMD}" = "kill" ]; then
   EXPECTED=`expr \( ${NUMOFMEMBERS} \* ${NUMOFMEMBERS} \) + 1 `
elif [ "${CMD}" = "rejoin" ]; then
   EXPECTED=`expr \( ${NUMOFMEMBERS} \* ${NUMOFMEMBERS} \) + 1`
elif [ "${CMD}" = "add" ]; then
   EXPECTED=`expr ${NUMOFMEMBERS} \* ${NUMOFINSTANCES} + 1 `
else
   EXPECTED=`expr ${NUMOFMEMBERS} \* ${NUMOFINSTANCES} + 1 `
fi
echo "Check for JOINED_AND_READY_EVENT over all logs. Expect ${EXPECTED},   Found: ${TMP}"
TMP=`grep "JOINED_AND_READY_EVENT for member:"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN} | wc -l`
echo "JoinAndReady in server    : ${TMP}"
count=1
num=0
while [ $count -le ${NUMOFINSTANCES} ]
do
    if [  ${count} -lt 10 ]; then
       num="0${count}"
    else
       num=${count}
    fi
    LOG=${LOGS_DIR}/instance${num}.log
    TMP=`grep "JOINED_AND_READY_EVENT for member:"  ${LOG} | grep -v ${APPLICATIONADMIN} | wc -l`
    echo "JoinAndReady in instance${num}: ${TMP}"
    count=`expr ${count} + 1`
done

echo
TMP=`grep "rejoining: missed reporting past" ${ALLLOGS} | grep "addJoinNotificationSignal" | grep -v ${APPLICATIONADMIN} | wc -l`
if [ "${CMD}" = "stop" ]; then
   EXPECTED=0
elif [ "${CMD}" = "kill" ]; then
   EXPECTED=0
elif [ "${CMD}" = "rejoin" ]; then
   EXPECTED=`expr ${NUMOFMEMBERS} - 1`
elif [ "${CMD}" = "add" ]; then
   EXPECTED=0
else
   EXPECTED=0
fi
echo "Check for REJOIN(addJoinNotificationSignal) over all logs. Expect ${EXPECTED},   Found: ${TMP}"
TMP=`grep "rejoining: missed reporting past"  ${SERVERLOG} | grep "addJoinNotificationSignal" | grep -v ${APPLICATIONADMIN} | wc -l`
echo "REJOIN(addJoinNotificationSignal) in server    : ${TMP}"
count=1
num=0
while [ $count -le ${NUMOFINSTANCES} ]
do
    if [  ${count} -lt 10 ]; then
       num="0${count}"
    else
       num=${count}
    fi
    LOG=${LOGS_DIR}/instance${num}.log
    TMP=`grep "rejoining: missed reporting past"  ${LOG} | grep "addJoinNotificationSignal" | grep -v ${APPLICATIONADMIN} | wc -l`
    echo "REJOIN(addJoinNotificationSignal) in instance${num}: ${TMP}"
    count=`expr ${count} + 1`
done

echo
TMP=`grep "rejoining: missed reporting past" ${ALLLOGS} | grep "addJoinedAndReadyNotificationSignal" | grep -v ${APPLICATIONADMIN} | wc -l`
if [ "${CMD}" = "stop" ]; then
   EXPECTED=0
elif [ "${CMD}" = "kill" ]; then
   EXPECTED=0
elif [ "${CMD}" = "rejoin" ]; then
   EXPECTED=${NUMOFMEMBERS}
elif [ "${CMD}" = "add" ]; then
   EXPECTED=0
else
   EXPECTED=0
fi
echo "Check for REJOIN(addJoinedAndReadyNotificationSignal) over all logs. Expect ${EXPECTED},   Found: ${TMP}"
TMP=`grep "rejoining: missed reporting past"  ${SERVERLOG} | grep "addJoinedAndReadyNotificationSignal" | grep -v ${APPLICATIONADMIN} | wc -l`
echo "REJOIN(addJoinedAndReadyNotificationSignal) in server    : ${TMP}"
count=1
num=0
while [ $count -le ${NUMOFINSTANCES} ]
do
    if [  ${count} -lt 10 ]; then
       num="0${count}"
    else
       num=${count}
    fi
    LOG=${LOGS_DIR}/instance${num}.log
    TMP=`grep "rejoining: missed reporting past"  ${LOG} | grep "addJoinedAndReadyNotificationSignal" | grep -v ${APPLICATIONADMIN} | wc -l`
    echo "REJOIN(addJoinedAndReadyNotificationSignal) in instance${num}: ${TMP}"
    count=`expr ${count} + 1`
done

echo
echo "*****************************************"
TMP=`grep "|SEVERE|" ${ALLLOGS}  | grep -v ${APPLICATIONADMIN} | wc -l`
if [ ${TMP} -eq 0 ];then
   echo "Number of Severe :  ${TMP}  [PASSED]"
   PASS_TOTAL=`expr ${PASS_TOTAL} + 1 `
else
   echo "Number of Severe :  ${TMP}  [FAILED]"
   FAIL_TOTAL=`expr ${FAIL_TOTAL} + ${TMP} `
fi

if [ "${CMD}" = "stop" ]; then
    TMP=`grep WARNING  ${ALLLOGS} | grep -v ${APPLICATIONADMIN} | wc -l`
elif [ "${CMD}" = "kill" ]; then
    TMP=`grep WARNING  ${ALLLOGS} | grep -v ${APPLICATIONADMIN} | grep -v "was restarted at" | grep -v "Note that there was no Failure notification" | wc -l`
elif [ "${CMD}" = "rejoin" ]; then
    TMP=`grep WARNING  ${ALLLOGS} | grep -v ${APPLICATIONADMIN} | grep -v "was restarted at" | grep -v "Note that there was no Failure notification" | wc -l`
elif [ "${CMD}" = "add" ]; then
    TMP=`grep WARNING  ${ALLLOGS} | grep -v ${APPLICATIONADMIN} | wc -l`
else
    TMP=`grep WARNING  ${ALLLOGS} | grep -v ${APPLICATIONADMIN} | wc -l`
fi
echo "Number of Warnings: ${TMP}"

TMP=`grep "Exception" ${ALLLOGS}  | grep -v ${APPLICATIONADMIN} | grep -v "stack trace" | wc -l`
if [ ${TMP} -eq 0 ];then
   echo "Number of Exceptions :  ${TMP}  [PASSED]"
   PASS_TOTAL=`expr ${PASS_TOTAL} + 1 `
else
   echo "Number of Exceptions :  ${TMP}  [FAILED]"
   FAIL_TOTAL=`expr ${FAIL_TOTAL} + ${TMP} `
fi


echo
echo "**************************"
echo "TEST SUMMARY"
echo "--------------------------"
echo "PASSED: ${PASS_TOTAL}"
echo "FAILED: ${FAIL_TOTAL}"
echo "**************************"
echo
echo
echo SEVERE events
grep "|SEVERE|"  ${ALLLOGS} | grep -v ${APPLICATIONADMIN}
echo WARNING events
if [ "${CMD}" = "stop" ]; then
    grep WARNING  ${ALLLOGS} | grep -v ${APPLICATIONADMIN}
elif [ "${CMD}" = "kill" ]; then
    grep WARNING  ${ALLLOGS} | grep -v ${APPLICATIONADMIN} | grep -v "was restarted at" | grep -v "Note that there was no Failure notification"
elif [ "${CMD}" = "rejoin" ]; then
    grep WARNING  ${ALLLOGS} | grep -v ${APPLICATIONADMIN} | grep -v "was restarted at" | grep -v "Note that there was no Failure notification"
elif [ "${CMD}" = "add" ]; then
    grep WARNING  ${ALLLOGS} | grep -v ${APPLICATIONADMIN}
else
    grep WARNING  ${ALLLOGS} | grep -v ${APPLICATIONADMIN}
fi
echo Exceptions 
grep "Exception" ${ALLLOGS}  | grep -v ${APPLICATIONADMIN} | grep -v "stack trace"
