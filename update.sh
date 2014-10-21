#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo Getting latest version...
cd "$DIR/DS-Nameserver"
git pull ds master
mkdir "$DIR/DS-Nameserver/bin" 2> /dev/null
echo Compiling...
javac -d $DIR/DS-Nameserver/bin/ $DIR/DS-Nameserver/src/be/uantwerpen/ds/ns/*.java $DIR/DS-Nameserver/src/be/uantwerpen/ds/ns/client/*.java $DIR/DS-Nameserver/src/be/uantwerpen/ds/ns/server/*.java $DIR/DS-Nameserver/src/be/uantwerpen/ds/test/*.java

