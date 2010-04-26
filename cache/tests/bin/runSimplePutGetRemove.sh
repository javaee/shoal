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


CACHEWORKSPACE=../..
LOGS_DIR=$CACHEWORKSPACE/LOGS/simpleputgetremove
SHOALWORKSPACE=${CACHEWORKSPACE}/../gms
SHOAL_LIB=$SHOALWORKSPACE/lib
SHOAL_DIST=$SHOALWORKSPACE/dist

CACHE_TESTS_DIST=$CACHEWORKSPACE/tests/dist
CACHE_DIST=$CACHEWORKSPACE/target
SHOAL_GROUP_COMMUNICATION_PROVIDER="SHOAL_GROUP_COMMUNICATION_PROVIDER=grizzly"


usage () {
    java -cp ${JARS} $MAINCLASS -h
    exit 0
}

MAINCLASS="com.sun.enterprise.ee.tests.DataStore.SimplePutGetRemoveTest"

GRIZZLY_JARS=${SHOAL_DIST}/shoal-gms-tests.jar:${SHOAL_DIST}/shoal-gms.jar:${SHOAL_LIB}/grizzly-framework.jar:${SHOAL_LIB}/grizzly-utils.jar
CACHE_JARS=${CACHE_DIST}/cache-1.0-SNAPSHOT.jar:${CACHE_TESTS_DIST}/cache-tests.jar
JARS=${GRIZZLY_JARS}:${CACHE_JARS}

TEST_LOG_LEVEL=WARNING
SHOALGMS_LOG_LEVEL=WARNING

TCPSTARTPORT=""
TCPENDPORT=""
MULTICASTADDRESS="-DMULTICASTADDRESS=229.9.1.2"
MULTICASTPORT="-DMULTICASTPORT=2299"
TRANSPORT=grizzly

DONEREQUIRED=false
while [ $# -ne 0 ]
do
     case ${1} in
       -h)
       usage
       exit 1
       ;;
       -ts)
       shift
       TCPSTARTPORT="-DTCPSTARTPORT=${1}"
       shift
       ;;
       -te)
       shift
       TCPENDPORT="-DTCPENDPORT=${1}"
       shift
       ;;
       -ma)
       shift
       MULTICASTADDRESS="-DMULTICASTADDRESS=${1}"
       shift
       ;;
       -mp)
       shift
       MULTICASTPORT="-DMULTICASTPORT=${1}"
       shift
       ;;
       -tl)
       shift
       TEST_LOG_LEVEL="${1}"
       shift
       ;;
       -sl)
       shift
       SHOALGMS_LOG_LEVEL="${1}"
       shift
       ;;
       *)
       if [ ${DONEREQUIRED} = false ]; then
           INSTANCEID=$1
           shift
           CLUSTERNAME=$1
           shift
           NUMINSTANCES=$1
           shift
           if [ "${INSTANCEID}" != "server" ];
           then
             NUMOBJECTS="-DNUMOBJECTS=$1"
             shift
             PAYLOADSIZE="-DPAYLOADSIZE=$1"
             shift
           fi
           DONEREQUIRED=true
       else
          echo "ERRROR: ignoring invalid argument $1"
          shift
       fi
       ;;
     esac
done

if [ -z ${INSTANCEID} -o -z ${CLUSTERNAME} -o -z ${NUMINSTANCES} ]; then
    echo "ERROR: Missing a required argument"
    usage;
fi
if [ "${INSTANCEID}" != "server" ];
then
    if [ -z ${NUMINSTANCES} -o -z ${NUMOBJECTS} ]; then
        echo "ERROR: Missing a required argument"
        usage;
    fi
fi


echo Running using Shoal with transport ${TRANSPORT}
echo "=========================="
#echo java -Dcom.sun.management.jmxremote  -DINSTANCEID=${INSTANCEID} -DCLUSTERNAME=${CLUSTERNAME} -DNUMINSTANCES=${NUMINSTANCES} ${NUMOBJECTS} ${PAYLOADSIZE} -DTEST_LOG_LEVEL=${TEST_LOG_LEVEL} -DLOG_LEVEL=${SHOALGMS_LOG_LEVEL}   -cp ${JARS} ${TCPSTARTPORT} ${TCPENDPORT} -DSHOAL_GROUP_COMMUNICATION_PROVIDER=${TRANSPORT} ${MULTICASTADDRESS} ${MULTICASTPORT} ${MAINCLASS};
java -Dcom.sun.management.jmxremote  -DINSTANCEID=${INSTANCEID} -DCLUSTERNAME=${CLUSTERNAME} -DNUMINSTANCES=${NUMINSTANCES} ${NUMOBJECTS} ${PAYLOADSIZE} -DTEST_LOG_LEVEL=${TEST_LOG_LEVEL} -DLOG_LEVEL=${SHOALGMS_LOG_LEVEL} -cp ${JARS} ${TCPSTARTPORT} ${TCPENDPORT} -DSHOAL_GROUP_COMMUNICATION_PROVIDER=${TRANSPORT} ${MULTICASTADDRESS} ${MULTICASTPORT} ${MAINCLASS};


