#!/bin/bash
cd /opt/build
rm -rf build
mkdir build
cd build

export CXX="clang"

cmake ..
make
make clean
make
make install