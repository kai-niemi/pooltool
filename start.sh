#!/bin/bash

pid=$(ps -ef | grep "java" | grep "pc.jar" | awk '{print $2}')
if [ ! -x ${pid} ]; then
   echo -e "Existing process found (${pid}) - is it running?"
   exit 1
fi

app_jarfile=pc.jar
if [ ! -f "$app_jarfile" ]; then
    app_jarfile=target/pc.jar
fi

if [ ! -f "$app_jarfile" ]; then
    echo -e "Building jar.."
    ./mvnw clean install
fi

nohup java -jar $app_jarfile $* > pc-stdout.log 2>&1 &

sleep 2

pid=$(ps -ef | grep "java" | grep "pc.jar" | awk '{print $2}')

if [ -x ${pid} ]; then
   echo -e "No pc.jar process found - check pc-stdout.log"
   exit 1
else
   echo -e "Start successful - check pc-stdout.log"
   exit 0
fi
