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

import ch.ntb.inf.libusb.Speed;

/**
 * enum for USB speed reported by libusb
 *
 */
public enum Speed {
	UNKNOWN(0),		// The OS doesn't report or know the device speed. 
	LOW(1),			// The device is operating at low speed (1.5MBit/s).
	FULL(2),		// The device is operating at full speed (12MBit/s).
	HIGH(3),		// The device is operating at high speed (480MBit/s).
	SUPER(4);		// The device is operating at super speed (5000MBit/s).
	
	private int code;
	
	private static Map<Integer, Speed> map = new HashMap<Integer, Speed>();
	
	static{
		for(Speed speedEnum: Speed.values()){
			map.put(speedEnum.code, speedEnum);
		}
	}
	
	private Speed(final int code){
		this.code = code;
	}
	
	public static Speed valueOf(int code){
		return map.get(code);
	}
}
