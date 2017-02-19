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

import java.io.Serializable;
import java.lang.reflect.Array;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;


public abstract class PtpDataType {

    protected abstract void write(PtpTransport.PayloadBuffer out);
    protected abstract void read(PtpTransport.PayloadBuffer in) throws PtpTransport.TransportDataError, PtpExceptions.MalformedDataType;
    @Override public String toString() {return "";}

    public void writeToBuffer(PtpTransport.PayloadBuffer out) {write(out);}


    // ---------------------------------------------------------------------------------------------
    // Primitive Datatypes


    public static class UInt16 extends PtpDataType  implements Serializable {
        public int mValue;

        @Override protected void write(PtpTransport.PayloadBuffer out) {out.writeUInt16(mValue);}
        @Override protected void read(PtpTransport.PayloadBuffer in) throws PtpTransport.TransportDataError {mValue = in.readUInt16();}
        @Override public String toString() {return String.format("0x%04x", mValue);}
        public UInt16() {}
        public UInt16(int value) {mValue = value;}
    }

    public static class UInt32 extends PtpDataType  implements Serializable {
        public long mValue;

        @Override protected void write(PtpTransport.PayloadBuffer out) {out.writeUInt32(mValue);}
        @Override protected void read(PtpTransport.PayloadBuffer in) throws PtpTransport.TransportDataError {mValue = in.readUInt32();}
        @Override public String toString() {return String.format("0x%08x", mValue);}
        public UInt32() {}
        public UInt32(long value) {mValue = value;}
    }

    public  static class UInt64 extends PtpDataType implements Serializable {
        public long mValue;

        @Override protected void write(PtpTransport.PayloadBuffer out) {out.writeUInt64(mValue);}
        @Override protected void read(PtpTransport.PayloadBuffer in) throws PtpTransport.TransportDataError {mValue = in.readUInt64();}
        @Override public String toString() {return String.format("0x%016x", mValue);}
        public UInt64() {}
        public UInt64(long value) {mValue = value;}
    }

    public static class Datacode extends UInt16 implements Serializable {}

    public static class PtpString extends PtpDataType implements Serializable {
        public String mString = "";

        @Override protected void write(PtpTransport.PayloadBuffer out) {
            if (mString.length() == 0) {out.writeUInt8((short) 0); return;}

            out.writeUInt8((short) (mString.length() + 1));
            for (int i = 0; i < mString.length(); i++)
                out.writeUInt16((short) mString.charAt(i));
            out.writeUInt16(0);
        }
        @Override protected void read(PtpTransport.PayloadBuffer in) throws PtpTransport.TransportDataError, PtpExceptions.MalformedDataType {
            int length = in.readUInt8();
            if (length == 0) {mString = ""; return;}

            char[] stringBuffer = new char[length - 1];
            for (int i = 0; i < (length - 1); i++) {
                stringBuffer[i] = (char) in.readUInt16();
                if (stringBuffer[i] == 0) throw new PtpExceptions.MalformedDataType("PtpString null-terminated before stated length!");
            }
            if (in.readUInt16() != 0) throw new PtpExceptions.MalformedDataType("PtpString not null-terminated at stated length!");
            mString = new String(stringBuffer, 0, length - 1);
        }
        @Override public String toString() {return "\"" + mString + "\"";}

        public PtpString() {}
        public PtpString(String s) {if (s.length() > 254) s = s.substring(0, 254); mString = s;}
    }


    public static class ArrayType<DT extends PtpDataType> extends PtpDataType implements Serializable {
        protected DT[] mArrayData;

