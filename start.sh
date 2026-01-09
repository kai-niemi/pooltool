#!/bin/bash

pid=$(ps -ef | grep "java" | grep "pooltool.jar" | awk '{print $2}')
if [ ! -x ${pid} ]; then
   echo -e "Existing process found (${pid}) - is it running?"
   exit 1
fi

app_jarfile=pooltool.jar
if [ ! -f "$app_jarfile" ]; then
    app_jarfile=target/pooltool.jar
fi

if [ ! -f "$app_jarfile" ]; then
    echo -e "Building jar.."
    ./mvnw clean install
fi

nohup java -jar $app_jarfile $* > pooltool-stdout.log 2>&1 &

sleep 2

pid=$(ps -ef | grep "java" | grep "pooltool" | awk '{print $2}')

if [ -x ${pid} ]; then
   echo -e "No pooltool.jar process found - check pooltool-stdout.log"
   exit 1
else
   echo -e "Start successful - check pooltool-stdout.log"
   exit 0
fi
