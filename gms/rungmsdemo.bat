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
set PUBLISH_HOME=.\dist
set LIB_HOME=.\lib
set JDK_HOME=%JAVA_HOME%\bin
echo %JDK_HOME%



set MAINCLASS=com.sun.enterprise.ee.cms.tests.ApplicationServer

set GRIZZLY_JARS=%PUBLISH_HOME%\shoal-gms-tests.jar;%PUBLISH_HOME%\shoal-gms.jar;%LIB_HOME%\grizzly-framework.jar;%LIB_HOME%\grizzly-utils.jar
set JXTA_JARS=%PUBLISH_HOME%\shoal-gms-tests.jar;%PUBLISH_HOME%\shoal-gms.jar;%LIB_HOME%\grizzly-framework.jar;%LIB_HOME%\grizzly-utils.jar;%LIB_HOME%\jxta.jar
set DEBUGARGS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005 -DjxtaMulticastPoolsize=25"
set NONDEBUGARGS="-Dcom.sun.management.jmxremote"
set JARS=%GRIZZLY_JARS%



set DEBUG=false
set TCPSTARTPORT=""
set TCPENDPORT=""
set MULTICASTADDRESS="-DMULTICASTADDRESS=229.9.1.2"
set MULTICASTPORT="-DMULTICASTPORT=2299"
set TRANSPORT=grizzly


if "%1a"=="a" goto usage
if "%2a"=="a" goto usage
if "%3a"=="a" goto usage
if "%4a"=="a" goto usage
if "%5a"=="a" goto usage
if "%6a"=="a" goto usage

set INSTANCEID=%1
shift
set CLUSTERNAME=%1
shift
set MEMBERTYPE=%1
shift
set LIFEINMILLIS=%1
shift
set LOGLEVEL=%1
shift
set TRANSPORT=%1

:setdebug
set DEBUG=true
shift
goto parseRemainingArgs

:settcpstartport
shift
set TCPSTARTPORT=%1
shift
goto parseRemainingArgs

:settcpendport
shift
set TCPENDPORT=%1
shift
goto parseRemainingArgs

:setmulticastaddress
shift
set MULTICASTADDRESS=%1
shift
goto parseRemainingArgs

:setmulticastport
shift
set MULTICASTPORT=%1
shift
goto parseRemainingArgs


:parseRemainingArgs
if ""%1""=="""" goto doneArgs
if ""%1""==""-h"" goto usage
if ""%1""==""-debug"" goto setdebug
if ""%1""==""-ts"" goto settcpstartport
if ""%1""==""-te"" goto settcpendport
if ""%1""==""-ma"" goto setmulticastaddress
if ""%1""==""-mp"" goto setmulticastport
echo "ERRROR: ignoring invalid argument %1"
shift
goto usage

:doneArgs


if "%MEMBERTYPE%" == "CORE" goto continue1
if "%MEMBERTYPE%" == "SPECTATOR" goto continue1
if "%MEMBERTYPE%" == "WATCHDOG" goto continue1
echo "ERROR: Invalid membertype specified"
goto usage

:continue1

if "%TRANSPORT%" == "grizzly" goto continue2
if "%TRANSPORT%" == "jxta" goto continue2
if "%TRANSPORT%" == "jxtanew" goto continue2
echo "ERROR: Invalid transport specified"
goto usage

:continue2

if "%TRANSPORT%" == "grizzly" goto continue3
set JARS=%JXTA_JARS%

:continue3

if "%DEBUG%" == "false" goto continue4
set OTHERARGS=%NONDEBUGARGS%
goto continue5
:continue4
set OTHERARGS=%DEBUGARGS%

:continue5


"%JDK_HOME%"\java %OTHERARGS% -DMEMBERTYPE=%MEMBERTYPE% -DINSTANCEID=%INSTANCEID% -DCLUSTERNAME=%CLUSTERNAME% -DMESSAGING_MODE=true -DLIFEINMILLIS=%LIFEINMILLIS% -DLOG_LEVEL=%LOGLEVEL% -cp %JARS% -DTCPSTARTPORT=%TCPSTARTPORT% -DTCPENDPORT=%TCPENDPORT% -DSHOAL_GROUP_COMMUNICATION_PROVIDER=%TRANSPORT% %MULTICASTADDRESS% %MULTICASTPORT% %MAINCLASS%

goto end

:usage
echo Usage: $0 parameters...
echo The required parameters are :
echo instance_id_token groupname membertype{CORE--or--SPECTATOR} Life-In-Milliseconds> log-level transport{grizzly,jxtanew,jxta} -ts tcpstartport -tp tcpendport  -ma multicastaddress -mp multicastport
echo
echo Life in milliseconds should be either 0 or at least 60000 to demo failure fencing.
echo
echo -ts tcpstartport, -te tcpendport, -ma multicastaddress, -mp multicastport  are optional parameters.
echo Grizzly and jxta transports have different defaults.


:end
endlocal
