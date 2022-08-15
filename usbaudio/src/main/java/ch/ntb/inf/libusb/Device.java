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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import ch.ntb.inf.libusb.ConfigurationDescriptor;
import ch.ntb.inf.libusb.Context;
import ch.ntb.inf.libusb.Device;
import ch.ntb.inf.libusb.DeviceDescriptor;
import ch.ntb.inf.libusb.Interface;
import ch.ntb.inf.libusb.InterfaceDescriptor;
import ch.ntb.inf.libusb.Libusb;
import ch.ntb.inf.libusb.Speed;
import ch.ntb.inf.libusb.exceptions.*;
import ch.ntb.inf.libusb.test.LibusbJavaListTest;

public class Device {

    private static final boolean testing = false;

    private DeviceDescriptor devDesc;
    private static int nofDevices = 0;
    private static List<Device> devList;
    private static boolean deviceListValid = false;

    private static List<Device> duplicateDevs;

    private Context ctx;

    private ByteBuffer nativeDevice;
    private ByteBuffer nativeHandle;
    private ByteBuffer transfer;

    //parameters of last open
    private int cfgValue = -2;
    private int ifaceNum = -2;
    private int altSetting = -2;

    //	private Device parent;
    static {
        devList = new ArrayList<Device>();
        duplicateDevs = new ArrayList<Device>();
    }

    public Device(Context ctx, ByteBuffer nativeDevice) { // TODO protected or not???
        // null context represents default context and is a valid parameter
        if (nativeDevice == null) {
            throw new NullPointerException("nativeDevice null");
        }
        this.ctx = ctx;
        this.nativeDevice = nativeDevice;
        nofDevices++;
    }

    private Device(Context ctx) {
        this.ctx = ctx;
        nofDevices++;
    }

    /**
     * search a desired device, this has limitations, if you have more
     * than one device with the same vid, pid you get just the first in the list
     *
     * @param ctx context for search, if null, standard context will be used
     * @param vid desired vid
     * @param pid desired pid
     * @return Device with desired parameters
     * @throws LibusbException
     */
    //TODO probably extend, that you see how many devices with this vid, pid are available and choose one then
    public static Device search(Context ctx, int vid, int pid) throws LibusbException {
        if (duplicateDevs.size() != 0) {
            duplicateDevs.clear();
        }

        getList(ctx);
        for (int i = 0; i < devList.size(); i++) {
            if (Context.dbg) {
                System.out.println("Dev" + i + ":");
                System.out.println("vid: " + devList.get(i).getDeviceDescriptor().getVendorId());
            }
            if (vid == devList.get(i).getDeviceDescriptor().getVendorId()) {
                if (Context.dbg) {
                    System.out.println("pid: " + devList.get(i).getDeviceDescriptor().getProductId());
                }
                if (pid == devList.get(i).getDeviceDescriptor().getProductId()) {
                    duplicateDevs.add(devList.get(i));
                }
            }
        }
        if (duplicateDevs.size() > 0) {
            return duplicateDevs.get(0);
        }
        throw new NoDeviceException();
    }

    public static int getNofDuplicates() {
        return duplicateDevs.size();
    }

    public static Device getDuplicate(Context ctx, int vid, int pid, int nofDuplicate) throws LibusbException {
        if (!deviceListValid) {
            throw new DeviceListNotValidException();
        }
        if (duplicateDevs.size() > 0 && nofDuplicate <= duplicateDevs.size()) {
            if (duplicateDevs.get(nofDuplicate).getDeviceDescriptor().getProductId() == pid && duplicateDevs.get(nofDuplicate).getDeviceDescriptor().getVendorId() == vid) {
                return duplicateDevs.get(nofDuplicate);
            }
            throw new NoDeviceException();
        }
        throw new NoDeviceException();
    }

