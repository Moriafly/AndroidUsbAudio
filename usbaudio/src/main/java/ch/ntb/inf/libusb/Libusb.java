/*
 * Java wrapper for libusb
 * 
 * Copyright 2015 - 2018 NTB University of Applied Sciences in Technology
 * Buchs, Switzerland, http://www.ntb.ch/inf
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package ch.ntb.inf.libusb;

import java.nio.ByteBuffer;
import java.util.List;

import ch.ntb.inf.libusb.Context;
import ch.ntb.inf.libusb.exceptions.LibusbException;

/**
 * JNI methods for libusb
 * @author A. Kalberer, S. Fink
 * @version 1_2
 *
 */
public class Libusb {

	/**
	 * Initialize libusb.
	 * This function must be called before calling any other libusb function.
	 * If you do not provide an output location for a context pointer, a default context will be 
	 * created. If there was already a default context, it will be reused (and nothing will be 
	 * initialized/reinitialized).<br>
	 *
	 * @return ctx	
	 * 			Output location for context pointer. Only valid if no LibusbException was thrown.<br>
	 *  See also: {@link Context} 

	 * @throws LibusbException<br>
	 */
	static native ByteBuffer init() throws LibusbException;
	
	/**
	 * Deinitialize libusb.
	 * Should be called after closing all open devices and before your application terminates.
	 * @param ctx 
	 * 			the context to deinitialize, or NULL for the default context 
	 */
	static native void exit(ByteBuffer ctx);
	
	/**
	 * Set log message verbosity.
	 * The default level is LIBUSB_LOG_LEVEL_NONE, which means no messages are ever printed. If you
	 *  choose to increase the message verbosity level, ensure that your application does not close 
	 *  the stdout/stderr file descriptors.<br>
	 * You are advised to use level LIBUSB_LOG_LEVEL_WARNING. libusb is conservative with its message 
	 * logging and most of the time, will only log messages that explain error conditions and other 
	 * oddities. This will help you debug your software.<br>
	 * If the LIBUSB_DEBUG environment variable was set when libusb was initialized, this function 
	 * does nothing: the message verbosity is fixed to the value in the environment variable.<br>
	 * If libusb was compiled without any message logging, this function does nothing: you'll never
	 * get any messages.<br>
	 * If libusb was compiled with verbose debug message logging, this function does nothing: you'll 
	 * always get messages from all levels.<br>
	 *
	 * @param ctx	
	 * 			the context to operate on, or NULL for the default context
	 * @param level	
	 * 			debug level to set 
	 */
	static native void setDebug(ByteBuffer ctx, int level);
	
	static native List<ByteBuffer> getDeviceList(ByteBuffer ctx);

	static native void freeDeviceList(boolean unrefDev);
	
	static native int getBusNumber(ByteBuffer nativeDevice);
	
	static native int getPortNumber(ByteBuffer nativeDevice);
	
	static native void unrefDevice(ByteBuffer nativeDevice);
	
	static native int getPortNumbers(ByteBuffer ctx, ByteBuffer nativeDevice, int[] portNumbers) throws LibusbException;
	
	static native ByteBuffer getParent(ByteBuffer nativeDevice);
		
	static native int getVendorId(ByteBuffer nativeDevice);

	static native int getProductId(ByteBuffer nativeDevice);
	
	static native int getNofConfigurations(ByteBuffer nativeDevice);

	static native ByteBuffer open(ByteBuffer nativeDevice) throws LibusbException;
	
	static native void close(ByteBuffer nativeHandle) throws LibusbException;

	static native int getDeviceAddress(ByteBuffer nativeDevice);
	
	static native int getDeviceSpeed(ByteBuffer nativeDevice);
	
	static native int getActiveConfigDescriptor(ByteBuffer nativeDevice) throws LibusbException;
	
	static native int getConfigValue(ByteBuffer nativeDevice, int configIndex);
	
	static native int getNofInterfaces(ByteBuffer nativeDevice, int configIndex);
		
	static native int getNofAltSettings(ByteBuffer nativeDevice, int configIndex) throws LibusbException;
	
	static native int getInterfaceNumber(ByteBuffer nativeDevice, int configIndex, int ifaceIndex, int altSetting) throws LibusbException;

