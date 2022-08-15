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

import java.util.HashMap;
import java.util.Map;

import ch.ntb.inf.libusb.DescriptorType;

public enum DescriptorType{
	DEVICE(0x01),
	CONFIG(0x02),
	STRING(0x03),
	IFACE(0x04),
	ENDPOINT(0x05),
	BOS(0x0f),
	DEVICE_CAPABILITY(0x10),
	HID(0x21),
	REPORT(0x22),
	PHYSICAL(0x23),
	HUB(0x29),
	SUPERSPEED_HUB(0x2a),
	SS_ENDPOINT_COMPANION(0x30);
	
	private int code;
	
	private static Map<Integer, DescriptorType> map = new HashMap<Integer, DescriptorType>();
	
	static{
		for(DescriptorType descriptorTypeEnum: DescriptorType.values()){
			map.put(descriptorTypeEnum.code, descriptorTypeEnum);
		}
	}
	
	private DescriptorType(final int code){
		this.code = code;
	}
	
	public static DescriptorType valueOf(int code){
		return map.get(code);
	}
}
