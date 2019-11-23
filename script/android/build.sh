#!/bin/bash
echo "### Cleaning..."
cd /opt/build || exit
rm -rf build
mkdir build
cd build

architectures="armeabi-v7a arm64-v8a x86 x86_64"
ndk_path=$(find /opt/ndk -mindepth 1 -maxdepth 1 -type d)
export CXX="clang"

for arch in $architectures; do
  echo "### Building $arch..."
  mkdir "$arch"
  (
    cd "$arch" || exit
    cmake ../.. \
      -DCMAKE_SYSTEM_NAME=Android \
      -DANDROID_TOOLCHAIN=clang \
      -DCMAKE_SYSTEM_VERSION=21 \
      -DANDROID_NATIVE_API_LEVEL=21 \
      -DCMAKE_ANDROID_ARCH_ABI="$arch" \
      -DCMAKE_TOOLCHAIN_FILE="$ndk_path/build/cmake/android.toolchain.cmake" \
      -DCMAKE_ANDROID_NDK="$ndk_path"
    make && (
      mkdir -p "/opt/target/android/$arch"
      cp libV8-wrapper.so "/opt/target/android/$arch"
      cp "/opt/build/v8/platforms/android-$arch"/* "/opt/target/android/$arch"
    )
  )
done
