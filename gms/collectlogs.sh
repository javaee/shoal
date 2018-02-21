#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
#
# The contents of this file are subject to the terms of either the GNU
# General Public License Version 2 only ("GPL") or the Common Development
# and Distribution License("CDDL") (collectively, the "License").  You
# may not use this file except in compliance with the License.  You can
# obtain a copy of the License at
# https://oss.oracle.com/licenses/CDDL+GPL-1.1
# or LICENSE.txt.  See the License for the specific
# language governing permissions and limitations under the License.
#
# When distributing the software, include this License Header Notice in each
# file and include the License file at LICENSE.txt.
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
