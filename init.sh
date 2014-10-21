if [ "$(id -u)" != "0" ]; then
 echo "Are you root?" 
 exit 1;
fi
if [ -z "$1" ]
then
 echo Usage: $0 newname
 exit 1
fi
add-apt-repository ppa:webubd8team/java
apt-get-update
apt-get install oracle-jdk7-installer

echo 'alias ds-client=~/ds-practicum/client.sh' > ~/.bashrc
echo 'alias ds-server=~/ds-practicum/server.sh' > ~/.bashrc

OLD=$(hostname)
NEW=$1

sed -i s/$OLD/$NEW/g /etc/hostname
sed -i s/$OLD/$NEW/g /etc/hosts

read -p "Press Enter to reboot..."
reboot 
