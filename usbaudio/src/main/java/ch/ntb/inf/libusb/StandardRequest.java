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
 * USB Standard Requests
 * @author libusb
 *
 */

public enum StandardRequest {

	GET_STATUS(0x00),				// Request status of the specific recipient 
	CLEAR_FEATURE(0x01),			// Clear or disable a specific feature
	// 0x02 is reserved
	SET_FEATURE(0x03),				// Set or enable a specific feature
	// 0x04 is reserved
	SET_ADDRESS(0x05),				// Set device address for all future accesses
	GET_DESCRIPTOR(0x06),			// Get the specified descriptor
	SET_DESCRIPTOR(0x07),			// Used to update existing descriptors or add new descriptors
	GET_CONFIGURATION(0x08),		// Get the current device configuration value
	SET_CONFIGURATION(0x09),		// Set device configuration
	GET_INTERFACE(0x0A),			// Return the selected alternate setting for the specified interface
	SET_INTERFACE(0x0B),			// Select an alternate interface for the specified interface
	SYNC_FRAME(0x0C),				// Set then report an endpoint's synchronization frame
	SET_SEL(0x30),					// Sets both the U1 and U2 Exit Latency
	SET_ISOCH_DELAY(0x31);			// Delay from the time a host transmits a packet to the time it is received by the device. 
		
	private int code;
	
	private static Map<Integer, StandardRequest> map = new HashMap<Integer, StandardRequest>();
	
	static{
		for(StandardRequest standardRequestEnum: StandardRequest.values()){
			map.put(standardRequestEnum.code, standardRequestEnum);
		}
	}
	
	private StandardRequest(final int code){
		this.code = code;
	}
	
	public static StandardRequest valueOf(int code){
		return map.get(code);
	}
	
	public int getCode(){
		return code;
	}
}
