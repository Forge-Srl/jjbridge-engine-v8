# Usage:
#
# cmake <...> -DJJB_TARGET_PLATFORM=Linux
# cmake <...> -DJJB_TARGET_PLATFORM=Windows
# cmake <...> -DJJB_TARGET_PLATFORM=macOS
# cmake <...> -DJJB_TARGET_PLATFORM=Android -DANDROID_ABI=x86_64      -DNDK_PATH=<...>
# cmake <...> -DJJB_TARGET_PLATFORM=Android -DANDROID_ABI=x86         -DNDK_PATH=<...>
# cmake <...> -DJJB_TARGET_PLATFORM=Android -DANDROID_ABI=arm64-v8a   -DNDK_PATH=<...>
# cmake <...> -DJJB_TARGET_PLATFORM=Android -DANDROID_ABI=armeabi-v7a -DNDK_PATH=<...>

cmake_minimum_required(VERSION 3.17.4)

set(CMAKE_C_COMPILER clang)
set(CMAKE_CXX_COMPILER clang++)

if(JJB_TARGET_PLATFORM STREQUAL "Android")
    set(CMAKE_TOOLCHAIN_FILE "${NDK_PATH}/build/cmake/android.toolchain.cmake")
    set(CMAKE_ANDROID_NDK "${NDK_PATH}")
    set(ANDROID_NATIVE_API_LEVEL 21)
    set(CMAKE_SYSTEM_VERSION 21)
endif()

# WARNING:
# Keep all the previous settings before project() otherwise they will be ignored!

project(JJBridge_V8)
set(SRC_DIR "${CMAKE_CURRENT_SOURCE_DIR}/core/src/main/cpp")
set(V8_DIR "${CMAKE_CURRENT_SOURCE_DIR}/jni/v8")

if(JJB_TARGET_PLATFORM STREQUAL "Linux")
    message("Building Linux x86_64")

    set(CLANG_ARCH_TRIPLE x86_64-pc-linux-gnu)
    set(CMAKE_SYSTEM_NAME Linux)
    set(CMAKE_SYSTEM_PROCESSOR x86_64)

    add_definitions(-DV8_COMPRESS_POINTERS)
    set(V8_BINARIES "${V8_DIR}/platforms/linux-x86_64")

elseif(JJB_TARGET_PLATFORM STREQUAL "Windows")
    message("Building Windows x86_64")

    set(CLANG_ARCH_TRIPLE x86_64-pc-windows)
    set(CMAKE_SYSTEM_NAME Windows)
    set(CMAKE_SYSTEM_PROCESSOR x86_64)

    add_definitions(-DV8_COMPRESS_POINTERS)
    set(V8_BINARIES "${V8_DIR}/platforms/windows-x86_64")

elseif(JJB_TARGET_PLATFORM STREQUAL "macOS")
    message("Building macOS x86_64")

    set(CLANG_ARCH_TRIPLE x86_64-apple-darwin)
    set(CMAKE_SYSTEM_NAME Darwin)
    set(CMAKE_SYSTEM_PROCESSOR x86_64)

    add_definitions(-DV8_COMPRESS_POINTERS)
    set(V8_BINARIES "${V8_DIR}/platforms/macos-x86_64")

elseif(JJB_TARGET_PLATFORM STREQUAL "Android")
    message("Building Android ${ANDROID_ABI}")

    set(CMAKE_SYSTEM_NAME Android)

    if((ANDROID_ABI STREQUAL "x86_64") OR (ANDROID_ABI STREQUAL "arm64-v8a"))
        add_definitions(-DV8_COMPRESS_POINTERS)
    elseif((ANDROID_ABI STREQUAL "x86") OR (ANDROID_ABI STREQUAL "armeabi-v7a"))
        # nothing to do here
    else()
        if(ANDROID_ABI)
            message(FATAL_ERROR "Invalid Android ABI: ${ANDROID_ABI}")
        else()
            message(FATAL_ERROR "Missing Android ABI. Please specify using -DANDROID_ABI")
        endif()
    endif()
    set(CMAKE_ANDROID_ARCH_ABI "${ANDROID_ABI}")
    set(V8_BINARIES "${V8_DIR}/platforms/android-${ANDROID_ABI}")

else()
    if(JJB_TARGET_PLATFORM)
        message(FATAL_ERROR "Invalid platform: ${JJB_TARGET_PLATFORM}")
    else()
        message(FATAL_ERROR "Missing target platform. Please specify using -DJJB_TARGET_PLATFORM")
    endif()
endif()

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall -Wno-unused-function -Wno-unused-variable -Wno-unused-result -O3 -fvisibility=hidden -fexceptions -ffunction-sections -fno-data-sections")
if(JJB_TARGET_PLATFORM STREQUAL "Linux")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -stdlib=libstdc++")
elseif(JJB_TARGET_PLATFORM STREQUAL "macOS")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -stdlib=libc++")
endif()
set(CMAKE_SHARED_LINKER_FLAGS "--verbose")
set(CMAKE_BUILD_TYPE Release)

