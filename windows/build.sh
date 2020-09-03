[ -d target ] || mkdir target
cd target
rm -rf native
mkdir native
cd native

cmake ../../.. -DJJB_TARGET_PLATFORM=Windows
make && (
  cd ../..
  mkdir -p jni/x86_64
  cp target/native/libV8-wrapper.dll jni/x86_64
  cp ../jni/v8/platforms/windows-x86_64/* jni/x86_64
)