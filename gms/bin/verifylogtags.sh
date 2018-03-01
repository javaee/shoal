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

usage () {
 echo "usage: [-h] [-v] directorytosearch  propertiesfile"
 echo "     -h   help"
 echo "     -v   keep the working files"
 echo "     path of directory to search"
 echo "     logical or virtual path to propertes file"
 echo " "
 echo "Examples:"
 echo "  ./verifylogtags.sh ../impl src/main/resources/com/sun/enterprise/ee/cms/logging/LogStrings.properties"
 echo "  ./verifylogtags.sh ../api src/main/resources/com/sun/enterprise/ee/cms/core/LogStrings.properties"
 echo "  ./verifylogtags.sh /workspaces/shoal/trunk/gms/impl /workspaces/shoal/trunk/gms/impl/src/main/resources/com/sun/enterprise/ee/cms/logging/LogStrings.properties"
 exit 1
}

if [ "$1" = "-h" ];then
  usage
fi

VERBOSE=false
if [ "$1" = "-v" ];then
  VERBOSE=true
  shift
fi

DIR="$1"
PROPFILE="$2"
PWD=`pwd`

cd $DIR
rm -rf logstrings.out*
find . -name "*.java" -print -exec grep "LOG.log" {} \; > logstrings.out

cat logstrings.out | egrep "Level.INFO|Level.WARNING|Level.SEVERE|\.java" > logstrings.out1

cat logstrings.out1 | sed -e "s/^.*LOG/LOG/g" | egrep ", \"|,$|\.java$" > logstrings.out2

echo "Checking for split lines"
echo "------------------------"
#for item in `cat logstrings.out2`
cat logstrings.out2 | while read item
do
   if [ "`echo $item | grep java`" == "" ]; then
      if [ "`echo $item | grep ',$' | grep -v '\"' `" != "" ]; then
        echo "Found split line: File = $file, Item = $item"
      fi
   else
      file=$item
   fi
done
echo "-------------------------"

FOUND=0
cat logstrings.out2 | while read item
do
   if [ "`echo $item | grep java`" == "" ]; then
      tmp="`echo \"$item\" |  awk -F, '{print $2}' | sed -e 's/\"//g' | sed -e 's/);//g' | sed -e 's/ //' | grep '\.' | grep -v '+' `"
      #echo "TMP=$tmp"
      if [ "$tmp" != "" ]; then
        # if we found a log entry write the filename once
        if [ $FOUND -eq 0 ]; then
            echo $file >> logstrings.out3
            FOUND=1
        fi
        echo $tmp >> logstrings.out3
      fi
   else
      FOUND=0
      file=$item
   fi
done

echo "Checking for missing tags"
echo "-------------------------"
for item in `cat logstrings.out3`
do
   if [ "`echo $item | grep java`" == "" ]; then
      if [ "`grep $item $PROPFILE`" == "" ]; then
         echo "Found missing tag: File = $file, Item = $item"
      fi
   else
      file=$item
   fi
done
echo "-------------------------"

if [ $VERBOSE = false ];then
   rm -rf logstrings.out*
fi

cd $PWD