    private static void getList(Context ctx) throws LibusbException {
        List<ByteBuffer> nativeList;
        if (testing) {
            nativeList = LibusbJavaListTest.getDeviceList(ctx.getNativePointer());
        } else {
            nativeList = Libusb.getDeviceList(ctx.getNativePointer());
        }

        if (devList.size() > 0) {    //refresh
            boolean devInList = false;

            //check if device already in the List or if one got cleared out
            for (int i = 0; i < devList.size(); i++) {
                devInList = false;
                for (int j = 0; j < nativeList.size() && !devInList; j++) {
                    if (devList.get(i).getNativeDevice() == nativeList.get(j)) {
                        //device found
                        devInList = true;
                        if (i != j) {
                            //list changed
                            devList.add(i + 1, devList.get(i));
                            devList.remove(i);
                            devList.add(i, new Device(ctx, nativeList.get(i)));
                        }
                    }
                }
                if (!devInList) {
                    devList.remove(i);        //element at position i is not in native List anymore -> remove
                    i--;                    //make sure that new element i will be checked
                }
            }
            if (devList.size() < nativeList.size()) {
                //add new devices at the end
                for (int i = devList.size(); i < nativeList.size(); i++) {
                    devList.add(i, new Device(ctx, nativeList.get(i)));
                }
            }
        } else {
            for (int i = 0; i < nativeList.size(); i++) {
                devList.add(new Device(ctx, nativeList.get(i)));
            }
        }
        deviceListValid = true;
    }

    /**
     * free the device list
     *
     * @param ctx
     * @param unrefDev
     * @throws LibusbException
     */
    public static void freeList(Context ctx, boolean unrefDev) throws LibusbException {
        if (deviceListValid) {
            if (ctx.getNativePointer() == devList.get(0).getContext().getNativePointer()) {
                Libusb.freeDeviceList(unrefDev);
                deviceListValid = false;
                if (Context.dbg) System.out.println("Device List freed.");
            } else {
                throw new WrongContextException();
            }
        } else {
            throw new DeviceListNotValidException();
        }
    }

    /**
     * gives bus number where device is attached
     *
     * @return bus number
     */
    public int getBusNumber() {
        if (nativeDevice == null) {
            throw new NullPointerException("nativeDevice null");
        }
        return Libusb.getBusNumber(nativeDevice);
    }

    /**
     * gives port number where device is attached
     *
     * @return port number
     */
    public int getPortNumber() {
        if (nativeDevice == null) {
            throw new NullPointerException("nativeDevice null");
        }
        return Libusb.getPortNumber(nativeDevice);
    }

    /**
     * gives all port numbers from device to root
     *
     * @param portNumbers array for portNumbers
     * @return length of portNumbers or error number, can be ignored already thrown a LibusbException with this code
     * @throws LibusbException
     */
    public int getPortNumbers(int[] portNumbers) throws LibusbException {
        if (portNumbers == null) {
            throw new NullPointerException("portNumbers null");
        }
        if (nativeDevice == null) {            // check for native device; ctx has to be initialized when there is a Device
            throw new NullPointerException("nativeDevice null");
        }
        return Libusb.getPortNumbers(ctx.getNativePointer(), nativeDevice, portNumbers);
    }

    /**
     * get parent Device, only valid as long as device list is not freed in native code with libusb_free_device_list!
     *
     * @return parent Device
     * @throws LibusbException
     */
    public Device getParent() throws LibusbException {
        if (!deviceListValid) {
            throw new DeviceListNotValidException();
        }
        if (nativeDevice == null) {
            throw new NullPointerException("nativeDevice null");
        }
        ByteBuffer nativeParent = Libusb.getParent(nativeDevice);
        if (nativeParent == null) {
            return null;            //no parent available
        }
        for (int i = 0; i < devList.size(); i++) {
            if (nativeParent == devList.get(i).nativeDevice) {
                return devList.get(i);
            }
        }
        return new Device(ctx, nativeParent);
    }

