#!/bin/csh
echo -n "Report for simulation " ; grep "Running using Shoal with transport" server.log
echo
echo -n "Check for JOIN in DAS log. Expect 10.      Found:"  
grep "Adding Join member:" server.log | wc -l
echo
echo -n "Check for JOINED_AND_READY_EVENT in DAS log. Expect 11.  Found:" 
grep "JOINED_AND_READY_EVENT for Member:" server.log | wc -l
echo
echo -n "PlannedShutdownEvent in DAS log. Expect 10. Found:" 
grep "Received PlannedShutdownEvent" server.log | wc -l
echo
echo "*****************************************"
echo
echo -n "Check for Join members over all logs.  Expect less than 110. (not all instances start up at once on one machine)  Found: "
grep "Adding Join member:" *.log | wc -l
echo -n "Join in server    : " ; grep "Adding Join member:" server.log | wc -l
echo -n "Join in instance01: " ; grep "Adding Join member:" instance01.log | wc -l
echo -n "Join in instance02: " ; grep "Adding Join member:" instance02.log | wc -l
echo -n "Join in instance03: " ; grep "Adding Join member:" instance03.log | wc -l
echo -n "Join in instance04: " ; grep "Adding Join member:" instance04.log | wc -l
echo -n "Join in instance05: " ; grep "Adding Join member:" instance05.log | wc -l
echo -n "Join in instance06: " ; grep "Adding Join member:" instance06.log | wc -l
echo -n "Join in instance07: " ; grep "Adding Join member:" instance07.log | wc -l
echo -n "Join in instance08: " ; grep "Adding Join member:" instance08.log | wc -l
echo -n "Join in instance09: " ; grep "Adding Join member:" instance09.log | wc -l
echo -n "Join in instance10: " ; grep "Adding Join member:" instance10.log | wc -l
echo
echo -n "Check for JOINED_AND_READY_EVENT over all logs. Expect 111. Found:"
grep  "JOINED_AND_READY_EVENT for Member:" *.log | wc -l
echo -n "JoinAndReady in server    : " ; grep "JOINED_AND_READY_EVENT for Member:" server.log | wc -l
echo -n "JoinAndReady in instance01: " ; grep "JOINED_AND_READY_EVENT for Member:" instance01.log | wc -l
echo -n "JoinAndReady in instance02: " ; grep "JOINED_AND_READY_EVENT for Member:" instance02.log | wc -l
echo -n "JoinAndReady in instance03: " ; grep "JOINED_AND_READY_EVENT for Member:" instance03.log | wc -l
echo -n "JoinAndReady in instance04: " ; grep "JOINED_AND_READY_EVENT for Member:" instance04.log | wc -l
echo -n "JoinAndReady in instance05: " ; grep "JOINED_AND_READY_EVENT for Member:" instance05.log | wc -l
echo -n "JoinAndReady in instance06: " ; grep "JOINED_AND_READY_EVENT for Member:" instance06.log | wc -l
echo -n "JoinAndReady in instance07: " ; grep "JOINED_AND_READY_EVENT for Member:" instance07.log | wc -l
echo -n "JoinAndReady in instance08: " ; grep "JOINED_AND_READY_EVENT for Member:" instance08.log | wc -l
echo -n "JoinAndReady in instance09: " ; grep "JOINED_AND_READY_EVENT for Member:" instance09.log | wc -l
echo -n "JoinAndReady in instance10: " ; grep "JOINED_AND_READY_EVENT for Member:" instance10.log | wc -l
echo
echo "*****************************************"
echo -n "Number of Severe : " ; grep "|SEVERE|" *.log | wc -l 
echo -n "Number of Warnings:"  ; grep WARNING *.log | wc -l
echo
echo
echo SEVERE events
grep "|SEVERE|" *.log
echo WARNING events
grep WARNING *.log 

