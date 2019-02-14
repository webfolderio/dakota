set curdir=%cd%
cd /D "%~dp0"
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
cmake --build . --target dakota --config Debug
cmake -- build .
echo %cd%
copy /Y Release\dakota.dll ..\..\src\main\resources\META-INF
cd %curdir%