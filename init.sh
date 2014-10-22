#!/bin/bash
if [ "$(id -u)" != "0" ]; then
 echo "Are you root?" 
 exit 1;
fi
if [ -z "$1" ]
then
 echo Usage: $0 newname
 exit 1
fi
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"


echo alias "ds-client=$DIR/client.sh" >> ~/.bashrc
echo alias "ds-server=$DIR/server.sh" >> ~/.bashrc

cd $DIR
git remote add ds https://github.com/uitgeholzerd/ds-practicum.git
OLD=$(hostname)
NEW=$1

sed -i s/$OLD/$NEW/g /etc/hostname
sed -i s/$OLD/$NEW/g /etc/hosts

read -p "Press Enter to reboot..."
reboot 
