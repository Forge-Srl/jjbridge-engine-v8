# Hack for string expansion to array in Zsh like Bash
if type emulate >/dev/null 2>/dev/null; then
  emulate ksh;
fi

[ -d target ] || mkdir target
cd target
rm -rf jni/macos
rm -rf jni/build/macos

mkdir -p jni/build/macos
cd jni/build/macos

cmake ../../../.. -DJJB_TARGET_PLATFORM=macOS
cmake --build . && (
  cd ../..
  mkdir -p macos/x86_64
  cp build/macos/libV8-wrapper.dylib macos/x86_64
  cp ../../jni/v8/platforms/macos-x86_64/* macos/x86_64
  cd macos/x86_64
  install_name_tool -change "@rpath/libv8.dylib" "@loader_path/libv8.dylib" libV8-wrapper.dylib
  install_name_tool -change "@rpath/libv8_libbase.dylib" "@loader_path/libv8_libbase.dylib" libV8-wrapper.dylib
  install_name_tool -change "@rpath/libv8_libplatform.dylib" "@loader_path/libv8_libplatform.dylib" libV8-wrapper.dylib
  install_name_tool -change "@rpath/libchrome_zlib.dylib" "@loader_path/libchrome_zlib.dylib" libV8-wrapper.dylib
)