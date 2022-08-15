#include <iostream>
#include <jni.h>
#include <libusb.h>
#include "ch_ntb_inf_libusb_Libusb.h"
#include <vector>
#include <memory>
#include "libusb-transfer.h"

#define ERROR_JAVA_REFERENCES_NOT_LOADED    -100
#define ERROR_JAVA_WRONG_ENVIRONMENT        -101
#define ERROR_JAVA_ILEGAL_DEVICE_HANDLE        -102

//maximum number of port depth. As per the USB 3.0 specs, this is the current limit
#define MAX_PORT_DEPTH 7
#define MAX_DEVICES 20

//#define DEBUGON

using namespace std;

vector<libusb_device_handle *> devHandleVector;

libusb_device **devList;
bool active = false;

JNIEXPORT jint JNICALL JNI_OnLoad_LibusbJava(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;

    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }
    //TODO

    return JNI_VERSION_1_4;
}

void JNI_OnUnload_LibusbJava(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;

    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_4) == JNI_OK) {
    }
}


static __inline void JNICALL ThrowLibusbException(JNIEnv *env, jint code) {
    jmethodID constructor = NULL;
    jthrowable libusbException = NULL;

    jclass clazz = env->FindClass("ch/ntb/inf/libusb/exceptions/LibusbException");
    if (clazz == NULL) {
        goto noClass;
    }

    constructor = env->GetMethodID(clazz, "<init>", "(I)V");
    if (constructor == NULL) {
        goto noConstructor;
    }

    libusbException = (jthrowable) env->NewObject(clazz, constructor, code);
    if (libusbException == NULL) {
        goto noObject;
    }
    if (env->Throw(libusbException) != 0) {
        goto throwFailed;
    }

    env->DeleteLocalRef(libusbException);
    env->DeleteLocalRef(clazz);

    return;

    /* Error Handling. All errors covered here are caused by JNI callbacks and have
     * therefore already thrown appropriate exceptions in the Java environment.
     * Therefore we only have to cleanup what we constructed. */

    throwFailed:
    env->DeleteLocalRef(libusbException);

    noObject:
    noConstructor:
    env->DeleteLocalRef(clazz);

    noClass:

    return;
}


/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    setDebug
 * Signature: (Ljava/nio/ByteBuffer;I)V
 */
extern "C" JNIEXPORT void JNICALL
Java_ch_ntb_inf_libusb_Libusb_setDebug(JNIEnv *env, jclass obj, jobject ctx, jint level) {
    libusb_context *context = nullptr;

    if (ctx) {
        context = static_cast<libusb_context *>(env->GetDirectBufferAddress(ctx));
    }
    // libusb_set_debug(context, level);
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    init
 * Signature: ()Ljava/nio/ByteBuffer;
 */
extern "C" JNIEXPORT jobject JNICALL Java_ch_ntb_inf_libusb_Libusb_init(JNIEnv *env, jclass obj) {
    int res;
    libusb_context *context = nullptr;

    res = libusb_init(&context);
    // libusb_set_debug(context, 3);

#ifdef DEBUGON
    //		std::cout << "res = " << res << std::endl;
    //		std::cout << "env = " << env << std::endl;
    //		std::cout << "context = " << context << std::endl;
    //		std::cout << "&context = " << &context << std::endl;
#endif

    if (res != 0) {
        ThrowLibusbException(env, res);
        return nullptr;
    } else {
        return env->NewDirectByteBuffer(context, 0);
    }
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    exit
 * Signature: (Ljava/nio/ByteBuffer;)V
 */
extern "C" JNIEXPORT void JNICALL
Java_ch_ntb_inf_libusb_Libusb_exit(JNIEnv *env, jclass obj, jobject ctx) {
    libusb_context *context = nullptr;

    if (ctx) {
        context = static_cast<libusb_context *>(env->GetDirectBufferAddress(ctx));
    }
//	std::cout << "exit called" << std::endl;
//	#ifdef DEBUGON
//		std::cout << "env = " << env << std::endl;
//		std::cout << "context = " << context << std::endl;
//		std::cout << "&context = " << &context << std::endl;
//	#endif

    libusb_exit(context);
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getDeviceList
 * Signature: (Ljava/nio/ByteBuffer;)Ljava/util/List;
 */
extern "C" JNIEXPORT jobject JNICALL
Java_ch_ntb_inf_libusb_Libusb_getDeviceList(JNIEnv *env, jclass obj, jobject ctx) {
    libusb_context *context = nullptr;
    ssize_t cnt;

    jmethodID constructorArrList = nullptr;
    jmethodID addArrList = nullptr;
    jclass clazzArrList = nullptr;
    jobject nativeDevList = nullptr;
    jobject changed = nullptr;
    jobject nativeDevPointer = nullptr;

    if (ctx) {
        context = static_cast<libusb_context *>(env->GetDirectBufferAddress(ctx));
    }

#ifdef DEBUGON
    //	std::cout << "env = " << env << std::endl;
    //	std::cout << "context = " << context << std::endl;
    //	std::cout << "&context = " << &context << std::endl;
#endif

    cnt = libusb_get_device_list(context, &devList);
    active = true;
    if (cnt < 0) {
        ThrowLibusbException(env, cnt);
        libusb_free_device_list(devList, 1);
        active = false;
        return nullptr;
    } else if (cnt == 0) {
        libusb_free_device_list(devList, 1);
        active = false;
        return nullptr;
    }

    clazzArrList = env->FindClass("java/util/ArrayList");
    if (clazzArrList == nullptr) {
        goto no_arr_class;
    }
    constructorArrList = env->GetMethodID(clazzArrList, "<init>", "(I)V");
    if (constructorArrList == nullptr) {
        goto no_arr_constructor;
    }
    nativeDevList = (jobject) env->NewObject(clazzArrList, constructorArrList, (int) cnt);
    if (nativeDevList == nullptr) {
        goto no_arr_object;
    }
    addArrList = env->GetMethodID(clazzArrList, "add", "(Ljava/lang/Object;)Z");
    if (addArrList == nullptr) {
        goto method_add_not_found;
    }

    for (int i = 0; i < cnt; i++) {
        nativeDevPointer = env->NewDirectByteBuffer(devList[i], 0);
#ifdef DEBUGON
        //TODO remove
//		std::cout << "devList = " << devList << std::endl;
//		std::cout << "devList[i] = " << devList[i] << std::endl;
//		std::cout << "native device ptr = " << nativeDevPointer << std::endl;
#endif
        changed = reinterpret_cast<jobject>(env->CallBooleanMethod(nativeDevList, addArrList,
                                                                   nativeDevPointer));
    }

    env->DeleteLocalRef(clazzArrList);

    return nativeDevList;

    no_dev_class:

    method_add_not_found:

    no_arr_object:

    no_arr_constructor:
    env->DeleteLocalRef(clazzArrList);

    no_arr_class:
    return nullptr;
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    freeDeviceList
 * Signature: (Z)V
 */
extern "C" JNIEXPORT void JNICALL
Java_ch_ntb_inf_libusb_Libusb_freeDeviceList(JNIEnv *env, jclass obj, jboolean unrefDev) {
    int unrefDevices = 0;

    if (unrefDev) {
        int unrefDevices = 1;
    }

    libusb_free_device_list(devList, unrefDevices);
    active = false;
}


/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getBusNumber
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getBusNumber(JNIEnv *env, jclass obj, jobject nativeDevice) {
    libusb_device *dev = nullptr;

    if (nativeDevice) {
        dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));
        return static_cast<jint>(libusb_get_bus_number(dev));
    } else {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return 0;
    }
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getPortNumber
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getPortNumber(JNIEnv *env, jclass obj, jobject nativeDevice) {
    libusb_device *dev = nullptr;

    if (nativeDevice) {
        dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));
        return static_cast<jint>(libusb_get_port_number(dev));
    } else {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return 0;
    }
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getPortNumbers
 * Signature: (Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;[I)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getPortNumbers(JNIEnv *env, jclass obj, jobject ctx,
                                             jobject nativeDevice, jintArray portNumbers) {

    if (!nativeDevice) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return 0;
    }

    uint8_t lenJ = static_cast<uint8_t> (env->GetArrayLength(portNumbers));
    if (lenJ < MAX_PORT_DEPTH) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
    }

    libusb_context *context = nullptr;
    libusb_device *dev = nullptr;
    uint8_t *nativePortNumbers = new uint8_t[MAX_PORT_DEPTH];

    dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));
    if (ctx) {
        context = static_cast<libusb_context *>(env->GetDirectBufferAddress(ctx));
    }
    jint *portNumsC = env->GetIntArrayElements(portNumbers, NULL);

    int nofNumbers = libusb_get_port_numbers(dev, nativePortNumbers, MAX_PORT_DEPTH);

    for (int j = 0; j < nofNumbers; j++) {
        portNumsC[j] = static_cast<jint>(nativePortNumbers[j]);
    }
    env->SetIntArrayRegion(portNumbers, 0, nofNumbers, portNumsC);

    return nofNumbers;
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getParent
 * Signature: (Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;
 */
extern "C" JNIEXPORT jobject JNICALL
Java_ch_ntb_inf_libusb_Libusb_getParent(JNIEnv *env, jclass obj, jobject nativeDevice) {
    libusb_device *dev = nullptr;
    libusb_device *parentDev = nullptr;

    if (!nativeDevice) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return nullptr;
    }
    // call is only valid between libusb_get_device_list and libusb_free_device_list, take care of it? else remove active
    if (active) {
        dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));
        parentDev = libusb_get_parent(dev);
    } else {
        parentDev = nullptr;
    }

    if (parentDev == nullptr) {
        ThrowLibusbException(env, LIBUSB_ERROR_NO_DEVICE);
        return env->NewDirectByteBuffer(parentDev, 0);
    }
