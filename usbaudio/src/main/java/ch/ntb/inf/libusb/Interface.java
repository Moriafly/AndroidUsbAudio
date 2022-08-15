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

import ch.ntb.inf.libusb.ConfigurationDescriptor;
import ch.ntb.inf.libusb.Device;
import ch.ntb.inf.libusb.InterfaceDescriptor;
import ch.ntb.inf.libusb.Libusb;
import ch.ntb.inf.libusb.exceptions.*;

public class Interface extends ConfigurationDescriptor{
	//contains one Interface with alternate settings
	private List<InterfaceDescriptor> altSettings;
	private int nofAltSettings;
	
	protected int ifaceIndex;
	
	protected Interface(Device dev, int configIndex, int ifaceIndex){
		super(dev, configIndex);
		this.ifaceIndex = ifaceIndex;
	}
	
	/**
	 * get List of alternate settings the interface supports
	 * @return List of alternate settings (InterfaceDescriptors)
	 * @throws LibusbException
	 */
	public List<InterfaceDescriptor> getAlternateSettings() throws LibusbException{
		if(altSettings == null){
			altSettings = new ArrayList<InterfaceDescriptor>();
		}
		for(int i = 0; i < getNumberOfAlternateSettings(); i++){
			if(altSettings.size() - 1 < i){
				altSettings.add(i, new InterfaceDescriptor(device, configIndex, ifaceIndex, i));
			}
			else{
				altSettings.set(i, new InterfaceDescriptor(device, configIndex, ifaceIndex, i));
			}			
		}
		return altSettings;
	}
	
	/**
	 * get number of alternate settings
	 * @return number of alternate settings
	 * @throws LibusbException
	 */
	public int getNumberOfAlternateSettings() throws LibusbException{
		nofAltSettings = Libusb.getNofAltSettings(device.getNativeDevice(), configIndex);
		return nofAltSettings;
	}
	
	/**
	 * get alternate setting (InterfaceDescriptor) with specified interface number
	 * @param ifaceNum interface number
	 * @return Interface Descriptor
	 * @throws LibusbException
	 */
	public InterfaceDescriptor getAlternateSetting(int ifaceNum) throws LibusbException{
		getAlternateSettings();
		
		for(int i = 0; i < altSettings.size(); i++){
			if( altSettings.get(i).getInterfaceNumber() == ifaceNum ){
				return altSettings.get(i);
			}
		}
		throw new InvalidParameterException();
	}
	
	/**
	 * NOT SUPPORTED at the moment, only relevant with Linux, not supported with Windows
	 * @param interfaceNumber
	 * @return
	 */
	public boolean kernelDriverActive(int interfaceNumber){
		return false; //TODO throws error, true if kernelDriverActive, false else 
		//LIBUSB_ERROR_NO_DEVICE, LIBUSB_ERROR_NOT_SUPPORTED, another LIBUSB_ERROR on other failure
	}
	
	/**
	 * NOT SUPPORTED at the moment, only relevant with Linux, not supported with Windows
	 * @param interfaceNumber
	 */
	public void detachKernelDriver(int interfaceNumber){
		//TODO throws LIBUSB_ERROR_NOT_FOUND, LIBUSB_ERROR_INVALID_PARAM, LIBUSB_ERROR_NO_DEVICE, LIBUSB_ERROR_NOT_SUPPORTED, another LIBUSB_ERROR code
	}
	
	/**
	 * NOT SUPPORTED at the moment, only relevant with Linux, not supported with Windows
	 * @param interfaceNumber
	 */
	public void attachKernelDriver(int interfaceNumber){
		//TODO throws LIBUSB_ERROR_NOT_FOUND, LIBUSB_ERROR_INVALID_PARAM, LIBUSB_ERROR_NO_DEVICE, LIBUSB_ERROR_NOT_SUPPORTED, LIBUSB_ERROR_BUSY, another LIBUSB_ERROR on other fail
	}
	
	/**
	 * NOT SUPPORTED at the moment, only relevant with Linux, not supported with Windows
	 * @param enable
	 */
	public void setAutoDetachKernelDriver(boolean enable){
		//TODO
		// throws LIBUSB_ERROR_NOT_SUPPORTED on platforms without this functionality, LIBUSB_SUCCESS on success
	}
	
	/**
	 * NOT SUPPORTED at the moment, only relevant with Linux, not supported with Windows
	 * @param interfaceNumber
	 * @param alternateSetting
	 */
	public void setAltSetting(int interfaceNumber, int alternateSetting){
		//TODO throws if fail
		//LIBUSB_ERROR_NOT_FOUND, LIBUSB_ERROR_NO_DEVICE, another LIBUSB_ERROR code on other failure
	}
}
