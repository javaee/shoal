@echo off

REM
REM  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
REM
REM  Copyright (c) 2004-2010 Oracle and/or its affiliates. All rights reserved.
REM
REM  The contents of this file are subject to the terms of either the GNU
REM  General Public License Version 2 only ("GPL") or the Common Development
REM  and Distribution License("CDDL") (collectively, the "License").  You
REM  may not use this file except in compliance with the License.  You can
REM  obtain a copy of the License at
REM  https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
REM  or packager/legal/LICENSE.txt.  See the License for the specific
REM  language governing permissions and limitations under the License.
REM
REM  When distributing the software, include this License Header Notice in each
REM  file and include the License file at packager/legal/LICENSE.txt.
REM
REM  GPL Classpath Exception:
REM  Oracle designates this particular file as subject to the "Classpath"
REM  exception as provided by Oracle in the GPL Version 2 section of the License
REM  file that accompanied this code.
REM
REM  Modifications:
REM  If applicable, add the following below the License Header, with the fields
REM  enclosed by brackets [] replaced by your own identifying information:
REM  "Portions Copyright [year] [name of copyright owner]"
REM
REM  Contributor(s):
REM  If you wish your version of this file to be governed by only the CDDL or
REM  only the GPL Version 2, indicate your decision by adding "[Contributor]
REM  elects to include this software in this distribution under the [CDDL or GPL
REM  Version 2] license."  If you don't indicate a single choice of license, a
REM  recipient has the option to distribute your version of this file under
REM  either the CDDL, the GPL Version 2 or to extend the choice of license to
REM  its licensees as provided above.  However, if you add GPL Version 2 code
REM  and therefore, elected the GPL Version 2 license, then the option applies
REM  only if the new code is made subject to such option by the copyright
REM  holder.
REM

setlocal
set publish_home=.\dist
set lib_home=.\lib
set jdk_home=%JAVA_HOME%
echo %jdk_home%


if "%1a"=="a" goto usage
if "%2a"=="a" goto usage
if "%3a"=="a" goto usage
if "%4a"=="a" goto usage
if "%5a"=="a" goto usage
if "%6a"=="a" goto usage

"%jdk_home%"\bin\java -Dcom.sun.management.jmxremote -DMEMBERTYPE=%3 -DINSTANCEID=%1 -DCLUSTERNAME=%2 -DMESSAGING_MODE=true -DLIFEINMILLIS=%4 -DLOG_LEVEL=%5 -DBIND_INTERFACE_ADDRESS=%6 -cp %publish_home%/shoal-gms-tests.jar;%publish_home%/shoal-gms.jar;%lib_home%/jxta.jar com.sun.enterprise.ee.cms.tests.ApplicationServer

goto end


:usage
echo Usage: %0 parameters... 
echo The required parameters are :
echo instance_id_token groupname membertype{CORE--OR--SPECTATOR} Life-In-Milliseconds log-level <bind_interface_ip_address>
echo Life in milliseconds should be at least 60000 to demo failure fencing.
echo <bind_interface_ip_address> refers to the ip address of a virtual or physical network interface 
echo which should be used by this test to bind to for all communications. Currently this test only accepts IPv4 addresses. 

:end
endlocal
