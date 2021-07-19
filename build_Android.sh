# Hack for string expansion to array in Zsh like Bash
if type emulate >/dev/null 2>/dev/null; then
  emulate ksh;
fi

[ -d tools ] || mkdir tools
cd tools
[ -d ndk ] || (
  echo "Android NDK not found. Downloading..."

  if [ "$(uname)" == "Darwin" ]; then
    curl -o ndk.zip https://dl.google.com/android/repository/android-ndk-r20b-darwin-x86_64.zip
  else
    curl -o ndk.zip https://dl.google.com/android/repository/android-ndk-r20b-linux-x86_64.zip
  fi
  unzip -o ndk.zip -d ./ndk
  rm ndk.zip
)
ndk_path=$(find "$PWD/ndk" -mindepth 1 -maxdepth 1 -type d)
cd ..

[ -d target ] || mkdir target
cd target
rm -rf jni/android
rm -rf jni/build/android

mkdir -p jni/build/android
cd jni/build/android

architectures="arm64-v8a armeabi-v7a x86_64 x86"
for arch in $architectures; do
  mkdir "$arch"
  (
    cd "$arch" || exit
    cmake ../../../../.. -DJJB_TARGET_PLATFORM=Android -DANDROID_ABI="$arch" -DNDK_PATH="$ndk_path"
    cmake --build . && (
      cd ../../..
      mkdir -p "android/$arch"
      mkdir -p "android_assets"
      cp "build/android/$arch/libV8-wrapper.so" "android/$arch"
      cp "../../jni/v8/platforms/android-$arch"/*.so "android/$arch"
      cp "../../jni/v8/platforms/android-$arch"/*.dat "android_assets"
    )
  )
done