cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall -Wno-unused-function -Wno-unused-variable -Wno-unused-result -O3 -fvisibility=hidden -fexceptions -ffunction-sections -fno-data-sections")
set(CMAKE_SHARED_LINKER_FLAGS "--verbose")
set(CMAKE_INSTALL_RPATH_USE_LINK_PATH TRUE)

find_package(Java 1.8 COMPONENTS Development)
find_package(JNI REQUIRED)

set(SRC_DIR "/opt/build/src")
set(V8_DIR "/opt/build/v8")
set(V8_BINARIES "${V8_DIR}/platforms/macos-x86_64")

add_definitions(-DV8_COMPRESS_POINTERS)

include_directories("${V8_DIR}/headers/include"
                    "${JNI_INCLUDE_DIRS}")

add_library(zlib SHARED IMPORTED)
set_target_properties(zlib PROPERTIES
                      IMPORTED_LOCATION ${V8_BINARIES}/libchrome_zlib.dylib)
add_library(v8 SHARED IMPORTED)
set_target_properties(v8 PROPERTIES
                      IMPORTED_LOCATION ${V8_BINARIES}/libv8.dylib)
add_library(v8-base SHARED IMPORTED)
set_target_properties(v8-base PROPERTIES
                      IMPORTED_LOCATION ${V8_BINARIES}/libv8_libbase.dylib)
add_library(v8-platform SHARED IMPORTED)
set_target_properties(v8-platform PROPERTIES
                      IMPORTED_LOCATION ${V8_BINARIES}/libv8_libplatform.dylib)

add_library(V8-wrapper MODULE
            "${SRC_DIR}/V8/Environment.cpp"
            "${SRC_DIR}/V8/Runtime.cpp"
            "${SRC_DIR}/V8/InspectorClient.cpp"
            "${SRC_DIR}/V8-wrapper.cpp")

target_link_libraries(V8-wrapper
                      v8 v8-base v8-platform zlib)