    /**
     * get address of Device
     *
     * @return device address
     */
    public int getDeviceAddress() {
        if (nativeDevice == null) {
            throw new NullPointerException("nativeDevice null");
        }
        return Libusb.getDeviceAddress(nativeDevice);
    }

    /**
     * get negotiated device speed
     *
     * @return device speed
     */
    public Speed getDeviceSpeed() {
        if (nativeDevice == null) {
            throw new NullPointerException("nativeDevice null");
        }
        return Speed.valueOf(Libusb.getDeviceSpeed(nativeDevice));
    }

//	public void refDevice(){
//		//TODO increment the reference count of a device
//	}
//	
//	public void unrefDevice(){
//		//TODO decrement the reference count of a device, if the decrement operation causes the reference count to reach zero, the device shall be destroyed.
//	}

    /**
     * open this device
     *
     * @throws LibusbException on failure
     */
    public void open() throws LibusbException {
        if (nativeDevice == null) {
            throw new NullPointerException("nativeDevice null");
        }
        nativeHandle = Libusb.open(nativeDevice);
        //TODO think about freeing: is a bit delicate because, if using Java Objects with dev Pointer in it, could go wrong if using underlaying infos, so don't free it automatically
        if (nativeHandle != null) {    //error occured during open, so we probably need Device List again
            freeList(ctx, true);    //otherwise: clean up Device List
        }
    }

    /**
     * open device with following parameters
     *
     * @param ctx context for search, if null, standard context will be used
     * @param vid desired vid
     * @param pid desired pid
     * @return device
     * @throws LibusbException on failure or device not found
     */
    public static Device open(Context ctx, int vid, int pid) throws LibusbException {
        ByteBuffer handle = Libusb.openVidPid(ctx.getNativePointer(), vid, pid);
        if (handle != null) {                    // Exception already thrown in JNI if went wrong
            devList.add(new Device(ctx));
            devList.get(devList.size() - 1).setNativeHandle(handle);
            deviceListValid = true;
            return devList.get(devList.size() - 1);
        }
        return null;
    }

    public void open(ConfigurationDescriptor cfgDesc, Interface iface, InterfaceDescriptor altSetting) throws LibusbException {
        throw new NotSupportedException();
        //TODO
    }

    /**
     * open this device with following parameters
     *
     * @param cfgValue   desired configuration descriptor
     * @param ifaceNum   desired interface number
     * @param altSetting desired alternate setting
     * @throws LibusbException on failure
     */
    public void open(int cfgValue, int ifaceNum, int altSetting) throws LibusbException {
        if (cfgValue < -1 && ifaceNum < -1 && altSetting < -1) {    //TODO think about init-values and how to handle exception
            throw new InvalidParameterException();
        }
        //open device
        nativeHandle = Libusb.open(nativeDevice);
        //claim interface
        try {
            Libusb.claimInterface(nativeHandle, ifaceNum);    //Exception already thrown from JNI if something went wrong
            this.cfgValue = cfgValue;
            this.ifaceNum = ifaceNum;
            this.altSetting = altSetting;
        } catch (LibusbException e) {
            throw e;
        }
        if (altSetting > 0) {        //use non-standard alt-setting
            Libusb.setInterfaceAltSetting(nativeDevice, ifaceNum, altSetting);
        }

    }

    /**
     * close this device
     *
     * @throws LibusbException
     */
    public void close() throws LibusbException {
//		releaseInterface(ifaceNum);
        if (nativeHandle == null) {
            throw new NullPointerException("nativeHandle null");
        } else {
            Libusb.close(nativeHandle);
            nativeHandle = null;        //destroy handle in Java too
        }
    }

    /**
     * check if this device is opened
     *
     * @return true if open, false if closed
     */
    public boolean isOpen() {
        if (nativeHandle == null) {
            return false;
        }
        return true;
    }

