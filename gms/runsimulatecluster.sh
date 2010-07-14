#!/bin/sh +x

EXECUTE_REMOTE_CONNECT=rsh

ADMINCLI_LOG_LEVEL=WARNING
ADMINCLI_SHOALGMS_LOG_LEVEL=WARNING

TEST_LOG_LEVEL=INFO
SHOALGMS_LOG_LEVEL=CONFIG

CLUSTER_CONFIGS="./configs/clusters"
if [ -f "./configs/clusters" ]; then
   echo "ERROR: the configs/clusters directory is missing"
   exit 1
fi
TRANSPORT=grizzly
CMD=normal
NUMOFMEMBERS=10

TCPSTARTPORT=9121
TCPENDPORT=9160
GROUPNAME=testgroup
MULTICASTADDRESS=229.9.1.`./randomNumber.sh`
MULTICASTPORT=2299
BINDINTERFACEADDRESS=""

DIST=false

usage () {
 echo "usage:"
 echo "   single machine:"
 echo "      [-h] [-t grizzly|jxta] [stop|kill|rejoin|default is normal)] [numberOfMembers(10 is default)] "
 echo "   distributed environment:"
 echo "      -d <-g groupname> [-t grizzly|jxta] [stop|kill|rejoin|default is normal)]"
 echo " "
 echo " Examples:"
 echo "     runsimulatecluster.sh"
 echo "     runsimulatecluster.sh 5 rejoin"
 echo "     runsimulatecluster.sh -d -g testgroup"
 echo "     runsimulatecluster.sh -d -g testgroup rejoin"
 exit 1
}


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
       -d)
         shift
         DIST=true
       ;;
       -g)
         shift
         GROUPNAME=${1}
         shift
         if [ -z "${GROUPNAME}" ] ;then
            echo "ERROR: Missing group name value"
            usage
         fi
       ;;
       -t)
         shift
         TRANSPORT=${1}
         shift
         if [ ! -z "${TRANSPORT}" ] ;then
            if [ "${TRANSPORT}" != "grizzly" -a "${TRANSPORT}" != "jxta" ]; then
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

echo ${MULTICASTADDRESS} > ./currentMulticastAddress.txt

echo Comand: ${CMD}
echo Transport: ${TRANSPORT}

INSTANCE_EFFECTED=instance00
if [ $DIST = true ]; then
    NUMOFMEMBERS=`find ${CLUSTER_CONFIGS}/${GROUPNAME} -name "*.properties" | grep -v server.properties | sort | wc -w`
fi
echo NumberOfMembers: ${NUMOFMEMBERS}

if [ ${NUMOFMEMBERS} -gt 1 ]; then
       # INSTANCE_EFFECTED=`./randomNumber.sh ${NUMOFMEMBERS}`
        INSTANCE_EFFECTED=`expr ${NUMOFMEMBERS} / 2 `
        if [  ${INSTANCE_EFFECTED} -lt 10 ]; then
            INSTANCE_EFFECTED="instance0${INSTANCE_EFFECTED}"
        else
            INSTANCE_EFFECTED="instance${INSTANCE_EFFECTED}"
        fi
        echo Instance Effected: ${INSTANCE_EFFECTED}
else
        echo "ERROR: The number of members specified [${NUMOFMEMBERS}] must be greater and 1 for command [${CMD}]"
        usage
fi


if [ "${CMD}" = "normal" ]; then
   LOGS_DIR=LOGS/simulateCluster
else
   LOGS_DIR=LOGS/simulateCluster_${CMD}
fi

echo "LOGS_DIRS=${LOGS_DIR}"

if [ ! -z ${BINDINGINTERFACEADDRESS} ]; then
    BIA="-bia ${BINDINGINTERFACEADDRESS}"
else
    BIA=""
