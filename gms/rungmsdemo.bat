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
