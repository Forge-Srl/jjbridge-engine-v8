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

for os_name in $operating_systems; do
  if [ -z "$(ls -A "$project_dir/$os_name/jni")" ]; then
    INFO "Build not found for '$os_name' -> Starting build process"

    cd "$os_name" || exit $MISSING_FOLDER

    export VAGRANT_DOTFILE_PATH="$project_dir/$os_name/target/vagrant"
    vagrant up #--provision
    if [ $? -ne 0 ]; then
        vagrant halt
        ERROR "An error occurred while building for '$os_name'. Please check the full log for more info."
        ERROR "For vagrant folder see $VAGRANT_DOTFILE_PATH"
        exit $VAGRANT_ERROR
    else
        vagrant halt
    fi

    cd ..
  else
    INFO "Build found for '$os_name' -> Skipping!"
  fi
done
INFO "==========[ Native Build End ]=========="