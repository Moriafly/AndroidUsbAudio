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

import java.nio.ByteBuffer;

import ch.ntb.inf.libusb.exceptions.LibusbException;

public class LibusbJavaTest {

	/**
	 * This method does not need to be tested. This test only exists to document
	 * the fact that this function has not been forgotten. 
	 */
	public void testSetDebug(){
		
	}
	
	public void testInit(){
		System.out.println("testInit:");
		//Test for ByteBuffer
//		try{
//			ByteBuffer handle = Libusb.init();
//			System.out.println("got handle: " + handle.toString());
//			Libusb.exit(handle);
//		}
//		catch(LibusbException e){
//			System.out.println("Initialization failed: " + e.toString());
//		}
	}
	
	public static void main(String[] args){
		LibusbJavaTest libtest = new LibusbJavaTest();
		libtest.testInit();
		libtest.testSetDebug();
	}
	
	static{
		System.loadLibrary("LibusbJava_1-1");
	}
}
