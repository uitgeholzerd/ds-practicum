#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo Getting latest version...
cd "$DIR/DS-SystemY"
git pull ds master
mkdir "$DIR/DS-SystemY/bin" 2> /dev/null
echo Compiling...
javac -d $DIR/DS-SystemY/bin/ $DIR/DS-SystemY/src/be/uantwerpen/ds/system_y/agents/*.java $DIR/DS-SystemY/src/be/uantwerpen/ds/system_y/client/*.java $DIR/DS-SystemY/src/be/uantwerpen/ds/system_y/server/*.java $DIR/DS-SystemY/src/be/uantwerpen/ds/system_y/test/*.java  $DIR/DS-SystemY/src/be/uantwerpen/ds/system_y/file/*.java $DIR/DS-SystemY/src/be/uantwerpen/ds/system_y/connection/*.java

