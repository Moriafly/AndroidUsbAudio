/*
 *
 * Dumb userspace USB Audio receiver
 * Copyright 2012 Joel Stanley <joel@jms.id.au>
 *
 * Based on the following:
 *
 * libusb example program to measure Atmel SAM3U isochronous performance
 * Copyright (C) 2012 Harald Welte <laforge@gnumonks.org>
 *
 * Copied with the author's permission under LGPL-2.1 from
 * http://git.gnumonks.org/cgi-bin/gitweb.cgi?p=sam3u-tests.git;a=blob;f=usb-benchmark-project/host/benchmark.c;h=74959f7ee88f1597286cd435f312a8ff52c56b7e
 *
 * An Atmel SAM3U test firmware is also available in the above repository.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <signal.h>
#include <stdbool.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <libusb.h>

#include <jni.h>

#include <android/log.h>

#define LOGD(...) \
    __android_log_print(ANDROID_LOG_DEBUG, "UsbAudioNative", __VA_ARGS__)

#define UNUSED __attribute__((unused))

/* The first PCM stereo AudioStreaming endpoint. */
#define EP_ISO_IN    0x84
#define IFACE_NUM   0

static int do_exit = 1;
static struct libusb_device_handle *devh = NULL;

static unsigned long num_bytes = 0, num_xfer = 0;
static struct timeval tv_start;

static JavaVM *java_vm = NULL;

static jclass au_id_jms_usbaudio_AudioPlayback = NULL;
static jmethodID au_id_jms_usbaudio_AudioPlayback_write;


int verbose = 2;

static void print_endpoint_comp(const struct libusb_ss_endpoint_companion_descriptor *ep_comp) {
    LOGD("      USB 3.0 Endpoint Companion:\n");
    LOGD("        bMaxBurst:           %u\n", ep_comp->bMaxBurst);
    LOGD("        bmAttributes:        %02xh\n", ep_comp->bmAttributes);
    LOGD("        wBytesPerInterval:   %u\n", ep_comp->wBytesPerInterval);
}

static void print_endpoint(const struct libusb_endpoint_descriptor *endpoint) {
    int i, ret;

    LOGD("      Endpoint:\n");
    LOGD("        bEndpointAddress:    %02xh\n", endpoint->bEndpointAddress);
    LOGD("        bmAttributes:        %02xh\n", endpoint->bmAttributes);
    LOGD("        wMaxPacketSize:      %u\n", endpoint->wMaxPacketSize);
    LOGD("        bInterval:           %u\n", endpoint->bInterval);
    LOGD("        bRefresh:            %u\n", endpoint->bRefresh);
    LOGD("        bSynchAddress:       %u\n", endpoint->bSynchAddress);

    for (i = 0; i < endpoint->extra_length;) {
        if (LIBUSB_DT_SS_ENDPOINT_COMPANION == endpoint->extra[i + 1]) {
            struct libusb_ss_endpoint_companion_descriptor *ep_comp;

            ret = libusb_get_ss_endpoint_companion_descriptor(NULL, endpoint, &ep_comp);
            if (LIBUSB_SUCCESS != ret)
                continue;

            print_endpoint_comp(ep_comp);

            libusb_free_ss_endpoint_companion_descriptor(ep_comp);
        }

        i += endpoint->extra[i];
    }
}

static void print_altsetting(const struct libusb_interface_descriptor *interface) {
    uint8_t i;

    LOGD("    Interface:\n");
    LOGD("      bInterfaceNumber:      %u\n", interface->bInterfaceNumber);
    LOGD("      bAlternateSetting:     %u\n", interface->bAlternateSetting);
    LOGD("      bNumEndpoints:         %u\n", interface->bNumEndpoints);
    LOGD("      bInterfaceClass:       %u\n", interface->bInterfaceClass);
    LOGD("      bInterfaceSubClass:    %u\n", interface->bInterfaceSubClass);
    LOGD("      bInterfaceProtocol:    %u\n", interface->bInterfaceProtocol);
    LOGD("      iInterface:            %u\n", interface->iInterface);

    for (i = 0; i < interface->bNumEndpoints; i++)
        print_endpoint(&interface->endpoint[i]);
}

