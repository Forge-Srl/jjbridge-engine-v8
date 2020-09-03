# Hack for string expansion to array in Zsh like Bash
if type emulate >/dev/null 2>/dev/null; then
  emulate ksh;
fi

# --- Definitions ---
# Error codes
MISSING_FOLDER=1

# Other variables
project_dir="$PWD"
operating_systems="macos android" #"macos windows linux android"

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
    sh ./build.sh
    cd ..
  else
    INFO "Build found for '$os_name' -> Skipping!"
  fi
done
INFO "==========[ Native Build End ]=========="