#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
echo DIR=$DIR
cd $DIR

java -cp target/Modbus2KNX-1.0.0-SNAPSHOT-jar-with-dependencies.jar de.root1.modbus2knx.Modbus2Knx ./WPM\ Firmware\ L/configdata.properties