    /**
     * get device descriptor of this device
     *
     * @return DeviceDesriptor
     * @throws LibusbException
     */
    public DeviceDescriptor getDeviceDescriptor() throws LibusbException {
        devDesc = new DeviceDescriptor(this);
        return devDesc;
    }

    /**
     * reset this device
     *
     * @throws LibusbException
     */
    public void reset() throws LibusbException {
        if (nativeHandle == null) {
            throw new JavaIllegalDeviceHandleException();
        }
        int retVal = Libusb.resetDevice(nativeHandle);
        if (retVal < 0) {                //exception already thrown in JNI
            nativeHandle = null;
        }
    }

    /**
     * @param requestType
     * @param request
     * @param value
     * @param index
     * @param data
     * @param length
     * @param timeout
     * @return
     * @throws LibusbException
     */
    public int controlTransfer(byte requestType, StandardRequest request, short value, short index, byte[] data, int length, int timeout) throws LibusbException {
        if (length > data.length) {
            throw new InvalidParameterException();
        }
        int retVal = Libusb.controlTransfer(nativeHandle, requestType, (byte) request.getCode(), value, index, data, length, timeout);
        return retVal;
    }

    /**
     * initiates a bulk transfer
     *
     * @param endpoint
     * @param data
     * @param length
     * @param timeout
     * @return number of transfered bytes
     * @throws LibusbException
     */
    public int bulkTransfer(int endpoint, byte[] data, int length, int timeout) throws LibusbException {
        if (length > data.length) {
            throw new InvalidParameterException();
        }
        int retVal = Libusb.bulkTransfer(nativeHandle, endpoint, data, length, timeout); //exceptions will be thrown in JNI
        return retVal;    //think about if Error Handling sufficient
    }

    /**
     * read data from device with a bulk transfer
     *
     * @param endpoint
     * @param data
     * @param length
     * @param timeout
     * @return number of read bytes
     * @throws LibusbException
     */
    public int readBulk(int endpoint, byte[] data, int length, int timeout) throws LibusbException {
        return bulkTransfer((endpoint | 0x80), data, length, timeout);
    }

    /**
     * read data from device with a bulk transfer, with option to reopen device on timeout and try again
     *
     * @param endpoint
     * @param data
     * @param length
     * @param timeout
     * @param reOpenOnTimeout
     * @return number of read bytes
     * @throws LibusbException
     */
    public int readBulk(int endpoint, byte[] data, int length, int timeout, boolean reOpenOnTimeout) throws LibusbException {
        int cnt = 0;
        try {
            cnt = bulkTransfer((endpoint | 0x80), data, length, timeout);
        } catch (TimeoutException e) {
            if (reOpenOnTimeout) {
                reset();
                open(cfgValue, ifaceNum, altSetting);
                return readBulk(endpoint, data, length, timeout, false);
            }
            throw e;
        }
        return cnt;
    }

    /**
     * write data to device with a bulk transfer
     *
     * @param endpoint
     * @param data
     * @param length
     * @param timeout
     * @return number of bytes written to device
     * @throws LibusbException
     */
    public int writeBulk(int endpoint, byte[] data, int length, int timeout) throws LibusbException {
        return bulkTransfer((endpoint & (~0x80)), data, length, timeout);
    }

    /**
     * write data to device with a bulk transfer, with option to reopen device on timeout and try again
     *
     * @param endpoint
     * @param data
     * @param length
     * @param timeout
     * @param reOpenOnTimeout
     * @return number of bytes written to device
     * @throws LibusbException
     */
    public int writeBulk(int endpoint, byte[] data, int length, int timeout, boolean reOpenOnTimeout) throws LibusbException {
        int cnt = 0;
        try {
            cnt = bulkTransfer((endpoint & (~0x80)), data, length, timeout);
        } catch (TimeoutException e) {
            if (reOpenOnTimeout) {
                reset();
                open(cfgValue, ifaceNum, altSetting);
                return writeBulk(endpoint, data, length, timeout, false);
            }
            throw e;
        }
        return cnt;
    }


