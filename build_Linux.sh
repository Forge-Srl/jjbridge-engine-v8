# Hack for string expansion to array in Zsh like Bash
if type emulate >/dev/null 2>/dev/null; then
  emulate ksh;
fi

[ -d target ] || mkdir target
cd target
rm -rf jni/linux
rm -rf jni/build/linux

mkdir -p jni/build/linux
cd jni/build/linux

cmake ../../../.. -DJJB_TARGET_PLATFORM=Linux
cmake --build . && (
  cd ../..
  mkdir -p linux/x86_64
  cp build/linux/libV8-wrapper.so linux/x86_64
  cp ../../jni/v8/platforms/linux-x86_64/* linux/x86_64
)