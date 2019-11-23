#!/bin/bash
# --- Definitions ---
# Error codes
MISSING_FOLDER=1
VAGRANT_ERROR=2

# Other variables
project_dir="$PWD"
operating_systems="linux android"

# Utility functions
INFO () { echo "[INFO] $*"; }
ERROR () { echo "[ERROR] $*"; }
# --- Definitions End ---

INFO "==========[ Native Build Start ]=========="
INFO "Native build script running from $project_dir"
INFO

cd script || exit $MISSING_FOLDER

for build_path in $operating_systems; do
  if [ -z "$(ls -A "$project_dir/target/jni/$build_path")" ]; then
    INFO "Build not found for $build_path -> Starting build process"

    cd "$build_path" || exit $MISSING_FOLDER

    export VAGRANT_DOTFILE_PATH="$project_dir/target/vagrant/$build_path"
    vagrant up #--provision
    if [ $? -ne 0 ]; then
        vagrant halt
        ERROR "An error occurred while building for $build_path. Please check the full log for more info."
        ERROR "For vagrant folder see $VAGRANT_DOTFILE_PATH"
        exit $VAGRANT_ERROR
    else
        vagrant halt
    fi

    cd ..
  else
    INFO "Build found for $build_path -> Skipping!"
  fi
done
INFO "==========[ Native Build End ]=========="