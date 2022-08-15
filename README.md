# Android USB Audio

这是 [椒盐音乐](https://github.com/Moriafly/SaltPlayerSource) 的独占 USB 音频开发部分，**它是未完成的**。

以开发者目前技术来说实现它几乎是不太可能的，如果你愿意和我一起完成它，欢迎参加到此项目（可以提交 issue 和我取得联系）。

## 项目介绍

独占 USB 即软件直接访问 USB 音频设备，向其发送音频数据。这避免了通过系统的一系列处理，达到了将音频输出上限提升到 USB 硬件一样的水平。

Android USB Connect 并不支持同步端点类型，Android 2 - 12 都不支持，所以需要使用其他的方法。
目前最好的可能是 [libusb](https://github.com/libusb/libusb) 库，以及基于它的 [libusb-java](https://gitlab.ost.ch/tech/inf/public/libusb-java)，
我将 libusb-java 移植到了 Android 平台，并修改了一些代码，且合并了 libusb 示例（[https://github.com/libusb/libusb/tree/master/android/examples](https://github.com/libusb/libusb/tree/master/android/examples)）
以解决在 Android 平台上的访问问题。且添加了 [usbaudio-android-demo](https://github.com/shenki/usbaudio-android-demo) 代码，用于测试，这是一个很老的项目，且仅是对于一个特定 USB 设备的输出，我修改了部分。

## 项目架构

Module usbaudio 是主要模块。

src/main 下是源代码。

## 参考项目

[https://github.com/libusb/libusb](https://github.com/libusb/libusb)

[https://gitlab.ost.ch/tech/inf/public/libusb-java/](https://gitlab.ost.ch/tech/inf/public/libusb-java/)

[https://github.com/shenki/usbaudio-android-demo](https://github.com/shenki/usbaudio-android-demo)

[https://github.com/Peter-St/unRootedSample](https://github.com/Peter-St/unRootedSample)

# 版权

libusb 开源协议为 LGPL 2.1 ，本项目中使用了编译后的动态链接库，位于 src/main/libusbLib 文件夹下。

本项目以 GPL V3 开源。