#!/bin/sh
echo Getting latest version...
git pull ds master
cd DS-Nameserver
echo Compiling...
javac -d bin/ src/be/uantwerpen/ds/ns/*.java src/be/uantwerpen/ds/ns/client/*.java src/be/uantwerpen/ds/ns/server/*.java src/be/uantwerpen/ds/test/*.java 

