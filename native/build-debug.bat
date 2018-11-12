if not exist .\build-debug mkdir build-debug
cd build-debug
cmake .. ^
 -G"Visual Studio 15 2017 Win64" ^
 -DCMAKE_TOOLCHAIN_FILE=C:\vcpkg\scripts\buildsystems\vcpkg.cmake ^
 -DVCPKG_TARGET_TRIPLET=x64-windows-static
cmake --build . --config Debug 