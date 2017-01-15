#!/bin/bash

# Get current position
TOP_DIR=$(pwd)

# Add all necessary jars
LIBPATH=libs/java-json.jar:libs/commons-logging-1.2.jar:libs/org.apache.httpcomponents.httpclient_4.5.2.jar:libs/httpcore-4.4.jar

#compile java file
javac -cp $LIBPATH src/com/jam/socket/User.java     -d ./bin/.
javac -cp $LIBPATH src/com/jam/socket/ClientThread.java     -d ./bin/.

javac -cp $TOP_DIR/bin:$LIBPATH src/com/jam/socket/ServerThread.java     -d ./bin/.
javac -cp $TOP_DIR/bin:$LIBPATH src/com/jam/socket/SocketServer.java     -d ./bin/.

javac -cp $TOP_DIR/bin:$LIBPATH src/com/jam/socket/SocketClient.java     -d ./bin/.
