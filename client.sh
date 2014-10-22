#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
bash "$DIR/update.sh" 
java -classpath "$DIR/DS-SystemY/bin/" be.uantwerpen.ds.test.ClientTest
