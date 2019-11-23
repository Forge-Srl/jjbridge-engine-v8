cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall -Wno-unused-function -Wno-unused-variable -Wno-unused-result -O3 -fvisibility=hidden -fexceptions -ffunction-sections -fno-data-sections")
set(CMAKE_BUILD_TYPE Release)
set(CMAKE_ANDROID_STL_TYPE c++_shared)
set(ANDROID_ALLOW_UNDEFINED_SYMBOLS FALSE)

set(SRC_DIR "/opt/build/src")
set(V8_DIR "/opt/build/v8")
set(V8_BINARIES "${V8_DIR}/platforms/android-${ANDROID_ABI}")

include_directories("${V8_DIR}/headers/include")

add_library(cpp-shared SHARED IMPORTED)
set_target_properties(cpp-shared PROPERTIES
                      IMPORTED_LOCATION ${V8_BINARIES}/libc++_shared.so)
add_library(v8 SHARED IMPORTED)
set_target_properties(v8 PROPERTIES
                      IMPORTED_LOCATION ${V8_BINARIES}/libv8.cr.so)
add_library(v8-base SHARED IMPORTED)
set_target_properties(v8-base PROPERTIES
                      IMPORTED_LOCATION ${V8_BINARIES}/libv8_libbase.cr.so)
add_library(v8-platform SHARED IMPORTED)
set_target_properties(v8-platform PROPERTIES
                      IMPORTED_LOCATION ${V8_BINARIES}/libv8_libplatform.cr.so)

add_library(V8-wrapper SHARED
            "${SRC_DIR}/V8/Environment.cpp"
            "${SRC_DIR}/V8/Runtime.cpp"
            "${SRC_DIR}/V8/InspectorClient.cpp"
            "${SRC_DIR}/V8-wrapper.cpp")

target_link_libraries(V8-wrapper
                      v8 v8-base v8-platform cpp-shared)
