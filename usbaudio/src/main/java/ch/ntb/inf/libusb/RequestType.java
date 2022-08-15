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

/**
 * Enum for Request Type in USB Setup Packet
 * 
 * @author Andreas Kalberer
 *
 */
public enum RequestType {
	HOST_TO_DEVICE(0 << 7),	
	DEVICE_TO_HOST(1 << 7),
	
	STANDARD(0 << 5),
	CLASS(1 << 5),
	VENDOR(2 << 5),
	
	DEVICE(0),
	INTERFACE(1),
	ENDPOINT(2),
	OTHER(3);

	private int code;

	private static Map<Integer, RequestType> map = new HashMap<Integer, RequestType>();

	static{
		for(RequestType requestTypeEnum: RequestType.values()){
			map.put(requestTypeEnum.code, requestTypeEnum);
		}
	}
	
	private RequestType(final int code){
		this.code = code;
	}

	public static RequestType valueOf(int code){
		return map.get(code);
	}
	
	public int getCode(){
		return code;
	}	
	
	public static byte getBitPattern(RequestType... reqType){
		byte pattern = 0;
		for(int i = 0; i < reqType.length; i++){
			pattern |= reqType[i].getCode();
		}
		return pattern;
	}
}
