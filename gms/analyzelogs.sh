#!/bin/sh +x

usage () {
 echo "usage: [-h] [-c stop|kill|rejoin|default is normal)]  [numberOfMembers(10 is default)] "
 exit 1
}

NUMOFMEMBERS=10
CMD=normal

while [ $# -ne 0 ]
do
     case ${1} in
       -h)
         usage
       ;;
       -c)
         shift
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
       *)
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
     esac
done


if [ $# -gt 0 ]; then
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
fi

APPLICATIONADMIN=admincli
if [ "${CMD}" = "stop" ]; then
    LOGS_DIR=LOGS/simulateCluster_stop
elif [ "${CMD}" = "kill" ]; then
    LOGS_DIR=LOGS/simulateCluster_kill
elif [ "${CMD}" = "rejoin" ]; then
    LOGS_DIR=LOGS/simulateCluster_rejoin
else
    LOGS_DIR=LOGS/simulateCluster
fi
SERVERLOG=${LOGS_DIR}/server.log
ALLLOGS=`ls ${LOGS_DIR}/*log | egrep "[server.log|instance*.log]"`

echo "Report for simulation "
grep "Running using Shoal with transport" ${SERVERLOG}
echo

TMP=`grep "Adding Join member:"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN} | wc -l`
if [ "${CMD}" = "stop" ]; then
   EXPECTED=`expr ${NUMOFMEMBERS} + 1`
elif [ "${CMD}" = "kill" ]; then
   EXPECTED=`expr ${NUMOFMEMBERS} + 1`
elif [ "${CMD}" = "rejoin" ]; then
   EXPECTED=`expr ${NUMOFMEMBERS} + 1`
else
   EXPECTED=${NUMOFMEMBERS}
fi
echo "Check for JOIN in DAS log. Expect ${EXPECTED}.      Found: ${TMP}"

echo
TMP=`grep "JOINED_AND_READY_EVENT for Member:"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN} | wc -l`
if [ "${CMD}" = "stop" ]; then
   EXPECTED=`expr ${NUMOFMEMBERS} + 2`
elif [ "${CMD}" = "kill" ]; then
   EXPECTED=`expr ${NUMOFMEMBERS} + 2`
elif [ "${CMD}" = "rejoin" ]; then
   EXPECTED=`expr ${NUMOFMEMBERS} + 2`
else
   EXPECTED=`expr ${NUMOFMEMBERS} + 1`
fi
echo "Check for JOINED_AND_READY_EVENT in DAS log. Expect ${EXPECTED}.  Found: ${TMP}"

echo
TMP=`grep "Received PlannedShutdownEvent"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN} | wc -l`
if [ "${CMD}" = "stop" ]; then
   EXPECTED=`expr ${NUMOFMEMBERS} + 1`
elif [ "${CMD}" = "kill" ]; then
   EXPECTED=${NUMOFMEMBERS}
elif [ "${CMD}" = "rejoin" ]; then
   EXPECTED=${NUMOFMEMBERS}
else
   EXPECTED=${NUMOFMEMBERS}
fi
echo "PlannedShutdownEvent in DAS log. Expect ${EXPECTED}. Found: ${TMP}"

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
echo "Check for GroupLeadershipNotifications in DAS log. Expect ${EXPECTED} from server. Found: ${TMP}"

echo "Check for issues in any members sending a GroupLeadershipNotification to server for this scenario"
grep "adding GroupLeadershipNotification"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN} | grep -v server
echo
echo "*****************************************"
echo
TMP=`grep "Adding Join member:"  ${ALLLOGS} | grep -v ${APPLICATIONADMIN} | wc -l`
if [ "${CMD}" = "stop" ]; then
   EXPECTED=`expr \( ${NUMOFMEMBERS} \* ${NUMOFMEMBERS} \) + ${NUMOFMEMBERS} + 6`
elif [ "${CMD}" = "kill" ]; then
   EXPECTED=`expr \( ${NUMOFMEMBERS} \* ${NUMOFMEMBERS} \) + ${NUMOFMEMBERS} + 4`
elif [ "${CMD}" = "rejoin" ]; then
   EXPECTED=`expr \( ${NUMOFMEMBERS} \* ${NUMOFMEMBERS} \) + ${NUMOFMEMBERS} + 6`
else
   EXPECTED=`expr \( ${NUMOFMEMBERS} \* ${NUMOFMEMBERS} \) + ${NUMOFMEMBERS}`
fi
echo "Check for Join members over all logs.  Expect ${EXPECTED}.  Found: ${TMP}"

TMP=`grep "Adding Join member:"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN}  | wc -l`
echo "Join in server    : ${TMP}"
 
count=1
num=0
while [ $count -le ${NUMOFMEMBERS} ]
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
   EXPECTED=`expr \( ${NUMOFMEMBERS} \* ${NUMOFMEMBERS} \) + ${NUMOFMEMBERS} + 5`
elif [ "${CMD}" = "kill" ]; then
   EXPECTED=`expr \( ${NUMOFMEMBERS} \* ${NUMOFMEMBERS} \) + ${NUMOFMEMBERS} + 5`
elif [ "${CMD}" = "rejoin" ]; then
   EXPECTED=`expr \( ${NUMOFMEMBERS} \* ${NUMOFMEMBERS} \) + ${NUMOFMEMBERS} + 7`
else
   EXPECTED=`expr \( ${NUMOFMEMBERS} \* ${NUMOFMEMBERS} \) + ${NUMOFMEMBERS} + 1`
fi
echo "Check for JOINED_AND_READY_EVENT over all logs. Expect ${EXPECTED}. Found: ${TMP}"

TMP=`grep "JOINED_AND_READY_EVENT for Member:"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN} | wc -l`
echo "JoinAndReady in server    : ${TMP}"

count=1
num=0
while [ $count -le ${NUMOFMEMBERS} ]
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
echo "Number of Severe :  ${TMP}"

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
