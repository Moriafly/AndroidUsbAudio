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

import ch.ntb.inf.libusb.Context;
import ch.ntb.inf.libusb.Libusb;

public class LibusbJava {
	/**
	 * Set log message verbosity.
	 * The default level is LIBUSB_LOG_LEVEL_NONE, which means no messages are ever printed. If you
	 *  choose to increase the message verbosity level, ensure that your application does not close 
	 *  the stdout/stderr file descriptors.<br>
	 * You are advised to use level LIBUSB_LOG_LEVEL_WARNING. libusb is conservative with its message 
	 * logging and most of the time, will only log messages that explain error conditions and other 
	 * oddities. This will help you debug your software.<br>
	 * If the LIBUSB_DEBUG environment variable was set when libusb was initialized, this function 
	 * does nothing: the message verbosity is fixed to the value in the environment variable.<br>
	 * If libusb was compiled without any message logging, this function does nothing: you'll never
	 * get any messages.<br>
	 * If libusb was compiled with verbose debug message logging, this function does nothing: you'll 
	 * always get messages from all levels.<br>
	 *
	 * @param ctx	
	 * 			the context to operate on, or NULL for the default context
	 * @param level	
	 * 			debug level to set 
	 */
	public static void setDebug(Context ctx, int level){
		//TODO Auswirkungen auf LibusbJava -> wirft Exceptions bei bestimmtem Level, sonst nur Error-Log etc. inkl. Beschreibung Javadoc
		Libusb.setDebug(ctx.getNativePointer(), level);		
	}
	
}
