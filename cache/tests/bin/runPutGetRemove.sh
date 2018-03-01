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

TEST_LOG_LEVEL=WARNING
SHOALGMS_LOG_LEVEL=WARNING
TRANSPORT=grizzly

MAINCLASS="com.sun.enterprise.ee.tests.DataStore.PutGetRemoveTest"
CACHEWORKSPACE=../..
LOGS_DIR=$CACHEWORKSPACE/LOGS/putgetremove
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


GRIZZLY_JARS=${SHOAL_DIST}/shoal-gms-tests.jar:${SHOAL_DIST}/shoal-gms.jar:${SHOAL_LIB}/grizzly-framework.jar:${SHOAL_LIB}/grizzly-utils.jar
CACHE_JARS=${CACHE_DIST}/shoal-cache.jar:${CACHE_TESTS_DIST}/cache-tests.jar
JARS=${GRIZZLY_JARS}:${CACHE_JARS}

TCPSTARTPORT=""
TCPENDPORT=""
MULTICASTADDRESS="-DMULTICASTADDRESS=229.9.1.2"
MULTICASTPORT="-DMULTICASTPORT=2299"

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

if [ -z ${INSTANCEID} -o -z ${CLUSTERNAME} ]; then
    echo "ERROR: Missing a required argument"
    usage;
fi
if [ "${INSTANCEID}" != "server" ];
then
    if [ -z ${NUMOBJECTS} ]; then
        echo "ERROR: Missing a required argument"
        usage;
    fi
fi


echo Running using Shoal with transport ${TRANSPORT}
echo "=========================="
#echo java -Dcom.sun.management.jmxremote  -DINSTANCEID=${INSTANCEID} -DCLUSTERNAME=${CLUSTERNAME} -DNUMINSTANCES=${NUMINSTANCES} ${NUMOBJECTS} ${PAYLOADSIZE} -DTEST_LOG_LEVEL=${TEST_LOG_LEVEL} -DLOG_LEVEL=${SHOALGMS_LOG_LEVEL}   -cp ${JARS} ${TCPSTARTPORT} ${TCPENDPORT} -DSHOAL_GROUP_COMMUNICATION_PROVIDER=${TRANSPORT} ${MULTICASTADDRESS} ${MULTICASTPORT} ${MAINCLASS};
java -Dcom.sun.management.jmxremote  -DINSTANCEID=${INSTANCEID} -DCLUSTERNAME=${CLUSTERNAME} ${NUMOBJECTS} ${PAYLOADSIZE} -DTEST_LOG_LEVEL=${TEST_LOG_LEVEL} -DLOG_LEVEL=${SHOALGMS_LOG_LEVEL} -cp ${JARS} ${TCPSTARTPORT} ${TCPENDPORT} -DSHOAL_GROUP_COMMUNICATION_PROVIDER=${TRANSPORT} ${MULTICASTADDRESS} ${MULTICASTPORT} ${MAINCLASS};