        @Override protected void write(PtpTransport.PayloadBuffer out) {
            out.writeUInt32(mArrayData.length);
            for (DT dt : mArrayData) dt.write(out);
        }
        @Override protected void read(PtpTransport.PayloadBuffer in) throws PtpTransport.TransportDataError, PtpExceptions.MalformedDataType {
            long arrayLength = in.readUInt32();
            Class dtClass = mArrayData.getClass().getComponentType();
            // TODO...: potentially dangerous, can lead to OOM
            mArrayData = (DT[]) Array.newInstance(dtClass, (int) arrayLength);

            for (int i = 0; i < arrayLength; i++) {
                try {mArrayData[i] = (DT) dtClass.newInstance();} catch (InstantiationException | IllegalAccessException e) {}
                mArrayData[i].read(in);
            }
        }
        @Override public String toString() {
            String output = "{ ";
            for (DT dt : mArrayData) output += dt.toString() + ", ";
            output += "}";
            return output;
        }

        public ArrayType(Class dtClass) {mArrayData = (DT[]) Array.newInstance(dtClass, 0);}
    }


    // ---------------------------------------------------------------------------------------------
    // Derived Datatypes


    public static class OperationCode       extends Datacode implements Serializable {
        public String toString() {return super.toString() + " (" + PtpOperation.OPSCODE_DESCRIPTIONS.get(mValue) + ")";}
        public OperationCode(int value) {mValue = value;} public OperationCode() {}
    }
    public static class ResponseCode        extends Datacode implements Serializable {
        public String toString() {return super.toString() + " (" + PtpOperation.RSPCODE_DESCRIPTIONS.get(mValue) + ")";}
        public ResponseCode(int value) {mValue = value;} public ResponseCode() {}
    }
    public static class EventCode           extends Datacode implements Serializable {
        public String toString() {return super.toString() + " (" + PtpEvent.EVENTCODE_DESCRIPTIONS.get(mValue) + ")";}
        public EventCode(int value) {mValue = value;} public EventCode() {}
    }
    public static class DevicePropCode      extends Datacode implements Serializable {}
    public static class ObjectFormatCode    extends Datacode implements Serializable {
        public enum ObjectType {UNKNOWN, ASSOCIATION, NON_IMAGE, IMAGE, RAW}

        // TODO...: we should probably properly list the association-codes somewhere...?
        public ObjectType getType() {
            switch (mValue) {
                case 0xb101: return ObjectType.RAW;
                case 0x3001: return ObjectType.ASSOCIATION;
            }
            if ((mValue >= 0x3000) && (mValue <= 0x300c)) return ObjectType.NON_IMAGE;
            if ((mValue & 0x0800) != 0) return ObjectType.IMAGE;
            return ObjectType.UNKNOWN;
        }
        public ObjectFormatCode() {}
        public ObjectFormatCode(int value) {mValue = value;}
    }

    public static class StorageID           extends UInt32 implements Serializable {public StorageID(long value) {mValue = value;} public StorageID() {}}
    public static class ObjectHandle        extends UInt32 implements Serializable {public ObjectHandle(long value) {mValue = value;} public ObjectHandle() {}};
    public static class AssociationCode     extends UInt16 implements Serializable {public AssociationCode(int value) {mValue = value;} public AssociationCode() {}}
    public static class AssociationDesc     extends UInt32 implements Serializable {public AssociationDesc(int value) {mValue = value;} public AssociationDesc() {}}

    public static class PtpDateTime extends PtpString implements Serializable {
        public Date mDate = new Date();

        @Override protected void write(PtpTransport.PayloadBuffer out) {
            mString = mDate != null ? new SimpleDateFormat("yyyyMMdd'T'HHmmssZ").format(mDate) : "";
            super.write(out);
        }
        @Override protected void read(PtpTransport.PayloadBuffer in) throws PtpTransport.TransportDataError, PtpExceptions.MalformedDataType {
            super.read(in);
            if ("".equals(mString)) {mDate = null; return;}
            mDate = new SimpleDateFormat("yyyyMMdd'T'HHmmss.SZ").parse(mString, new ParsePosition(0));
            if (mDate == null) mDate = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ").parse(mString, new ParsePosition(0));
            if (mDate == null) mDate = new SimpleDateFormat("yyyyMMdd'T'HHmmss.S").parse(mString, new ParsePosition(0));
            if (mDate == null) mDate = new SimpleDateFormat("yyyyMMdd'T'HHmmss").parse(mString, new ParsePosition(0));
            if (mDate == null) throw new PtpExceptions.MalformedDataType("Cannot parse Date string (\"" + mString + "\")");
        }
        @Override public String toString() {return "[" + mDate + "]";}
    }

