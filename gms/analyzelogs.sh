#!/bin/sh +x

usage () {
 echo "usage: [-h] [numberOfMembers(10 is default)] "
 exit 1
}

NUMOFMEMBERS=10
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
LOGS_DIR=LOGS/simulateCluster
SERVERLOG=${LOGS_DIR}/server.log
ALLLOGS=`ls ${LOGS_DIR}/*log | egrep "[server.log|instance*.log]"`

echo "Report for simulation "
grep "Running using Shoal with transport" ${SERVERLOG}
echo

TMP=`grep "Adding Join member:"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN} | wc -l`
echo "Check for JOIN in DAS log. Expect ${NUMOFMEMBERS}.      Found: ${TMP}"
echo
JARE_DAS=`expr ${NUMOFMEMBERS} + 1`
TMP=`grep "JOINED_AND_READY_EVENT for Member:"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN} | wc -l`
echo "Check for JOINED_AND_READY_EVENT in DAS log. Expect ${JARE_DAS}.  Found: ${TMP}"
echo
TMP=`grep "Received PlannedShutdownEvent"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN} | wc -l`
echo "PlannedShutdownEvent in DAS log. Expect ${NUMOFMEMBERS}. Found: ${TMP}"

echo
TMP=`grep "adding GroupLeadershipNotification"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN} | wc -l`
echo "Check for GroupLeadershipNotifications in DAS log. Expect 2 from server. Found: ${TMP}"

echo "Check for issues in any members sending a GroupLeadershipNotification to server for this scenario"
grep "adding GroupLeadershipNotification"  ${SERVERLOG} | grep -v ${APPLICATIONADMIN} | grep -v server
echo
echo "*****************************************"
echo
TMP=`grep "Adding Join member:"  ${ALLLOGS} | grep -v ${APPLICATIONADMIN} | wc -l`
JOIN_IN_ALLLOGS=`expr \( ${NUMOFMEMBERS} \* ${NUMOFMEMBERS} \) + ${NUMOFMEMBERS}`
echo "Check for Join members over all logs.  Expect ${JOIN_IN_ALLLOGS}.  Found: ${TMP}"

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
JARE_ALL_LOGS=`expr \( ${NUMOFMEMBERS} \* ${NUMOFMEMBERS} \) + ${NUMOFMEMBERS} + 1`

#echo -n "Check for JOINED_AND_READY_EVENT over all logs. Expect 111. Found:"
TMP=`grep "JOINED_AND_READY_EVENT for Member:"  ${ALLLOGS} | grep -v ${APPLICATIONADMIN} | wc -l`
echo "Check for JOINED_AND_READY_EVENT over all logs. Expect ${JARE_ALL_LOGS}. Found: ${TMP}"

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

TMP=`grep WARNING ${ALLLOGS} | grep -v ${APPLICATIONADMIN} | wc -l`
echo "Number of Warnings: ${TMP}"
echo
echo
echo SEVERE events
grep "|SEVERE|"  ${ALLLOGS} | grep -v ${APPLICATIONADMIN}
echo WARNING events
grep WARNING  ${ALLLOGS} | grep -v ${APPLICATIONADMIN}

