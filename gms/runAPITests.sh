#!/bin/sh +x
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
#
# The contents of this file are subject to the terms of either the GNU
# General Public License Version 2 only ("GPL") or the Common Development
# and Distribution License("CDDL") (collectively, the "License").  You
# may not use this file except in compliance with the License.  You can
# obtain a copy of the License at
# https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
# or packager/legal/LICENSE.txt.  See the License for the specific
# language governing permissions and limitations under the License.
#
# When distributing the software, include this License Header Notice in each
# file and include the License file at packager/legal/LICENSE.txt.
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
#-------------------------
#
# REVIEW THESE ENTRIES
#
#
# This needs to ne a complete path not a relative path to the shoal workspace
SHOALWORKSPACE=`pwd`

#PROVIDER="jxta"
PROVIDER="grizzly"

#
# if members are distributed across machines, the groupName
# must be set to a unique value so that all members join the
# same group
#
groupName="TestGroup_`uname -n`"
if [ "${groupName}" = "" ]; then
    groupName="TestGroup"
fi

#
#
#
#-------------------------
TMPDIR=$SHOALWORKSPACE/tmp
if [ ! -d ${TMPDIR} ]; then
    mkdir -p ${TMPDIR}
fi
rm -rf ${TMPDIR}/grouphandle.sh
rm -rf ${TMPDIR}/groupmanagementservice.sh


#########################################
# Create the scripts used to run the test
#########################################

#===============================================
# Create the scripts that actually runs the test
#===============================================
cat << ENDSCRIPT > ${TMPDIR}/grouphandle.sh
#!/bin/sh +x

