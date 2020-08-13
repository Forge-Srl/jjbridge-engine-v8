echo "### Cleaning..."
cd /opt/build || exit
rm -rf build
mkdir build
cd build

echo "### Building..."
export CXX="clang"
cmake ..
make && (
  mkdir -p /opt/target/x86_64
  cp libV8-wrapper.dylib /opt/target/x86_64
  cp /opt/build/v8/platforms/macos-x86_64/* /opt/target/x86_64
)