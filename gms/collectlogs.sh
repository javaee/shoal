 TESTS="default add stop rejoin kill"
    TMP_SAVE_LOG_DIR=shoal_gms_results/LOGS
    GROUPNAME=samplesgroup
    EXECUTE_REMOTE_CONNECT=/usr/bin/ssh
# create the directory to save the logs i
    SERVER=configs/clusters/${GROUPNAME}/server.properties
    TMP=`egrep "^MACHINE_NAME" ${SERVER}`
    MACHINE_NAME=`echo $TMP | awk -F= '{print $2}' `
    COUNT=1
    ${EXECUTE_REMOTE_CONNECT} ${MACHINENAME} "mkdir -p ${TMP_SAVE_LOG_DIR}/${COUNT}"

    MEMBERS=`find configs/clusters/${GROUPNAME} -name "*.properties" `
    for member in ${MEMBERS}
    do
          TMP=`egrep "^MACHINE_NAME" ${member}`
          MACHINE_NAME=`echo $TMP | awk -F= '{print $2}' `
          TMP=`egrep "^WORKSPACE_HOME" ${member}`
          WORKSPACE_HOME=`echo $TMP | awk -F= '{print $2}' `
          ${EXECUTE_REMOTE_CONNECT} ${MACHINENAME}  "cd ${WORKSPACE_HOME}; chmod -R 777 LOGS/simulateCluster*; cp -r LOGS/simulateCluster* ${TMP_SAVE_LOG_DIR}/${COUNT}" &
    done
    echo "Waiting for all logs to be collected"
    wait
    echo "Done collecting the logs"
    echo "Analyzing collected the logs"
    for TEST in ${TESTS}
    do
       echo "processing TEST ${TEST}..."
       if [ "${TEST}" = "default" ];then
          RESULTFILE=${TMP_SAVE_LOG_DIR}/${COUNT}/simulateCluster_analyzelogs.results
       else
          RESULTFILE=${TMP_SAVE_LOG_DIR}/${COUNT}/simulateCluster_${TEST}_analyzelogs.results
       fi

       SERVER=configs/clusters/${GROUPNAME}/server.properties
       TMP=`egrep "^MACHINE_NAME" ${SERVER}`
       MACHINE_NAME=`echo $TMP | awk -F= '{print $2}' `
       ${EXECUTE_REMOTE_CONNECT} ${MACHINENAME} "cd ${WORKSPACE_HOME}; analyzelogs.sh -l ${TMP_SAVE_LOG_DIR}/${COUNT} ${TEST} > ${RESULTFILE}; chmod 777 ${RESULTFILE}"       

    done
