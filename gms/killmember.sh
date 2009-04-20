#!/bin/sh +x

PID=`jps -mlVv | grep ApplicationServer | grep ${1} | awk '{print $1}'`
if [ ! -z "$PID" ]; then
   echo "Killing instance PIDs :|$PID|" 
   echo "Date:";date 
   kill -9  $PID 
   jps -v | grep ${1}
else
   echo "The pid was not found"
   exit 1
fi