//	std::cout << "parentDev native: " << parentDev << std::endl;

    return env->NewDirectByteBuffer(parentDev, 0);
    //TODO improve error handling
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getVendorId
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getVendorId(JNIEnv *env, jclass obj, jobject nativeDevice) {
    if (!nativeDevice) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return 0;
    }

    libusb_device *dev;
    jint vid;

    dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));

    libusb_device_descriptor devDesc;
    int cnt = libusb_get_device_descriptor(dev, &devDesc);

    if (cnt != 0) {
        ThrowLibusbException(env, cnt);
        return -1;
    }

#ifdef DEBUGON
    //	std::cout << "cVid: " << static_cast<int>(devDesc->idVendor) << std::endl;
#endif
    vid = static_cast<jint>(devDesc.idVendor);

    return vid;
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getProductId
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getProductId(JNIEnv *env, jclass obj, jobject nativeDevice) {

    if (!nativeDevice) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return 0;
    }
    libusb_device *dev;
    jint pid;

    dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));

    libusb_device_descriptor devDesc;
    int cnt = libusb_get_device_descriptor(dev, &devDesc);

    if (cnt != 0) {
        ThrowLibusbException(env, cnt);
        return -1;
    }

#ifdef DEBUGON
    //	std::cout << "cPid: " << static_cast<int>(devDesc->idProduct) << std::endl;
#endif
    pid = static_cast<jint>(devDesc.idProduct);

    return pid;
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getNofConfigurations
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getNofConfigurations(JNIEnv *env, jclass obj, jobject nativeDevice) {

    if (!nativeDevice) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return 0;
    }

    libusb_device *dev;

    dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));

    libusb_device_descriptor devDesc;
    int cnt = libusb_get_device_descriptor(dev, &devDesc);

    if (cnt != 0) {
        ThrowLibusbException(env, cnt);
        return -1;
    }

    return static_cast<jint>(devDesc.bNumConfigurations);
}


/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    open
 * Signature: (Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;
 */
extern "C" JNIEXPORT jobject JNICALL
Java_ch_ntb_inf_libusb_Libusb_open(JNIEnv *env, jclass obj, jobject nativeDevice) {

    if (!nativeDevice) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return nullptr;
    }

    libusb_device *dev = nullptr;
    libusb_device_handle *handle;

    dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));

    int cnt = libusb_open(dev, &handle);

    if (cnt != 0) {
        ThrowLibusbException(env, cnt);
        return nullptr;
    } else {
        devHandleVector.push_back(handle);
        return env->NewDirectByteBuffer(devHandleVector[devHandleVector.size() - 1], 0);
    }
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    close
 * Signature: (Ljava/nio/ByteBuffer;)V
 */
extern "C" JNIEXPORT void JNICALL
Java_ch_ntb_inf_libusb_Libusb_close(JNIEnv *env, jclass obj, jobject nativeHandle) {
    libusb_device_handle *handle = nullptr;

    if (!nativeHandle) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
    }
    handle = static_cast<libusb_device_handle *>(env->GetDirectBufferAddress(nativeHandle));

    for (int i = 0; i < devHandleVector.size(); i++) {
        if (devHandleVector[i] == handle) {
            libusb_close(devHandleVector[i]);
            devHandleVector.erase(devHandleVector.begin() + i);
            return;
        }
    }
    ThrowLibusbException(env, LIBUSB_ERROR_NO_DEVICE);
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getDeviceAddress
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getDeviceAddress(JNIEnv *env, jclass obj, jobject nativeDevice) {
    libusb_device *dev = nullptr;

    if (!nativeDevice) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return 0;
    }
    dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));
    return static_cast<jint>(libusb_get_device_address(dev));
}


