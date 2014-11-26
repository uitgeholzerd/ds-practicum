#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
bash "$DIR/update.sh" 
if [ $? != 0 ]; then
 echo "Update/compile failed" 1>&2
 exit 1
fi

java -classpath "$DIR/DS-SystemY/bin/" be.uantwerpen.ds.system_y.test.ClientTest
