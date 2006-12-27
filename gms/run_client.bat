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
java -Dcom.sun.management.jmxremote -DINAME=client%1 -cp .\lib\jxta.jar;dist\shoal-gms.jar com.sun.enterprise.jxtamgmt.ClusterManager
endlocal
