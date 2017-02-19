/*  Copyright (C) 2017 Fimagena (fimagena at gmail dot com)

    This file is part of libptp.

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; see the file COPYING.  If not, 
    see <http://www.gnu.org/licenses/>.
*/

package com.fimagena.libptp;

import com.fimagena.libptp.ptpip.PtpIpConnection;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;


public class PtpTester {

    private static final short[] guid = new short[] {
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xff, 0xff, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    }; // adjust MAC
    private static final String friendlyName = "MyName.name";


    public static void main(String[] args) throws Exception {
        InetAddress ip = InetAddress.getByName("192.168.1.50");  // adjust IP
        if (args.length > 0) ip = InetAddress.getByName(args[0]);

        System.out.print("Initialise: ");
        PtpTransport.ResponderAddress address = new PtpIpConnection.PtpIpAddress(ip);
        PtpTransport.HostId hostId = new PtpIpConnection.PtpIpHostId(guid, friendlyName, 1, 1);
        PtpTransport transport = new PtpIpConnection();
        PtpConnection connection = new PtpConnection(transport);
        System.out.println("ok");

        System.out.print("Connect: ");
        connection.connect(address, hostId);
        System.out.println("ok");

        System.out.print("OpenSession: ");
        PtpSession session = connection.openSession();
        System.out.println("ok");

        System.out.println("GetDeviceInfo:");
        PtpDataType.DeviceInfoDataSet deviceInfoDataSet = session.getDeviceInfo();
        System.out.println(deviceInfoDataSet.toString());

        System.out.println("GetStorageIDs:");
        PtpDataType.StorageID[] storageIds = session.getStorageIDs();
        System.out.print(storageIds.length + " IDs: ");
        for (PtpDataType.StorageID storageId : storageIds) System.out.print(storageId.mValue + ", ");
        System.out.println();

        for (PtpDataType.StorageID storageId : storageIds) {
            System.out.println("GetStorageInfo: " + storageId.mValue);
            PtpDataType.StorageInfoDataSet storageInfo = session.getStorageInfo(storageId);
            System.out.println(storageInfo.toString());
        }

        System.out.println("GetObjectHandles:");
        PtpDataType.ObjectHandle[] objectHandles = session.getObjectHandles(storageIds[0], new PtpDataType.ObjectFormatCode(0xB101));
        System.out.print(objectHandles.length + " handles: ");
        for (PtpDataType.ObjectHandle objectHandle : objectHandles) System.out.print(objectHandle.mValue + ", ");
        System.out.println();

        for (PtpDataType.ObjectHandle objectHandle : objectHandles) {
            System.out.println("GetObjectInfo: " + objectHandle.mValue);
            PtpDataType.ObjectInfoDataSet objectInfo = session.getObjectInfo(objectHandle);
            System.out.println(objectInfo.toString());
        }

        for (PtpDataType.ObjectHandle objectHandle : objectHandles) {
            System.out.println("GetThumb: " + objectHandle.mValue);
            byte[] thumb = session.getThumb(objectHandle);
            FileOutputStream fout = new FileOutputStream(new File(Long.toString(objectHandle.mValue)));
            fout.write(thumb);
            fout.close();
            System.out.println(thumb.toString());
        }

        connection.close();
    }
}
