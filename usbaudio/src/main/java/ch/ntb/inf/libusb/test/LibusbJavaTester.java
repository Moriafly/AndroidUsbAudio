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

package ch.ntb.inf.libusb.test;

import java.util.List;

import ch.ntb.inf.libusb.ConfigurationDescriptor;
import ch.ntb.inf.libusb.Context;
import ch.ntb.inf.libusb.Device;
import ch.ntb.inf.libusb.DeviceDescriptor;
import ch.ntb.inf.libusb.EndpointDescriptor;
import ch.ntb.inf.libusb.Interface;
import ch.ntb.inf.libusb.InterfaceDescriptor;
import ch.ntb.inf.libusb.exceptions.LibusbException;

public class LibusbJavaTester {

	private static final int MAX_PORT_DEPTH = 7;
	private static int currentStatus;
	
	private final static int vid = 0xAFFE;
	private final static int pid = 0x6001;
	
	public static void main(String[] args){
		LibusbJavaTester ljt = new LibusbJavaTester();
		System.out.println(ljt.getClass().getName());
		System.out.println("start libusbJavaTester");
		Context useCtx = null;
		Device usbDev = null;
		DeviceDescriptor devDesc = null;
		List<ConfigurationDescriptor> configDescriptors;
		ConfigurationDescriptor activeCfgDesc;
		List<Interface> ifaceDev;
		List<InterfaceDescriptor> ifaceDescDev;
		List<EndpointDescriptor> epDescDev;
		System.out.println("UseTests started:");
		try {
			useCtx = new Context();
		} catch (LibusbException e) {
			System.out.println("Init failed:");
			e.printStackTrace();
		}
		System.out.println("search Device:");
		try {
			usbDev = Device.search(useCtx, vid, pid);
			devDesc = usbDev.getDeviceDescriptor();
			System.out.println("Device found with vid 0x" + Integer.toHexString(devDesc.getVendorId()) 
					+ ", pid 0x" + Integer.toHexString(devDesc.getProductId()));
			System.out.println("Bus: " + usbDev.getBusNumber() + " Port: " + usbDev.getPortNumber());
			
		} catch (LibusbException e) {
			System.out.println("Error occured: search");
			e.printStackTrace();
		}
		if(usbDev == null) return;
		System.out.println("finished.");
		int nofPorts = 0;
		int[] portNumbers = new int [MAX_PORT_DEPTH];
		try{
			nofPorts = usbDev.getPortNumbers(portNumbers);
			System.out.print("Port Numbers to root: ");
			for(int i = 0; i < nofPorts; i++){
				System.out.print(portNumbers[i] + " ");
			}
			System.out.println();
			System.out.println("Device Address: 0x" + Integer.toHexString(usbDev.getDeviceAddress()));
			System.out.println("Device Speed: " + usbDev.getDeviceSpeed().toString());
			System.out.println("--Parent Device--");
			Device parentDev = usbDev.getParent();
			if(parentDev != null){
				System.out.println("Parent Device: vid: 0x" + Integer.toHexString(parentDev.getDeviceDescriptor().getVendorId()) 
						+ ", pid: 0x" + Integer.toHexString(parentDev.getDeviceDescriptor().getProductId()));
				System.out.println("Bus: " + parentDev.getBusNumber() + " Port: " + parentDev.getPortNumber());
				System.out.print("Port Numbers to root: ");
				nofPorts = parentDev.getPortNumbers(portNumbers);
				for(int i = 0; i < nofPorts; i++){
					System.out.print(portNumbers[i] + " ");
				}
				System.out.println();
				System.out.println("-----------------");
			}
			else{
				System.out.println("No Parent Device.");
			}			
			
			//ConfigDescriptor

			devDesc = usbDev.getDeviceDescriptor();
			DeviceDescriptor devDescParent = parentDev.getDeviceDescriptor();
			System.out.println("Config Descriptors:\nnofConfigs: " + devDesc.getNofConfigurations());
			
			configDescriptors = devDesc.getConfigDescriptors();
			if(configDescriptors == null){
				System.out.println("configDesc == null");
			}
			for(int i = 0; i < configDescriptors.size(); i++){
				System.out.println("Config " + i + ": configValue: " + configDescriptors.get(i).getConfigValue());
			}
			
			System.out.println("Active Descriptor of devDesc: ");
			activeCfgDesc = devDesc.getActiveConfigDescriptor();
			
			//System.out.println("Active Descriptor of devDescParent:");
			//ConfigurationDescriptor cfgDescParent = devDescParent.getActiveConfigDescriptor();
			
//			System.out.println("--------Interfaces---------");
//			System.out.println("NofInterfaces device: " + activeCfgDesc.getNofInterfaces());
//			System.out.println("NofInterfaces parent: " + cfgDescParent.getNofInterfaces());
			
			ifaceDev = activeCfgDesc.getAllInterfaces();
//			List<Interface> ifaceParent = cfgDescParent.getAllInterfaces();
			//read number of alternate settings
			System.out.println("device:");
			System.out.println("------------");
			for(int i = 0; i < ifaceDev.size(); i++){
				System.out.println("Interface " + i + " nofAltSettings: " + ifaceDev.get(i).getNumberOfAlternateSettings());
				//Interface Number:
				ifaceDescDev = ifaceDev.get(i).getAlternateSettings();
				for(int j = 0; j < ifaceDescDev.size(); j++){
					System.out.println("Interface Number dev:" + ifaceDescDev.get(j).getInterfaceNumber());
					//Alternate Setting (used to select)
					System.out.println("Alternate Setting:" + ifaceDescDev.get(j).getAlternateSetting());
					//Number of endpoints:
					System.out.println("Number of Endpoints: " + ifaceDescDev.get(j).getNumberOfEndpoints());
					epDescDev = ifaceDescDev.get(j).getEndpointDescriptors();
					System.out.println("---Endpoints---");
					for(int k = 0; k < epDescDev.size(); k++){
						//Endpoint address
						System.out.println("Endpoint " + k + ":");
						System.out.println("Address: " + epDescDev.get(k).getEndpointAddress());
						//Endpoint direction
						if(epDescDev.get(k).getEndpointDirection()){
							System.out.println("Direction: Endpoint-IN");
						}
						else{
							System.out.println("Direction: Endpoint-OUT");
						}
						//MaxPacketSize:
						System.out.println("MaxPacketSize: " + epDescDev.get(k).getMaxPacketSize());
						//MaxIsoPacketSize:
						System.out.println("MaxIsoPacketSize: " + epDescDev.get(k).getMaxIsoPacketSize());
						//TransferType:
						System.out.println("TransferType of Endpoint: " + epDescDev.get(k).getTransferType().toString());
					}
				}
			}

		}
		catch(LibusbException e){
			System.out.println("Error occured: get device properties");
			e.printStackTrace();
		}
		try {
			usbDev.open(1, 0, -1);			//1,0,-1
			System.out.println("Device open");
			byte[] data = {(byte)0x41, (byte)0x42, (byte)0x43, (byte)0x30};
			System.out.println("Data length 1: " + data.length);
			int res = usbDev.bulkTransfer(3, data, data.length, 0);
			if(res == data.length){
				System.out.println("Bulk transfer successful.");
			}
			else{
				System.out.println("Bulk transfer failed.");
			}
			usbDev.reset();
			usbDev.releaseInterface(0);
			usbDev.close();
			System.out.println("Device closed.");
		} catch (LibusbException e) {
			System.out.println("Error occured: bulk transfer");
			e.printStackTrace();
		}
				
		
		try{
			Device usbDevice = Device.open(useCtx, vid, pid);
			System.out.println("Device open with vid, pid successful.");
			usbDevice.claimInterface(0);
			byte[] dataIn = new byte[8];
			System.out.println("Data length 4: " + dataIn.length);
			int res = usbDevice.interruptTransfer(0x81, dataIn, 4, 0);
			if(res == 0){
				System.out.println("Interrupt tranfer successful.");
			}
			else{
				System.out.println("Interrupt transfer failed.");
			}
			usbDevice.reset();
			usbDevice.releaseInterface(0);
			usbDevice.close();
			System.out.println("Device closed.");
			
			
		}catch(LibusbException e){
			System.out.println("Error occured: interrupt transfer");
			e.printStackTrace();
		}
		
		try{
			Device usbDevice = Device.open(useCtx, vid, pid);
			System.out.println("Device open with vid, pid successful.");
			usbDevice.claimInterface(0);
			byte[] dataOut = {(byte)0x30, (byte)0x31, (byte)0x32, (byte)0x33};
			System.out.println("Data length 5: " + dataOut.length);
			
			currentStatus = 0;
			
			boolean gotTransfer = usbDevice.allocTransfer(4);

			if(gotTransfer){
				usbDevice.fillIsoTransfer(5, dataOut, dataOut.length, 4, "transferCallback", 0, 0, "ch/ntb/inf/libusb/test/LibusbJavaTester");
	
				int submit = usbDevice.submitTransfer();
				
				if(submit!= 0){
					System.out.println("submit != 0");
					return;
				}
	
				while(currentStatus == 0){
					System.out.println("handling");
					usbDevice.handleEvents(useCtx);	
				}
			}
			System.out.println();
			
			usbDevice.releaseInterface(0);
			usbDevice.close();
			System.out.println("Device closed.");
			
			
		}catch(LibusbException e){
			System.out.println("Error occured: isochronous transfer");
			e.printStackTrace();
		}
				
	}
	
	private void transferCallback(int transferStatus){
		//System.out.println("in transferCallback");
		currentStatus = 1;
		switch(transferStatus){
		case 0:
			System.out.println("transfer completed");
			break;
		case 3:
			System.out.println("transfer cancelled");
			break;
		case 1:
			System.out.println("transfer error");
			break;
		case 5:
			System.out.println("transfer no device");
			break;
		case 6:
			System.out.println("transfer overflow");
			break;
		case 4:
			System.out.println("transfer stall");
			break;
		case 2:
			System.out.println("transfer time out");
			break;
		}
	}

}
