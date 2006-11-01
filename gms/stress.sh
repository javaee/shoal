#!/bin/sh
rm -rf logs
mkdir -p logs
iterations=$1
count=0
while [ $count -lt 6 ]
do
	echo Running instance $count 
	pid=exec java -Dcom.sun.management.jmxremote -DINAME=client$count -cp ./lib/jxta.jar:./lib/log4j.jar:./lib/bcprov-jdk14.jar:dist/shoal-gms.jar com.sun.enterprise.jxtamgmt.ClusterManager >& logs/client$count &
	sleep 2s
	echo $pid
	count=`expr $count + 1`
done

