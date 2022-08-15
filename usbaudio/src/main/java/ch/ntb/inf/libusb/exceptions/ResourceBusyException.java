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

package ch.ntb.inf.libusb.exceptions;

public class ResourceBusyException extends LibusbException{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7307496028686694179L;

	public ResourceBusyException(){
		super();
	}
	
	public ResourceBusyException(String message){
		super(message);
	}
	
	public ResourceBusyException(String message, Throwable cause){
		super(message, cause);
	}
	
	public ResourceBusyException(Throwable cause){
		super(cause);
	}
}
