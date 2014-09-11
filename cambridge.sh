#!/bin/bash

# Cambridge
#
# This script should get Wit.ai's office system up and running on a new Raspberry Pi.
# It takes ~20min to run and you will need to configure a few settings (spotify username, etc.)
#
# Hardware:
# - 1 Raspberry Pi with a microphone and speakers
# - 1 Philips Hue
# - Wit
# - HipChat
# - a Spotify premium account
#
# Before that:
# - install Raspbian
# - force audio output to jack 3.5mm
# - configure wifi, e.g. in /etc/wpa_supplicant/wpa_supplicant.conf:
#   ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev
#   update_config=1
#   
#   network={
#     ssid="myssid"
#     psk="mypwd"
#     proto=RSN
#     key_mgmt=WPA-PSK
#     pairwise=CCMP
#     auth_alg=OPEN
#   }
#
# wit instance id: 53dd3dff-274f-4c7a-ba2c-d25cd22d4651

set -e -x

TMP=~/tmp_cambridge
CHERRY_CONFIG=$TMP/config.json

mkdir -p $TMP
cd $TMP

# install required packages
apt-get update
apt-get -y install git curl cmake libao-dev libdbus-glib-1-dev libsox-dev \
  libjson-glib-dev libnotify-dev libsoup2.4-dev

# install libspotify for spop
SPOTIFY=libspotify-12.1.103-Linux-armv6-bcm2708hardfp-release
curl -L -O "https://developer.spotify.com/download/libspotify/$SPOTIFY.tar.gz"
tar xzvf $SPOTIFY.tar.gz
cd $SPOTIFY
make install
cd -

# install spop
SPOP_CONFIG=~/.config/spop/spopd.conf
mkdir -p ~/.config/spop
curl -L -o $SPOP_CONFIG https://raw.githubusercontent.com/Schnouki/spop/master/spopd.conf.sample

if [ -d ./spop ]; then
  cd spop
  git pull
else
  git clone https://github.com/Schnouki/spop.git
  cd spop
fi

./build_and_run
cd -

# install node.js
NODE=node_latest_armhf.deb
curl -L -O http://node-arm.herokuapp.com/$NODE
dpkg -i $NODE

# install cherry and cherry-spotify
chown -R $SUDO_USER /usr/local
su $SUDO_USER -c 'npm install -g cherry-core cherry-spotify'

# install witd
curl -L -o witd https://github.com/wit-ai/witd/raw/master/witd-arm

cat > $CHERRY_CONFIG <<EOF
{
  "port": 4433,
  "witd_url": "http://localhost:9877",
  "wit_token": "MY_WIT_TOKEN",
  "hipchat_jid": "38802_1058254@chat.hipchat.com",
  "hipchat_pwd": "my_hipchat_pw",
  "hipchat_room": "38802_witmusic@conf.hipchat.com/Electric Cherry",
  "hue_host": "http://192.168.1.169",
  "hue_user": "my_hue_user",
  "spop_ip": "192.168.1.68",
  "spop_port": 6602,
  "tts": {
    "host": "mac_ip",
    "port": 1334
  },
  "gpio_pins": {
    "22": ["in", "both"]
  },
  "demo_port": 5576,
  "unused": [
    "cherry.integration.myo",
    "./examples/light_switch.js"
  ],
  "plugins": [
    "cherry-spotify",
    "./tts.js",
    "cherry.integration.hipchat",
    "cherry.integration.wit",
    "cherry.integration.hue",
    "cherry.integration.gpio",
    "cherry.integration.demo_home",
    "cherry.contrib.cambridge"
  ]
}
EOF

set +x

echo "Yay, almost done! You still need to:"
echo "- edit spop config: $SPOP_CONFIG"
echo "- edit cherry config: $CHERRY_CONFIG"
echo "- run witd and spop"
echo ""
echo "You can then run: cherry $CHERRY_CONFIG"
