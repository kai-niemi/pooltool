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

java -jar $app_jarfile $*