    public static class StorageIdArray    extends ArrayType<StorageID>    implements Serializable {public StorageIdArray() {super(StorageID.class);}}
    public static class ObjectHandleArray extends ArrayType<ObjectHandle> implements Serializable {public ObjectHandleArray() {super(ObjectHandle.class);}}


    public static class DeviceInfoDataSet extends PtpDataType implements Serializable {
        public UInt16 mStandardVersion = new UInt16();
        public UInt32 mVendorExtensionId = new UInt32();
        public UInt16 mVendorExtensionVersion = new UInt16();
        public PtpString mVendorExtensionDesc = new PtpString();
        public UInt16 mFunctionalMode = new UInt16();
        public ArrayType<OperationCode> mOperationsSupported = new ArrayType<>(OperationCode.class);
        public ArrayType<EventCode> mEventsSupported = new ArrayType<>(EventCode.class);
        public ArrayType<DevicePropCode> mDevicePropertiesSupported = new ArrayType<>(DevicePropCode.class);
        public ArrayType<ObjectFormatCode> mCaptureFormats = new ArrayType<>(ObjectFormatCode.class);
        public ArrayType<ObjectFormatCode> mImageFormats = new ArrayType<>(ObjectFormatCode.class);
        public PtpString mManufacturer = new PtpString();
        public PtpString mModel = new PtpString();
        public PtpString mDeviceVersion = new PtpString();
        public PtpString mSerialNumber = new PtpString();

        @Override protected void write(PtpTransport.PayloadBuffer out) {
            mStandardVersion.write(out);
            mVendorExtensionId.write(out);
            mVendorExtensionVersion.write(out);
            mVendorExtensionDesc.write(out);
            mFunctionalMode.write(out);
            mOperationsSupported.write(out);
            mEventsSupported.write(out);
            mDevicePropertiesSupported.write(out);
            mCaptureFormats.write(out);
            mImageFormats.write(out);
            mManufacturer.write(out);
            mModel.write(out);
            mDeviceVersion.write(out);
            mSerialNumber.write(out);
        }

        @Override protected void read(PtpTransport.PayloadBuffer in) throws PtpTransport.TransportDataError, PtpExceptions.MalformedDataType {
            mStandardVersion.read(in);
            mVendorExtensionId.read(in);
            mVendorExtensionVersion.read(in);
            mVendorExtensionDesc.read(in);
            mFunctionalMode.read(in);
            mOperationsSupported.read(in);
            mEventsSupported.read(in);
            mDevicePropertiesSupported.read(in);
            mCaptureFormats.read(in);
            mImageFormats.read(in);
            mManufacturer.read(in);
            mModel.read(in);
            mDeviceVersion.read(in);
            mSerialNumber.read(in);
        }

        @Override public String toString() {
            return  "\n[DeviceInfoDataSet"                                          + "]\n" +
                    "    [StandardVersion: "           + mStandardVersion           + "]\n" +
                    "    [VendorExtensionId: "         + mVendorExtensionId         + "]\n" +
                    "    [VendorExtensionVersion: "    + mVendorExtensionVersion    + "]\n" +
                    "    [VendorExtensionDesc: "       + mVendorExtensionDesc       + "]\n" +
                    "    [FunctionalMode: "            + mFunctionalMode            + "]\n" +
                    "    [OperationsSupported: "       + mOperationsSupported       + "]\n" +
                    "    [EventsSupported: "           + mEventsSupported           + "]\n" +
                    "    [DevicePropertiesSupported: " + mDevicePropertiesSupported + "]\n" +
                    "    [CaptureFormats: "            + mCaptureFormats            + "]\n" +
                    "    [ImageFormats: "              + mImageFormats              + "]\n" +
                    "    [Manufacturer: "              + mManufacturer              + "]\n" +
                    "    [Model: "                     + mModel                     + "]\n" +
                    "    [DeviceVersion: "             + mDeviceVersion             + "]\n" +
                    "    [SerialNumber: "              + mSerialNumber              + "]\n";
        }
    }


