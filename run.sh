#!/bin/bash

# Get current position
TOP_DIR=$(pwd)

# Add all necessary jars
LIBPATH=libs/java-json.jar:libs/commons-logging-1.2.jar:libs/org.apache.httpcomponents.httpclient_4.5.2.jar:libs/httpcore-4.4.jar

#run program
java -cp $TOP_DIR/bin:$LIBPATH com/jam/socket/SocketServer
