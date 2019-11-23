#!/bin/bash
echo "### Cleaning..."
cd /opt/build || exit
rm -rf build
mkdir build
cd build

echo "### Building..."
export CXX="clang"
cmake ..
make && (
  mkdir -p /opt/target/linux/x86_64
  cp libV8-wrapper.so /opt/target/linux/x86_64
  cp /opt/build/v8/platforms/linux-x86_64/* /opt/target/linux/x86_64
)