    public static class StorageInfoDataSet extends PtpDataType implements Serializable {
        public UInt16 mStorageType = new UInt16();
        public UInt16 mFileSystemType = new UInt16();
        public UInt16 mAccessCapability = new UInt16();
        public UInt64 mMaxCapacity = new UInt64();
        public UInt64 mFreeSpaceInBytes = new UInt64();
        public UInt32 mFreeSpaceInImages = new UInt32();
        public PtpString mStorageDescription = new PtpString();
        public PtpString mVolumeLabel = new PtpString();

        @Override protected void write(PtpTransport.PayloadBuffer out) {
            mStorageType.write(out);
            mFileSystemType.write(out);
            mAccessCapability.write(out);
            mMaxCapacity.write(out);
            mFreeSpaceInBytes.write(out);
            mFreeSpaceInImages.write(out);
            mStorageDescription.write(out);
            mVolumeLabel.write(out);
        }

        @Override protected void read(PtpTransport.PayloadBuffer in) throws PtpTransport.TransportDataError, PtpExceptions.MalformedDataType {
            mStorageType.read(in);
            mFileSystemType.read(in);
            mAccessCapability.read(in);
            mMaxCapacity.read(in);
            mFreeSpaceInBytes.read(in);
            mFreeSpaceInImages.read(in);
            mStorageDescription.read(in);
            mVolumeLabel.read(in);
        }

        @Override public String toString() {
            return  "\n[StorageInfoDataSet"                                + "]\n" +
                    "    [StorageType: "           + mStorageType          + "]\n" +
                    "    [FileSystemType: "        + mFileSystemType       + "]\n" +
                    "    [AccessCapability: "      + mAccessCapability     + "]\n" +
                    "    [MaxCapacity: "           + mMaxCapacity          + "]\n" +
                    "    [FreeSpaceInBytes: "      + mFreeSpaceInBytes     + "]\n" +
                    "    [FreeSpaceInImages: "     + mFreeSpaceInImages    + "]\n" +
                    "    [StorageDescription: "    + mStorageDescription   + "]\n" +
                    "    [VolumeLabel: "           + mVolumeLabel          + "]\n";
        }
    }


    public static class ObjectInfoDataSet extends PtpDataType implements Serializable {
        public StorageID mStorageID = new StorageID();
        public ObjectFormatCode mObjectFormatCode = new ObjectFormatCode();
        public UInt16 mProtectionStatus = new UInt16();
        public UInt32 mObjectCompressedSize = new UInt32();
        public ObjectFormatCode mThumbFormat = new ObjectFormatCode();
        public UInt32 mThumbCompressedSize = new UInt32();
        public UInt32 mThumbPixWidth = new UInt32();
        public UInt32 mThumbPixHeight = new UInt32();
        public UInt32 mImagePixWidth = new UInt32();
        public UInt32 mImagePixHeight = new UInt32();
        public UInt32 mImageBitDepth = new UInt32();
        public ObjectHandle mParentObject = new ObjectHandle();
        public AssociationCode mAssociationType = new AssociationCode();
        public AssociationDesc mAssociationDesc = new AssociationDesc();
        public UInt32 mSequenceNumber = new UInt32();
        public PtpString mFilename = new PtpString();
        public PtpDateTime mCaptureDate = new PtpDateTime();
        public PtpDateTime mModificationDate = new PtpDateTime();
        public PtpString mKeywords = new PtpString();

