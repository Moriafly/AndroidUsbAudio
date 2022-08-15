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

import ch.ntb.inf.libusb.TransferType;

/**
 * enum for TransferType reported by USB
 * @author libusb
 *
 */
public enum TransferType {

	CONTROL(0),				// Control endpoint
	ISOCHRONOUS(1),			// Isochronous endpoint
	BULK(2),				// Bulk endpoint
	INTERRUPT(3),			// Interrupt endpoint
	STREAM(4);				// Stream endpoint (USB 3.0)
	
	private int code;
	
	private static Map<Integer, TransferType> map = new HashMap<Integer, TransferType>();
	
	static{
		for(TransferType transferTypeEnum: TransferType.values()){
			map.put(transferTypeEnum.code, transferTypeEnum);
		}
	}
	
	private TransferType(final int code){
		this.code = code;
	}
	
	public static TransferType valueOf(int code){
		return map.get(code);
	}
}
