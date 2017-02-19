# libptp
Java library, implementing most of the Picture Transfer Protocol (PTP - used 
by many cameras and smartphones to transfer photos) and its IP transport mode
(PTPIP). This library is licensed under the LGPL 2.1.

It is written to the PTP-spec and tested mostly with Sony cameras (with which 
it works flawlessly), but it should work with any PTP-compatible device.

The underlying PTP-transport layer is abstracted through the PtpTransport class
but only PTPIP (e.g., for cameras' wifi connections) is implemented. 
Specifically, USB is currently not implemented.

All PTP-data types are implemented but not all PTP-operations are supported, 
focus is on listing and downloading files. Implementing some of the missing 
operations might be trivial - or not. Overview:
 - PTP-ops supported: *GetDeviceInfo, OpenSession, CloseSession, GetStorageIDs,
   GetStorageInfo, GetNumObjects, GetObjectHandles, GetObjectInfo, GetObject, 
   GetThumb*
 - PTP-ops not supported: *DeleteObject, SendObjectInfo, SendObject,
   InitiateCapture, FormatStore, ResetDevice, SelfTest, SetObjectProtection,
   PowerDown, GetDevicePropDesc, GetDevicePropValue, SetDevicePropValue,
   ResetDevicePropValue, TerminateOpenCapture, MoveObject, CopyObject, 
   GetPartialObject, InitiateOpenCapture*

The library is not well documented but the PtpTester-class gives an example
on how to use most implemented functions.

Copyright (C) 2017 Fimagena (fimagena at gmail dot com)