        @Override protected void write(PtpTransport.PayloadBuffer out) {
            mStorageID.write(out);
            mObjectFormatCode.write(out);
            mProtectionStatus.write(out);
            mObjectCompressedSize.write(out);
            mThumbFormat.write(out);
            mThumbCompressedSize.write(out);
            mThumbPixWidth.write(out);
            mThumbPixHeight.write(out);
            mImagePixWidth.write(out);
            mImagePixHeight.write(out);
            mImageBitDepth.write(out);
            mParentObject.write(out);
            mAssociationType.write(out);
            mAssociationDesc.write(out);
            mSequenceNumber.write(out);
            mFilename.write(out);
            mCaptureDate.write(out);
            mModificationDate.write(out);
            mKeywords.write(out);
        }

        @Override protected void read(PtpTransport.PayloadBuffer in) throws PtpTransport.TransportDataError, PtpExceptions.MalformedDataType {
            mStorageID.read(in);
            mObjectFormatCode.read(in);
            mProtectionStatus.read(in);
            mObjectCompressedSize.read(in);
            mThumbFormat.read(in);
            mThumbCompressedSize.read(in);
            mThumbPixWidth.read(in);
            mThumbPixHeight.read(in);
            mImagePixWidth.read(in);
            mImagePixHeight.read(in);
            mImageBitDepth.read(in);
            mParentObject.read(in);
            mAssociationType.read(in);
            mAssociationDesc.read(in);
            mSequenceNumber.read(in);
            mFilename.read(in);
            mCaptureDate.read(in);
            mModificationDate.read(in);
            mKeywords.read(in);
        }

        @Override public String toString() {
            return  "\n[ObjectInfoDataSet"                                 + "]\n" +
                    "    [StorageID: "             + mStorageID            + "]\n" +
                    "    [ObjectFormatCode: "      + mObjectFormatCode     + "]\n" +
                    "    [ProtectionStatus: "      + mProtectionStatus     + "]\n" +
                    "    [ObjectCompressedSize: "  + mObjectCompressedSize + "]\n" +
                    "    [ThumbFormat: "           + mThumbFormat          + "]\n" +
                    "    [ThumbCompressedSize: "   + mThumbCompressedSize  + "]\n" +
                    "    [ThumbPixWidth: "         + mThumbPixWidth        + "]\n" +
                    "    [ThumbPixHeight: "        + mThumbPixHeight       + "]\n" +
                    "    [ImagePixWidth: "         + mImagePixWidth        + "]\n" +
                    "    [ImagePixHeight: "        + mImagePixHeight       + "]\n" +
                    "    [ImageBitDepth: "         + mImageBitDepth        + "]\n" +
                    "    [ParentObject: "          + mParentObject         + "]\n" +
                    "    [AssociationType: "       + mAssociationType      + "]\n" +
                    "    [AssociationDesc: "       + mAssociationDesc      + "]\n" +
                    "    [SequenceNumber: "        + mSequenceNumber       + "]\n" +
                    "    [Filename: "              + mFilename             + "]\n" +
                    "    [CaptureDate: "           + mCaptureDate          + "]\n" +
                    "    [ModificationDate: "      + mModificationDate     + "]\n" +
                    "    [Keywords: "              + mKeywords             + "]\n";
        }
    }


    public abstract static class DevicePropDesc extends PtpDataType implements Serializable {
        // TODO...: implement (not abstract)
    }

    public static class Object extends PtpDataType implements Serializable {
        public byte[] mObject = new byte[0];

        protected void write(PtpTransport.PayloadBuffer out) {out.writeObject(mObject);}
        protected void read(PtpTransport.PayloadBuffer in) {mObject = in.readObject();}
        @Override public String toString() {
            String objectString= "";
            for (int i = 0; (i < mObject.length) && (i < 16); i++) objectString += String.format("%02x ", mObject[i]);
            if (mObject.length > 16) objectString += "+ " + (mObject.length - 16) + " more bytes";
            return "[Object][Length: " + mObject.length + " {" + objectString + "}]";
        }
    }
}
