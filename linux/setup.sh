#!/bin/bash
echo "### Setting up the environment..."

apt-get update
apt-get -y install \
	cmake \
	clang \
	openjdk-8-jdk