    /**
     * read/write data to endpoint with an interrupt transfer
     *
     * @param endpoint
     * @param data
     * @param length
     * @param timeout
     * @return number of bytes written to device
     * @throws LibusbException
     */
    public int interruptTransfer(int endpoint, byte[] data, int length, int timeout) throws LibusbException {
        int retVal;
        if (length > data.length) {
            throw new InvalidParameterException();
        }

        retVal = Libusb.interruptTransfer(nativeHandle, endpoint, data, length, timeout); //exceptions will be thrown in JNI

        return retVal;

    }

    public int writeInterrupt(int endpoint, byte[] data, int length, int timeout) throws LibusbException {
        return interruptTransfer(endpoint & ~(0x80), data, length, timeout);
    }

    public int readInterrupt(int endpoint, byte[] data, int length, int timeout) throws LibusbException {
        return interruptTransfer(endpoint | 0x80, data, length, timeout);
    }

    protected void finalize() {
        Libusb.unrefDevice(nativeDevice);
        nofDevices--;
    }

    /**
     * get the number of created devices
     *
     * @return number of devices
     */
    public static int getNofDevices() {
        return nofDevices;
    }

    /**
     * get the context this device is using
     *
     * @return context
     */
    public Context getContext() {
        return ctx;
    }

    /**
     * get native device pointer, can't be used directly, must be wrapped through jni, so no real use for user
     *
     * @return native device pointer
     */
    public ByteBuffer getNativeDevice() {
        return nativeDevice;
    }

    private void setNativeHandle(ByteBuffer nativeHandle) {
        this.nativeHandle = nativeHandle;
    }

    /**
     * claim the interface specified by its number
     *
     * @param interfaceNumber
     * @throws LibusbException
     */
    public void claimInterface(int interfaceNumber) throws LibusbException {
        Libusb.claimInterface(nativeHandle, interfaceNumber);
        this.ifaceNum = interfaceNumber;
    }

    /**
     * release the interface specified by its number
     *
     * @param interfaceNumber
     * @throws LibusbException
     */
    public void releaseInterface(int interfaceNumber) throws LibusbException {
        Libusb.relaseInterface(nativeHandle, interfaceNumber);
    }

    /**
     * get the string descriptor of this device
     *
     * @param descriptorIndex
     * @param langId
     * @return string descriptor in UNICODE format, see USB Specifications
     * @throws LibusbException
     */
    public String getStringDescriptor(int descriptorIndex, int langId) throws LibusbException {
        return Libusb.getStringDescriptor(nativeHandle, descriptorIndex, langId);
    }

    /**
     * get the string descriptor of this device
     *
     * @param descriptorIndex
     * @return string descriptor in ASCII format
     * @throws LibusbException
     */
    public String getStringDescriptorAscii(int descriptorIndex) throws LibusbException {
        return Libusb.getStringDescriptorAscii(nativeHandle, descriptorIndex);
    }


    /**
     * Allocates a libusb transfer
     *
     * @param isoPackets number of iso packets
     * @return true on success
     * @throws LibusbException
     */
    public boolean allocTransfer(int isoPackets) throws LibusbException {
        if (isoPackets < 0) throw new InvalidParameterException();
        this.transfer = Libusb.allocTransfer(isoPackets);
        if (this.transfer != null) return true;
        return false;
    }

    /**
     * Fills the allocated transfer with isochronous data
     *
     * @param endpoint
     * @param data
     * @param length
     * @param numIsoPackets
     * @param callbackFunctionName
     * @param userData
     * @param timeout
     * @param fullQualifiedClassName
     * @throws LibusbException
     */
    public void fillIsoTransfer(int endpoint, byte[] data, int length, int numIsoPackets, String callbackFunctionName, Object userData, int timeout, String fullQualifiedClassName) throws LibusbException {
        if (length > data.length) {
            throw new InvalidParameterException();
        }

        Libusb.fillIsoTransfer(transfer, nativeHandle, endpoint, data, length, numIsoPackets, callbackFunctionName, userData, timeout, fullQualifiedClassName);
    }