static void print_2_0_ext_cap(struct libusb_usb_2_0_extension_descriptor *usb_2_0_ext_cap) {
    LOGD("    USB 2.0 Extension Capabilities:\n");
    LOGD("      bDevCapabilityType:    %u\n", usb_2_0_ext_cap->bDevCapabilityType);
    LOGD("      bmAttributes:          %08xh\n", usb_2_0_ext_cap->bmAttributes);
}

static void print_ss_usb_cap(struct libusb_ss_usb_device_capability_descriptor *ss_usb_cap) {
    LOGD("    USB 3.0 Capabilities:\n");
    LOGD("      bDevCapabilityType:    %u\n", ss_usb_cap->bDevCapabilityType);
    LOGD("      bmAttributes:          %02xh\n", ss_usb_cap->bmAttributes);
    LOGD("      wSpeedSupported:       %u\n", ss_usb_cap->wSpeedSupported);
    LOGD("      bFunctionalitySupport: %u\n", ss_usb_cap->bFunctionalitySupport);
    LOGD("      bU1devExitLat:         %u\n", ss_usb_cap->bU1DevExitLat);
    LOGD("      bU2devExitLat:         %u\n", ss_usb_cap->bU2DevExitLat);
}

static void print_bos(libusb_device_handle *handle) {
    struct libusb_bos_descriptor *bos;
    uint8_t i;
    int ret;

    ret = libusb_get_bos_descriptor(handle, &bos);
    if (ret < 0)
        return;

    LOGD("  Binary Object Store (BOS):\n");
    LOGD("    wTotalLength:            %u\n", bos->wTotalLength);
    LOGD("    bNumDeviceCaps:          %u\n", bos->bNumDeviceCaps);

    for (i = 0; i < bos->bNumDeviceCaps; i++) {
        struct libusb_bos_dev_capability_descriptor *dev_cap = bos->dev_capability[i];

        if (dev_cap->bDevCapabilityType == LIBUSB_BT_USB_2_0_EXTENSION) {
            struct libusb_usb_2_0_extension_descriptor *usb_2_0_extension;

            ret = libusb_get_usb_2_0_extension_descriptor(NULL, dev_cap, &usb_2_0_extension);
            if (ret < 0)
                return;

            print_2_0_ext_cap(usb_2_0_extension);
            libusb_free_usb_2_0_extension_descriptor(usb_2_0_extension);
        } else if (dev_cap->bDevCapabilityType == LIBUSB_BT_SS_USB_DEVICE_CAPABILITY) {
            struct libusb_ss_usb_device_capability_descriptor *ss_dev_cap;

            ret = libusb_get_ss_usb_device_capability_descriptor(NULL, dev_cap, &ss_dev_cap);
            if (ret < 0)
                return;

            print_ss_usb_cap(ss_dev_cap);
            libusb_free_ss_usb_device_capability_descriptor(ss_dev_cap);
        }
    }

    libusb_free_bos_descriptor(bos);
}

static void print_interface(const struct libusb_interface *interface) {
    int i;

    for (i = 0; i < interface->num_altsetting; i++)
        print_altsetting(&interface->altsetting[i]);
}

static void print_configuration(struct libusb_config_descriptor *config) {
    uint8_t i;

    LOGD("  Configuration:\n");
    LOGD("    wTotalLength:            %u\n", config->wTotalLength);
    LOGD("    bNumInterfaces:          %u\n", config->bNumInterfaces);
    LOGD("    bConfigurationValue:     %u\n", config->bConfigurationValue);
    LOGD("    iConfiguration:          %u\n", config->iConfiguration);
    LOGD("    bmAttributes:            %02xh\n", config->bmAttributes);
    LOGD("    MaxPower:                %u\n", config->MaxPower);

    for (i = 0; i < config->bNumInterfaces; i++)
        print_interface(&config->interface[i]);
}

