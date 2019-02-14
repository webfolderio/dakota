cmake_minimum_required(VERSION 3.8.0)

project(dakota)

set(VCPKG_TARGET_ARCHITECTURE x64)
set(VCPKG_LIBRARY_LINKAGE static)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(THREADS_PREFER_PTHREAD_FLAG ON)
set(CMAKE_SUPPRESS_REGENERATION ON)

if(MSVC)
    set(CMAKE_CXX_FLAGS_RELEASE "/MT")
    set(CMAKE_CXX_FLAGS_DEBUG "/ZI /Od /MTd")
    set_source_files_properties(stdafx.cpp PROPERTIES COMPILE_FLAGS "/Ycstdafx.h")
    set_source_files_properties(dakota.cpp PROPERTIES COMPILE_FLAGS "/Yustdafx.h")
    add_definitions(-D_WIN32_WINNT=0x0601)
endif()

find_package(JNI REQUIRED)
find_package(unofficial-http-parser REQUIRED)
find_package(fmt REQUIRED)
find_package(restinio REQUIRED)
find_package(Threads REQUIRED)

message(STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
message(STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
message(STATUS "JAVA_INCLUDE_PATH=${JAVA_INCLUDE_PATH}")
message(STATUS "JAVA_INCLUDE_PATH2=${JAVA_INCLUDE_PATH2}")

INCLUDE_DIRECTORIES(${JAVA_INCLUDE_PATH})
INCLUDE_DIRECTORIES(${JAVA_INCLUDE_PATH2})

add_library(dakota SHARED stdafx.h stdafx.cpp dakota.cpp)

target_link_libraries(dakota PRIVATE restinio::restinio Threads::Threads)

set_property(DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR} PROPERTY VS_STARTUP_PROJECT dakota)
