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

Usage()
{
  echo "usage: [-h] [-u] [-r] filename"
  echo "      -u: get udp data only (default is all)"
  echo "      -r: remove previous file"
  echo "filename: name of the file to store the output in"

  exit 0
}

UDP=false
if [ "${1}" = "-u" ]; then
   UDP=true
   shift
fi
REMOVE=false
if [ "${1}" = "-r" ]; then
   REMOVE=true
   shift
fi

FILE=""
if [ ! -z "${1}" ]; then
   FILE=${1}
   shift
fi

if [ ${REMOVE} = true ]; then
   rm -rf ${FILE}
fi

LOGDIR=`dirname ${FILE}`
if [ ! -z "${LOGDIR}" ]; then
   if [ ! -d ${LOGDIR} ];then
      mkdir -p ${LOGDIR}
   fi
fi

touch ${FILE}
date >> ${FILE}

mkdir tmp > /dev/null 2>&1
rm -rf tmp/netstat.tmp

if [ ${UDP} = true ]; then
    netstat -s | sed -n -e '/^[uU]dp:/,$p' > tmp/netstat.tmp
    first=1
    while read line
    do
      if [ $first -eq 1 ]
      then
         echo "$line" >> ${FILE}
         first=0
      else
         a=`echo "$line" | grep '^.*:$'`
         if [ ! -z  "$a" ]
         then
            break
         else
            echo "$line" >> ${FILE}
         fi
      fi
    done  < tmp/netstat.tmp

else
   netstat -u >> ${FILE}
fi