#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
echo DIR=$DIR
cd $DIR
DEBUG="-Xdebug -agentlib:jdwp=transport=dt_socket,server=y,address=192.168.200.3:4148,suspend=n"

java $DEBUG -cp target/Modbus2KNX-1.0.0-SNAPSHOT-jar-with-dependencies.jar de.root1.modbus2knx.Modbus2Knx ./SDM630/main.properties ./SDM630/config1.properties ./SDM630/config2.properties
