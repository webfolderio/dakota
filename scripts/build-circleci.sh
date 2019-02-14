cd ..
cd native
mkdir build
cd build
CC=gcc-8 CXX=g++-8 cmake .. -DCMAKE_TOOLCHAIN_FILE=/home/circleci/repo/vcpkg/scripts/buildsystems/vcpkg.cmake -DVCPKG_TARGET_TRIPLET=x64-linux
cmake .. -DCMAKE_BUILD_TYPE=Release
make
copy /Y Release\dakota.dll ..\..\src\main\resources\META-INF