fi
#--------------------------
# STARTING OF THE MASTER
if [ $DIST = false ]; then
    mkdir -p ${LOGS_DIR}
    echo "Removing old logs"
    rm -f ${LOGS_DIR}/*.log
    echo "Starting server"
    ./rungmsdemo.sh server ${GROUPNAME} SPECTATOR 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts ${TCPSTARTPORT} -te ${TCPENDPORT} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} -l ${LOGS_DIR} ${BIA}
else
    if [ -f ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties ]; then
       TMP=`egrep "^MACHINE_NAME" ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties`
       MACHINE_NAME=`echo $TMP | awk -F= '{print $2}' `
       TMP=`egrep "^WORKSPACE_HOME" ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties`
       WORKSPACE_HOME=`echo $TMP | awk -F= '{print $2}' `
       TMP=`egrep "^TCPSTARTPORT" ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties`
       if [ ! -z ${TMP} ]; then
           TSA="-ts `echo $TMP | awk -F= '{print $2}' ` "
       else
           TSA=""
       fi
       TMP=`egrep "^TCPENDPORT" ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties`
       if [ ! -z ${TMP} ]; then
           TEA="-te `echo $TMP | awk -F= '{print $2}' ` "
       else
           TEA=""
       fi
       TMP=`egrep "^BIND_INTERFACE_ADDRESS" ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties`
       if [ ! -z ${TMP} ]; then
           BIA="-bia `echo $TMP | awk -F= '{print $2}' ` "
       else
           BIA=""
       fi
       echo "Starting server on ${MACHINE_NAME}"
       ${EXECUTE_REMOTE_CONNECT} ${MACHINE_NAME} "cd ${WORKSPACE_HOME};killmembers.sh; rm -rf ${LOGS_DIR}/server.log; mkdir -p ${LOGS_DIR}; ${WORKSPACE_HOME}/rungmsdemo.sh server ${GROUPNAME} SPECTATOR 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} ${TSA} ${TEA} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} -l ${WORKSPACE_HOME}/${LOGS_DIR} ${BIA}"
    else
       echo "ERROR: Could not find ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties"
       exit 1
    fi
fi

#--------------------------
# Give time for the master to startup before starting the other members
sleep 5

SDTCP=`expr ${TCPENDPORT} + 1`
EDTCP=`expr ${SDTCP} + 30`
if [ ! -z ${BINDINGINTERFACEADDRESS} ]; then
    BIA="-bia ${BINDINGINTERFACEADDRESS}"
else
    BIA=""
fi
if [ $DIST = false ]; then
    # single machine startup
    echo "Starting ${NUMOFMEMBERS} CORE members"
    count=1
    while [ $count -le ${NUMOFMEMBERS} ]
    do
        if [  ${count} -lt 10 ]; then
           INSTANCE_NAME="instance0${count}"
        else
           INSTANCE_NAME=instance${count}
        fi
        echo "Starting ${INSTANCE_NAME}"
        MEMBERSTARTCMD="./rungmsdemo.sh ${INSTANCE_NAME} ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts ${SDTCP} -te ${EDTCP} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} -l ${LOGS_DIR} ${BIA}"
        ${MEMBERSTARTCMD}

        if [ ${INSTANCE_NAME} = ${INSTANCE_EFFECTED} ]; then
           EFFECTED_MEMBERSTARTCMD=${MEMBERSTARTCMD}
        fi

        SDTCP=`expr ${EDTCP} + 1`
        EDTCP=`expr ${SDTCP} + 30`
        count=`expr ${count} + 1`
    done
else
   # distributed environment startup
    echo "Starting CORE members in the distributed environment"

   MEMBERS=`find ${CLUSTER_CONFIGS}/${GROUPNAME} -name "*.properties" | grep -v server.properties  `
   for member in ${MEMBERS}
   do
      TMP=`egrep "^MACHINE_NAME" ${member}`
      MACHINE_NAME=`echo $TMP | awk -F= '{print $2}' `
      TMP=`egrep "^INSTANCE_NAME" ${member}`
      INSTANCE_NAME=`echo $TMP | awk -F= '{print $2}' `
      TMP=`egrep "^WORKSPACE_HOME" ${member}`
      WORKSPACE_HOME=`echo $TMP | awk -F= '{print $2}' `
      TMP=`egrep "^TCPSTARTPORT" ${member}`
      if [ ! -z ${TMP} ]; then
           SDTCP=`echo $TMP | awk -F= '{print $2}' `
      fi
      TMP=`egrep "^TCPENDPORT" ${member}`
      if [ ! -z ${TMP} ]; then
           EDTCP=`echo $TMP | awk -F= '{print $2}' `
      fi
      TMP=`egrep "^BIND_INTERFACE_ADDRESS" ${member}`
      if [ ! -z ${TMP} ]; then
           BIA="-bia `echo $TMP | awk -F= '{print $2}' ` "
      fi
      echo "Starting ${INSTANCE_NAME} on ${MACHINE_NAME}"
      MEMBERSTARTCMD="./rungmsdemo.sh $INSTANCE_NAME ${GROUPNAME} CORE 0 ${SHOALGMS_LOG_LEVEL} ${TRANSPORT} -tl ${TEST_LOG_LEVEL} -ts ${SDTCP} -te ${EDTCP} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} -l ${WORKSPACE_HOME}/${LOGS_DIR} ${BIA}"
      ${EXECUTE_REMOTE_CONNECT} ${MACHINE_NAME} "cd ${WORKSPACE_HOME};killmembers.sh; rm -rf ${LOGS_DIR}/$INSTANCE_NAME.log; mkdir -p ${LOGS_DIR}; ${MEMBERSTARTCMD}"
      if [ ${INSTANCE_NAME} = ${INSTANCE_EFFECTED} ]; then
           EFFECTED_MEMBERSTARTCMD=${MEMBERSTARTCMD}
           EFFECTED_MEMBER_MACHINE_NAME=${MACHINE_NAME}
           EFFECTED_MEMBER_WORKSPACE_HOME=${WORKSPACE_HOME}
      fi
   done
   TMP=`egrep "^MACHINE_NAME" ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties`
   MASTER_MACHINE_NAME=`echo $TMP | awk -F= '{print $2}' `
   TMP=`egrep "^WORKSPACE_HOME" ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties`
   MASTER_WORKSPACE_HOME=`echo $TMP | awk -F= '{print $2}' `
fi


if [ $DIST = false ]; then
    if [ ! -z ${BINDINGINTERFACEADDRESS} ]; then
         BIA="-bia ${BINDINGINTERFACEADDRESS}"
    else
        BIA=""
    fi
else
    if [ -f ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties ]; then
       TMP=`egrep "^BIND_INTERFACE_ADDRESS" ${CLUSTER_CONFIGS}/${GROUPNAME}/server.properties`
       if [ ! -z ${TMP} ]; then
           BIA="-bia `echo $TMP | awk -F= '{print $2}' ` "
       else
           BIA=""
       fi
    else
       BIA=""
    fi
fi

echo "Waiting for group [${GROUPNAME}] to complete startup"
# we do not want test or shoal output unless we really needit, there we set both types of logging to the same value
ADMINCMD="./gms_admin.sh waits ${GROUPNAME} -t ${TRANSPORT} -tl ${ADMINCLI_LOG_LEVEL} -sl ${ADMINCLI_SHOALGMS_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} ${BIA}"
if [ $DIST = false ]; then
    ${ADMINCMD}
else
    ${EXECUTE_REMOTE_CONNECT} ${MASTER_MACHINE_NAME} "cd ${MASTER_WORKSPACE_HOME}; ${ADMINCMD}"
fi

echo "Group startup has completed"

if [ "${CMD}" = "stop" ]; then
       echo "Stopping ${INSTANCE_EFFECTED}"
       ADMINCMD="./gms_admin.sh stopm ${GROUPNAME} ${INSTANCE_EFFECTED} -resend -t ${TRANSPORT} -tl ${ADMINCLI_LOG_LEVEL} -sl ${ADMINCLI_SHOALGMS_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} ${BIA}"

       if [ $DIST = false ]; then
           ${ADMINCMD}
       else
           ${EXECUTE_REMOTE_CONNECT} ${MASTER_MACHINE_NAME} "cd ${MASTER_WORKSPACE_HOME}; ${ADMINCMD}"
       fi
       echo "sleeping 15 seconds"
       sleep 15
       echo "Restarting ${INSTANCE_EFFECTED}"
       if [ $DIST = false ]; then
           ${EFFECTED_MEMBERSTARTCMD}
       else
           ${EXECUTE_REMOTE_CONNECT} ${EFFECTED_MEMBER_MACHINE_NAME} "cd ${EFFECTED_MEMBER_WORKSPACE_HOME}; ${EFFECTED_MEMBERSTARTCMD}"
       fi
       count=1
       CMD_OK=false
       while [ true ]
       do
         ADMINCMD="./gms_admin.sh list ${GROUPNAME} ${INSTANCE_EFFECTED} -t ${TRANSPORT} -tl ${ADMINCLI_LOG_LEVEL} -sl ${ADMINCLI_SHOALGMS_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} ${BIA}"
         if [ $DIST = false ]; then
             TMP=`${ADMINCMD}`
         else
             TMP=`${EXECUTE_REMOTE_CONNECT} ${MASTER_MACHINE_NAME} "cd ${MASTER_WORKSPACE_HOME}; ${ADMINCMD}" `
         fi
         #echo $TMP
         _TMP=`echo ${TMP} | grep "WAS SUCCESSFUL"`
         if [ ! -z "${_TMP}" ];then
            CMD_OK=true
            break;
         fi
         count=`expr ${count} + 1`
         if [ ${count} -gt 10 ]; then
            break
         fi
         sleep 1
       done
       if [ ${CMD_OK} = true ]; then
            echo "Instance ${INSTANCE_EFFECTED} has restarted"
       else
            echo "ERROR: Instance ${INSTANCE_EFFECTED} DID NOT restarted"
       fi
elif [ "${CMD}" = "kill" ]; then
       echo "Killing ${INSTANCE_EFFECTED}"
       ADMINCMD="./gms_admin.sh killm ${GROUPNAME} ${INSTANCE_EFFECTED} -t ${TRANSPORT} -tl ${ADMINCLI_LOG_LEVEL} -sl ${ADMINCLI_SHOALGMS_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} ${BIA}"
       if [ $DIST = false ]; then
           ${ADMINCMD}
       else
           ${EXECUTE_REMOTE_CONNECT} ${MASTER_MACHINE_NAME} "cd ${MASTER_WORKSPACE_HOME}; ${ADMINCMD}"
       fi
       echo "sleeping 15 seconds"
       sleep 15
       echo "Restarting ${INSTANCE_EFFECTED}"
       if [ $DIST = false ]; then
           ${EFFECTED_MEMBERSTARTCMD}
       else
           ${EXECUTE_REMOTE_CONNECT} ${EFFECTED_MEMBER_MACHINE_NAME} "cd ${EFFECTED_MEMBER_WORKSPACE_HOME}; ${EFFECTED_MEMBERSTARTCMD}"
       fi
       count=1
       CMD_OK=false
       while [ true ]
       do
         ADMINCMD="./gms_admin.sh list ${GROUPNAME} ${INSTANCE_EFFECTED} -t ${TRANSPORT} -tl ${ADMINCLI_LOG_LEVEL} -sl ${ADMINCLI_SHOALGMS_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} ${BIA}"
         if [ $DIST = false ]; then
             TMP=`${ADMINCMD}`
         else
             TMP=`${EXECUTE_REMOTE_CONNECT} ${MASTER_MACHINE_NAME} "cd ${MASTER_WORKSPACE_HOME}; ${ADMINCMD}" `
         fi
         #echo $TMP
         _TMP=`echo ${TMP} | grep "WAS SUCCESSFUL"`
         if [ ! -z "${_TMP}" ];then
            CMD_OK=true
            break;
         fi
         count=`expr ${count} + 1`
         if [ ${count} -gt 10 ]; then
            break
         fi
         sleep 1
       done
       if [ ${CMD_OK} = true ]; then
            echo "Instance ${INSTANCE_EFFECTED} has restarted"
       else
            echo "ERROR: Instance ${INSTANCE_EFFECTED} DID NOT restarted"
       fi
elif [ "${CMD}" = "rejoin" ]; then
       echo "Killing ${INSTANCE_EFFECTED}"
       ADMINCMD="./gms_admin.sh killm ${GROUPNAME} ${INSTANCE_EFFECTED} -t ${TRANSPORT} -tl ${ADMINCLI_LOG_LEVEL} -sl ${ADMINCLI_SHOALGMS_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} ${BIA}"
       if [ $DIST = false ]; then
           ${ADMINCMD}
       else
           ${EXECUTE_REMOTE_CONNECT} ${MASTER_MACHINE_NAME} "cd ${MASTER_WORKSPACE_HOME}; ${ADMINCMD}"
       fi
       echo "Restarting ${INSTANCE_EFFECTED}"
       if [ $DIST = false ]; then
           ${EFFECTED_MEMBERSTARTCMD}
       else
           ${EXECUTE_REMOTE_CONNECT} ${EFFECTED_MEMBER_MACHINE_NAME} "cd ${EFFECTED_MEMBER_WORKSPACE_HOME}; ${EFFECTED_MEMBERSTARTCMD}"
       fi
       count=1
       CMD_OK=false
       while [ true ]
       do
         ADMINCMD="./gms_admin.sh list ${GROUPNAME} ${INSTANCE_EFFECTED} -t ${TRANSPORT} -tl ${ADMINCLI_LOG_LEVEL} -sl ${ADMINCLI_SHOALGMS_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} ${BIA}"
         if [ $DIST = false ]; then
             TMP=`${ADMINCMD}`
         else
             TMP=`${EXECUTE_REMOTE_CONNECT} ${MASTER_MACHINE_NAME} "cd ${MASTER_WORKSPACE_HOME}; ${ADMINCMD}" `
         fi
         _TMP=`echo ${TMP} | grep "WAS SUCCESSFUL"`
         if [ ! -z "${_TMP}" ];then
            CMD_OK=true
            break;
         fi
         count=`expr ${count} + 1`
         if [ ${count} -gt 10 ]; then
            break
         fi
         sleep 1
       done
       if [ ${CMD_OK} = true ]; then
            echo "Instance ${INSTANCE_EFFECTED} has restarted"
       else
            echo "ERROR: Instance ${INSTANCE_EFFECTED} DID NOT restarted"
       fi
       # do a quick little sleep just to make sure everything gets started
       # since everyone might not notice the instance went down and up quickly
       sleep 5
fi

echo "Shutting down group [${GROUPNAME}]"
   # we do not want test or shoal output unless we really needit, there we set both types of logging to the same value
ADMINCMD="./gms_admin.sh stopc ${GROUPNAME} -t ${TRANSPORT} -tl ${ADMINCLI_LOG_LEVEL} -sl ${ADMINCLI_SHOALGMS_LOG_LEVEL} -ma ${MULTICASTADDRESS} -mp ${MULTICASTPORT} ${BIA}"
if [ $DIST = false ]; then
    ${ADMINCMD}
else
    ${EXECUTE_REMOTE_CONNECT} ${MASTER_MACHINE_NAME} "cd ${MASTER_WORKSPACE_HOME}; ${ADMINCMD}"
fi