static void print_device(libusb_device *dev, libusb_device_handle *handle) {
    struct libusb_device_descriptor desc;
    unsigned char string[256];
    const char *speed;
    int ret;
    uint8_t i;

    switch (libusb_get_device_speed(dev)) {
        case LIBUSB_SPEED_LOW:
            speed = "1.5M";
            break;
        case LIBUSB_SPEED_FULL:
            speed = "12M";
            break;
        case LIBUSB_SPEED_HIGH:
            speed = "480M";
            break;
        case LIBUSB_SPEED_SUPER:
            speed = "5G";
            break;
        case LIBUSB_SPEED_SUPER_PLUS:
            speed = "10G";
            break;
        default:
            speed = "Unknown";
    }

    ret = libusb_get_device_descriptor(dev, &desc);
    if (ret < 0) {
        LOGD("failed to get device descriptor");
        return;
    }

    LOGD("Dev (bus %u, device %u): %04X - %04X speed: %s\n",
         libusb_get_bus_number(dev), libusb_get_device_address(dev),
         desc.idVendor, desc.idProduct, speed);

    if (!handle)
        libusb_open(dev, &handle);

    if (handle) {
        if (desc.iManufacturer) {
            ret = libusb_get_string_descriptor_ascii(handle, desc.iManufacturer, string,
                                                     sizeof(string));
            if (ret > 0)
                LOGD("  Manufacturer:              %s\n", (char *) string);
        }

        if (desc.iProduct) {
            ret = libusb_get_string_descriptor_ascii(handle, desc.iProduct, string, sizeof(string));
            if (ret > 0)
                LOGD("  Product:                   %s\n", (char *) string);
        }

        if (desc.iSerialNumber && verbose) {
            ret = libusb_get_string_descriptor_ascii(handle, desc.iSerialNumber, string,
                                                     sizeof(string));
            if (ret > 0)
                LOGD("  Serial Number:             %s\n", (char *) string);
        }
    }

    if (verbose) {
        for (i = 0; i < desc.bNumConfigurations; i++) {
            struct libusb_config_descriptor *config;

            ret = libusb_get_config_descriptor(dev, i, &config);
            if (LIBUSB_SUCCESS != ret) {
                LOGD("  Couldn't retrieve descriptors\n");
                continue;
            }

            print_configuration(config);

            libusb_free_config_descriptor(config);
        }

        if (handle && desc.bcdUSB >= 0x0201)
            print_bos(handle);
    }

    if (handle)
        libusb_close(handle);
}


static void cb_xfr(struct libusb_transfer *xfr) {
    unsigned int i;

    int len = 0;

    // Get an env handle
    JNIEnv *env;
    void *void_env;
    bool had_to_attach = false;
    jint status = (*java_vm)->GetEnv(java_vm, &void_env, JNI_VERSION_1_6);

    if (status == JNI_EDETACHED) {
        (*java_vm)->AttachCurrentThread(java_vm, &env, NULL);
        had_to_attach = true;
    } else {
        env = void_env;
    }

    // Create a jbyteArray.
    int start = 0;
    jbyteArray audioByteArray = (*env)->NewByteArray(env, 192 * xfr->num_iso_packets);

    for (i = 0; i < xfr->num_iso_packets; i++) {
        struct libusb_iso_packet_descriptor *pack = &xfr->iso_packet_desc[i];

        if (pack->status != LIBUSB_TRANSFER_COMPLETED) {
            LOGD("Error (status %d: %s) :", pack->status,
                 libusb_error_name(pack->status));
            /* This doesn't happen, so bail out if it does. */
            exit(EXIT_FAILURE);
        }

        const unsigned char *data = libusb_get_iso_packet_buffer_simple(xfr, i);
        // TODO warning: initializing 'const char *' with an expression of type 'unsigned char *' converts between pointers to integer types with different sign [-Wpointer-sign]
        //        const char *data = libusb_get_iso_packet_buffer_simple(xfr, i);
        // (*env)->SetByteArrayRegion(env, audioByteArray, len, pack->length, data);

        len += pack->length;
    }

    // Call write()
    (*env)->CallStaticVoidMethod(env, au_id_jms_usbaudio_AudioPlayback,
                                 au_id_jms_usbaudio_AudioPlayback_write, audioByteArray);
    (*env)->DeleteLocalRef(env, audioByteArray);
    if ((*env)->ExceptionCheck(env)) {
        LOGD("Exception while trying to pass sound data to java");
        return;
    }

    num_bytes += len;
    num_xfer++;

    if (had_to_attach) {
        (*java_vm)->DetachCurrentThread(java_vm);
    }


    if (libusb_submit_transfer(xfr) < 0) {
        LOGD("error re-submitting URB\n");
        exit(1);
    }
}

