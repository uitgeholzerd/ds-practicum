#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo Getting latest version...
git pull ds master
mkdir "$DIR/DS-Nameserver/bin" 2> /dev/null
echo Compiling...
javac -d "$DIR/bin/" "$DIR/src/be/uantwerpen/ds/ns/*.java" "$DIR/src/be/uantwerpen/ds/ns/client/*.java" "$DIR/src/be/uantwerpen/ds/ns/server/*.java" "$DIR/src/be/uantwerpen/ds/test/*.java"

