#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
bash $DIR/update.sh
IP=$(ip -4 a show dev eth0 | sed -En 's/.*inet (addr:)?(([0-9]+\.){3}[0-9])+.*/\2/p')
java -Djava.rmi.server.hostname=$IP -classpath $DIR/DS-SystemY/bin/ be.uantwerpen.ds.system_y.test.NameServerTest