/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getDeviceSpeed
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getDeviceSpeed(JNIEnv *env, jclass obj, jobject nativeDevice) {
    libusb_device *dev = nullptr;

    if (!nativeDevice) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return 0;
    }
    dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));
    return static_cast<jint>(libusb_get_device_speed(dev));
}


/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getActiveConfigDescriptor
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getActiveConfigDescriptor(JNIEnv *env, jclass obj,
                                                        jobject nativeDevice) {
    libusb_device *dev = nullptr;
    libusb_config_descriptor *tmpConfigDesc;

    if (!nativeDevice) {

        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return LIBUSB_ERROR_INVALID_PARAM;
    }

    dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));


    int cnt = libusb_get_active_config_descriptor(dev, &tmpConfigDesc);

    if (cnt != 0) {
        ThrowLibusbException(env, cnt);
        return cnt;
    }

    jint configValue = static_cast<jint>(tmpConfigDesc->bConfigurationValue);
    libusb_free_config_descriptor(tmpConfigDesc);

    return configValue;

}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getConfigValue
 * Signature: (Ljava/nio/ByteBuffer;I)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getConfigValue(JNIEnv *env, jclass obj, jobject nativeDevice,
                                             jint configIndex) {

    if (!nativeDevice || (configIndex < 0)) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return LIBUSB_ERROR_INVALID_PARAM;
    }
    libusb_device *dev = nullptr;

    dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));

    libusb_config_descriptor *configDesc;

    int cnt = libusb_get_config_descriptor(dev, static_cast<uint8_t>(configIndex), &configDesc);
    if (cnt != 0) {
        ThrowLibusbException(env, cnt);
        return -1;
    }

    jint configValue = static_cast<jint>((*configDesc).bConfigurationValue);
    libusb_free_config_descriptor(configDesc);

    return configValue;
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getNofInterfaces
 * Signature: (Ljava/nio/ByteBuffer;I)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getNofInterfaces(JNIEnv *env, jclass obj, jobject nativeDevice,
                                               jint configIndex) {

    if (!nativeDevice || (configIndex < 0)) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return LIBUSB_ERROR_INVALID_PARAM;
    }
    libusb_device *dev = nullptr;

    dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));

    libusb_config_descriptor *configDesc;

    int cnt = libusb_get_config_descriptor(dev, static_cast<uint8_t>(configIndex), &configDesc);

    if (cnt != 0) {
        ThrowLibusbException(env, cnt);
        return cnt;
    }
    jint nofInterfaces = static_cast<jint>((*configDesc).bNumInterfaces);
    libusb_free_config_descriptor(configDesc);

    return nofInterfaces;
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getNofAltSettings
 * Signature: (Ljava/nio/ByteBuffer;I)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getNofAltSettings(JNIEnv *env, jclass obj, jobject nativeDevice,
                                                jint configIndex) {

    if (!nativeDevice || (configIndex < 0)) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return LIBUSB_ERROR_INVALID_PARAM;
    }
    libusb_device *dev = nullptr;

    dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));

    libusb_config_descriptor *configDesc;

    int cnt = libusb_get_config_descriptor(dev, static_cast<uint8_t>(configIndex), &configDesc);

    if (cnt != 0) {
        ThrowLibusbException(env, cnt);
        return static_cast<jint>(cnt);
    }

    jint nofAltsetting = static_cast<jint>((*configDesc).interface->num_altsetting);
    libusb_free_config_descriptor(configDesc);

    return nofAltsetting;
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getInterfaceNumber
 * Signature: (Ljava/nio/ByteBuffer;III)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getInterfaceNumber(JNIEnv *env, jclass obj, jobject nativeDevice,
                                                 jint configIndex, jint ifaceIndex,
                                                 jint altSetting) {
    if (!nativeDevice || (configIndex < 0) || (ifaceIndex < 0) || (altSetting < 0)) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return LIBUSB_ERROR_INVALID_PARAM;
    }
    libusb_device *dev = nullptr;

    dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));

    libusb_config_descriptor *configDesc;

    int cnt = libusb_get_config_descriptor(dev, static_cast<uint8_t>(configIndex), &configDesc);

    if (cnt != 0) {
        ThrowLibusbException(env, cnt);
        return static_cast<jint>(cnt);
    }

    jint ifaceNum = static_cast<jint>((*configDesc).interface[ifaceIndex].altsetting[altSetting].bInterfaceNumber);