if(NOT (JJB_TARGET_PLATFORM STREQUAL "Android"))
    set(CMAKE_C_COMPILER_TARGET ${CLANG_ARCH_TRIPLE})
    set(CMAKE_CXX_COMPILER_TARGET ${CLANG_ARCH_TRIPLE})
    set(CMAKE_INSTALL_RPATH_USE_LINK_PATH TRUE)

    # TODO: bisogna avere gli header java per queste piattaforme target!
    #       Vedi: https://bsideup.github.io/posts/jni-cross-compilation/
    #       Vedi: https://adoptopenjdk.net/installation.html?variant=openjdk8&jvmVariant=hotspot#archives
    #       Sarà da risolvere qualora cross-compileremo da e verso tutte le piattaforme
    #       Per il momento è sufficiente avere la JDK installata.
    find_package(Java 1.8 COMPONENTS Development)
    find_package(JNI REQUIRED)
    include_directories("${JNI_INCLUDE_DIRS}")
else()
    set(CMAKE_ANDROID_STL_TYPE c++_shared)
    set(ANDROID_ALLOW_UNDEFINED_SYMBOLS FALSE)
endif()


##############################################################################
# Add all source files and external libraries

include_directories("${V8_DIR}/headers/include")
add_library(V8-wrapper SHARED
            "${SRC_DIR}/V8/Environment.cpp"
            "${SRC_DIR}/V8/Runtime.cpp"
            "${SRC_DIR}/V8/InspectorClient.cpp"
            "${SRC_DIR}/V8-wrapper.cpp")

if(JJB_TARGET_PLATFORM STREQUAL "Linux")
    add_library(zlib SHARED IMPORTED)
    set_target_properties(zlib PROPERTIES IMPORTED_LOCATION ${V8_BINARIES}/libchrome_zlib.so)
    add_library(v8 SHARED IMPORTED)
    set_target_properties(v8 PROPERTIES IMPORTED_LOCATION ${V8_BINARIES}/libv8.so)
    add_library(v8-base SHARED IMPORTED)
    set_target_properties(v8-base PROPERTIES IMPORTED_LOCATION ${V8_BINARIES}/libv8_libbase.so)
    add_library(v8-platform SHARED IMPORTED)
    set_target_properties(v8-platform PROPERTIES IMPORTED_LOCATION ${V8_BINARIES}/libv8_libplatform.so)

    target_link_libraries(V8-wrapper v8 v8-base v8-platform zlib)

elseif(JJB_TARGET_PLATFORM STREQUAL "macOS")
    add_library(zlib SHARED IMPORTED)
    set_target_properties(zlib PROPERTIES IMPORTED_LOCATION ${V8_BINARIES}/libchrome_zlib.dylib)
    add_library(v8 SHARED IMPORTED)
    set_target_properties(v8 PROPERTIES IMPORTED_LOCATION ${V8_BINARIES}/libv8.dylib)
    add_library(v8-base SHARED IMPORTED)
    set_target_properties(v8-base PROPERTIES IMPORTED_LOCATION ${V8_BINARIES}/libv8_libbase.dylib)
    add_library(v8-platform SHARED IMPORTED)
    set_target_properties(v8-platform PROPERTIES IMPORTED_LOCATION ${V8_BINARIES}/libv8_libplatform.dylib)

    target_link_libraries(V8-wrapper v8 v8-base v8-platform zlib)

elseif(JJB_TARGET_PLATFORM STREQUAL "Android")
    add_library(cpp-shared SHARED IMPORTED)
    set_target_properties(cpp-shared PROPERTIES IMPORTED_LOCATION ${V8_BINARIES}/libc++_shared.so)
    add_library(v8 SHARED IMPORTED)
    set_target_properties(v8 PROPERTIES IMPORTED_LOCATION ${V8_BINARIES}/libv8.cr.so)
    add_library(v8-base SHARED IMPORTED)
    set_target_properties(v8-base PROPERTIES IMPORTED_LOCATION ${V8_BINARIES}/libv8_libbase.cr.so)
    add_library(v8-platform SHARED IMPORTED)
    set_target_properties(v8-platform PROPERTIES IMPORTED_LOCATION ${V8_BINARIES}/libv8_libplatform.cr.so)
    add_library(zlib SHARED IMPORTED)
    set_target_properties(zlib PROPERTIES IMPORTED_LOCATION ${V8_BINARIES}/libchrome_zlib.cr.so)

    target_link_libraries(V8-wrapper v8 v8-base v8-platform cpp-shared zlib)

elseif(JJB_TARGET_PLATFORM STREQUAL "Windows")
    # TODO

endif()