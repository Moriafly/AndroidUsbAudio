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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import ch.ntb.inf.libusb.Context;
import ch.ntb.inf.libusb.Device;
import ch.ntb.inf.libusb.exceptions.LibusbException;
import ch.ntb.inf.libusb.exceptions.NoDeviceException;

public class LibusbJavaListTest{
	
	static Context testCtx = null;
	
	static List<ByteBuffer> nativeList;
	static List<Device> deviceList;
	
	static Class<?> deviceClazz;
	static Constructor<?>[] devConstructor;
	static Field reflectedDeviceList;

	static List<Device> uutDeviceList;
	
	static int errorSum = 0, error = 0;
	
	public static void main(String[] args){
		try {
			testCtx = new Context();
		} catch (LibusbException e) {
			System.out.println("Context init failed");
			e.printStackTrace();
		}
		System.out.println("Successful init.");
		
		try {
			testLibusbList();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		testCtx = null;
		
		System.out.println("Test List finished with " + errorSum + " error(s)");
		System.exit(errorSum);
	}
	
	@SuppressWarnings("unchecked") //for cast of List from Reflection -> can't be checked at runtime
	private static void testLibusbList() throws NoSuchMethodException, SecurityException, InstantiationException, 
												IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
												NoSuchFieldException, ClassNotFoundException{
		@SuppressWarnings("unused") // dev is used in many try/catch blocks
		Device dev = null;
		int nofDuplicates = 0;
		ByteBuffer adrDev0 = ByteBuffer.allocateDirect(8);
		adrDev0.putInt(0xaa);
		
		deviceClazz = Class.forName("ch.ntb.inf.libusbJava.Device");
		devConstructor = deviceClazz.getConstructors();
		reflectedDeviceList = deviceClazz.getDeclaredField("devList");
		reflectedDeviceList.setAccessible(true);
		ArrayList<Device> privateList = new ArrayList<Device>();
		//check if correct deviceList in class Device
		uutDeviceList = (List<Device>)reflectedDeviceList.get(privateList);
				
		setList(0, adrDev0);
		ByteBuffer adrDev1 = ByteBuffer.allocateDirect(8);
		adrDev1.putInt(0xbb);
		setList(1, adrDev1);
		ByteBuffer adrDev2 = ByteBuffer.allocateDirect(8);
		adrDev2.putInt(0xcc);
		setList(2, adrDev2);
		ByteBuffer adrDev3 = ByteBuffer.allocateDirect(8);
		adrDev3.putInt(0xdd);
		setList(3, adrDev3);
		
		try {
			System.out.println("--> search 1:");
			dev = Device.search(testCtx, 0x8235, 0x100); //kick on search
		} 
		catch (NoDeviceException e){
			System.out.println("Device not found, continuing List Test");
		}
		catch (LibusbException e) {
			System.out.println("search failed");
			e.printStackTrace();
		}
		error = 0;
		if(deviceList.size() == uutDeviceList.size()){
			for(int i = 0; i < deviceList.size(); i++){
				System.out.println("Dev " + i + ": 0x" + Integer.toHexString(deviceList.get(i).getNativeDevice().getInt(0))
							+ " libusbJava: 0x" + Integer.toHexString(uutDeviceList.get(i).getNativeDevice().getInt(0)));
				if(deviceList.get(i).getNativeDevice().getInt(0) != uutDeviceList.get(i).getNativeDevice().getInt(0)){
					error++;
					System.out.println("-->Failure: Device Lists not equal: testList: 0x" + Integer.toHexString(deviceList.get(i).getNativeDevice().getInt(0))
							+ " libusbJava: 0x" + Integer.toHexString(uutDeviceList.get(i).getNativeDevice().getInt(0)));
				}
			}
		}
		else{
			error++;
			System.out.println("-->Failure: length of Device Lists is not equal: testList: " + deviceList.size() + " libusbJava: " + uutDeviceList.size());
		}
		errorSum += error;
		
		ByteBuffer adrDev4 = ByteBuffer.allocateDirect(8);
		adrDev4.putInt(0xee);
		setList(2, adrDev4);
		
		try {
			System.out.println("--> search 2:");
			dev = Device.search(testCtx, 0x8235, 0x100); //kick on search
		} 
		catch (NoDeviceException e){
			System.out.println("Device not found, continuing List Test");
		}
		catch (LibusbException e) {
			System.out.println("search failed");
			e.printStackTrace();
		}
		error = 0;
		if(deviceList.size() == uutDeviceList.size()){
			for(int i = 0; i < deviceList.size(); i++){
				System.out.println("Dev " + i + ": 0x" + Integer.toHexString(deviceList.get(i).getNativeDevice().getInt(0))
							+ " libusbJava: 0x" + Integer.toHexString(uutDeviceList.get(i).getNativeDevice().getInt(0)));
				if(deviceList.get(i).getNativeDevice().getInt(0) != uutDeviceList.get(i).getNativeDevice().getInt(0)){
					error++;
					System.out.println("-->Failure: Device Lists not equal: testList: 0x" + Integer.toHexString(deviceList.get(i).getNativeDevice().getInt(0))
							+ " libusbJava: 0x" + Integer.toHexString(uutDeviceList.get(i).getNativeDevice().getInt(0)));
				}
			}
		}
		else{
			error++;
			System.out.println("-->Failure: length of Device Lists is not equal: testList: " + deviceList.size() + " libusbJava: " + uutDeviceList.size());
		}
		errorSum += error;
		
		removeElement(3);
		removeElement(3);
		
		try {
			System.out.println("--> search 3:");
			dev = Device.search(testCtx, 0x8235, 0x100); //kick on search
		} 
		catch (NoDeviceException e){
			System.out.println("Device not found, continuing List Test");
		}
		catch (LibusbException e) {
			System.out.println("search failed");
			e.printStackTrace();
		}
		error = 0;
		if(deviceList.size() == uutDeviceList.size()){
			for(int i = 0; i < deviceList.size(); i++){
				System.out.println("Dev " + i + ": 0x" + Integer.toHexString(deviceList.get(i).getNativeDevice().getInt(0))
							+ " libusbJava: 0x" + Integer.toHexString(uutDeviceList.get(i).getNativeDevice().getInt(0)));
				if(deviceList.get(i).getNativeDevice().getInt(0) != uutDeviceList.get(i).getNativeDevice().getInt(0)){
					error++;
					System.out.println("-->Failure: Device Lists not equal: testList: 0x" + Integer.toHexString(deviceList.get(i).getNativeDevice().getInt(0))
							+ " libusbJava: 0x" + Integer.toHexString(uutDeviceList.get(i).getNativeDevice().getInt(0)));
				}
			}
		}
		else{
			error++;
			System.out.println("-->Failure: length of Device Lists is not equal: testList: " + deviceList.size() + " libusbJava: " + uutDeviceList.size());
		}
		errorSum += error;
		
		ByteBuffer adrDev5 = ByteBuffer.allocateDirect(8);
		adrDev5.putInt(0xab);
		setList(2, adrDev5);
		
		try {
			System.out.println("--> search 4:");
			dev = Device.search(testCtx, 0x8235, 0x100); //kick on search
		} 
		catch (NoDeviceException e){
			System.out.println("Device not found, continuing List Test");
		}
		catch (LibusbException e) {
			System.out.println("search failed");
			e.printStackTrace();
		}
		error = 0;
		if(deviceList.size() == uutDeviceList.size()){
			for(int i = 0; i < deviceList.size(); i++){
				System.out.println("Dev " + i + ": 0x" + Integer.toHexString(deviceList.get(i).getNativeDevice().getInt(0))
							+ " libusbJava: 0x" + Integer.toHexString(uutDeviceList.get(i).getNativeDevice().getInt(0)));
				if(deviceList.get(i).getNativeDevice().getInt(0) != uutDeviceList.get(i).getNativeDevice().getInt(0)){
					error++;
					System.out.println("-->Failure: Device Lists not equal: testList: 0x" + Integer.toHexString(deviceList.get(i).getNativeDevice().getInt(0))
							+ " libusbJava: 0x" + Integer.toHexString(uutDeviceList.get(i).getNativeDevice().getInt(0)));
				}
			}
		}
		else{
			error++;
			System.out.println("-->Failure: length of Device Lists is not equal: testList: " + deviceList.size() + " libusbJava: " + uutDeviceList.size());
		}
		errorSum += error;
		
		removeElement(1);
		removeElement(1);
		removeElement(0);
		removeElement(0);
		try {
			System.out.println("--> search 5:");
			dev = Device.search(testCtx, 0x8235, 0x100); //kick on search
		} 
		catch (NoDeviceException e){
			System.out.println("Device not found, continuing List Test");
		}
		catch (LibusbException e) {
			System.out.println("search failed");
			e.printStackTrace();
		}
		error = 0;
		if(deviceList.size() == uutDeviceList.size()){
			for(int i = 0; i < deviceList.size(); i++){
				System.out.println("Dev " + i + ": 0x" + Integer.toHexString(deviceList.get(i).getNativeDevice().getInt(0))
							+ " libusbJava: 0x" + Integer.toHexString(uutDeviceList.get(i).getNativeDevice().getInt(0)));
				if(deviceList.get(i).getNativeDevice().getInt(0) != uutDeviceList.get(i).getNativeDevice().getInt(0)){
					error++;
					System.out.println("-->Failure: Device Lists not equal: testList: 0x" + Integer.toHexString(deviceList.get(i).getNativeDevice().getInt(0))
							+ " libusbJava: 0x" + Integer.toHexString(uutDeviceList.get(i).getNativeDevice().getInt(0)));
				}
			}
		}
		else{
			error++;
			System.out.println("-->Failure: length of Device Lists is not equal: testList: " + deviceList.size() + " libusbJava: " + uutDeviceList.size());
		}
		errorSum += error;

		ByteBuffer adrDev6 = ByteBuffer.allocateDirect(8);
		adrDev6.putInt(0xac);
		setList(0, adrDev6); 	//0xac
		setList(1, adrDev5);	//0xab
		setList(2, adrDev4);	//0xee
		setList(3, adrDev3);	//0xdd
		setList(2, adrDev2);	//0xcc
		setList(2, adrDev1);	//0xbb
		setList(1, adrDev0);	//0xaa
		removeElement(1);
		removeElement(3);
		removeElement(0);
		setList(2, adrDev6);
		
		try {
			System.out.println("--> search 6:");
			dev = Device.search(testCtx, 0x8235, 0x100); //kick on search
		} 
		catch (NoDeviceException e){
			System.out.println("Device not found, continuing List Test");
		}
		catch (LibusbException e) {
			System.out.println("search failed");
			e.printStackTrace();
		}
		error = 0;
		if(deviceList.size() == uutDeviceList.size()){
			for(int i = 0; i < deviceList.size(); i++){
				System.out.println("Dev " + i + ": 0x" + Integer.toHexString(deviceList.get(i).getNativeDevice().getInt(0))
							+ " libusbJava: 0x" + Integer.toHexString(uutDeviceList.get(i).getNativeDevice().getInt(0)));
				if(deviceList.get(i).getNativeDevice().getInt(0) != uutDeviceList.get(i).getNativeDevice().getInt(0)){
					error++;
					System.out.println("-->Failure: Device Lists not equal: testList: 0x" + Integer.toHexString(deviceList.get(i).getNativeDevice().getInt(0))
							+ " libusbJava: 0x" + Integer.toHexString(uutDeviceList.get(i).getNativeDevice().getInt(0)));
				}
			}
		}
		else{
			error++;
			System.out.println("-->Failure: length of Device Lists is not equal: testList: " + deviceList.size() + " libusbJava: " + uutDeviceList.size());
		}
		errorSum += error;
		
		removeElement(2);
		removeElement(2);
		setList(2, adrDev2);	//0xcc
		setList(2, adrDev0);	//0xaa
		try {
			System.out.println("--> search 7:");
			dev = Device.search(testCtx, 0x8235, 0x100); //kick on search
		} 
		catch (NoDeviceException e){
			System.out.println("Device not found, continuing List Test");
		}
		catch (LibusbException e) {
			System.out.println("search failed");
			e.printStackTrace();
		}
		error = 0;
		if(deviceList.size() == uutDeviceList.size()){
			for(int i = 0; i < deviceList.size(); i++){
				System.out.println("Dev " + i + ": 0x" + Integer.toHexString(deviceList.get(i).getNativeDevice().getInt(0))
							+ " libusbJava: 0x" + Integer.toHexString(uutDeviceList.get(i).getNativeDevice().getInt(0)));
				if(deviceList.get(i).getNativeDevice().getInt(0) != uutDeviceList.get(i).getNativeDevice().getInt(0)){
					error++;
					System.out.println("-->Failure: Device Lists not equal: testList: 0x" + Integer.toHexString(deviceList.get(i).getNativeDevice().getInt(0))
							+ " libusbJava: 0x" + Integer.toHexString(uutDeviceList.get(i).getNativeDevice().getInt(0)));
				}
			}
		}
		else{
			error++;
			System.out.println("-->Failure: length of Device Lists is not equal: testList: " + deviceList.size() + " libusbJava: " + uutDeviceList.size());
		}
		errorSum += error;
		
		setList(2, adrDev2);	//0xcc
		setList(2, adrDev0);	//0xaa
		removeElement(2);
		removeElement(2);
		
		try {
			System.out.println("--> search 8:");
			dev = Device.search(testCtx, 0x8235, 0x100); //kick on search
		} 
		catch (NoDeviceException e){
			System.out.println("Device not found, continuing List Test");
		}
		catch (LibusbException e) {
			System.out.println("search failed");
			e.printStackTrace();
		}
		error = 0;
		if(deviceList.size() == uutDeviceList.size()){
			for(int i = 0; i < deviceList.size(); i++){
				System.out.println("Dev " + i + ": 0x" + Integer.toHexString(deviceList.get(i).getNativeDevice().getInt(0))
							+ " libusbJava: 0x" + Integer.toHexString(uutDeviceList.get(i).getNativeDevice().getInt(0)));
				if(deviceList.get(i).getNativeDevice().getInt(0) != uutDeviceList.get(i).getNativeDevice().getInt(0)){
					error++;
					System.out.println("-->Failure: Device Lists not equal: testList: 0x" + Integer.toHexString(deviceList.get(i).getNativeDevice().getInt(0))
							+ " libusbJava: 0x" + Integer.toHexString(uutDeviceList.get(i).getNativeDevice().getInt(0)));
				}
			}
		}
		else{
			error++;
			System.out.println("-->Failure: length of Device Lists is not equal: testList: " + deviceList.size() + " libusbJava: " + uutDeviceList.size());
		}
		errorSum += error;
		
		removeElement(4);
		removeElement(3);
		removeElement(2);
		removeElement(1);
		removeElement(0);
		try {
			System.out.println("--> search 9:");
			dev = Device.search(testCtx, 0x8235, 0x100); //kick on search
		} 
		catch (NoDeviceException e){
			System.out.println("Device not found, continuing List Test");
		}
		catch (LibusbException e) {
			System.out.println("search failed");
			e.printStackTrace();
		}
		error = 0;
		if(deviceList.size() == uutDeviceList.size()){
			for(int i = 0; i < deviceList.size(); i++){
				System.out.println("Dev " + i + ": 0x" + Integer.toHexString(deviceList.get(i).getNativeDevice().getInt(0))
							+ " libusbJava: 0x" + Integer.toHexString(uutDeviceList.get(i).getNativeDevice().getInt(0)));
				if(deviceList.get(i).getNativeDevice().getInt(0) != uutDeviceList.get(i).getNativeDevice().getInt(0)){
					error++;
					System.out.println("-->Failure: Device Lists not equal: testList: 0x" + Integer.toHexString(deviceList.get(i).getNativeDevice().getInt(0))
							+ " libusbJava: 0x" + Integer.toHexString(uutDeviceList.get(i).getNativeDevice().getInt(0)));
				}
			}
		}
		else{
			error++;
			System.out.println("-->Failure: length of Device Lists is not equal: testList: " + deviceList.size() + " libusbJava: " + uutDeviceList.size());
		}
		errorSum += error;
	}
	
	static{
		System.loadLibrary("LibusbJava");
		nativeList = new ArrayList<ByteBuffer>();	
		deviceList = new ArrayList<Device>();
	}
	
	private static void setList(int position, ByteBuffer addr) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		nativeList.add(position, addr);
		deviceList.add(position, (Device)devConstructor[0].newInstance(testCtx, addr));
		System.out.println("added to list: 0x" + Integer.toHexString(deviceList.get(position).getNativeDevice().getInt(0)));
	}
	
	private static void removeElement(int position){
		nativeList.remove(position);
		System.out.println("removed from list: 0x" + Integer.toHexString(deviceList.remove(position).getNativeDevice().getInt(0)));
	}
	
	//Test method for Device List
	public static List<ByteBuffer> getDeviceList(ByteBuffer ctx){
		return nativeList;
	}
}