set curdir=%cd%
cd /D "%~dp0"
"C:\Program Files\git\usr\bin\patch.exe" "C:\tools\vcpkg\installed\x64-windows-static\include\restinio\impl\connection.hpp" "native\connection-counter.patch"
cd ..
cd native
if not exist .\build mkdir build
cd build
cmake .. ^
 -G"Visual Studio 15 2017 Win64" ^
 -DCMAKE_TOOLCHAIN_FILE=C:/Tools/vcpkg/scripts/buildsystems/vcpkg.cmake ^
 -DVCPKG_TARGET_TRIPLET=x64-windows-static
cmake --build . --target dakota --config Release
cmake -- build .
cd %curdir%