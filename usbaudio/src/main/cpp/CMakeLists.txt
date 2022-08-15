project(usbaudio)

# CMake 最低版本要求
cmake_minimum_required(VERSION 3.10.2)

# 支持 gnu++11
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

## 定义 libusb so 库和头文件所在目录，方便后续使用
set(libusb_so_dir ${CMAKE_SOURCE_DIR}/../libusbLib/${ANDROID_ABI})
set(libusb_h_dir ${CMAKE_SOURCE_DIR}/libusb)

## 添加 libusb so 头文件目录
include_directories(${libusb_h_dir}/include)

## 添加 libusb so
add_library(libusb
        SHARED
        IMPORTED)
set_target_properties(libusb
        PROPERTIES IMPORTED_LOCATION
        ${libusb_so_dir}/libusb.so)

# 配置目标 so 库编译信息
add_library( # Sets the name of the library.
        usbaudio

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ch_ntb_inf_libusb_Libusb.h
        libusb-transfer.h
        LibusbJava.cpp
        usbaudio_dump.c
        )

# 指定编译目标库时，cmake要链接的库
target_link_libraries(
#        log-lib
        usbaudio
        libusb
        log
)