//#ifdef DEBUGON
//	cout << "C: ifaceNum: " << ifaceNum << endl;
//#endif
    libusb_free_config_descriptor(configDesc);

    return ifaceNum;
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getAlternateSetting
 * Signature: (Ljava/nio/ByteBuffer;III)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getAlternateSetting(JNIEnv *env, jclass obj, jobject nativeDevice,
                                                  jint configIndex, jint ifaceIndex,
                                                  jint altSetting) {
    if (!nativeDevice || (configIndex < 0) || (ifaceIndex < 0) || (altSetting < 0)) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return LIBUSB_ERROR_INVALID_PARAM;
    }
    libusb_device *dev = nullptr;

    dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));

    libusb_config_descriptor *configDesc;

    int cnt = libusb_get_config_descriptor(dev, static_cast<uint8_t>(configIndex), &configDesc);

    if (cnt != 0) {
        ThrowLibusbException(env, cnt);
        return static_cast<jint>(cnt);
    }

    jint alternateSetting = static_cast<jint>((*configDesc).interface[ifaceIndex].altsetting[altSetting].bAlternateSetting);
    //#ifdef DEBUGON
    //	cout << "C: ifaceNum: " << ifaceNum << endl;
    //#endif
    libusb_free_config_descriptor(configDesc);

    return alternateSetting;
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getNofEndpoints
 * Signature: (Ljava/nio/ByteBuffer;III)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getNofEndpoints(JNIEnv *env, jclass obj, jobject nativeDevice,
                                              jint configIndex, jint ifaceIndex, jint altSetting) {

    if (!nativeDevice || (configIndex < 0) || (ifaceIndex < 0) || (altSetting < 0)) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return LIBUSB_ERROR_INVALID_PARAM;
    }
    libusb_device *dev = nullptr;

    dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));

    libusb_config_descriptor *configDesc;

    int cnt = libusb_get_config_descriptor(dev, static_cast<uint8_t>(configIndex), &configDesc);

    if (cnt != 0) {
        ThrowLibusbException(env, cnt);
        return static_cast<jint>(cnt);
    }

    jint nofEndpoints = static_cast<jint>((*configDesc).interface[ifaceIndex].altsetting[altSetting].bNumEndpoints);
    //#ifdef DEBUGON
    //	cout << "C: nofEndpoints: " << nofEndpoints << endl;
    //#endif
    libusb_free_config_descriptor(configDesc);

    return nofEndpoints;
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getEndpointAddress
 * Signature: (Ljava/nio/ByteBuffer;IIII)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getEndpointAddress(JNIEnv *env, jclass obj, jobject nativeDevice,
                                                 jint configIndex, jint ifaceIndex, jint altSetting,
                                                 jint endpointIndex) {

    if (!nativeDevice || (configIndex < 0) || (ifaceIndex < 0) || (altSetting < 0) ||
        (endpointIndex < 0)) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return LIBUSB_ERROR_INVALID_PARAM;
    }
    libusb_device *dev = nullptr;

    dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));

    libusb_config_descriptor *configDesc;

    int cnt = libusb_get_config_descriptor(dev, static_cast<uint8_t>(configIndex), &configDesc);

    if (cnt != 0) {
        ThrowLibusbException(env, cnt);
        return static_cast<jint>(cnt);
    }

    jint endpointAddress = static_cast<jint>((*configDesc).interface[ifaceIndex].altsetting[altSetting].endpoint[endpointIndex].bEndpointAddress);
    //#ifdef DEBUGON
    //	cout << "C: endpointAddress: " << endpointAddress << endl;
    //#endif
    libusb_free_config_descriptor(configDesc);

    return endpointAddress;
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getMaxPacketSize
 * Signature: (Ljava/nio/ByteBuffer;IIII)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getMaxPacketSize(JNIEnv *env, jclass obj, jobject nativeDevice,
                                               jint configIndex, jint ifaceIndex, jint altSetting,
                                               jint endpointIndex) {
    if (!nativeDevice || (configIndex < 0) || (ifaceIndex < 0) || (altSetting < 0) ||
        (endpointIndex < 0)) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return LIBUSB_ERROR_INVALID_PARAM;
    }
    libusb_device *dev = nullptr;

    dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));

    libusb_config_descriptor *configDesc;

    int cnt = libusb_get_config_descriptor(dev, static_cast<uint8_t>(configIndex), &configDesc);

    if (cnt != 0) {
        ThrowLibusbException(env, cnt);
        return static_cast<jint>(cnt);
    }

    jint maxPacketSize = static_cast<jint>((*configDesc).interface[ifaceIndex].altsetting[altSetting].endpoint[endpointIndex].wMaxPacketSize);
    //#ifdef DEBUGON
    //	cout << "C: maxPacketSize: " << maxPacketSize << endl;
    //#endif
    libusb_free_config_descriptor(configDesc);
    if (maxPacketSize < 0) {
        ThrowLibusbException(env, cnt);
    }
    return maxPacketSize;
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getMaxIsoPacketSize
 * Signature: (Ljava/nio/ByteBuffer;IIII)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getMaxIsoPacketSize(JNIEnv *env, jclass obj, jobject nativeDevice,
                                                  jint configIndex, jint ifaceIndex,
                                                  jint altSetting, jint endpointIndex) {
    if (!nativeDevice || (configIndex < 0) || (ifaceIndex < 0) || (altSetting < 0) ||
        (endpointIndex < 0)) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return LIBUSB_ERROR_INVALID_PARAM;
    }
    libusb_device *dev = nullptr;

    dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));

    libusb_config_descriptor *configDesc;

    int cnt = libusb_get_config_descriptor(dev, static_cast<uint8_t>(configIndex), &configDesc);

    if (cnt != 0) {
        ThrowLibusbException(env, cnt);
        return static_cast<jint>(cnt);
    }

    uint8_t endpointAddress = (*configDesc).interface[ifaceIndex].altsetting[altSetting].endpoint[endpointIndex].bEndpointAddress;
    jint maxIsoPacketSize = static_cast<jint>(libusb_get_max_packet_size(dev, endpointAddress));
    libusb_free_config_descriptor(configDesc);

    if (maxIsoPacketSize < 0) {
        ThrowLibusbException(env, cnt);
    }
    //#ifdef DEBUGON
    //	cout << "C: maxIsoPacketSize: " << maxPacketSize << endl;
    //#endif
    return maxIsoPacketSize;
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    claimInterface
 * Signature: (Ljava/nio/ByteBuffer;I)V
 */
extern "C" JNIEXPORT void JNICALL
Java_ch_ntb_inf_libusb_Libusb_claimInterface(JNIEnv *env, jclass obj, jobject nativeHandle,
                                             jint ifaceNum) {
    if (!nativeHandle || (ifaceNum < 0)) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
    }

    libusb_device_handle *devHandle = nullptr;
    devHandle = static_cast<libusb_device_handle *>(env->GetDirectBufferAddress(nativeHandle));

    for (int i = 0; i < devHandleVector.size(); i++) {
        if (devHandleVector[i] == devHandle) {
            int retVal = libusb_claim_interface(devHandle, static_cast<uint8_t>(ifaceNum));
            if (retVal != 0) {
                ThrowLibusbException(env, retVal);
            }
            return;
        }
    }

    ThrowLibusbException(env, LIBUSB_ERROR_NO_DEVICE);
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    resetDevice
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_resetDevice(JNIEnv *env, jclass obj, jobject nativeHandle) {
    if (!nativeHandle) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return LIBUSB_ERROR_INVALID_PARAM;
    }

    libusb_device_handle *devHandle = nullptr;
    devHandle = static_cast<libusb_device_handle *>(env->GetDirectBufferAddress(nativeHandle));

    int retVal = libusb_reset_device(devHandle);
    if (retVal != 0) {
        ThrowLibusbException(env, retVal);
    }
    return static_cast<jint>(retVal);
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getTransferType
 * Signature: (Ljava/nio/ByteBuffer;IIII)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getTransferType(JNIEnv *env, jclass obj, jobject nativeDevice,
                                              jint configIndex, jint ifaceIndex, jint altSetting,
                                              jint endpointIndex) {

    if (!nativeDevice || (configIndex < 0) || (ifaceIndex < 0) || (altSetting < 0) ||
        (endpointIndex < 0)) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return LIBUSB_ERROR_INVALID_PARAM;
    }
    libusb_device *dev = nullptr;

    dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));

    libusb_config_descriptor *configDesc;

    int cnt = libusb_get_config_descriptor(dev, static_cast<uint8_t>(configIndex), &configDesc);

    if (cnt != 0) {
        ThrowLibusbException(env, cnt);
        return static_cast<jint>(cnt);
    }

    uint8_t bmAttributes = (*configDesc).interface[ifaceIndex].altsetting[altSetting].endpoint[endpointIndex].bmAttributes;
    //#ifdef DEBUGON
    //	cout << "C: bmAttributes: " << bmAttributes << endl;
    //#endif
    libusb_free_config_descriptor(configDesc);

    return static_cast<jint>((bmAttributes & LIBUSB_TRANSFER_TYPE_MASK));
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getSerialNumberIndex
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getSerialNumberIndex(JNIEnv *env, jclass obj, jobject nativeDevice) {
    if (!nativeDevice) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return 0;
    }

    libusb_device *dev;

    dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));

    libusb_device_descriptor devDesc;
    int cnt = libusb_get_device_descriptor(dev, &devDesc);

    if (cnt != 0) {
        ThrowLibusbException(env, cnt);
        return -1;
    }

