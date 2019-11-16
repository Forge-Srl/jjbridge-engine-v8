cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall -Wno-unused-function -Wno-unused-variable -Wno-unused-result -O3 -fvisibility=hidden -ffunction-sections -fno-data-sections")
set(CMAKE_INSTALL_RPATH ".")
set(CMAKE_INSTALL_RPATH_USE_LINK_PATH TRUE)

find_package(Java 1.8 COMPONENTS Development)
find_package(JNI REQUIRED)

set(SRC_DIR "/opt/build/src")
set(V8_DIR "/opt/build/v8")
set(V8_BINARIES "${V8_DIR}/platforms/linux-x86_64")
set(INSTALL_PREFIX "/opt/target")

include_directories("${V8_DIR}/headers/include"
                    "${PROJECT_BINARY_DIR}"
                    "${PROJECT_SOURCE_DIR}"
                    "${JNI_INCLUDE_DIRS}")

link_directories("${V8_BINARIES}/")

add_library(cpp-shared SHARED IMPORTED)
set_target_properties(cpp-shared PROPERTIES
                      IMPORTED_LOCATION ${V8_BINARIES}/libc++.so)
add_library(v8 SHARED IMPORTED)
set_target_properties(v8 PROPERTIES
                      IMPORTED_LOCATION ${V8_BINARIES}/libv8.so)
add_library(v8-base SHARED IMPORTED)
set_target_properties(v8-base PROPERTIES
                      IMPORTED_LOCATION ${V8_BINARIES}/libv8_libbase.so)
add_library(v8-platform SHARED IMPORTED)
set_target_properties(v8-platform PROPERTIES
                      IMPORTED_LOCATION ${V8_BINARIES}/libv8_libplatform.so)

add_library(V8-wrapper MODULE
            "${SRC_DIR}/V8/Environment.cpp"
            "${SRC_DIR}/V8/Runtime.cpp"
            "${SRC_DIR}/V8/InspectorClient.cpp"
            "${SRC_DIR}/V8-wrapper.cpp")

target_link_libraries(V8-wrapper
                      v8 v8-base v8-platform cpp-shared)

install(DIRECTORY "${V8_BINARIES}/" DESTINATION "${INSTALL_PREFIX}/linux/x86_64")
install(TARGETS V8-wrapper LIBRARY DESTINATION "${INSTALL_PREFIX}/linux/x86_64")