	static native int getAlternateSetting(ByteBuffer nativeDevice, int configIndex, int ifaceIndex, int altSetting) throws LibusbException;

	static native int getNofEndpoints(ByteBuffer nativeDevice, int configIndex, int ifaceIndex, int altSetting) throws LibusbException;

	static native int getEndpointAddress(ByteBuffer nativeDevice, int configIndex, int ifaceIndex, int altSetting, int endpointIndex) throws LibusbException;

	static native int getMaxPacketSize(ByteBuffer nativeDevice,	int configIndex, int ifaceIndex, int altSetting, int endpointIndex) throws LibusbException;

	static native int getMaxIsoPacketSize(ByteBuffer nativeDevice, int configIndex, int ifaceIndex, int altSetting, int endpointIndex) throws LibusbException;

	static native void claimInterface(ByteBuffer nativeHandle, int ifaceNum) throws LibusbException;
	
	static native int releaseInterface(ByteBuffer nativeHandle, int ifaceNum) throws LibusbException;

	static native int resetDevice(ByteBuffer nativeHandle) throws LibusbException;

	static native int getTransferType(ByteBuffer nativeDevice, int configIndex, int ifaceIndex, int altSetting, int endpointIndex) throws LibusbException;

	static native int getSerialNumberIndex(ByteBuffer nativeDevice) throws LibusbException;

	static native String getStringDescriptor(ByteBuffer nativeHandle, int descriptorIndex, int langId) throws LibusbException;

	static native String getStringDescriptorAscii(ByteBuffer nativeHandle, int descriptorIndex) throws LibusbException;

	static native int getManufacturerDescIndex(ByteBuffer nativeDevice) throws LibusbException;

	static native int getProductDescIndex(ByteBuffer nativeDevice) throws LibusbException;

	static native void relaseInterface(ByteBuffer nativeHandle, int interfaceNumber) throws LibusbException;

	static native void setInterfaceAltSetting(ByteBuffer nativeHandle, int ifaceNum, int altSetting) throws LibusbException;

	static native ByteBuffer openVidPid(ByteBuffer nativePointer, int vid, int pid) throws LibusbException;

	static native int controlTransfer(ByteBuffer nativeHandle, byte requestType, byte request, short value, short index, byte[] data, int length, int timeout) throws LibusbException;
	
	static native int bulkTransfer(ByteBuffer nativeHandle, int endpoint, byte[] data, int length, int timeout) throws LibusbException;
	
	static native int interruptTransfer(ByteBuffer nativeHandle, int endpoint, byte[] data, int length, int timeout) throws LibusbException;
	
	static native ByteBuffer allocTransfer(int isoPackets) throws LibusbException;
	
	static native void fillIsoTransfer(ByteBuffer transfer, ByteBuffer nativeHandle, int endpoint, byte[] buffer, int length, int numIsoPackets, String callbackFunctionName, Object userData, int timeout, String fullQualifiedClassName);
	
	static native void fillInterruptTransfer(ByteBuffer transfer, ByteBuffer nativeHandle, int endpoint, byte[] buffer, int length, String callbackFunctionName, Object userData, int timeout, String fullQualifiedClassName);
	
	static native void fillBulkTransfer(ByteBuffer transfer, ByteBuffer nativeHandle, int endpoint, byte[] buffer, int length, String callbackFunctionName, Object userData, int timeout, String fullQualifiedClassName);
	
	static native void fillControlTransfer(ByteBuffer transfer, ByteBuffer nativeHandle, int endpoint, byte[] buffer, int length, String callbackFunctionName, Object userData, int timeout, String fullQualifiedClassName);
	
	static native void fillBulkStreamTransfer(ByteBuffer transfer, ByteBuffer nativeHandle, int endpoint, int streamID, byte[] buffer, int length, String callbackFunctionName, Object userData, int timeout, String fullQualifiedClassName);
	
	static native int submitTransfer(ByteBuffer transfer);
	
	static native int handleEvents(ByteBuffer context);
	
	static native void freeTransfer(ByteBuffer transfer);
	
	static native String errorName(int errorcode);
	
	static native void setIsoPacketLength(ByteBuffer transfer, int isoLength);
	
}
