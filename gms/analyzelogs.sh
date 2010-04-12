#!/bin/csh

set LOGS_DIR=LOGS/simulateCluster
set SERVERLOG=${LOGS_DIR}/server.log
set ALLLOGS=`ls ${LOGS_DIR}/*log | egrep "[server.log|instance*.log]"`

echo -n "Report for simulation " ; grep "Running using Shoal with transport" ${SERVERLOG}
echo
echo -n "Check for JOIN in DAS log. Expect 10.      Found:"  
grep "Adding Join member:"  ${SERVERLOG} | wc -l
echo
echo -n "Check for JOINED_AND_READY_EVENT in DAS log. Expect 11.  Found:" 
grep "JOINED_AND_READY_EVENT for Member:"  ${SERVERLOG} | wc -l
echo
echo -n "PlannedShutdownEvent in DAS log. Expect 10. Found:" 
grep "Received PlannedShutdownEvent"  ${SERVERLOG} | wc -l
echo
echo -n "Check for GroupLeadershipNotifications in DAS log. Expect 2 from server. Found:"
grep "adding GroupLeadershipNotification"  ${SERVERLOG} | wc -l
echo "Check for issues in any members sending a GroupLeadershipNotification to server for this scenario"
grep "adding GroupLeadershipNotification"  ${SERVERLOG} | grep -v server
echo
echo "*****************************************"
echo
echo -n "Check for Join members over all logs.  Expect 100.  Found: "
grep "Adding Join member:"  ${ALLLOGS} | wc -l
echo -n "Join in server    : " ; grep "Adding Join member:"  ${SERVERLOG} | wc -l
echo -n "Join in instance01: " ; grep "Adding Join member:"  ${LOGS_DIR}/instance01.log | wc -l
echo -n "Join in instance02: " ; grep "Adding Join member:"  ${LOGS_DIR}/instance02.log | wc -l
echo -n "Join in instance03: " ; grep "Adding Join member:"  ${LOGS_DIR}/instance03.log | wc -l
echo -n "Join in instance04: " ; grep "Adding Join member:"  ${LOGS_DIR}/instance04.log | wc -l
echo -n "Join in instance05: " ; grep "Adding Join member:"  ${LOGS_DIR}/instance05.log | wc -l
echo -n "Join in instance06: " ; grep "Adding Join member:"  ${LOGS_DIR}/instance06.log | wc -l
echo -n "Join in instance07: " ; grep "Adding Join member:"  ${LOGS_DIR}/instance07.log | wc -l
echo -n "Join in instance08: " ; grep "Adding Join member:"  ${LOGS_DIR}/instance08.log | wc -l
echo -n "Join in instance09: " ; grep "Adding Join member:"  ${LOGS_DIR}/instance09.log | wc -l
echo -n "Join in instance10: " ; grep "Adding Join member:"  ${LOGS_DIR}/instance10.log | wc -l
echo
echo -n "Check for JOINED_AND_READY_EVENT over all logs. Expect 111. Found:"
grep  "JOINED_AND_READY_EVENT for Member:"  ${ALLLOGS} | wc -l
echo -n "JoinAndReady in server    : " ; grep "JOINED_AND_READY_EVENT for Member:"  ${SERVERLOG} | wc -l
echo -n "JoinAndReady in instance01: " ; grep "JOINED_AND_READY_EVENT for Member:"  ${LOGS_DIR}/instance01.log | wc -l
echo -n "JoinAndReady in instance02: " ; grep "JOINED_AND_READY_EVENT for Member:"  ${LOGS_DIR}/instance02.log | wc -l
echo -n "JoinAndReady in instance03: " ; grep "JOINED_AND_READY_EVENT for Member:"  ${LOGS_DIR}/instance03.log | wc -l
echo -n "JoinAndReady in instance04: " ; grep "JOINED_AND_READY_EVENT for Member:"  ${LOGS_DIR}/instance04.log | wc -l
echo -n "JoinAndReady in instance05: " ; grep "JOINED_AND_READY_EVENT for Member:"  ${LOGS_DIR}/instance05.log | wc -l
echo -n "JoinAndReady in instance06: " ; grep "JOINED_AND_READY_EVENT for Member:"  ${LOGS_DIR}/instance06.log | wc -l
echo -n "JoinAndReady in instance07: " ; grep "JOINED_AND_READY_EVENT for Member:"  ${LOGS_DIR}/instance07.log | wc -l
echo -n "JoinAndReady in instance08: " ; grep "JOINED_AND_READY_EVENT for Member:"  ${LOGS_DIR}/instance08.log | wc -l
echo -n "JoinAndReady in instance09: " ; grep "JOINED_AND_READY_EVENT for Member:"  ${LOGS_DIR}/instance09.log | wc -l
echo -n "JoinAndReady in instance10: " ; grep "JOINED_AND_READY_EVENT for Member:"  ${LOGS_DIR}/instance10.log | wc -l
echo
echo "*****************************************"
echo -n "Number of Severe : " ; grep "|SEVERE|" ${ALLLOGS} | wc -l
echo -n "Number of Warnings:"  ; grep WARNING  ${ALLLOGS} | wc -l
echo
echo
echo SEVERE events
grep "|SEVERE|"  ${ALLLOGS}
echo WARNING events
grep WARNING  ${ALLLOGS}

