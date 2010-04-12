#!/bin/sh +x 

#
# Copyright 2010 Sun Microsystems, Inc.  All rights reserved.
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


TCPSTARTPORT=9060
TCPENDPORT=9089
MULTICASTADDRESS=229.9.1.2
MULTICASTPORT=2299
LOGLEVEL=WARNING

SHOALWORKSPACE=`pwd`
PUBLISH_HOME=$SHOALWORKSPACE/dist
LIB_HOME=$SHOALWORKSPACE/lib
JARS=${PUBLISH_HOME}/shoal-gms-tests.jar:${PUBLISH_HOME}/shoal-gms.jar:${LIB_HOME}/grizzly-framework.jar:${LIB_HOME}/grizzly-utils.jar
COMMUNICATION_PROVIDER=grizzly

MAINCLASS=com.sun.enterprise.ee.cms.tests.GMSAdminCLI

TMPDIR=$SHOALWORKSPACE/tmp



if [ ! -d ${TMPDIR} ] ; then
    mkdir ${TMPDIR}
else
    rm -rf ${TMPDIR}/script*
fi

#########################################
# Create the scripts used to run the test
#########################################

#===============================================
# Create the script that actually runs the test
#===============================================
cat << ENDSCRIPT > ${TMPDIR}/applicationadmin.sh
#!/bin/sh +x

ECHO=\`which echo\`


usage () {
    echo "Usage:"
    echo "    list groupName [memberName(all is default)] - list member(s)"
    echo "    stopc groupName - stops a cluster"
    echo "    stopm groupName memberName - stop a member"
    echo "    killm groupName memberName - kill a member"
    echo "    killa groupName - kills all members of the cluster "
    echo "    waits groupName - wait for the cluster to complete startup"
    echo "    state groupName gmsmemberstate  - list member(s) in the specific state"

    echo
    echo " optional arguments:"
    echo "      [<-l loglevel> <-ts TCPSTARTPORT> <-te TCPENDPORT> <-ma multicastaddress> <-mp multicastport>]"
    exit 0
}


_TCPSTARTPORT=${TCPSTARTPORT}
_TCPENDPORT=${TCPENDPORT}
_MULTICASTADDRESS=${MULTICASTADDRESS}
_MULTICASTPORT=${MULTICASTPORT}
_LOGLEVEL=${LOGLEVEL}

DONEREQUIRED=false
while [ \$# -ne 0 ]
do
     case \$1 in
       -h)
       usage
       ;;
       -ts)
       shift
       _TCPSTARTPORT=\${1}
       shift
       ;;
       -te)
       shift
       _TCPENDPORT=\${1}
       shift
       ;;
       -ma)
       shift
       _MULTICASTADDRESS=\${1}
       shift
       ;;
       -mp)
       shift
       _MULTICASTPORT=\${1}
       shift
       ;;
       -l)
       shift
       _LOGLEVEL=\${1}
       shift
       ;;
       *)
       if [ \$DONEREQUIRED = false ]; then
           COMMAND=\${1}
           shift
           GROUPNAME=\${1}
           shift           
           DONEREQUIRED=true
       else
           MEMBERNAME=\${1}
           shift
       fi
       ;;
     esac
done

LOG_LEVEL="-DLOG_LEVEL=\${_LOGLEVEL}"

java -Dcom.sun.management.jmxremote  -DSHOAL_GROUP_COMMUNICATION_PROVIDER=${COMMUNICATION_PROVIDER} -DTCPSTARTPORT=\${_TCPSTARTPORT} -DTCPENDPORT=\${_TCPENDPORT} -DMULTICASTADDRESS=\${_MULTICASTADDRESS} -DMULTICASTPORT=\${_MULTICASTPORT} -cp ${JARS} \${LOG_LEVEL} $MAINCLASS \${COMMAND} \${GROUPNAME} \${MEMBERNAME}

ENDSCRIPT
#=====================================================================

############################################
# This is where test execution really begins
############################################

chmod 755 ${TMPDIR}/applicationadmin.sh


usage () {

${TMPDIR}/applicationadmin.sh -h
exit 1
}

if [ "$1" = "-h" ]; then
    usage
fi

if [ "$1" = "-programhelp" ]; then
    java -cp ${JARS} $MAINCLASS -h
    exit 0
fi

if [ $# -lt 2 ]; then
     echo "Error: Command and GroupName must be specified"
     usage
fi

${TMPDIR}/applicationadmin.sh  $@  







