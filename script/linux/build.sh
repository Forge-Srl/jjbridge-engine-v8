#!/bin/bash
echo "### Cleaning..."
cd /opt/build || exit
rm -rf build
mkdir build
cd build

echo "### Building..."
export CXX="clang"
cmake ..
make && make clean && make && make install