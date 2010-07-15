#!/bin/sh +x

usage () {
 echo "usage: [-h] [-l logdir] [stop|kill|rejoin|default is normal)]"
 exit 1
}
LOGS_DIR=LOGS
CMD=normal
while [ $# -ne 0 ]
do
     case ${1} in
       -h)
         usage
       ;;
       stop|kill|rejoin)
         CMD=${1}
         shift
         if [ ! -z "${CMD}" ] ;then
            if [ "${CMD}" != "stop" -a "${CMD}" != "kill" -a "${CMD}" != "rejoin" -a "${CMD}" != "normal" ]; then
               echo "ERROR: Invalid command specified"
               usage
            fi
         else
            echo "ERROR: Missing command value"
            usage
         fi
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
         echo "ERROR: Invalid argument"
         usage         
       ;;
     esac
done
if [ "${CMD}" = "stop" ]; then
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
ALLLOGS=`ls ${LOGS_DIR}/*log | grep "[server.log|instance*.log]"`
NUMOFMEMBERS=`ls ${LOGS_DIR}/*log | grep "[server.log|instance*.log]" | wc -l`
NUMOFINSTANCES=`ls ${LOGS_DIR}/inst*log | wc -l`

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
else
   EXPECTED=${NUMOFINSTANCES}
fi

if [ ${TMP} -eq ${EXPECTED} ];then
   echo "Check for JOIN in DAS log. Expect: ${EXPECTED},  Found: ${TMP} [PASSED]"
   PASS_TOTAL=`expr ${PASS_TOTAL} + 1 `
else
   echo "Check for JOIN in DAS log. Expect: ${EXPECTED},  Found: ${TMP} [FAILED]"
   FAIL_TOTAL=`expr ${FAIL_TOTAL} + 1 `
fi


echo
TMP=`grep "JOINED_AND_READY_EVENT for Member:"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN} | wc -l`
if [ "${CMD}" = "stop" ]; then
   EXPECTED=`expr ${NUMOFMEMBERS} + 1`
elif [ "${CMD}" = "kill" ]; then
   EXPECTED=`expr ${NUMOFMEMBERS} + 1`
elif [ "${CMD}" = "rejoin" ]; then
   EXPECTED=`expr ${NUMOFMEMBERS} + 1`
else
   EXPECTED=`expr ${NUMOFMEMBERS}`
fi
if [ ${TMP} -eq ${EXPECTED} ];then
   echo "Check for JOINED_AND_READY_EVENT in DAS log. Expect: ${EXPECTED},  Found: ${TMP}  [PASSED]"
   PASS_TOTAL=`expr ${PASS_TOTAL} + 1 `
else
   echo "Check for JOINED_AND_READY_EVENT in DAS log. Expect: ${EXPECTED},  Found: ${TMP} [FAILED]"
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
else
   EXPECTED=${NUMOFINSTANCES}
fi

if [ ${TMP} -eq ${EXPECTED} ];then
   echo "PlannedShutdownEvent in DAS log. Expect: ${EXPECTED},   Found: ${TMP}  [PASSED]"
   PASS_TOTAL=`expr ${PASS_TOTAL} + 1 `
else
   echo "PlannedShutdownEvent in DAS log. Expect: ${EXPECTED},   Found: ${TMP} [FAILED]"
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
else
   EXPECTED=2
fi
if [ ${TMP} -eq ${EXPECTED} ];then
   echo "Check for GroupLeadershipNotifications in DAS log. Expect: ${EXPECTED},   from server. Found: ${TMP}  [PASSED]"
   PASS_TOTAL=`expr ${PASS_TOTAL} + 1 `
else
   echo "Check for GroupLeadershipNotifications in DAS log. Expect: ${EXPECTED},   from server. Found: ${TMP} [FAILED]"
   FAIL_TOTAL=`expr ${FAIL_TOTAL} + 1 `
fi

echo "Check for issues in any members sending a GroupLeadershipNotification to server for this scenario"
grep "adding GroupLeadershipNotification"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN} | grep -v server
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
TMP=`grep "JOINED_AND_READY_EVENT for Member:"  ${ALLLOGS} | grep -v ${APPLICATIONADMIN} | wc -l`
if [ "${CMD}" = "stop" ]; then
   EXPECTED=`expr \( ${NUMOFMEMBERS} \* ${NUMOFMEMBERS} \) + 1`
elif [ "${CMD}" = "kill" ]; then
   EXPECTED=`expr \( ${NUMOFMEMBERS} \* ${NUMOFMEMBERS} \) + 1 `
elif [ "${CMD}" = "rejoin" ]; then
   EXPECTED=`expr \( ${NUMOFMEMBERS} \* ${NUMOFMEMBERS} \) + 1`
else
   EXPECTED=`expr ${NUMOFMEMBERS} \* ${NUMOFINSTANCES} + 1 `
fi
echo "Check for JOINED_AND_READY_EVENT over all logs. Expect ${EXPECTED},   Found: ${TMP}"



TMP=`grep "JOINED_AND_READY_EVENT for Member:"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN} | wc -l`
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
    TMP=`grep "JOINED_AND_READY_EVENT for Member:"  ${LOG} | grep -v ${APPLICATIONADMIN} | wc -l`
    echo "JoinAndReady in instance${num}: ${TMP}"
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
else
    TMP=`grep WARNING  ${ALLLOGS} | grep -v ${APPLICATIONADMIN} | wc -l`
fi
echo "Number of Warnings: ${TMP}"
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
else
    grep WARNING  ${ALLLOGS} | grep -v ${APPLICATIONADMIN}
fi
