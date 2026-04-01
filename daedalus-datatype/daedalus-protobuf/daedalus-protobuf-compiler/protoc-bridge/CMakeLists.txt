cmake_minimum_required(VERSION 3.20)
project(protoc_bridge CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_OBJECT_PATH_MAX 4096)
set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")

# Path to protobuf source (passed via -DPROTOBUF_SRC_DIR=...)
if(NOT PROTOBUF_SRC_DIR)
    message(FATAL_ERROR "PROTOBUF_SRC_DIR must be set to the protobuf source directory")
endif()

# Protobuf build options: only build libprotobuf (contains parser + descriptor pool)
set(protobuf_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(protobuf_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(protobuf_BUILD_PROTOC_BINARIES OFF CACHE BOOL "" FORCE)
set(protobuf_BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(protobuf_INSTALL OFF CACHE BOOL "" FORCE)
set(protobuf_BUILD_LIBUPB OFF CACHE BOOL "" FORCE)
set(protobuf_DISABLE_RTTI ON CACHE BOOL "" FORCE)

add_subdirectory(${PROTOBUF_SRC_DIR} ${CMAKE_BINARY_DIR}/protobuf)

add_library(protoc_bridge SHARED protoc_bridge.cpp)
target_link_libraries(protoc_bridge PRIVATE libprotobuf)
target_include_directories(protoc_bridge PRIVATE ${PROTOBUF_SRC_DIR}/src)