ECHO=\`which echo\`

publish_home=$SHOALWORKSPACE/dist
lib_home=$SHOALWORKSPACE/lib
\$ECHO "Arg1=\${1}"
\$ECHO "Arg2=\${2}"
\$ECHO "Arg3=\${3}"
\$ECHO "Arg4=\${4}"
\$ECHO "Arg5=\${5}"
\$ECHO "Arg6=\${6}"
\$ECHO "Arg7=\${7}"
\$ECHO "Arg8=\${8}"
\$ECHO "Starting \${1}"

if [ "\${1}" = "master" ] ; then
    java -Dcom.sun.management.jmxremote -DSHOAL_GROUP_COMMUNICATION_PROVIDER=${PROVIDER} -DTCPSTARTPORT=\$5 -DTCPENDPORT=\$6 -DLOG_LEVEL=\$7 -cp \${publish_home}/shoal-gms-tests.jar:\${publish_home}/shoal-gms.jar:\${lib_home}/bcprov-jdk14.jar:\${lib_home}/grizzly-framework.jar:\${lib_home}/grizzly-utils.jar:\${lib_home}/jxta.jar com.sun.enterprise.ee.cms.tests.core.GroupHandleTest \$1 \$2 \$3 \$4
else
    java -Dcom.sun.management.jmxremote -DSHOAL_GROUP_COMMUNICATION_PROVIDER=${PROVIDER} -DTCPSTARTPORT=\$4 -DTCPENDPORT=\$5 -DLOG_LEVEL=\$6 -cp \${publish_home}/shoal-gms-tests.jar:\${publish_home}/shoal-gms.jar:\${lib_home}/bcprov-jdk14.jar:\${lib_home}/grizzly-framework.jar:\${lib_home}/grizzly-utils.jar:\${lib_home}/jxta.jar com.sun.enterprise.ee.cms.tests.core.GroupHandleTest \$1 \$2 \$3
fi
ENDSCRIPT

cat << ENDSCRIPT > ${TMPDIR}/killoutstandingtestprocesses.sh
#!/bin/sh +x

ECHO=\`which echo\`
#==================================================
# HERE IS WHERE THE TOTAL TESTTIME CAN BE ADJUSTED
#==================================================
\$ECHO "Waiting a maximum of 5 minutes for test to complete then force a kill of the test processes"
sleep 300
\$ECHO  "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
\$ECHO  "SEVERE:Timeout occurred, killing any outstanding test processes"
\$ECHO  "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
PIDS=\`ps -ef | grep "com.sun.enterprise.ee.cms.tests.core.GroupHandleTest" | grep "\${groupName}" | grep -v grep | awk '{printf ("%d ", \$2)}'\`
if [ ! -z "\${PIDS}" ]; then
   \$ECHO  "The following pids were found [\${PIDS}], killing them"
   kill -9 \$PIDS
fi

ENDSCRIPT

############################################
# This is where test execution really begins
############################################

ECHO=`which echo`
$ECHO  "=========================================================================="
$ECHO "This test requires a maximum of 5 minutes of runtime since there is a"
$ECHO "default timeout of 5 minutes to ensure that the test completes and all"
$ECHO "test processes are terminated. If the amount of testing time is determined"
$ECHO "to be less than that, the time can be adjusted."
$ECHO  "=========================================================================="
$ECHO  "Killing any previous outstanding test processes"
PIDS=`ps -ef | grep "com.sun.enterprise.ee.cms.tests.core.GroupHandleTest" | grep "${groupName}" | grep -v grep | awk '{printf ("%d ", $2)}'`
if [ ! -z "${PIDS}" ]; then
   $ECHO  "The following pids were found [${PIDS}], killing them"
   kill -9 $PIDS
fi
PIDS1=`ps -ef | grep killoutstandingtestprocesses.sh | grep -v grep | awk '{printf ("%d ", $2)}' `
if [ ! -z "${PIDS1}" ]; then
   # get the sleep process and the killoutstandingtestprocesses.sh process
   PIDS=`ps -ef | grep ${PIDS1} | awk  '{printf ("%d ", $2)}' `
   $ECHO  "The following killoutstandingtestprocesses pids were also found [${PIDS}], killing them"
   kill -9 $PIDS
fi

chmod 755 ${TMPDIR}/*.sh

LOGDIR=$SHOALWORKSPACE/LOGS/apitests
logLevel=INFO

$ECHO "Log Directory=${LOGDIR}"
$ECHO "TMP Directory=${TMPDIR}"

if [ ! -d ${LOGDIR} ] ; then
    mkdir -p ${LOGDIR}
else
    rm -rf ${LOGDIR}/*.log  ${LOGDIR}/*.out ${LOGDIR}/*.done
fi

#-----------------------------------------------
$ECHO "Start Testing for GroupHandler"
numInstances="3"
$ECHO "Number of Instances=${numInstances}"

$ECHO "Date: `date`"
$ECHO "Starting SPECTOR/MASTER"
${TMPDIR}/grouphandle.sh master ${groupName} ${numInstances} ${LOGDIR} 9100 9200 ${logLevel} >& ${LOGDIR}/GroupHandle_master.log &
$ECHO "Finished starting SPECTOR/MASTER"

# give time for the SPECTATOR and WATCHDOG to start
sleep 5
$ECHO "Starting CORE members on `uname -n`"

${TMPDIR}/grouphandle.sh core103 ${groupName} ${LOGDIR} 9100 9200 ${logLevel} >& ${LOGDIR}/GroupHandle_core103.log &
${TMPDIR}/grouphandle.sh core102 ${groupName} ${LOGDIR} 9100 9200 ${logLevel} >& ${LOGDIR}/GroupHandle_core102.log &
${TMPDIR}/grouphandle.sh core101 ${groupName} ${LOGDIR} 9100 9200 ${logLevel} >& ${LOGDIR}/GroupHandle_core101.log &
core101_pid=$!
$ECHO "CORE101 pid=${core101_pid}"

$ECHO "Finished starting CORE members"

$ECHO "Starting killoutstandingtestprocesses.sh process"
${TMPDIR}/killoutstandingtestprocesses.sh >& ${LOGDIR}/killoutstandingtestprocesses.log &

$ECHO "Waiting for CORE101 to complete testing or timeout to occur"
# CORE101 should be the last process running because the tests actually do some testing where core101 sends a shutdown
# to the master and then does some api testing after that point. This situation would cause
# that existing master to finish its processing. Once we leave the group, we should become the
# master of ourselves, hence the testing that is done
wait ${core101_pid}

$ECHO "Date: `date`"

$ECHO  "==============="
grep -a "Testing Complete for" ${LOGDIR}/*.log | awk -F"|" '{print $7}'
$ECHO  "==============="
$ECHO  "The following are severe messages found in the logs:"
$ECHO  "==============="
grep -a "SEVERE" ${LOGDIR}/*.log
grep -a "SEVERE" ${LOGDIR}/killoutstandingtestprocesses.log
$ECHO  "==============="
$ECHO
$ECHO  "Killing any outstanding test processes"
PIDS=`ps -ef | grep "com.sun.enterprise.ee.cms.tests.core.GroupHandleTest" | grep "${groupName}" | grep -v grep | awk '{printf ("%d ", $2)}'`
if [ ! -z "${PIDS}" ]; then
   $ECHO  "The following pids were found [${PIDS}], killing them"  
   kill -9 $PIDS
fi
PIDS1=`ps -ef | grep killoutstandingtestprocesses.sh | grep -v grep | awk '{printf ("%d ", $2)}' `
if [ ! -z "${PIDS1}" ]; then
   # get the sleep process and the killoutstandingtestprocesses.sh process
   PIDS=`ps -ef | grep ${PIDS1} | awk  '{printf ("%d ", $2)}' `
   $ECHO  "The following killoutstandingtestprocesses pids were also found [${PIDS}], killing them"
   kill -9 $PIDS
fi

$ECHO "DONE Testing "
