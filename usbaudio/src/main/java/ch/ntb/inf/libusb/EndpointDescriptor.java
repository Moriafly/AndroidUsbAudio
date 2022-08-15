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

import ch.ntb.inf.libusb.Device;
import ch.ntb.inf.libusb.InterfaceDescriptor;
import ch.ntb.inf.libusb.Libusb;
import ch.ntb.inf.libusb.TransferType;
import ch.ntb.inf.libusb.exceptions.LibusbException;

public class EndpointDescriptor extends InterfaceDescriptor{

	protected int endpointIndex;
	
	protected EndpointDescriptor(Device device, int configIndex, int ifaceIndex, int altSetting, int endpointIndex){
		super(device, configIndex, ifaceIndex, altSetting);
		this.endpointIndex = endpointIndex;
	}
	
	/**
	 * get maximum packet size for endpoint
	 * @return maximum packet size
	 * @throws LibusbException
	 */
	public int getMaxPacketSize() throws LibusbException{
		return Libusb.getMaxPacketSize(device.getNativeDevice(), configIndex, ifaceIndex, altSetting, endpointIndex);
	}
	
	/**
	 * get maximum isochronous packet size
	 * @return maximum isochronous packet size
	 * @throws LibusbException
	 */
	public int getMaxIsoPacketSize() throws LibusbException{
		//TODO, throws LIBUSB_ERROR_NOT_FOUND, LIBUSB_ERROR_OTHER
		return Libusb.getMaxIsoPacketSize(device.getNativeDevice(), configIndex, ifaceIndex, altSetting, endpointIndex);
	}
	
	/**
	 * NOT SUPPORTED at the moment
	 */
	public void clearHalt(){
		//TODO throws LIBUSB_ERROR_NOT_FOUND, LIBUSB_ERROR_NO_DEVICE, another LIBUSB_ERROR code on other failure
	}
	
	/**
	 * get address of desired endpoint
	 * @return endpoint address
	 * @throws LibusbException
	 */
	public int getEndpointAddress() throws LibusbException{
		return Libusb.getEndpointAddress(device.getNativeDevice(), configIndex, ifaceIndex, altSetting, endpointIndex);
	}
	
	/**
	 * get endpoint direction
	 * @return true if Endpoint-In (device-to-host), false if Endpoint-Out (host-to-device)
	 * @throws LibusbException
	 */
	public boolean getEndpointDirection() throws LibusbException{
		if((Libusb.getEndpointAddress(device.getNativeDevice(), configIndex, ifaceIndex, altSetting, endpointIndex) & 0x80) == 0x80){
			//endpoint In
			return true;
		}
		else{
			//endpoint Out
			return false;
		}
	}
	
	/**
	 * get type of transfer
	 * @return TransferType
	 * @throws LibusbException
	 */
	public TransferType getTransferType() throws LibusbException{
		return TransferType.valueOf(Libusb.getTransferType(device.getNativeDevice(), configIndex, ifaceIndex, altSetting, endpointIndex));
	}
}
