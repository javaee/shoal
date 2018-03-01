#!/bin/sh +x
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2004-2010 Oracle and/or its affiliates. All rights reserved.
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

SHOALWORKSPACE=`pwd`
TMPDIR=$SHOALWORKSPACE/tmp
LOGDIR=$SHOALWORKSPACE/LOGS/hamessagesimulator

ECHO=`which echo`
if [ ! -d ${TMPDIR} ] ; then
    mkdir ${TMPDIR}
else
    rm -rf ${TMPDIR}/script*
fi
if [ ! -d ${LOGDIR} ] ; then
    mkdir ${LOGDIR}
else
    rm -rf ${LOGDIR}/instance*.log ${LOGDIR}/server.log
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

if [ "\${1}" == "server" ] ; then
java -Dcom.sun.management.jmxremote -DLOG_LEVEL=\$3 -cp \${publish_home}/shoal-gms-tests.jar:\${publish_home}/shoal-gms.jar:\${lib_home}/grizzly2-framework.jar:\${lib_home}/grizzly-framework.jar:\${lib_home}/grizzly-utils.jar  -DSHOAL_GROUP_COMMUNICATION_PROVIDER=grizzly com.sun.enterprise.shoal.messagesenderreceivertest.HAMessagingSimulator \$1 \$2
else
java -Dcom.sun.management.jmxremote -DLOG_LEVEL=\$8 -cp \${publish_home}/shoal-gms-tests.jar:\${publish_home}/shoal-gms.jar:\${lib_home}/grizzly2-framework.jar:\${lib_home}/grizzly-framework.jar:\${lib_home}/grizzly-utils.jar -DTCPSTARTPORT=\$6 -DTCPENDPORT=\$7 -DSHOAL_GROUP_COMMUNICATION_PROVIDER=grizzly com.sun.enterprise.shoal.messagesenderreceivertest.HAMessagingSimulator \$1 \$2 \$3 \$4 \$5
fi

ENDSCRIPT
#===============================================

#=====================================================================
# Create the script monitors that monitors when  testing is complete
#=====================================================================
cat << ENDSCRIPT > ${TMPDIR}/script2
#!/bin/sh +x