#define NUM_TRANSFERS 10
#define PACKET_SIZE 192
#define NUM_PACKETS 10

static int benchmark_in(uint8_t ep) {
    static uint8_t buf[PACKET_SIZE * NUM_PACKETS];
    static struct libusb_transfer *xfr[NUM_TRANSFERS];
    int num_iso_pack = NUM_PACKETS;
    int i;

    /* NOTE: To reach maximum possible performance the program must
     * submit *multiple* transfers here, not just one.
     *
     * When only one transfer is submitted there is a gap in the bus
     * schedule from when the transfer completes until a new transfer
     * is submitted by the callback. This causes some jitter for
     * isochronous transfers and loss of throughput for bulk transfers.
     *
     * This is avoided by queueing multiple transfers in advance, so
     * that the host controller is always kept busy, and will schedule
     * more transfers on the bus while the callback is running for
     * transfers which have completed on the bus.
     */
    for (i = 0; i < NUM_TRANSFERS; i++) {
        xfr[i] = libusb_alloc_transfer(num_iso_pack);
        if (!xfr[i]) {
            LOGD("Could not allocate transfer");
            return -ENOMEM;
        }

        libusb_fill_iso_transfer(xfr[i], devh, ep, buf,
                                 sizeof(buf), num_iso_pack, cb_xfr, NULL, 1000);
        libusb_set_iso_packet_lengths(xfr[i], sizeof(buf) / num_iso_pack);

        libusb_submit_transfer(xfr[i]);
    }

    gettimeofday(&tv_start, NULL);

    return 1;
}

unsigned int measure(void) {
    struct timeval tv_stop;
    unsigned int diff_msec;

    gettimeofday(&tv_stop, NULL);

    diff_msec = (tv_stop.tv_sec - tv_start.tv_sec) * 1000;
    diff_msec += (tv_stop.tv_usec - tv_start.tv_usec) / 1000;

    printf("%lu transfers (total %lu bytes) in %u miliseconds => %lu bytes/sec\n",
           num_xfer, num_bytes, diff_msec, (num_bytes * 1000) / diff_msec);

    return num_bytes;
}

JNIEXPORT jint JNICALL
Java_au_id_jms_usbaudio_UsbAudio_measure(JNIEnv *env UNUSED, jobject foo UNUSED) {
    return measure();
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved UNUSED) {
    LOGD("libusbaudio: loaded");
    java_vm = vm;

    return JNI_VERSION_1_6;
}


JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved UNUSED) {
    JNIEnv *env;
    void *void_env;
    (*java_vm)->GetEnv(vm, &void_env, JNI_VERSION_1_6);
    env = void_env;

    (*env)->DeleteGlobalRef(env, au_id_jms_usbaudio_AudioPlayback);

    LOGD("libusbaudio: unloaded");
}

