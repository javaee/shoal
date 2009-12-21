#!/bin/sh 

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
#!/bin/sh

PWD=`pwd`
ECHO=`which echo`


#########################################
# Create the scripts used to run the test
#########################################

#===============================================
# Create the script that actually runs the test
#===============================================
rm -rf /tmp/script1
cat << ENDSCRIPT > /tmp/script1
#!/bin/sh +x

publish_home=$PWD/dist
lib_home=$PWD/lib

java -Dcom.sun.management.jmxremote -DLOG_LEVEL=\$5 -cp \${publish_home}/shoal-gms.jar:\${lib_home}/bcprov-jdk14.jar:\${lib_home}/grizzly-framework.jar:\${lib_home}/grizzly-utils.jar -DTCPSTARTPORT=\$6 -DTCPENDPORT=\$7 -DSHOAL_GROUP_COMMUNICATION_PROVIDER=grizzly com.sun.enterprise.shoal.messagesenderreceiver.SenderReceiver \$1 \$2 \$3 \$4

ENDSCRIPT
#===============================================

#=====================================================================
# Create the script monitors that monitors when  testing is complete
#=====================================================================
rm -rf /tmp/script2
cat << ENDSCRIPT > /tmp/script2
#!/bin/sh +x

ECHO=\`which echo\`
num=\`ls -al *log | wc -l | sed -e 's/ //g' \`
count=\`grep "Testing Complete" *log | wc -l | sed -e 's/ //g' \`
\$ECHO "Waiting for the DAS and instances (\$num) to complete testing"
\$ECHO -n "\$count"
while [ \$count -ne \$num ]
do
\$ECHO -n ",\$count"
count=\`grep "Testing Complete" *log | wc -l | sed -e 's/ //g' \`
sleep 5
done
\$ECHO  ", \$count"
exit 0

ENDSCRIPT
#=====================================================================

############################################
# This is where test execution really begins
############################################

usage () {
    cat << USAGE
Usage: $0
`/tmp/script1 -h`
The optional parameters are : <log level> <tcpstartport num> <tcpendport>

   <tcpstartport> and <tcpendport> are optional.  Grizzly and jxta transports have different defaults.
USAGE
exit 1
}


chmod 755 /tmp/script1
chmod 755 /tmp/script2
rm -rf instance*.log server.log

if [ "$1" == "-h" ]; then
usage
fi



$ECHO "Starting DAS"
/tmp/script1 server 10 >& server.log &
# give time for the DAS to start
sleep 5
$ECHO "Starting instances"
/tmp/script1 instance101 10 1024 7000 9130 9160 >& instance101.log &
/tmp/script1 instance102 10 1024 7000 9160 9190 >& instance102.log &
/tmp/script1 instance103 10 1024 7000 9230 9260 >& instance103.log &
/tmp/script1 instance104 10 1024 7000 9261 9290 >& instance104.log &
/tmp/script1 instance105 10 1024 7000 9330 9360 >& instance105.log &
/tmp/script1 instance106 10 1024 7000 9361 9390 >& instance106.log &
/tmp/script1 instance107 10 1024 7000 9430 9460 >& instance107.log &
/tmp/script1 instance108 10 1024 7000 9461 9490 >& instance108.log &
/tmp/script1 instance109 10 1024 7000 9530 9560 >& instance109.log &
/tmp/script1 instance110 10 1024 7000 9561 9590 >& instance110.log &


# give time for the instances to start
sleep 3
# monitor when the testing is complete
/tmp/script2
