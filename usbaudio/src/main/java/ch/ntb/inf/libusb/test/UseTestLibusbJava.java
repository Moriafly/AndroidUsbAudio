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

import java.util.EnumSet;
import java.util.List;

import ch.ntb.inf.libusb.ConfigurationDescriptor;
import ch.ntb.inf.libusb.Context;
import ch.ntb.inf.libusb.Device;
import ch.ntb.inf.libusb.DeviceDescriptor;
import ch.ntb.inf.libusb.EndpointDescriptor;
import ch.ntb.inf.libusb.Interface;
import ch.ntb.inf.libusb.InterfaceDescriptor;
import ch.ntb.inf.libusb.StandardRequest;
import ch.ntb.inf.libusb.exceptions.LibusbException;
import ch.ntb.inf.libusb.exceptions.NotSupportedException;
import ch.ntb.inf.libusb.RequestType;

/**
 * Test for MPC555 USB BDI developed at the NTB Buchs,
 * see http://wiki.ntb.ch/infoportal/embedded_systems/mpc555/usb-bdi
 * 
 * @author Andreas Kalberer
 *
 */

public class UseTestLibusbJava {

	private static final int MAX_PORT_DEPTH = 7;
	
	public static void main(String[] args){
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
		int nofDuplicates = 0;
		try {
			usbDev = Device.search(useCtx, 0x8235, 0x100);
			nofDuplicates = Device.getNofDuplicates();
			System.out.println(nofDuplicates + " duplicates found");
			if(nofDuplicates > 1){
				for(int i = 0; i < nofDuplicates; i++){
					//check which one of the composite device is the correct one
					try {
						usbDev = Device.getDuplicate(useCtx, 0x8235, 0x100, i);
						usbDev.open(1, 0, -1);
						usbDev.close();
						System.out.println("closed again");
						break;
					}
					catch(NotSupportedException e){
						// try next device
						System.out.println("trying next, now: " + i);
					}
					catch(LibusbException e){
						System.out.println("search failed");
					}
				}
				
			}
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
			
//			System.out.println("Active Descriptor of devDescParent:");
//			ConfigurationDescriptor cfgDescParent = devDescParent.getActiveConfigDescriptor();
			
			System.out.println("--------Interfaces---------");
			System.out.println("NofInterfaces device: " + activeCfgDesc.getNofInterfaces());
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
					}
				}
			}
			//read number of alternate settings for parent
