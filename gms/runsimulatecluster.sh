#!/bin/sh +x

ADMINCLI_LOG_LEVEL=WARNING
ADMINCLI_SHOALGMS_LOG_LEVEL=WARNING

TEST_LOG_LEVEL=INFO
SHOALGMS_LOG_LEVEL=INFO

TRANSPORT=grizzly
CMD=normal
NUMOFMEMBERS=10

GROUPNAME=cluster
MULTICASTADDRESS=229.9.1.`./randomNumber.sh`
echo ${MULTICASTADDRESS} > ./currentMulticastAddress.txt
MULTICASTPORT=2299



usage () {
 echo "usage: [-h] [-t grizzly|jxta|jxtanew] [-c stop|kill|rejoin|default is normal)] [numberOfMembers(10 is default)] "
 exit 1
}


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
echo Comand: ${CMD}
echo Transport: ${TRANSPORT}
echo NumberOfMembers: ${NUMOFMEMBERS}
INSTANCE_EFFECTED=-1
if [ "${CMD}" != "normal" ]; then
   if [ ${NUMOFMEMBERS} -gt 1 ]; then
       # INSTANCE_EFFECTED=`./randomNumber.sh ${NUMOFMEMBERS}`
        INSTANCE_EFFECTED=`expr ${NUMOFMEMBERS} / 2 `
        if [  ${INSTANCE_EFFECTED} -lt 10 ]; then
            INSTANCE_EFFECTED="0${INSTANCE_EFFECTED}"
        fi
        echo Instance Effected: ${INSTANCE_EFFECTED}
    else
        echo "ERROR: The number of members specified [${NUMOFMEMBERS}] must be greater and 1 for command [${CMD}]"
        usage
    fi
fi

if [ "${CMD}" = "normal" ]; then
   LOGS_DIR=LOGS/simulateCluster
else
   LOGS_DIR=LOGS/simulateCluster_${CMD}
fi

echo "LOGS_DIRS=${LOGS_DIR}"
mkdir -p ${LOGS_DIR}
echo "Removing old logs"
rm -f ${LOGS_DIR}/server.log ${LOGS_DIR}/instance*.log ./currentMulticastAddress.txt




echo "Starting admin"
./rungmsdemo.sh server ${GROUPNAME} SPECTATOR 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9130 -te 9160 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/server.log 2>&1 &
sleep 5

echo "Starting ${NUMOFMEMBERS} CORE members"

sdtcp=9161
edtcp=`expr ${sdtcp} + 30`
count=1
while [ $count -le ${NUMOFMEMBERS} ]
do
    if [  ${count} -lt 10 ]; then
       NUM="0${count}"
    else
       NUM=${count}
    fi
    _EXECCMD="./rungmsdemo.sh instance${NUM} ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts ${sdtcp} -te ${edtcp} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT}"
    ${_EXECCMD} > ${LOGS_DIR}/instance${NUM}.log 2>&1 &

    if [ ${NUM} = ${INSTANCE_EFFECTED} ]; then
       INSTANCE_EFFECTED_EXECCMD=${_EXECCMD}
    fi

    sdtcp=`expr ${edtcp} + 1`
    edtcp=`expr ${sdtcp} + 30`
    count=`expr ${count} + 1`
done
# replaced the following with the while code above
#./rungmsdemo.sh instance01 ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9161 -te 9190 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/instance01.log 2>&1 &
#./rungmsdemo.sh instance02 ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9230 -te 9260 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/instance02.log 2>&1 &
#./rungmsdemo.sh instance03 ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9261 -te 9290 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/instance03.log 2>&1 &
#./rungmsdemo.sh instance04 ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9330 -te 9360 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/instance04.log 2>&1 &
#./rungmsdemo.sh instance05 ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9361 -te 9390 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/instance05.log 2>&1 &
#./rungmsdemo.sh instance06 ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9430 -te 9460 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/instance06.log 2>&1 &
#./rungmsdemo.sh instance07 ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9461 -te 9490 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/instance07.log 2>&1 &
#./rungmsdemo.sh instance08 ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9530 -te 9560 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/instance08.log 2>&1 &
#./rungmsdemo.sh instance09 ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9561 -te 9590 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/instance09.log 2>&1 &
#./rungmsdemo.sh instance10 ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts 9631 -te 9690 -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} > ${LOGS_DIR}/instance10.log 2>&1 &


