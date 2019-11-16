#!/bin/bash
MISSING_FOLDER=1
VAGRANT_ERROR=2

INFO () { echo "[INFO] $*"; }
ERROR () { echo "[ERROR] $*"; }

INFO "==========[ Native Build Start ]=========="
INFO "Native build script running from $PWD"
INFO
cd script || exit $MISSING_FOLDER

export system="linux"
export build_path="$system/x86_64"

if [ -z "$(ls -A "../target/jni/$build_path")" ]; then
    INFO "Build not found for $build_path -> Starting build process"

    cd $system || exit $MISSING_FOLDER
    vagrant up #--provision
    if [ $? -ne 0 ]; then
        vagrant halt
        ERROR "An error occurred while building for $build_path. Please check the full log for more info."
        exit $VAGRANT_ERROR
    else
        vagrant halt
    fi

    cd ..
else
    INFO "Build found for $build_path -> Skipping!"
fi
INFO "==========[ Native Build End ]=========="