//			System.out.println("parent:");
//			System.out.println("------------");
//			for(int i = 0; i < ifaceParent.size(); i++){
//				System.out.println("Interface " + i + " nofAltSettings: " + ifaceParent.get(i).getNumberOfAlternateSettings());
//				//Interface Number:
//				List<InterfaceDescriptor> ifaceDescParent = ifaceParent.get(i).getAlternateSettings();
//				for(int j = 0; j < ifaceDescParent.size(); j++){
//					System.out.println("Interface Number parentDev:" + ifaceDescParent.get(j).getInterfaceNumber());
//					//Alternate Setting (used to select)
//					System.out.println("Alternate Setting:" + ifaceDescParent.get(j).getAlternateSetting());
//					//Number of endpoints:
//					System.out.println("Number of Endpoints: " + ifaceDescParent.get(j).getNumberOfEndpoints());
//					List<EndpointDescriptor> epDescParent = ifaceDescParent.get(j).getEndpointDescriptors();
//					System.out.println("---Endpoints---");
//					for(int k = 0; k < epDescParent.size(); k++){
//						//Endpoint address
//						System.out.println("Endpoint " + k + ":");
//						System.out.println("Address: " + epDescParent.get(k).getEndpointAddress());
//						//Endpoint direction
//						if(epDescParent.get(k).getEndpointDirection()){
//							System.out.println("Direction: Endpoint-IN");
//						}
//						else{
//							System.out.println("Direction: Endpoint-OUT");
//						}
//						//MaxPacketSize:
//						System.out.println("MaxPacketSize: " + epDescParent.get(k).getMaxPacketSize());
//						//MaxIsoPacketSize:
//						System.out.println("MaxIsoPacketSize: " + epDescParent.get(k).getMaxIsoPacketSize());
//					}
//				}
//			}
			
			
			
		}
		catch(LibusbException e){
			System.out.println("Error occured: get device properties");
			e.printStackTrace();
		}
		try {
			usbDev.open(1, 0, -1);
			byte[] data = {(byte)0x40, (byte)0x80, (byte)0x12, (byte)0x16};
			int res = usbDev.bulkTransfer(2, data, data.length, 0);
			if(res == data.length){
				System.out.println("Bulk tranfer 1 successful.");
			}
			else{
				System.out.println("Bulk transfer 1 failed.");
			}
			usbDev.reset();
			res = 0;
			res = usbDev.bulkTransfer(2, data, data.length, 0);
			if(res == data.length){
				System.out.println("Bulk tranfer 2 successful.");
			}
			else{
				System.out.println("Bulk transfer 2 failed.");
			}
			usbDev.close();
			System.out.println("Device closed.");
		} catch (LibusbException e) {
			System.out.println("Error occured: transfer");
			e.printStackTrace();
		}
		
		// control transfer
		System.out.println("Control transfer:");
		try {
			usbDev.open(1,0,-1);
			byte[] dataControl = new byte[10];
			
			System.out.println("reqType DEVICE_TO_HOST (0x80): 0x" + Integer.toHexString(0xFF&RequestType.getBitPattern(RequestType.DEVICE_TO_HOST)));
			System.out.println("reqType HOST_TO_DEVICE (0x00): 0x" + Integer.toHexString(0xFF&RequestType.getBitPattern(RequestType.HOST_TO_DEVICE)));
			System.out.println("reqType VENDOR (0x40): 0x" + Integer.toHexString(0xFF&RequestType.getBitPattern(RequestType.VENDOR)));
			System.out.println("reqType DEVICE_TO_HOST+VENDOR+ENDPOINT(0xC2): 0x" + Integer.toHexString(0xFF&RequestType.getBitPattern(RequestType.DEVICE_TO_HOST,RequestType.VENDOR,RequestType.ENDPOINT)));
			System.out.println("reqType DEVICE_TO_HOST+CLASS+INTERFACE(0xA1): 0x" + Integer.toHexString(0xFF&RequestType.getBitPattern(RequestType.DEVICE_TO_HOST,RequestType.CLASS,RequestType.INTERFACE)));
			System.out.println("reqType DEVICE_TO_HOST+OTHER(0x83): 0x" + Integer.toHexString(0xFF&RequestType.getBitPattern(RequestType.DEVICE_TO_HOST,RequestType.OTHER)));
			
			int res = usbDev.controlTransfer(RequestType.getBitPattern(RequestType.DEVICE_TO_HOST), StandardRequest.GET_STATUS, (short)0, (short)0, dataControl, dataControl.length, 0);
			if(res == 2){
				System.out.print("Control transfer successful, status: ");
				for(int i = 1; i >= 0; i--){
					System.out.print(dataControl[i]);
				}
				System.out.println();
			}
			else{
				System.out.println("Control transfer failed");
				System.out.println("nofBytes: " + res);
			}
			
			byte[] dataConfig = new byte[10];
			res = 0;
			res = usbDev.controlTransfer(RequestType.getBitPattern(RequestType.DEVICE_TO_HOST), StandardRequest.GET_CONFIGURATION, (short)0, (short)0, dataConfig, dataConfig.length, 0);
			if(res == 1){
				System.out.print("Control transfer successful, configuration: ");
				System.out.println(dataConfig[0]);
			}
			else{
				System.out.println("Control transfer failed of get_configuration");
				System.out.println("nofBytes: " + res);
			}
		} catch (LibusbException e) {
			System.out.println("Error occured: Control Transfer Block");
			e.printStackTrace();
		}
	}
	
}
