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

import java.util.ArrayList;
import java.util.List;

import ch.ntb.inf.libusb.Device;
import ch.ntb.inf.libusb.EndpointDescriptor;
import ch.ntb.inf.libusb.Interface;
import ch.ntb.inf.libusb.Libusb;
import ch.ntb.inf.libusb.exceptions.*;

public class InterfaceDescriptor extends Interface{

	private List<EndpointDescriptor> endpointDesc;
	
	protected int altSetting;
	
	protected InterfaceDescriptor(Device device, int configIndex, int ifaceIndex, int altSetting){
		super(device, configIndex, ifaceIndex);
		this.altSetting = altSetting;
	}
	
	/**
	 * get all endpoint descriptors of this interface
	 * @return List of endpoint descriptors
	 * @throws LibusbException
	 */
	public List<EndpointDescriptor> getEndpointDescriptors() throws LibusbException{
		if(endpointDesc == null){
			endpointDesc = new ArrayList<EndpointDescriptor>();
		}
		for(int i = 0; i < getNumberOfEndpoints(); i++){
			if(endpointDesc.size() - 1 < i){
				endpointDesc.add(i, new EndpointDescriptor(device, configIndex, ifaceIndex, altSetting, i));
			}
			else{
				endpointDesc.set(i, new EndpointDescriptor(device, configIndex, ifaceIndex, altSetting, i));
			}
		}
		
		return endpointDesc;
	}
	
	/**
	 * get endpoint descriptor with specified endpoint address
	 * @param endpointAddress
	 * @return desired EndpointDescriptor
	 * @throws LibusbException
	 */
	public EndpointDescriptor getEndpointDescriptor(int endpointAddress) throws LibusbException{
		getEndpointDescriptors();
		
		for(int i = 0; i < endpointDesc.size(); i++){
			if(endpointDesc.get(i).getEndpointAddress() == endpointAddress){
				return endpointDesc.get(i);
			}
		}
		
		throw new InvalidParameterException();
	}
	
	/**
	 * get number of endpoints this interface has
	 * @return number of endpoints
	 * @throws LibusbException
	 */
	public int getNumberOfEndpoints() throws LibusbException{
		return Libusb.getNofEndpoints(device.getNativeDevice(), configIndex, ifaceIndex, altSetting);
	}
	
	/**
	 * get interface number of this interface
	 * @return interface number
	 * @throws LibusbException
	 */
	public int getInterfaceNumber() throws LibusbException{
		return Libusb.getInterfaceNumber(device.getNativeDevice(), configIndex, ifaceIndex, altSetting);		
	}
	
	/**
	 * get alternate setting of this interface
	 * @return interface number of alternate setting
	 * @throws LibusbException
	 */
	public int getAlternateSetting() throws LibusbException{
		return Libusb.getAlternateSetting(device.getNativeDevice(), configIndex, ifaceIndex, altSetting);
	}
}
