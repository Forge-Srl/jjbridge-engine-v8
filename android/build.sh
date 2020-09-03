[ -d target ] || mkdir target
cd target

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

rm -rf native
mkdir native
cd native

architectures="arm64-v8a armeabi-v7a x86_64 x86"
for arch in $architectures; do
  mkdir "$arch"
  (
    cd "$arch" || exit
    cmake ../../../.. -DJJB_TARGET_PLATFORM=Android -DANDROID_ABI="$arch" -DNDK_PATH="$ndk_path"
    make && (
      cd ../../..
      mkdir -p "jni/$arch"
      cp "target/native/$arch/libV8-wrapper.so" "jni/$arch"
      cp "../jni/v8/platforms/android-$arch"/* "jni/$arch"
    )
  )
done
