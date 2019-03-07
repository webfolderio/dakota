set curdir=%cd%
cd /D "%~dp0"
cd ..
"C:\Program Files\git\usr\bin\patch.exe" "C:\tools\vcpkg\installed\x64-windows-static\include\restinio\impl\connection.hpp" "native\connection-counter.patch"
cd %curdir%