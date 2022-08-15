/*
 * Java wrapper for libusb
 *
 * Copyright (C) 2015-2016 NTB University of Applied Sciences in Technology
 * Buchs, Switzerland, http://www.ntb.ch/inf
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package ch.ntb.inf.libusb;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import ch.ntb.inf.libusb.Libusb;
import ch.ntb.inf.libusb.exceptions.LibusbException;

public class Context {

    public static final boolean dbg = false;

    private ByteBuffer context;
    private static int nofContext = 0;

    static {
        if (System.getProperty("os.name").equals("Linux")) {
            System.setProperty("java.library.path", "/usr/lib");
        } else {
            System.setProperty("java.library.path", "C:/Windows/System32/");
        }


        Field fieldSysPath;
        try {
            Field[] fields = ClassLoader.class.getDeclaredFields();
            if (fields.length > 0) {
                fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
                System.out.println(fieldSysPath.hashCode());
                fieldSysPath.setAccessible(true);
                fieldSysPath.set(null, null);
            }

        } catch (NoSuchFieldException e) {
            System.out.println("no such field");
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

//		try {
//			System.loadLibrary("LibusbJava-1_2");
//		}catch (Exception e) {
//			e.printStackTrace();
//		}
    }

    /**
     * create a new LibusbJava context, this is the entry point for the use of LibusbJava (initializes libusb)
     *
     * @throws LibusbException
     */
    public Context() throws LibusbException {
        context = Libusb.init();
        nofContext++;
    }

    public ByteBuffer getNativePointer() {
        return context;
    }

    /**
     * exit libusb
     */
    public void finalize() throws Throwable {
        try {
            nofContext--;
            if (nofContext <= 0) {
                Libusb.exit(context);
            }
        } finally {
            super.finalize();
        }
    }
}
