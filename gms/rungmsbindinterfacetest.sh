#!/bin/sh
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2004-2018 Oracle and/or its affiliates. All rights reserved.
#
# The contents of this file are subject to the terms of either the GNU
# General Public License Version 2 only ("GPL") or the Common Development
# and Distribution License("CDDL") (collectively, the "License").  You
# may not use this file except in compliance with the License.  You can
# obtain a copy of the License at
# https://oss.oracle.com/licenses/CDDL+GPL-1.1
# or LICENSE.txt.  See the License for the specific
# language governing permissions and limitations under the License.
#
# When distributing the software, include this License Header Notice in each
# file and include the License file at LICENSE.txt.
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

publish_home=./dist
lib_home=./lib

usage () {
    cat << USAGE 
Usage: $0 <parameters...> 
The required parameters are :
 <instance_id_token> <groupname> <membertype{CORE|SPECTATOR}> <Life In Milliseconds> <log level> <bind_interface_ip_address>
Life in milliseconds should be at least 60000 to demo failure fencing.
<bind_interface_ip_address> refers to the IP address of a virtual or physical network interface which should be used 
by this test to bind to, for all communications. Currently this test only accepts IPv4 addresses. 
USAGE
   exit 0
}

if [ $# -lt 3 ]; then
    usage;
fi

if [ -n $5 ]; then 
	java -Dcom.sun.management.jmxremote -DMEMBERTYPE=$3 -DINSTANCEID=$1 -DCLUSTERNAME=$2 -DMESSAGING_MODE=true -DLIFEINMILLIS=$4 -DLOG_LEVEL=$5 -DBIND_INTERFACE_ADDRESS=$6 -cp ${publish_home}/shoal-gms-tests.jar:${publish_home}/shoal-gms.jar:${lib_home}/jxta.jar:${lib_home}/bcprov-jdk14.jar com.sun.enterprise.ee.cms.tests.ApplicationServer;
fi

