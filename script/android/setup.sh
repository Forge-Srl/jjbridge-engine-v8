#!/bin/bash
echo "### Setting up the environment..."

apt-get update
apt-get -y install \
	cmake \
	clang \
	openjdk-8-jdk \
	unzip

rm -rf /opt/ndk
mkdir /opt/ndk
wget -O ndk.zip https://dl.google.com/android/repository/android-ndk-r20b-linux-x86_64.zip
unzip -o ndk.zip -d /opt/ndk
rm ndk.zip