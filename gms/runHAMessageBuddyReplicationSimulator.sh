#!/bin/sh +x

#
# Copyright 2004-2005 Sun Microsystems, Inc.  All rights reserved.
# Use is subject to license terms.
#
 #
 # The contents of this file are subject to the terms
 # of the Common Development and Distribution License
 # (the License).  You may not use this file except in
 # compliance with the License.
 #
 # You can obtain a copy of the license at
 # https://shoal.dev.java.net/public/CDDLv1.0.html
 #
 # See the License for the specific language governing
 # permissions and limitations under the License.
 #
 # When distributing Covered Code, include this CDDL
 # Header Notice in each file and include the License file
 # at
 # If applicable, add the following below the CDDL Header,
 # with the fields enclosed by brackets [] replaced by
 # you own identifying information:
 # "Portions Copyrighted [year] [name of copyright owner]"
 #
 # Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 #
#-----------------------------------------------------

# This program creates a cluster conprised of a master and N number of core members.
# Each core member acts as an instance in the cluster which replicates(sends messages)
# to the instance that is one greater than itself. The last instance replicates
# to the first instance in the cluster. The names of the instances are instance101,
# instance102, etc... The master node is called master. Based on the arguments
# passed into the program the instances will send M objects*N messages to the replica.
# The replica saves the messages and verifies that the number of objects/messages
# were received and that the content was correct. Once each instance is done
# sending their messages a final message (DONE) is sent to the replica. The replica
# upon receiving the message forwards this message to the master. Once the master
# has received a DONE message from each instance, it calls group shutdown on the cluster.

#-----------------------------------------------------
# VERIFY THE CONTENTS CONTAINED WITHIN

SHOALWORKSPACE=`pwd`
TMPDIR=$SHOALWORKSPACE/tmp
LOGDIR=$SHOALWORKSPACE/runhamessagebuddyreplicasimulator_logs
NUMOFINSTANCES=10
NUMOFOBJECTS=100
NUMOFMSGSPEROBJECT=500
MSGSIZE=4096
STARTINSTANCENUM=101
STARTTCPPORT=9130
LOGLEVEL=INFO
PUBLISH_HOME=$SHOALWORKSPACE/dist
LIB_HOME=$SHOALWORKSPACE/lib
JARS=${PUBLISH_HOME}/shoal-gms-tests.jar:${PUBLISH_HOME}/shoal-gms.jar:${LIB_HOME}/jxta.jar:${LIB_HOME}/grizzly-framework.jar:${LIB_HOME}/grizzly-utils.jar
SHOAL_GROUP_COMMUNICATION_PROVIDER="SHOAL_GROUP_COMMUNICATION_PROVIDER=jxta"
#
# if members are distributed across machines, the GROUPNAME
# must be set to a unique value so that all members join the
# same group
#
# COMMENTED OUT technique of generating unique groupname.  Use different MULTICASTADDRESS and/or MULTICASTPORT per group/cluster to avoid collisions.
#GROUPNAME="TestGroup_`uname -n`"
#if [ "${GROUPNAME}" == "" ]; then
    GROUPNAME="TestGroup"
#fi
MAINCLASS="com.sun.enterprise.shoal.messagesenderreceivertest.HAMessageBuddyReplicationSimulator"

#-----------------------------------------------------

ECHO=`which echo`
if [ ! -d ${TMPDIR} ] ; then
    mkdir ${TMPDIR}
else
    rm -rf ${TMPDIR}/script*
fi
if [ ! -d ${LOGDIR} ] ; then
    mkdir ${LOGDIR}
else
    rm -rf ${LOGDIR}/instance*.log ${LOGDIR}/master.log
fi


#########################################
# Create the scripts used to run the test
#########################################

#===============================================
# Create the script that actually runs the test
#===============================================
cat << ENDSCRIPT > ${TMPDIR}/script1
#!/bin/sh +x