#ifdef DEBUGON
    //	std::cout << "cVid: " << static_cast<int>(devDesc->idVendor) << std::endl;
#endif

    return static_cast<jint>(devDesc.iSerialNumber);
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getStringDescriptor
 * Signature: (Ljava/nio/ByteBuffer;II)Ljava/lang/String;
 */
extern "C" JNIEXPORT jstring JNICALL
Java_ch_ntb_inf_libusb_Libusb_getStringDescriptor(JNIEnv *env, jclass obj, jobject nativeHandle,
                                                  jint descIndex, jint langId) {
    if (!nativeHandle) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return nullptr;
    }
    libusb_device_handle *devHandle;
    devHandle = static_cast<libusb_device_handle *>(env->GetDirectBufferAddress(nativeHandle));

    unsigned char data[50];
//	int cnt =  libusb_get_string_descriptor(devHandle, static_cast<uint8_t>(descIndex), static_cast<uint16_t>(langId), data, 50);
//	int cnt =  libusb_get_string_descriptor(devHandle, static_cast<uint8_t>(0x01), static_cast<uint16_t>(0x0409), data, 50);
//TODO	int cnt =  libusb_get_string_descriptor_ascii(devHandle, static_cast<uint8_t>(0x03), data, 50);
    int cnt = 0;
//	cout << "C descriptor: " << endl;
//	cout << "length: " << cnt << endl;
//	cout << data << endl;
    jstring str = env->NewStringUTF(reinterpret_cast<const char *>(data));
    return str;
}


/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getStringDescriptorAscii
 * Signature: (Ljava/nio/ByteBuffer;I)Ljava/lang/String;
 */
extern "C" JNIEXPORT jstring JNICALL
Java_ch_ntb_inf_libusb_Libusb_getStringDescriptorAscii(JNIEnv *env, jclass obj,
                                                       jobject nativeHandle, jint descIndex) {
    if (!nativeHandle) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return nullptr;
    }
    libusb_device_handle *devHandle;
    devHandle = static_cast<libusb_device_handle *>(env->GetDirectBufferAddress(nativeHandle));

    unsigned char data[50];
    int cnt = libusb_get_string_descriptor_ascii(devHandle, static_cast<uint8_t>(descIndex), data,
                                                 50);
    return env->NewStringUTF(reinterpret_cast<const char *>(data));

}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getManufacturerDescIndex
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getManufacturerDescIndex(JNIEnv *env, jclass obj,
                                                       jobject nativeDevice) {
    if (!nativeDevice) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return 0;
    }

    libusb_device *dev;
    dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));

    libusb_device_descriptor devDesc;
    int cnt = libusb_get_device_descriptor(dev, &devDesc);
    if (cnt != 0) {
        ThrowLibusbException(env, cnt);
        return -1;
    }
    return static_cast<jint>(devDesc.iManufacturer);
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    getProductDescIndex
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_getProductDescIndex(JNIEnv *env, jclass obj, jobject nativeDevice) {
    if (!nativeDevice) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return 0;
    }

    libusb_device *dev;
    dev = static_cast<libusb_device *>(env->GetDirectBufferAddress(nativeDevice));

    libusb_device_descriptor devDesc;
    int cnt = libusb_get_device_descriptor(dev, &devDesc);
    if (cnt != 0) {
        ThrowLibusbException(env, cnt);
        return -1;
    }

    return static_cast<jint>(devDesc.iProduct);
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    relaseInterface
 * Signature: (Ljava/nio/ByteBuffer;I)V
 */
extern "C" JNIEXPORT void JNICALL
Java_ch_ntb_inf_libusb_Libusb_relaseInterface(JNIEnv *env, jclass obj, jobject nativeHandle,
                                              jint ifaceNum) {
    if (!nativeHandle || (ifaceNum < 0)) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
    }

    libusb_device_handle *devHandle = nullptr;
    devHandle = static_cast<libusb_device_handle *>(env->GetDirectBufferAddress(nativeHandle));

    for (int i = 0; i < devHandleVector.size(); i++) {
        if (devHandleVector[i] == devHandle) {
            int retVal = libusb_release_interface(devHandle, static_cast<uint8_t>(ifaceNum));
            if (retVal != 0) {
                ThrowLibusbException(env, retVal);
            }
            return;
        }
    }

    ThrowLibusbException(env, LIBUSB_ERROR_NO_DEVICE);
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    setInterfaceAltSetting
 * Signature: (Ljava/nio/ByteBuffer;II)V
 */
extern "C" JNIEXPORT void JNICALL
Java_ch_ntb_inf_libusb_Libusb_setInterfaceAltSetting(JNIEnv *env, jclass obj, jobject nativeHandle,
                                                     jint ifaceNum, jint altSetting) {
    if (!nativeHandle || (ifaceNum < 0)) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
    }

    libusb_device_handle *devHandle = nullptr;
    devHandle = static_cast<libusb_device_handle *>(env->GetDirectBufferAddress(nativeHandle));

    for (int i = 0; i < devHandleVector.size(); i++) {
        if (devHandleVector[i] == devHandle) {
            int retVal = libusb_set_interface_alt_setting(devHandle, static_cast<uint8_t>(ifaceNum),
                                                          static_cast<uint8_t>(altSetting));
            if (retVal != 0) {
                ThrowLibusbException(env, retVal);
            }
            return;
        }
    }

    ThrowLibusbException(env, LIBUSB_ERROR_NO_DEVICE);
}


/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    openVidPid
 * Signature: (Ljava/nio/ByteBuffer;II)Ljava/nio/ByteBuffer;
 */
