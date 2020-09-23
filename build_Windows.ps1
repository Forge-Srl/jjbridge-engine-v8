If (-Not (Test-Path "target" -PathType Container)) {
    New-Item -ItemType "directory" -Name "target"
}
Set-Location "target"
If (Test-Path "jni\windows" -PathType Container) {
    Remove-Item "jni\windows" -Recurse -Force
}
If (Test-Path "jni\build\windows" -PathType Container) {
    Remove-Item "jni\build\windows" -Recurse -Force
}

New-Item -ItemType "directory" -Name "jni\build\windows"
Set-Location "jni\build\windows"

$srcPath = Resolve-Path '..\..\..\..' | Select-Object -ExpandProperty Path
cmake $srcPath -DJJB_TARGET_PLATFORM=Windows
cmake --build . --config Release
If ($?) {
    Set-Location ..\..
    New-Item -ItemType "directory" -Name "windows\x86_64"

    Copy-Item "build\windows\Release\V8-wrapper.dll" -Destination "windows\x86_64"
    Copy-Item "build\windows\Release\V8-wrapper.lib" -Destination "windows\x86_64"
    Copy-Item "build\windows\Release\V8-wrapper.exp" -Destination "windows\x86_64"
    Copy-Item -Path "..\..\jni\v8\platforms\windows-x86_64\*.dll" -Destination "windows\x86_64"
}
Set-Location $srcPath