If (-Not (Test-Path "tools" -PathType Container)) {
    New-Item -ItemType "directory" -Name "tools"
}
Set-Location "tools"
If (-Not (Test-Path "ndk" -PathType Container)) {
    Write-Output "Android NDK not found. Downloading..."

    Invoke-WebRequest https://dl.google.com/android/repository/android-ndk-r20b-windows-x86_64.zip -OutFile ndk.zip
    Expand-Archive -Path ndk.zip -DestinationPath ".\ndk"
    Remove-Item ndk.zip
}
$ndk_path = Get-ChildItem -Path "ndk" -Depth 0 -Directory | Select-Object -ExpandProperty FullName
Set-Location ".."

If (-Not (Test-Path "target" -PathType Container)) {
    New-Item -ItemType "directory" -Name "target"
}
Set-Location "target"
If (Test-Path "jni\android" -PathType Container) {
    Remove-Item "jni\android" -Recurse -Force
}
If (Test-Path "jni\build\android" -PathType Container) {
    Remove-Item "jni\build\android" -Recurse -Force
}

New-Item -ItemType "directory" -Name "jni\build\android"
Set-Location "jni\build\android"

$ninja_path = Get-ChildItem -Path 'C:\Program Files (x86)\Microsoft Visual Studio\*' -Recurse -Force -Include ninja.exe | Select-Object -ExpandProperty FullName

$architectures = @("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
foreach ($arch in $architectures) {
    New-Item -ItemType "directory" -Name $arch
    Push-Location $arch

    $srcPath = Resolve-Path '..\..\..\..\..' | Select-Object -ExpandProperty Path
    cmake $srcPath -G Ninja -DCMAKE_MAKE_PROGRAM="$ninja_path" -DJJB_TARGET_PLATFORM=Android -DANDROID_ABI="$arch" -DNDK_PATH="$ndk_path"
    cmake --build .
    If ($?) {
        Set-Location ..\..\..
        New-Item -ItemType "directory" -Name "android\$arch"
        New-Item -ItemType "directory" -Name "android_assets"

        Copy-Item "build\android\$arch\libV8-wrapper.so" -Destination "android\$arch"
        Copy-Item -Path "..\..\jni\v8\platforms\android-$arch\*.so" -Destination "android\$arch"
        Copy-Item -Path "..\..\jni\v8\platforms\android-$arch\*.dat" -Destination "android_assets"
    }
    Pop-Location
}