ECHO=\`which echo\`



if [ "\${1}" == "-h" ] ; then
    java -Dcom.sun.management.jmxremote -cp ${JARS} $MAINCLASS \$1
    exit 0
fi

\$ECHO "Arg1=\${1}"
\$ECHO "Arg2=\${2}"
\$ECHO "Arg3=\${3}"
\$ECHO "Arg4=\${4}"
\$ECHO "Arg5=\${5}"
\$ECHO "Arg6=\${6}"
\$ECHO "Arg7=\${7}"
\$ECHO "Arg8=\${8}"
\$ECHO "Arg9=\${9}"

if [ "\${1}" == "master" ] ; then
java -Dcom.sun.management.jmxremote -D${SHOAL_GROUP_COMMUNICATION_PROVIDER} -DTCPSTARTPORT=\$4 -DTCPENDPORT=\$5 -DLOG_LEVEL=\$6 -cp ${JARS} $MAINCLASS \$1 \$2 \$3
else
java -Dcom.sun.management.jmxremote -D${SHOAL_GROUP_COMMUNICATION_PROVIDER} -DTCPSTARTPORT=\$7 -DTCPENDPORT=\$8 -DLOG_LEVEL=\$9 -cp ${JARS} $MAINCLASS \$1 \$2 \$3 \$4 \$5 \$6
fi

ENDSCRIPT
#===============================================

#=====================================================================
# Create the script that monitors when testing is complete
#=====================================================================
cat << ENDSCRIPT > ${TMPDIR}/script2
#!/bin/sh +x

ECHO=\`which echo\`
num=\$1
phrase="Testing Complete"
count=\`grep "\${phrase}" ${LOGDIR}/master.log | wc -l | sed -e 's/ //g' \`
\$ECHO "Waiting for the master and (\$num) instances to complete testing"
\$ECHO -n "\$count"
while [ \$count -lt 1 ]
do
\$ECHO -n ",\$count"
count=\`grep "\${phrase}" ${LOGDIR}/master.log | wc -l | sed -e 's/ //g' \`
sleep 5
done

\$ECHO  ", \$count"
sleep 5
\$ECHO  "==============="
\$ECHO  "The following are the time results for sending messages:"
grep "Sending Messages Time data" ${LOGDIR}/*log
\$ECHO  "==============="
\$ECHO  "The following are the time results for receiving messages:"
grep "Receiving Messages Time data" ${LOGDIR}/*log
\$ECHO  "==============="
\$ECHO  "The following logs contain failures:"
\$ECHO  "==============="
grep "FAILED" ${LOGDIR}/*log
\$ECHO  "==============="
\$ECHO  "The following are EXCEPTIONS found in the logs:"
\$ECHO  "==============="
grep "Exception" ${LOGDIR}/i*log
grep "Exception" ${LOGDIR}/master.log
\$ECHO  "==============="
\$ECHO  "The following are SEVERE messages found in the logs:"
\$ECHO  "==============="
grep "SEVERE" ${LOGDIR}/i*log
grep "SEVERE" ${LOGDIR}/master.log
\$ECHO  "==============="

exit 0

ENDSCRIPT
#=====================================================================

#=====================================================================
# Create the script monitors that monitors when  testing is complete
#=====================================================================
cat << ENDSCRIPT > ${TMPDIR}/script3
#!/bin/sh +x


ECHO=\`which echo\`
num=\$1
phrase="numberOfJoinAndReady received so far is: \$num"
count=\`grep "\${phrase}" ${LOGDIR}/master.log | wc -l | sed -e 's/ //g' \`
\$ECHO "Waiting for the (\$num) instances to join group"
\$ECHO -n "\$count"
while [ \$count -lt 1 ]
do
\$ECHO -n ",\$count"
count=\`grep "\${phrase}" ${LOGDIR}/master.log | wc -l | sed -e 's/ //g' \`
sleep 5
done
\$ECHO  ", \$count"
\$ECHO "All (\$num) instances have joined the group, testing will now begin"

exit 0

ENDSCRIPT
#=====================================================================

############################################
# This is where test execution really begins
############################################

chmod 755 ${TMPDIR}/script1
chmod 755 ${TMPDIR}/script2
chmod 755 ${TMPDIR}/script3

usage () {
    cat << USAGE
Usage: $0
`${TMPDIR}/script1 -h|-dist`
USAGE
exit 1
}

if [ "$1" == "-h" ]; then
    usage
fi


$ECHO "Number of Instances=${NUMOFINSTANCES}"
$ECHO "GroupName=${GROUPNAME}"
$ECHO "Number of Objects=${NUMOFOBJECTS}"
$ECHO "Number of messages Per Object=${NUMOFMSGSPEROBJECT}"
$ECHO "Message size=${MSGSIZE}"
$ECHO "Log Directory=${LOGDIR}"

if [ "$1" != "-dist" ]; then
    # non-distributed environment

    $ECHO "Starting DAS"
    sdtcp=${STARTTCPPORT}
    edtcp=`expr ${sdtcp} + 30`
    ${TMPDIR}/script1 master ${GROUPNAME} ${NUMOFINSTANCES} ${sdtcp} ${edtcp} ${LOGLEVEL} >& ${LOGDIR}/master.log &
    #${TMPDIR}/script1 master ${GROUPNAME} ${NUMOFINSTANCES} 9130 9160 ${LOGLEVEL} >& ${LOGDIR}/master.log &

    # give time for the DAS to start
    sleep 5
    $ECHO "Starting ${NUMOFINSTANCES} instances"
    sin=${STARTINSTANCENUM}
    sdtcp=`expr ${edtcp} + 1`
    edtcp=`expr ${sdtcp} + 30`
    count=1
    while [ $count -le ${NUMOFINSTANCES} ]
    do
         ${TMPDIR}/script1 instance${sin} ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFOBJECTS} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} ${sdtcp} ${edtcp} ${LOGLEVEL} >& ${LOGDIR}/instance${sin}.log &
         sin=`expr ${sin} + 1`
         sdtcp=`expr ${edtcp} + 1`
         edtcp=`expr ${sdtcp} + 30`
        count=`expr ${count} + 1`
    done

    #${TMPDIR}/script1 instance101 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFMSGSPEROBJECT} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9161 9191 INFO >& ${LOGDIR}/instance101.log &
    #${TMPDIR}/script1 instance102 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFMSGSPEROBJECT} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9192 9222 INFO >& ${LOGDIR}/instance102.log &
    #${TMPDIR}/script1 instance103 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFMSGSPEROBJECT} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9223 9253 INFO >& ${LOGDIR}/instance103.log &
    #${TMPDIR}/script1 instance104 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFMSGSPEROBJECT} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9254 9284 INFO >& ${LOGDIR}/instance104.log &
    #${TMPDIR}/script1 instance105 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFMSGSPEROBJECT} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9285 9315 INFO >& ${LOGDIR}/instance105.log &
    #${TMPDIR}/script1 instance106 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFMSGSPEROBJECT} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9316 9346 INFO >& ${LOGDIR}/instance106.log &
    #${TMPDIR}/script1 instance107 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFMSGSPEROBJECT} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9347 9377 INFO >& ${LOGDIR}/instance107.log &
    #${TMPDIR}/script1 instance108 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFMSGSPEROBJECT} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9378 9408 INFO >& ${LOGDIR}/instance108.log &
    #${TMPDIR}/script1 instance109 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFMSGSPEROBJECT} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9409 9439 INFO >& ${LOGDIR}/instance109.log &
    #${TMPDIR}/script1 instance110 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFMSGSPEROBJECT} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9440 9470 INFO >& ${LOGDIR}/instance110.log &
    #${TMPDIR}/script1 instance110 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFMSGSPEROBJECT} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9471 9500 INFO >& ${LOGDIR}/instance110.log &
    $ECHO "Finished starting ${NUMOFINSTANCES} instances"

    # give time for the instances to start
    sleep 3
    # monitor for the testing to begin
    ${TMPDIR}/script3 ${NUMOFINSTANCES}
    # monitor when the testing is complete
    ${TMPDIR}/script2 ${NUMOFINSTANCES}

else
    # distributed environment
   # $ECHO "Starting DAS"
   # sdtcp=${STARTTCPPORT}
   # edtcp=`expr ${sdtcp} + 30`
   # ${TMPDIR}/script1 master ${GROUPNAME} ${NUMOFINSTANCES} 9130 9160 ${LOGLEVEL} >& ${LOGDIR}/master.log &

    # give time for the DAS to start
   # sleep 5
    $ECHO "Starting instances"

    #${TMPDIR}/script1 instance101 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFOBJECTS} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9161 9191 INFO >& ${LOGDIR}/instance101.log &
    #${TMPDIR}/script1 instance102 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFOBJECTS} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9192 9222 INFO >& ${LOGDIR}/instance102.log &
    ${TMPDIR}/script1 instance103 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFOBJECTS} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9223 9253 INFO >& ${LOGDIR}/instance103.log &
    ${TMPDIR}/script1 instance104 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFOBJECTS} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9254 9284 INFO >& ${LOGDIR}/instance104.log &
    #${TMPDIR}/script1 instance105 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFOBJECTS} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9285 9315 INFO >& ${LOGDIR}/instance105.log &
    #${TMPDIR}/script1 instance106 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFOBJECTS} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9316 9346 INFO >& ${LOGDIR}/instance106.log &
    #${TMPDIR}/script1 instance107 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFOBJECTS} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9347 9377 INFO >& ${LOGDIR}/instance107.log &
    #${TMPDIR}/script1 instance108 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFOBJECTS} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9378 9408 INFO >& ${LOGDIR}/instance108.log &
    #${TMPDIR}/script1 instance109 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFOBJECTS} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9409 9439 INFO >& ${LOGDIR}/instance109.log &
    #${TMPDIR}/script1 instance110 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFOBJECTS} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9440 9470 INFO >& ${LOGDIR}/instance110.log &
    #${TMPDIR}/script1 instance110 ${GROUPNAME} ${NUMOFINSTANCES} ${NUMOFOBJECTS} ${NUMOFMSGSPEROBJECT} ${MSGSIZE} 9471 9500 INFO >& ${LOGDIR}/instance110.log &
    $ECHO "Finished starting ${NUMOFINSTANCES} instances"

fi