    /**
     * Fills the transfer with interrupt data
     *
     * @param endpoint
     * @param data
     * @param length
     * @param callbackFunctionName
     * @param userData
     * @param timeout
     * @param fullQualifiedClassName
     * @throws LibusbException
     */
    public void fillInterruptTransfer(int endpoint, byte[] data, int length, String callbackFunctionName, Object userData, int timeout, String fullQualifiedClassName) throws LibusbException {
        if (length > data.length) {
            throw new InvalidParameterException();
        }

        Libusb.fillInterruptTransfer(transfer, nativeHandle, endpoint, data, length, callbackFunctionName, userData, timeout, fullQualifiedClassName);
    }

    /**
     * Fills the allocated transfer with bulk data
     *
     * @param endpoint
     * @param data
     * @param length
     * @param callbackFunctionName
     * @param userData
     * @param timeout
     * @param fullQualifiedClassName
     * @throws LibusbException
     */
    public void fillBulkTransfer(int endpoint, byte[] data, int length, String callbackFunctionName, Object userData, int timeout, String fullQualifiedClassName) throws LibusbException {
        if (length > data.length) {
            throw new InvalidParameterException();
        }

        Libusb.fillBulkTransfer(transfer, nativeHandle, endpoint, data, length, callbackFunctionName, userData, timeout, fullQualifiedClassName);
    }

    /**
     * Fills the allocated transfer with control data
     *
     * @param endpoint
     * @param data
     * @param length
     * @param callbackFunctionName
     * @param userData
     * @param timeout
     * @param fullQualifiedClassName
     * @throws LibusbException
     */
    public void fillControlTransfer(int endpoint, byte[] data, int length, String callbackFunctionName, Object userData, int timeout, String fullQualifiedClassName) throws LibusbException {
        if (length > data.length) {
            throw new InvalidParameterException();
        }

        Libusb.fillControlTransfer(transfer, nativeHandle, endpoint, data, length, callbackFunctionName, userData, timeout, fullQualifiedClassName);
    }


    /**
     * Fills the allocated transfer with bulk stream data
     *
     * @param endpoint
     * @param streamID
     * @param data
     * @param length
     * @param callbackFunctionName
     * @param userData
     * @param timeout
     * @param fullQualifiedClassName
     * @throws LibusbException
     */
    public void fillBulkStreamTransfer(int endpoint, int streamID, byte[] data, int length, String callbackFunctionName, Object userData, int timeout, String fullQualifiedClassName) throws LibusbException {
        if (length > data.length) {
            throw new InvalidParameterException();
        }

        Libusb.fillBulkStreamTransfer(transfer, nativeHandle, endpoint, streamID, data, length, callbackFunctionName, userData, timeout, fullQualifiedClassName);
    }

    /**
     * Submits the allocated and filled transfer
     *
     * @return 0 on success, libusb_error code on failure
     */
    public int submitTransfer() {
        return Libusb.submitTransfer(transfer);
    }

    /**
     * Eventhandler for asychnronous transfers
     *
     * @param context
     * @return 0 on success, libusb_error code on failure
     */
    public int handleEvents(Context context) {
        return Libusb.handleEvents(context.getNativePointer());
    }

    /**
     * Frees the allocated transfer
     */
    public void freeTransfer() {
        Libusb.freeTransfer(transfer);
    }


    /**
     * @param errorCode
     * @return name of the error
     */
    public String errorName(int errorCode) {
        return Libusb.errorName(errorCode);
    }

    /**
     * Sets the isoPacketLength of the transfer
     *
     * @param isoPacketLength
     */
    public void setIsoPacketLength(int isoPacketLength) {
        Libusb.setIsoPacketLength(transfer, isoPacketLength);
    }
}
