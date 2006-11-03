@echo off
rem 
rem  Copyright 2004-2005 Sun Microsystems, Inc.  All rights reserved.
rem  Use is subject to license terms.
rem 
rem 
rem  The contents of this file are subject to the terms
rem  of the Common Development and Distribution License
rem  (the License).  You may not use this file except in
rem  compliance with the License.
rem 
rem  You can obtain a copy of the license at
rem https://shoal.dev.java.net/public/CDDLv1.0.html
rem
rem See the License for the specific language governing
rem permissions and limitations under the License.
rem
rem When distributing Covered Code, include this CDDL
rem Header Notice in each file and include the License file
rem at
rem If applicable, add the following below the CDDL Header,
rem with the fields enclosed by brackets [] replaced by
rem you own identifying information:
rem "Portions Copyrighted [year] [name of copyright owner]"
rem
rem Copyright 2006 Sun Microsystems, Inc. All rights reserved.
rem

setlocal
set publish_home=.\dist
set lib_home=.\lib
set jdk_home=%JAVA_HOME%\bin
echo %jdk_home%


if "%1a"=="a" goto usage
if "%2a"=="a" goto usage
if "%3a"=="a" goto usage
if "%4a"=="a" goto usage
if "%5a"=="a" goto usage

if "%5"=="-debug" goto debug

"%jdk_home%"\java -Dcom.sun.management.jmxremote -DMEMBERTYPE=%3 -DINSTANCEID=%1 -DCLUSTERNAME=%2 -DMESSAGING_MODE=true -DLIFEINMILLIS=%4 -DLOG_LEVEL=%5 -cp %publish_home%/shoal-gms.jar;%lib_home%/jxta.jar com.sun.enterprise.ee.cms.tests.ApplicationServer

goto end

:debug
"%jdk_home%"\java -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp;transport=dt_socket,server=y,suspend=y,address=5005 -DMEMBERTYPE=%3 -DINSTANCEID=%1 -DCLUSTERNAME=%2 -DMESSAGING_MODE=true -DLIFEINMILLIS=%4 -DLOG_LEVEL=INFO -cp %publish_home%/shoal-gms.jar;%lib_home%/jxta.jar com.sun.enterprise.ee.cms.tests.ApplicationServer
goto end

:usage
echo Usage: %0 parameters... 
echo The required parameters are :
echo instance_id_token groupname membertype{CORE--OR--SPECTATOR} Life-In-Milliseconds log-level
echo Life in milliseconds should be at least 60000 to demo failure fencing.

:end
endlocal
