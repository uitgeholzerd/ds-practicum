#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo Getting latest version...
cd "$DIR/DS-SystemY"
git pull ds master
mkdir "$DIR/DS-SystemY/bin" 2> /dev/null
echo Compiling...
javac -d $DIR/DS-SystemY/bin/ $DIR/DS-SystemY/src/be/uantwerpen/ds/ns/*.java $DIR/DS-SystemY/src/be/uantwerpen/ds/ns/client/*.java $DIR/DS-SystemY/src/be/uantwerpen/ds/ns/server/*.java $DIR/DS-SystemY/src/be/uantwerpen/ds/test/*.java