extern "C" JNIEXPORT jobject JNICALL
Java_ch_ntb_inf_libusb_Libusb_openVidPid(JNIEnv *env, jclass obj, jobject ctx, jint vid, jint pid) {
    if (ctx == nullptr) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
    }

    libusb_context *context;
    context = static_cast<libusb_context *>(env->GetDirectBufferAddress(ctx));

    libusb_device_handle *devHandle;

    devHandle = libusb_open_device_with_vid_pid(context, static_cast<uint16_t>(vid),
                                                static_cast<uint16_t>(pid));

    if (devHandle == nullptr) {
        ThrowLibusbException(env, LIBUSB_ERROR_NO_DEVICE);
        return nullptr;
    }

    devHandleVector.push_back(devHandle);
    return env->NewDirectByteBuffer(devHandleVector[devHandleVector.size() - 1], 0);
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    controlTransfer
 * Signature: (Ljava/nio/ByteBuffer;BBSS[BII)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_controlTransfer(JNIEnv *env, jclass obj, jobject nativeHandle,
                                              jbyte requestType, jbyte request, jshort value,
                                              jshort index, jbyteArray data, jint length,
                                              jint timeout) {
    if (!nativeHandle || !data) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return LIBUSB_ERROR_INVALID_PARAM;
    }
    unsigned char *data_buff = reinterpret_cast<unsigned char *>(env->GetByteArrayElements(data,
                                                                                           NULL));
#ifdef DEBUGON
    //	cout << "data: " << data_buff << endl;
#endif
    libusb_device_handle *devHandle = nullptr;
    devHandle = static_cast<libusb_device_handle *>(env->GetDirectBufferAddress(nativeHandle));

    for (int i = 0; i < devHandleVector.size(); i++) {
        if (devHandleVector[i] == devHandle) {
            int retVal = libusb_control_transfer(devHandle, static_cast<uint8_t>(requestType),
                                                 static_cast<uint8_t>(request),
                                                 static_cast<uint16_t>(value),
                                                 static_cast<uint16_t>(index), data_buff,
                                                 static_cast<uint16_t>(length),
                                                 static_cast<unsigned int>(timeout));
            env->ReleaseByteArrayElements(data, (jbyte *) data_buff, 0);

            if (retVal < 0) {
                ThrowLibusbException(env, retVal);
            }
            return static_cast<jint>(retVal);
        }

    }
    ThrowLibusbException(env, LIBUSB_ERROR_NO_DEVICE);
    return LIBUSB_ERROR_NO_DEVICE;
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    bulkTransfer
 * Signature: (Ljava/nio/ByteBuffer;I[BII)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_bulkTransfer(JNIEnv *env, jclass obj, jobject nativeHandle,
                                           jint endpoint, jbyteArray buffer, jint length,
                                           jint timeout) {
    if (!nativeHandle || (endpoint < 0) || !buffer) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return LIBUSB_ERROR_INVALID_PARAM;
    }
    int bytes_transferred = 0;
    unsigned char *data = reinterpret_cast<unsigned char *>(env->GetByteArrayElements(buffer,
                                                                                      NULL));
#ifdef DEBUGON
    //	cout << "data: " << data << endl;
#endif
    libusb_device_handle *devHandle = nullptr;
    devHandle = static_cast<libusb_device_handle *>(env->GetDirectBufferAddress(nativeHandle));

    for (int i = 0; i < devHandleVector.size(); i++) {
        if (devHandleVector[i] == devHandle) {
            int retVal = libusb_bulk_transfer(devHandle, static_cast<uint8_t>(endpoint), data,
                                              static_cast<int>(length), &bytes_transferred,
                                              static_cast<unsigned int>(timeout));
            env->ReleaseByteArrayElements(buffer, (jbyte *) data, 0);
//			cout << "retVal: " << retVal << endl;
            if (retVal != 0) {
                ThrowLibusbException(env, retVal);
                return static_cast<jint>(retVal);
            } else {
                return static_cast<jint>(bytes_transferred);
            }
        }

    }
    ThrowLibusbException(env, LIBUSB_ERROR_NO_DEVICE);
    return LIBUSB_ERROR_NO_DEVICE;

}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    interruptTransfer
 * Signature: (Ljava/nio/ByteBuffer;I[BI[BI)I
 *///TODO test interruptTransfer
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_interruptTransfer(JNIEnv *env, jclass obj, jobject nativeHandle,
                                                jint endpoint, jbyteArray buffer, jint length,
                                                jint timeout) {
    if (!nativeHandle || (endpoint < 0) || !buffer) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return LIBUSB_ERROR_INVALID_PARAM;
    }
    int bytes_transferred = 0;
    unsigned char *data = reinterpret_cast<unsigned char *>(env->GetByteArrayElements(buffer,
                                                                                      NULL));
#ifdef DEBUGON
    //	cout << "data: " << data << endl;
#endif
    libusb_device_handle *devHandle = nullptr;
    devHandle = static_cast<libusb_device_handle *>(env->GetDirectBufferAddress(nativeHandle));

    for (int i = 0; i < devHandleVector.size(); i++) {
        if (devHandleVector[i] == devHandle) {
            int retVal = libusb_interrupt_transfer(devHandle, static_cast<uint8_t>(endpoint), data,
                                                   static_cast<int>(length), &bytes_transferred,
                                                   static_cast<unsigned int>(timeout));
            env->ReleaseByteArrayElements(buffer, (jbyte *) data, 0);
            //			cout << "retVal: " << retVal << endl;
            if (retVal != 0) {
                ThrowLibusbException(env, retVal);
                return static_cast<jint>(retVal);
            } else {
                return static_cast<jint>(bytes_transferred);
            }
        }

    }
    ThrowLibusbException(env, LIBUSB_ERROR_NO_DEVICE);
    return LIBUSB_ERROR_NO_DEVICE;
}



/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    allocTransfer
 * Signature: (I)Ljava/nio/ByteBuffer;
 */
extern "C" JNIEXPORT jobject JNICALL
Java_ch_ntb_inf_libusb_Libusb_allocTransfer(JNIEnv *env, jclass obj, jint numIsoPacks) {
    libusb_transfer *transfer;
    transfer = libusb_alloc_transfer(static_cast<int>(numIsoPacks));
    return env->NewDirectByteBuffer(transfer, transfer->actual_length);
}

/*
 * variables for callback java
 */

JavaVM *g_jvm;
jobject g_obj;
jmethodID g_mid;

/*
 * end variable for callback java
 */

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    fillIsoTransfer
 * Signature: (Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;I[CIIILjava/lang/Object;I)I
 */
extern "C" JNIEXPORT void JNICALL Java_ch_ntb_inf_libusb_Libusb_fillIsoTransfer
        (JNIEnv *env, jclass obj, jobject transfer, jobject nativeHandle, jint endpoint,
         jbyteArray buffer, jint length, jint numIsoPackets, jstring callbackFunction,
         jobject user_data, jint timeout, jstring fullQualifiedClassName) {

    if (!nativeHandle || (endpoint < 0) || !buffer) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return;
    }
    int bytes_transferred = 0;
    unsigned char *data = reinterpret_cast<unsigned char *>(env->GetByteArrayElements(buffer,
                                                                                      NULL));

#ifdef DEBUGON
    //	cout << "data: " << data << endl;
#endif

    g_obj = env->NewGlobalRef(obj);
    jclass g_clazz = env->GetObjectClass(g_obj);

    const char *functionName = env->GetStringUTFChars(callbackFunction, (jboolean *) false);
    g_mid = env->GetMethodID(
            env->FindClass(env->GetStringUTFChars(fullQualifiedClassName, (jboolean *) false)),
            functionName, "(I)V");


    libusb_device_handle *devHandle = nullptr;
    devHandle = static_cast<libusb_device_handle *>(env->GetDirectBufferAddress(nativeHandle));

    libusb_transfer *transferUsed = static_cast<libusb_transfer *>(env->GetDirectBufferAddress(
            transfer));

    void *userData = static_cast<void *>(env->GetDirectBufferAddress(user_data));

    transferUsed->callback = &transferCallback;
    transferUsed->user_data = userData;


    for (int i = 0; i < devHandleVector.size(); i++) {
        if (devHandleVector[i] == devHandle) {

            libusb_fill_iso_transfer(transferUsed, devHandle, static_cast<uint8_t>(endpoint), data,
                                     static_cast<int>(length), static_cast<int>(numIsoPackets),
                                     transferUsed->callback, userData,
                                     static_cast<unsigned int>(timeout));
            env->ReleaseByteArrayElements(buffer, (jbyte *) data, 0);


            return;

        }

    }
    ThrowLibusbException(env, LIBUSB_ERROR_NO_DEVICE);
    return;
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    fillInterruptTransfer
 * Signature: (Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;I[BILjava/lang/String;Ljava/lang/Object;I)I
 */
extern "C" JNIEXPORT void JNICALL
Java_ch_ntb_inf_libusb_Libusb_fillInterruptTransfer(JNIEnv *env, jclass obj, jobject transfer,
                                                    jobject nativeHandle, jint endpoint,
                                                    jbyteArray buffer, jint length,
                                                    jstring callbackFunction, jobject user_data,
                                                    jint timeout, jstring fullQualifiedClassName) {
    if (!nativeHandle || (endpoint < 0) || !buffer) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return;
    }
    int bytes_transferred = 0;
    unsigned char *data = reinterpret_cast<unsigned char *>(env->GetByteArrayElements(buffer,
                                                                                      NULL));

#ifdef DEBUGON
    //	cout << "data: " << data << endl;
#endif

    g_obj = env->NewGlobalRef(obj);
    jclass g_clazz = env->GetObjectClass(g_obj);

    const char *functionName = env->GetStringUTFChars(callbackFunction, (jboolean *) false);
    g_mid = env->GetMethodID(
            env->FindClass(env->GetStringUTFChars(fullQualifiedClassName, (jboolean *) false)),
            functionName, "(I)V");


    libusb_device_handle *devHandle = nullptr;
    devHandle = static_cast<libusb_device_handle *>(env->GetDirectBufferAddress(nativeHandle));

    libusb_transfer *transferUsed = static_cast<libusb_transfer *>(env->GetDirectBufferAddress(
            transfer));
    void *userData = static_cast<void *>(env->GetDirectBufferAddress(user_data));

    transferUsed->callback = &transferCallback;
    transferUsed->user_data = userData;

    for (int i = 0; i < devHandleVector.size(); i++) {
        if (devHandleVector[i] == devHandle) {

            libusb_fill_interrupt_transfer(transferUsed, devHandle, static_cast<uint8_t>(endpoint),
                                           data, static_cast<int>(length), transferUsed->callback,
                                           userData, static_cast<unsigned int>(timeout));
            env->ReleaseByteArrayElements(buffer, (jbyte *) data, 0);

            return;

        }

    }
    ThrowLibusbException(env, LIBUSB_ERROR_NO_DEVICE);
    return;
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    fillBulkTransfer
 * Signature: (Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;I[BILjava/lang/String;Ljava/lang/Object;I)I
 */
extern "C" JNIEXPORT void JNICALL
Java_ch_ntb_inf_libusb_Libusb_fillBulkTransfer(JNIEnv *env, jclass obj, jobject transfer,
                                               jobject nativeHandle, jint endpoint,
                                               jbyteArray buffer, jint length,
                                               jstring callbackFunction, jobject user_data,
                                               jint timeout, jstring fullQualifiedClassName) {
    if (!nativeHandle || (endpoint < 0) || !buffer) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return;
    }
    int bytes_transferred = 0;
    unsigned char *data = reinterpret_cast<unsigned char *>(env->GetByteArrayElements(buffer,
                                                                                      nullptr));

#ifdef DEBUGON
    //	cout << "data: " << data << endl;
#endif

    g_obj = env->NewGlobalRef(obj);
    jclass g_clazz = env->GetObjectClass(g_obj);

    const char *functionName = env->GetStringUTFChars(callbackFunction, (jboolean *) false);
    g_mid = env->GetMethodID(
            env->FindClass(env->GetStringUTFChars(fullQualifiedClassName, (jboolean *) false)),
            functionName, "(I)V");

    libusb_device_handle *devHandle = nullptr;
    devHandle = static_cast<libusb_device_handle *>(env->GetDirectBufferAddress(nativeHandle));

    libusb_transfer *transferUsed = static_cast<libusb_transfer *>(env->GetDirectBufferAddress(
            transfer));
    void *userData = static_cast<void *>(env->GetDirectBufferAddress(user_data));

    transferUsed->callback = &transferCallback;
    transferUsed->user_data = userData;

    for (int i = 0; i < devHandleVector.size(); i++) {
        if (devHandleVector[i] == devHandle) {

            libusb_fill_bulk_transfer(transferUsed, devHandle, static_cast<uint8_t>(endpoint), data,
                                      static_cast<int>(length), transferUsed->callback, userData,
                                      static_cast<unsigned int>(timeout));
            env->ReleaseByteArrayElements(buffer, (jbyte *) data, 0);

            return;

        }

    }
    ThrowLibusbException(env, LIBUSB_ERROR_NO_DEVICE);
    return;
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    fillControlTransfer
 * Signature: (Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;I[BILjava/lang/String;Ljava/lang/Object;I)I
 */
extern "C" JNIEXPORT void JNICALL
Java_ch_ntb_inf_libusb_Libusb_fillControlTransfer(JNIEnv *env, jclass obj, jobject transfer,
                                                  jobject nativeHandle, jint endpoint,
                                                  jbyteArray buffer, jint length,
                                                  jstring callbackFunction, jobject user_data,
                                                  jint timeout, jstring fullQualifiedClassName) {
    if (!nativeHandle || (endpoint < 0) || !buffer) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return;
    }
    int bytes_transferred = 0;
    unsigned char *data = reinterpret_cast<unsigned char *>(env->GetByteArrayElements(buffer,
                                                                                      NULL));

#ifdef DEBUGON
    //	cout << "data: " << data << endl;
#endif

    g_obj = env->NewGlobalRef(obj);
    jclass g_clazz = env->GetObjectClass(g_obj);

    const char *functionName = env->GetStringUTFChars(callbackFunction, (jboolean *) false);
    g_mid = env->GetMethodID(
            env->FindClass(env->GetStringUTFChars(fullQualifiedClassName, (jboolean *) false)),
            functionName, "(I)V");

    libusb_device_handle *devHandle = nullptr;
    devHandle = static_cast<libusb_device_handle *>(env->GetDirectBufferAddress(nativeHandle));

    libusb_transfer *transferUsed = static_cast<libusb_transfer *>(env->GetDirectBufferAddress(
            transfer));
    void *userData = static_cast<void *>(env->GetDirectBufferAddress(user_data));

    transferUsed->callback = &transferCallback;
    transferUsed->user_data = userData;

    for (int i = 0; i < devHandleVector.size(); i++) {
        if (devHandleVector[i] == devHandle) {

            libusb_fill_control_transfer(transferUsed, devHandle, data, transferUsed->callback,
                                         userData, static_cast<unsigned int>(timeout));
            env->ReleaseByteArrayElements(buffer, (jbyte *) data, 0);

            return;

        }

    }
    ThrowLibusbException(env, LIBUSB_ERROR_NO_DEVICE);
    return;
}

extern "C"
JNIEXPORT void JNICALL
Java_ch_ntb_inf_libusb_Libusb_fillBulkStreamTransfer(JNIEnv *env, jclass clazz, jobject transfer,
                                                     jobject native_handle, jint endpoint,
                                                     jint stream_id, jbyteArray buffer, jint length,
                                                     jstring callback_function_name,
                                                     jobject user_data, jint timeout,
                                                     jstring full_qualified_class_name) {
    if (!native_handle || (endpoint < 0) || !buffer) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return;
    }
    int bytes_transferred = 0;
    unsigned char *data = reinterpret_cast<unsigned char *>(env->GetByteArrayElements(buffer,
                                                                                      NULL));

#ifdef DEBUGON
    //	cout << "data: " << data << endl;
#endif

    g_obj = env->NewGlobalRef(clazz);
    jclass g_clazz = env->GetObjectClass(g_obj);

    g_mid = env->GetMethodID(env->GetObjectClass(clazz),
                             env->GetStringUTFChars(callback_function_name, (jboolean *) false), "(I)V");


    libusb_device_handle *devHandle = nullptr;
    devHandle = static_cast<libusb_device_handle *>(env->GetDirectBufferAddress(native_handle));

    libusb_transfer *transferUsed = static_cast<libusb_transfer *>(env->GetDirectBufferAddress(
            transfer));
    void *userData = static_cast<void *>(env->GetDirectBufferAddress(user_data));

    transferUsed->callback = &transferCallback;
    transferUsed->user_data = userData;

    for (int i = 0; i < devHandleVector.size(); i++) {
        if (devHandleVector[i] == devHandle) {

            libusb_fill_bulk_stream_transfer(transferUsed, devHandle,
                                             static_cast<unsigned char>(endpoint),
                                             static_cast<uint32_t>(stream_id), data,
                                             static_cast<int>(length), transferUsed->callback,
                                             userData, static_cast<unsigned int>(timeout));
            env->ReleaseByteArrayElements(buffer, (jbyte *) data, 0);

            return;

        }

    }
    ThrowLibusbException(env, LIBUSB_ERROR_NO_DEVICE);
    return;
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    submitTransfer
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_submitTransfer(JNIEnv *env, jclass obj, jobject transfer) {
    if (!transfer) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return -1;
    }
    libusb_transfer *transferUsed = static_cast<libusb_transfer *>(env->GetDirectBufferAddress(
            transfer));

    int retVal = libusb_submit_transfer(transferUsed);
    return retVal;
}

JNIEnv *currentEnv;
jclass currentObj;
/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    handleEvents
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
extern "C" JNIEXPORT jint JNICALL
Java_ch_ntb_inf_libusb_Libusb_handleEvents(JNIEnv *env, jclass obj, jobject ctx) {
    currentEnv = env;
    currentObj = obj;
    libusb_context *context = nullptr;

    if (ctx) {
        context = static_cast<libusb_context *>(env->GetDirectBufferAddress(ctx));
    }

    int retVal = libusb_handle_events(context);

    currentEnv = NULL;
    currentObj = NULL;
    return retVal;
}


static void LIBUSB_CALL transferCallback(struct libusb_transfer *transfer) {
    JNIEnv *g_env;
    if (g_env == NULL) {
        int getEnvStat = g_jvm->GetEnv((void **) &g_env, JNI_VERSION_1_4);
    }
    currentEnv->CallVoidMethod(currentObj, g_mid, transfer->status);
}


/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    freeTransfer
 * Signature: (Ljava/nio/ByteBuffer;)I
 */
extern "C" JNIEXPORT void JNICALL
Java_ch_ntb_inf_libusb_Libusb_freeTransfer(JNIEnv *env, jclass obj, jobject transfer) {
    if (!transfer) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return;
    }
    libusb_transfer *transferUsed = static_cast<libusb_transfer *>(env->GetDirectBufferAddress(
            transfer));

    libusb_free_transfer(transferUsed);

    return;
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    errorName
 * Signature: (I)Ljava/lang/String;
 */
extern "C" JNIEXPORT jstring JNICALL
Java_ch_ntb_inf_libusb_Libusb_errorName(JNIEnv *env, jclass obj, jint errorName) {
    jstring str = env->NewStringUTF(libusb_error_name(static_cast<int>(errorName)));
    return str;
}

/*
 * Class:     ch_ntb_inf_libusb_Libusb
 * Method:    setIsoPacketLength
 * Signature: (Ljava/nio/ByteBuffer;I)V
 */
extern "C" JNIEXPORT void JNICALL
Java_ch_ntb_inf_libusb_Libusb_setIsoPacketLength(JNIEnv *env, jclass obj, jobject transfer,
                                                 jint isoLength) {
    if (!transfer) {
        ThrowLibusbException(env, LIBUSB_ERROR_INVALID_PARAM);
        return;
    }
    libusb_transfer *transferUsed = static_cast<libusb_transfer *>(env->GetDirectBufferAddress(
            transfer));
    libusb_set_iso_packet_lengths(transferUsed, static_cast<unsigned int>(isoLength));

    return;
}