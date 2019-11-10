#!/bin/bash

echo "Native build script running from $PWD"
cd script

export system="linux"
export build_path="$system/x86_64"

if [ -z "$(ls -A "../target/jni/$build_path")" ]; then
   echo "Build not found for $build_path -> Starting build process"

   cd $system
   vagrant up #--provision
   vagrant halt
   cd ..
else
   echo "Build found for $build_path -> Skipping!"
fi