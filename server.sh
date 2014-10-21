bash update.sh
IP=$(ip -4 a show dev eth0 | sed -En 's/.*inet (addr:)?(([0-9]+\.){3}[0-9])+.*/\2/p')
java -Djava.rmi.server.hostname=$IP -classpath DS-Nameserver/bin/ be.uantwerpen.ds.test.NameServerTest
