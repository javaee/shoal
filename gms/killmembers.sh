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
#
if [ -z "${1}" ] ;then
   PID=`jps -mlVv | grep "com.sun.enterprise.shoal" | awk '{print $1}'`
else
   PID=`jps -mlVv | grep "com.sun.enterprise.shoal" | grep ${1} | awk '{print $1}'`
fi
if [ ! -z "$PID" ]; then
   echo "Killing instance PID(s) :|$PID|"
   echo "Date:";date 
   kill -9  $PID
   if [ -z "${1}" ] ;then
       jps -v | grep ApplicationServer
   else
       jps -v | grep ${1}
   fi
else
   echo "No pid(s) found"
   exit 1
fi