ECHO=\`which echo\`
num=\$1
phrase="Testing Complete"
#num=\`ls -al instance*log | wc -l | sed -e 's/ //g' \`
count=\`grep "\${phrase}" ${LOGDIR}/instance*log | wc -l | sed -e 's/ //g' \`
\$ECHO "Waiting for the (\$num) instances to complete testing"
\$ECHO -n "\$count"
while [ \$count -ne \$num ]
do
\$ECHO -n ",\$count"
count=\`grep "\${phrase}" ${LOGDIR}/instance*log | wc -l | sed -e 's/ //g' \`
sleep 5
done
\$ECHO ", \$count"

count=\`grep "\${phrase}" ${LOGDIR}/server.log | wc -l | sed -e 's/ //g' \`
\$ECHO "Waiting for the DAS to complete testing"
\$ECHO -n "\$count"
while [ \$count -ne 1 ]
do
\$ECHO -n ",\$count"
count=\`grep "\${phrase}" ${LOGDIR}/server.log | wc -l | sed -e 's/ //g' \`
sleep 5
done

\$ECHO  ", \$count"
\$ECHO  "The following logs contain failures:"
\$ECHO  "==============="
grep "FAILED" ${LOGDIR}/instance*log
\$ECHO  "==============="
\$ECHO  "The following are the time results for sending messages:"
grep "Sending Messages Time data" ${LOGDIR}/instance*log
\$ECHO  "==============="
\$ECHO  "The following are the time results for receiving messages:"
grep "Receiving Messages Time data" ${LOGDIR}/instance*log
\$ECHO  "==============="
\$ECHO  "The following are EXCEPTIONS found in the logs:"
\$ECHO  "==============="
grep "Exception" ${LOGDIR}/instance*log
grep "Exception" ${LOGDIR}/server.log
\$ECHO  "==============="
\$ECHO  "The following are SEVERE messages found in the logs:"
\$ECHO  "==============="
grep "SEVERE" ${LOGDIR}/instance*log
grep "SEVERE" ${LOGDIR}/server.log
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
phrase="All members are joined and ready in the group"
#num=\`ls -al instance*log | wc -l | sed -e 's/ //g' \`
count=\`grep "\${phrase}" ${LOGDIR}/instance*log | wc -l | sed -e 's/ //g' \`
\$ECHO "Waiting for the (\$num) instances to join group"
\$ECHO -n "\$count"
while [ \$count -ne \$num ]
do
\$ECHO -n ",\$count"
count=\`grep "\${phrase}" ${LOGDIR}/instance*log | wc -l | sed -e 's/ //g' \`
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
`${TMPDIR}/script1 -h`
The optional parameters are : <log level> <tcpstartport num> <tcpendport>

   <tcpstartport> and <tcpendport> are optional.  Grizzly and jxta transports have different defaults.
USAGE
exit 1
}

if [ "$1" == "-h" ]; then
    usage
fi

numInstances="10"

numOfObjects=7
numOfMsgsPerObject=1000
startInstanceNum=101
msgSize=1024
startdtcpport=9130
logLevel=FINER

$ECHO "Number of Instances=${numInstances}"
$ECHO "Number of Objects=${numOfObjects}"
$ECHO "Number of messages Per Object=${numOfMsgsPerObject}"
$ECHO "Message size=${msgSize}"
$ECHO "Log Directory=${LOGDIR}"

$ECHO "Starting DAS"
${TMPDIR}/script1 server ${numInstances} ${logLevel} >& ${LOGDIR}/server.log &

# give time for the DAS to start
sleep 5
$ECHO "Starting ${numInstances} instances"

sin=${startInstanceNum}
sdtcp=${startdtcpport}
edtcp=`expr ${sdtcp} + 30`
count=1
while [ $count -le ${numInstances} ]
do
     ${TMPDIR}/script1 instance${sin} ${numInstances} ${numOfObjects} ${numOfMsgsPerObject} ${msgSize} ${sdtcp} ${edtcp} ${logLevel} >& ${LOGDIR}/instance${sin}.log &
     sin=`expr ${sin} + 1`
     sdtcp=`expr ${edtcp} + 1`
     edtcp=`expr ${sdtcp} + 30`
     count=`expr ${count} + 1`

done

#${TMPDIR}/script1 instance101 ${numInstances} ${numOfMsgsPerObject} ${numOfMsgsPerObject} ${msgSize} 9130 9160 INFO >& ${LOGDIR}/instance101.log &
#${TMPDIR}/script1 instance102 ${numInstances} ${numOfMsgsPerObject} ${numOfMsgsPerObject} ${msgSize} 9161 9191 INFO >& ${LOGDIR}/instance102.log &
#${TMPDIR}/script1 instance103 ${numInstances} ${numOfMsgsPerObject} ${numOfMsgsPerObject} ${msgSize} 9192 9222 INFO >& ${LOGDIR}/instance103.log &
#${TMPDIR}/script1 instance104 ${numInstances} ${numOfMsgsPerObject} ${numOfMsgsPerObject} ${msgSize} 9223 9253 INFO >& ${LOGDIR}/instance104.log &
#${TMPDIR}/script1 instance105 ${numInstances} ${numOfMsgsPerObject} ${numOfMsgsPerObject} ${msgSize} 9254 9284 INFO >& ${LOGDIR}/instance105.log &
#${TMPDIR}/script1 instance106 ${numInstances} ${numOfMsgsPerObject} ${numOfMsgsPerObject} ${msgSize} 9285 9315 INFO >& ${LOGDIR}/instance106.log &
#${TMPDIR}/script1 instance107 ${numInstances} ${numOfMsgsPerObject} ${numOfMsgsPerObject} ${msgSize} 9316 9346 INFO >& ${LOGDIR}/instance107.log &
#${TMPDIR}/script1 instance108 ${numInstances} ${numOfMsgsPerObject} ${numOfMsgsPerObject} ${msgSize} 9347 9377 INFO >& ${LOGDIR}/instance108.log &
#${TMPDIR}/script1 instance109 ${numInstances} ${numOfMsgsPerObject} ${numOfMsgsPerObject} ${msgSize} 9378 9408 INFO >& ${LOGDIR}/instance109.log &
#${TMPDIR}/script1 instance110 ${numInstances} ${numOfMsgsPerObject} ${numOfMsgsPerObject} ${msgSize} 9409 9439 INFO >& ${LOGDIR}/instance110.log &


# give time for the instances to start
sleep 3
# monitor for the testing to begin
${TMPDIR}/script3 ${numInstances} 
# monitor when the testing is complete
${TMPDIR}/script2 ${numInstances} 