echo "Waiting for group [${GROUPNAME}] to complete startup"
# we do not want test or shoal output unless we really needit, there we set both types of logging to the same value
./gms_admin.sh waits ${GROUPNAME} -tl ${ADMINCLI_LOG_LEVEL} -sl ${ADMINCLI_SHOALGMS_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT}

echo "Group startup has completed"

if [ "${CMD}" = "stop" ]; then
    echo "Stopping instance${INSTANCE_EFFECTED}"
    ./gms_admin.sh stopm ${GROUPNAME} instance${INSTANCE_EFFECTED} -resend -tl ${ADMINCLI_LOG_LEVEL} -sl ${ADMINCLI_SHOALGMS_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT}
    echo "sleeping 20 seconds"
    sleep 20
    echo "Restarting instance${INSTANCE_EFFECTED}"
    ${INSTANCE_EFFECTED_EXECCMD} >> ${LOGS_DIR}/instance${INSTANCE_EFFECTED}.log 2>&1 &
    while [ 1 ]
    do
      TMP=`./gms_admin.sh list ${GROUPNAME} instance${INSTANCE_EFFECTED} -tl ${ADMINCLI_LOG_LEVEL} -sl ${ADMINCLI_SHOALGMS_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT}`
      echo $TMP
      if [ ! -z "${TMP}" ];then
         echo "Instanced instance${INSTANCE_EFFECTED} has restarted"
         break;
      fi
      sleep 1
    done
elif [ "${CMD}" = "kill" ]; then
    echo "Killing instance${INSTANCE_EFFECTED}"
    ./gms_admin.sh killm ${GROUPNAME} instance${INSTANCE_EFFECTED} -tl ${ADMINCLI_LOG_LEVEL} -sl ${ADMINCLI_SHOALGMS_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT}
    echo "sleeping 20 seconds"
    sleep 20
elif [ "${CMD}" = "rejoin" ]; then
    echo "Killing instance${INSTANCE_EFFECTED}"
    ./gms_admin.sh killm ${GROUPNAME} instance${INSTANCE_EFFECTED} -tl ${ADMINCLI_LOG_LEVEL} -sl ${ADMINCLI_SHOALGMS_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT}
    echo "Restarting instance${INSTANCE_EFFECTED}"
    ${INSTANCE_EFFECTED_EXECCMD} >> ${LOGS_DIR}/instance${INSTANCE_EFFECTED}.log 2>&1 &
    while [ 1 ]
    do
      TMP=`./gms_admin.sh list ${GROUPNAME} instance${INSTANCE_EFFECTED} -tl ${ADMINCLI_LOG_LEVEL} -sl ${ADMINCLI_SHOALGMS_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT}`
      if [ ! -z "${TMP}" ];then
         echo "Instanced instance${INSTANCE_EFFECTED} has restarted"
         break;
      fi
      sleep 1
    done
fi

echo "Shutting down group [${GROUPNAME}]"
# we do not want test or shoal output unless we really needit, there we set both types of logging to the same value
./gms_admin.sh stopc ${GROUPNAME} -tl ${ADMINCLI_LOG_LEVEL} -sl ${ADMINCLI_SHOALGMS_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT}


# The old way of running this test scenario
#./rungmsdemo.sh server cluster SPECTATOR 180000 INFO ${TRANSPORT} > ${LOGS_DIR}/server1.log 2>&1 &
# sleep 5
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


