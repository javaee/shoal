#!/bin/sh +x

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
if [ "${groupName}" == "" ]; then
    groupName="TestGroup"
fi

#
#
#
#-------------------------
TMPDIR=$SHOALWORKSPACE/tmp
if [ ! -d ${TMPDIR} ]; then
    mkdir ${TMPDIR}
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

if [ "\${1}" == "master" ] ; then
    java -Dcom.sun.management.jmxremote -DSHOAL_GROUP_COMMUNICATION_PROVIDER=${PROVIDER} -DTCPSTARTPORT=\$5 -DTCPENDPORT=\$6 -DLOG_LEVEL=\$7 -cp \${publish_home}/shoal-gms-tests.jar:\${publish_home}/shoal-gms.jar:\${lib_home}/bcprov-jdk14.jar:\${lib_home}/grizzly-framework.jar:\${lib_home}/grizzly-utils.jar:\${lib_home}/jxta.jar com.sun.enterprise.ee.cms.tests.core.GroupHandleTest \$1 \$2 \$3 \$4
else
    java -Dcom.sun.management.jmxremote -DSHOAL_GROUP_COMMUNICATION_PROVIDER=${PROVIDER} -DTCPSTARTPORT=\$4 -DTCPENDPORT=\$5 -DLOG_LEVEL=\$6 -cp \${publish_home}/shoal-gms-tests.jar:\${publish_home}/shoal-gms.jar:\${lib_home}/bcprov-jdk14.jar:\${lib_home}/grizzly-framework.jar:\${lib_home}/grizzly-utils.jar:\${lib_home}/jxta.jar com.sun.enterprise.ee.cms.tests.core.GroupHandleTest \$1 \$2 \$3
fi
ENDSCRIPT


############################################
# This is where test execution really begins
############################################

chmod 755 ${TMPDIR}/grouphandle.sh

ECHO=`which echo`


LOGDIR=$SHOALWORKSPACE/LOGS/apitests
logLevel=INFO

$ECHO "Log Directory=${LOGDIR}"
$ECHO "TMP Directory=${TMPDIR}"

if [ ! -d ${TMPDIR} ] ; then
    mkdir ${TMPDIR}
else
    rm -rf ${TMPDIR}/script*
fi
if [ ! -d ${LOGDIR} ] ; then
    mkdir ${LOGDIR}
else
    rm -rf ${LOGDIR}/*.log  ${LOGDIR}/*.out ${LOGDIR}/*.done
fi

#-----------------------------------------------
$ECHO "Start Testing for GroupHandler"
numInstances="3"
$ECHO "Number of Instances=${numInstances}"

$ECHO "Starting SPECTOR/MASTER"
${TMPDIR}/grouphandle.sh master ${groupName} ${numInstances} ${LOGDIR} 9130 9160 ${logLevel} >& ${LOGDIR}/GroupHandle_master.log &

# give time for the SPECTATOR and WATCHDOG to start
sleep 5
$ECHO "Starting CORE members on `uname -n`"

${TMPDIR}/grouphandle.sh core103 ${groupName} ${LOGDIR} 9223 9253 ${logLevel} >& ${LOGDIR}/GroupHandle_core103.log &
${TMPDIR}/grouphandle.sh core102 ${groupName} ${LOGDIR} 9192 9222 ${logLevel} >& ${LOGDIR}/GroupHandle_core102.log &
${TMPDIR}/grouphandle.sh core101 ${groupName} ${LOGDIR} 9161 9191 ${logLevel} >& ${LOGDIR}/GroupHandle.log
#-----------------------------------------------


$ECHO  "The following are SEVERE messages found in the logs:"
$ECHO  "==============="
grep -a "SEVERE" ${LOGDIR}/*.log
$ECHO  "==============="
$ECHO  "Number of tests executed are the combination of the following:"
$ECHO  "==============="
grep -a "Testing Complete for" ${LOGDIR}/*.log
$ECHO

$ECHO "DONE Testing "