JNIEXPORT jboolean JNICALL
Java_au_id_jms_usbaudio_UsbAudio_setup(JNIEnv *env UNUSED, jobject foo UNUSED, int fileDescriptor,
                                       jint pid, jint vid) {
//	int rc;
//
//	rc = libusb_init(NULL);
//    rc = libusb_set_option(NULL, LIBUSB_OPTION_NO_DEVICE_DISCOVERY, NULL);
//    if (rc != LIBUSB_SUCCESS) {
//        LOGD("libusb_set_option failed: %s\n", libusb_error_name(rc));
//        return -1;
//    }
//	if (rc < 0) {
//		LOGD("Error initializing libusb: %s\n", libusb_error_name(rc));
//        return false;
//	}

    libusb_context *ctx = NULL;
    // libusb_device_handle *devh = NULL;
    int rc = 0;
    rc = libusb_set_option(NULL, LIBUSB_OPTION_NO_DEVICE_DISCOVERY, NULL);
    if (rc != LIBUSB_SUCCESS) {
        LOGD("libusb_set_option failed: %d\n", rc);
        return false;
    }
    rc = libusb_init(&ctx);
    if (rc < 0) {
        LOGD("libusb_init failed: %d\n", rc);
        return false;
    }
    rc = libusb_wrap_sys_device(ctx, (intptr_t) fileDescriptor, &devh);
    if (rc < 0) {
        LOGD("libusb_wrap_sys_device failed: %d\n", rc);
        return false;
    } else if (devh == NULL) {
        LOGD("libusb_wrap_sys_device returned invalid handle\n");
        return false;
    }

    // print_device(libusb_get_device(devh), devh);

    // 打开 USB
    struct libusb_device_descriptor desc;
    libusb_device *dev = libusb_get_device(devh);
    int ret = libusb_get_device_descriptor(dev, &desc);
    if (ret < 0) {
        LOGD("failed to get device descriptor");
        return false;
    }

    unsigned char string[256];
    if (!devh) {
        libusb_open(dev, &devh);


    } else {
        ret = libusb_get_string_descriptor_ascii(devh, desc.iProduct, string, sizeof(string));
        if (ret > 0)
            LOGD("Open USB - Product:                   %s\n", (char *) string);
    }

    /* This device is the TI PCM2900C Audio CODEC default VID/PID. */

    /* This device is the KM-HIFI-384KHZ Audio CODEC default VID 0x06bc /PID 0x1595. */
    // devh = libusb_open_device_with_vid_pid(NULL, vid, pid);
    if (!devh) {
        LOGD("Error finding USB device\n");
        libusb_exit(NULL);
        return false;
    }

    rc = libusb_kernel_driver_active(devh, IFACE_NUM);
    if (rc == 1) {
        rc = libusb_detach_kernel_driver(devh, IFACE_NUM);
        if (rc < 0) {
            LOGD("Could not detach kernel driver: %s\n",
                 libusb_error_name(rc));
            libusb_close(devh);
            libusb_exit(NULL);
            return false;
        }
    }

    rc = libusb_claim_interface(devh, IFACE_NUM);
    if (rc < 0) {
        LOGD("Error claiming interface: %s\n", libusb_error_name(rc));
        libusb_close(devh);
        libusb_exit(NULL);
        return false;
    }

//    rc = libusb_set_interface_alt_setting(devh, IFACE_NUM, 1);
//    if (rc < 0) {
//        LOGD("Error setting alt setting: %s\n", libusb_error_name(rc));
//        libusb_close(devh);
//        libusb_exit(NULL);
//        return false;
//    }

    // Get write callback handle
    jclass clazz = (*env)->FindClass(env, "au/id/jms/usbaudio/AudioPlayback");
    if (!clazz) {
        LOGD("Could not find au.id.jms.usbaudio.AudioPlayback");
        libusb_close(devh);
        libusb_exit(NULL);
        return false;
    }
    au_id_jms_usbaudio_AudioPlayback = (*env)->NewGlobalRef(env, clazz);

    au_id_jms_usbaudio_AudioPlayback_write = (*env)->GetStaticMethodID(env,
                                                                       au_id_jms_usbaudio_AudioPlayback,
                                                                       "write", "([B)V");
    if (!au_id_jms_usbaudio_AudioPlayback_write) {
        LOGD("Could not find au.id.jms.usbaudio.AudioPlayback");
        (*env)->DeleteGlobalRef(env, au_id_jms_usbaudio_AudioPlayback);
        libusb_close(devh);
        libusb_exit(NULL);
        return false;
    }


    // Good to go
    do_exit = 0;
    LOGD("Starting capture");
    if ((rc = benchmark_in(EP_ISO_IN)) < 0) {
        LOGD("Capture failed to start: %d", rc);
        return false;
    }
    return true;
}


JNIEXPORT void JNICALL
Java_au_id_jms_usbaudio_UsbAudio_stop(JNIEnv *env UNUSED, jobject foo UNUSED) {
    do_exit = 1;
    measure();
}

JNIEXPORT jboolean JNICALL
Java_au_id_jms_usbaudio_UsbAudio_close(JNIEnv *env UNUSED, jobject foo UNUSED) {
    if (do_exit == 0) {
        return false;
    }
    libusb_release_interface(devh, IFACE_NUM);
    if (devh)
        libusb_close(devh);
    libusb_exit(NULL);
    return true;
}


JNIEXPORT void JNICALL
Java_au_id_jms_usbaudio_UsbAudio_loop(JNIEnv *env UNUSED, jobject foo UNUSED) {
    while (!do_exit) {
        int rc = libusb_handle_events(NULL);
        if (rc != LIBUSB_SUCCESS)
            break;
    }
}

