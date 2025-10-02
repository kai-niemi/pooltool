#!/bin/bash

pid=$(ps -ef | grep "java" | grep "pool-tool.jar" | awk '{print $2}')
if [ ! -x ${pid} ]; then
   echo -e "Existing process found (${pid}) - is it running?"
   exit 1
fi

app_jarfile=pool-tool.jar
if [ ! -f "$app_jarfile" ]; then
    app_jarfile=target/pool-tool.jar
fi

if [ ! -f "$app_jarfile" ]; then
    echo -e "Building jar.."
    ./mvnw clean install
fi

nohup java -jar $app_jarfile $* > pool-tool-stdout.log 2>&1 &

sleep 2

pid=$(ps -ef | grep "java" | grep "pool-tool" | awk '{print $2}')

if [ -x ${pid} ]; then
   echo -e "No pool-tool.jar process found - check pool-tool-stdout.log"
   exit 1
else
   echo -e "Start successful - check pool-tool-stdout.log"
   exit 0
fi
