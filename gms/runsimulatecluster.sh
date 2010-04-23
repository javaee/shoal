#!/bin/sh +x

TRANSPORT=grizzly
if [ ! -z "${1}" ]; then
    TRANSPORT=${1}
fi
echo Running with transport ${TRANSPORT}


LOGS_DIR=LOGS/simulateCluster

mkdir -p ${LOGS_DIR}
echo "Removing old logs"
rm -f ${LOGS_DIR}/server.log ${LOGS_DIR}/instance??.log ./currentMulticastAddress.txt


GROUPNAME=cluster
MULTICASTADDRESS=229.9.1.`./randomNumber.sh`
echo ${MULTICASTADDRESS} > ./currentMulticastAddress.txt
MULTICASTPORT=2299

TEST_LOG_LEVEL=WARNING
SHOALGMS_LOG_LEVEL=INFO

echo "Starting admin"
./rungmsdemo.sh server ${GROUPNAME} SPECTATOR 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9130 -te 9160 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/server.log 2>&1 &
sleep 5
echo "Starting CORE members"
./rungmsdemo.sh instance01 ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9161 -te 9190 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/instance01.log 2>&1 &
./rungmsdemo.sh instance02 ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9230 -te 9260 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/instance02.log 2>&1 &
./rungmsdemo.sh instance03 ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9261 -te 9290 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/instance03.log 2>&1 &
./rungmsdemo.sh instance04 ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9330 -te 9360 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/instance04.log 2>&1 &
./rungmsdemo.sh instance05 ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9361 -te 9390 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/instance05.log 2>&1 &
./rungmsdemo.sh instance06 ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9430 -te 9460 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/instance06.log 2>&1 &
./rungmsdemo.sh instance07 ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9461 -te 9490 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/instance07.log 2>&1 &
./rungmsdemo.sh instance08 ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9530 -te 9560 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/instance08.log 2>&1 &
./rungmsdemo.sh instance09 ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9561 -te 9590 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/instance09.log 2>&1 &
./rungmsdemo.sh instance10 ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9631 -te 9690 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/instance10.log 2>&1 &

echo "Waiting for group [${GROUPNAME}] to complete startup"
# we do not want test or shoal output unless we really needit, there we set both types of logging to the same value
./gms_admin.sh waits ${GROUPNAME} -tl ${TEST_LOG_LEVEL} -sl ${TEST_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT}

echo "Group startup has completed, shutting down group [${GROUPNAME}]"
# we do not want test or shoal output unless we really needit, there we set both types of logging to the same value
./gms_admin.sh stopc ${GROUPNAME} -tl ${TEST_LOG_LEVEL} -sl ${TEST_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT}


# The old way of running this test scenario
#./rungmsdemo.sh server cluster SPECTATOR 180000 INFO ${TRANSPORT} > ${LOGS_DIR}/server1.log 2>&1 &
#sleep 5
#./rungmsdemo.sh instance21 cluster CORE 120000 INFO ${TRANSPORT} 9130 9160 > ${LOGS_DIR}/instance21.log 2>&1 &
#./rungmsdemo.sh instance22 cluster CORE 120000 INFO ${TRANSPORT} 9160 9190 > ${LOGS_DIR}/instance22.log 2>&1 &
#./rungmsdemo.sh instance23 cluster CORE 120000 CONFIG ${TRANSPORT} 9230 9260 > ${LOGS_DIR}/instance23.log 2>&1 &
#./rungmsdemo.sh instance24 cluster CORE 120000 CONFIG ${TRANSPORT} 9261 9290 > ${LOGS_DIR}/instance24.log 2>&1 &
#./rungmsdemo.sh instance25 cluster CORE 120000 CONFIG ${TRANSPORT} 9330 9360 > ${LOGS_DIR}/instance25.log 2>&1 &
#./rungmsdemo.sh instance26 cluster CORE 120000 CONFIG ${TRANSPORT} 9361 9390 > ${LOGS_DIR}/instance26.log 2>&1 &
#./rungmsdemo.sh instance27 cluster CORE 120000 CONFIG ${TRANSPORT} 9430 9460 > ${LOGS_DIR}/instance27.log 2>&1 &
#./rungmsdemo.sh instance28 cluster CORE 120000 CONFIG ${TRANSPORT} 9461 9490 > ${LOGS_DIR}/instance28.log 2>&1 &
#./rungmsdemo.sh instance29 cluster CORE 120000 CONFIG ${TRANSPORT} 9530 9560 > ${LOGS_DIR}/instance29.log 2>&1 &
#./rungmsdemo.sh instance30 cluster CORE 120000 CONFIG ${TRANSPORT} 9561 9590 > ${LOGS_DIR}/instance30.log 2>&1 &


