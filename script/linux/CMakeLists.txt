cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall -Wno-unused-function -Wno-unused-variable -Wno-unused-result -O3 -fvisibility=hidden -ffunction-sections -fno-data-sections")
set(JAVA_INCLUDE "/usr/lib/jvm/java-8-openjdk-amd64/include/")
set(JAVA_INCLUDE_OS "/usr/lib/jvm/java-8-openjdk-amd64/include/linux")
set(SRC_DIR "/opt/build/src")
set(V8_DIR "/opt/build/v8")
set(V8_BINARIES "${V8_DIR}/platforms/linux-x86_64")
set(INSTALL_PREFIX "/opt/target")

#add_library(cpp-shared SHARED IMPORTED)
#set_target_properties(cpp-shared PROPERTIES
#                      IMPORTED_LOCATION ${V8_BINARIES}/libc++.so)
add_library(v8 SHARED IMPORTED)
set_target_properties(v8 PROPERTIES
                      IMPORTED_LOCATION ${V8_BINARIES}/libv8.cr.so)
add_library(v8-base SHARED IMPORTED)
set_target_properties(v8-base PROPERTIES
                      IMPORTED_LOCATION ${V8_BINARIES}/libv8_libbase.cr.so)
add_library(v8-platform SHARED IMPORTED)
set_target_properties(v8-platform PROPERTIES
                      IMPORTED_LOCATION ${V8_BINARIES}/libv8_libplatform.cr.so)

include_directories("${V8_DIR}/headers/include" "${PROJECT_BINARY_DIR}" "${PROJECT_SOURCE_DIR}" "${JAVA_INCLUDE}")

link_directories("${V8_BINARIES}")
include_directories("${JAVA_INCLUDE_OS}")

add_library(V8-wrapper SHARED
            "${SRC_DIR}/V8/Environment.cpp"
            "${SRC_DIR}/V8/Runtime.cpp"
            "${SRC_DIR}/V8/InspectorClient.cpp"
            "${SRC_DIR}/V8-wrapper.cpp")

target_link_libraries(V8-wrapper
                      v8 v8-base v8-platform)

install(DIRECTORY "${V8_BINARIES}/" DESTINATION "${INSTALL_PREFIX}/linux/x86_64")
install(TARGETS V8-wrapper DESTINATION "${INSTALL_PREFIX}/linux/x86_64")

