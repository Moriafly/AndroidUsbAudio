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
import ch.ntb.inf.libusb.DeviceDescriptor;
import ch.ntb.inf.libusb.Interface;
import ch.ntb.inf.libusb.Libusb;
import ch.ntb.inf.libusb.exceptions.InvalidParameterException;
import ch.ntb.inf.libusb.exceptions.LibusbException;

public class ConfigurationDescriptor extends DeviceDescriptor{
	
	//evtl. Collection libusb_interface zuerst darueber legen und dort drin altsettings
	//numberOfAltSettings entspricht iface.size()
	private List<Interface> iface;
	private int nofInterfaces;
	
	protected int configIndex;
			
	protected ConfigurationDescriptor(Device dev){
		super(dev);
	}
	
	protected ConfigurationDescriptor(Device dev, int configIndex){
		super(dev);
		this.configIndex = configIndex;
	}
	
	/**
	 * get the number of interfaces supported by this configruation
	 * @return number of interfaces
	 */
	public int getNofInterfaces(){
		nofInterfaces = Libusb.getNofInterfaces(device.getNativeDevice(), configIndex);
		return nofInterfaces;
	}
	
	/**
	 * get the specified interface
	 * @param number of desired interface
	 * @return specified interface
	 * @throws LibusbException
	 */
	public Interface getInterface(int number) throws LibusbException{
		if(number > getNofInterfaces()){
			throw new InvalidParameterException();
		}
		getAllInterfaces();
		return iface.get(number);
	}
	
	/**
	 * get a List of all interfaces supported by this configuration
	 * @return List of interfaces
	 */
	public List<Interface> getAllInterfaces(){
		if(iface == null){
			iface = new ArrayList<Interface>();
		}
		for(int i = 0; i < getNofInterfaces(); i++){			
			if(iface.size()-1 < i ){
				iface.add(i, new Interface(device, configIndex, i));
			}
			else{
				iface.set(i, new Interface(device, configIndex, i));
			}
			
		}
		return iface;
	}
	
	/**
	 * get the configuration value of the current configuration
	 * @return configuration value
	 */
	public int getConfigValue(){
		return Libusb.getConfigValue(device.getNativeDevice(), configIndex);
	}
}
