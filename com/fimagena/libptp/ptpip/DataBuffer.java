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

package com.fimagena.libptp.ptpip;

import com.fimagena.libptp.PtpTransport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class DataBuffer implements PtpTransport.PayloadBuffer {

    private ByteArrayOutputStream mOut;
    private ByteArrayInputStream mIn;
    private byte[] mInData;

    private enum Status {READ, WRITE};
    private Status mStatus;

    private boolean mIsLE = true;

    public DataBuffer()                                          {this(null, 0, true);}
    public DataBuffer(boolean isLittleEndian)                    {this(null, 0, isLittleEndian);}
    public DataBuffer(int size)                                  {this(null, size, true);}
    public DataBuffer(int size, boolean isLittleEndian)          {this(null, size, isLittleEndian);}
    public DataBuffer(byte[] packetData)                         {this(packetData, 0, true);}
    public DataBuffer(byte[] packetData, boolean isLittleEndian) {this(packetData, 0, isLittleEndian);}

    private DataBuffer(byte[] packetData, int size, boolean isLittleEndian) {
        if (packetData == null) {
            mOut = size > 0 ? new ByteArrayOutputStream(size) : new ByteArrayOutputStream();
            mStatus = Status.WRITE;
        }
        else {
            mIn = new ByteArrayInputStream(packetData);
            mInData = packetData;
            mStatus = Status.READ;
        }
        mIsLE = isLittleEndian;
    }

    private void startWrite() {
        if (mStatus != Status.WRITE) {
            mOut = new ByteArrayOutputStream();
            mIn = null; mInData = null;
            mStatus = Status.WRITE;
        }
    }

    private void startRead(int length) throws PtpIpExceptions.MalformedPacket {
        if (mStatus != Status.READ) {
            mInData = mOut.toByteArray(); mIn = new ByteArrayInputStream(mInData);
            mOut = null;
            mStatus = Status.READ;
        }
        if (mIn.available() < length) throw new PtpIpExceptions.MalformedPacket("Insufficient data in buffer (expected " + length + " bytes)!");
    }

    interface LoadingListener {void onLoaded(int loadedBytes);}
    public void fill(InputStream inputStream, int length) throws IOException {fill(inputStream, length, null);}
    public void fill(InputStream inputStream, int length, LoadingListener listener) throws IOException {
        byte[] buffer = new byte[length];
        int pos = 0;
        while (pos < length) {
            int read = inputStream.read(buffer, pos, length - pos);
            if (read == -1) throw new IOException("InputStream closed unexpectedly!");
            pos += read;
            if ((pos < length) && (listener != null)) listener.onLoaded(pos);
        }
        mIn = new ByteArrayInputStream(buffer);
        mInData = buffer;
        mStatus = Status.READ;
    }

    public byte[] getData() {return mStatus == Status.READ ? mInData : mOut.toByteArray();}

    public int available() {
        if (mStatus == Status.WRITE) return 0;
        return mIn.available();
    }

    public void reset() {
        if (mStatus == Status.READ) mIn.reset();
        else mOut.reset();
    }

    public long size() {return mStatus == Status.WRITE ? mOut.size() : mInData.length;}

    @Override public DataBuffer writeUInt8(short uint8) {
        startWrite();
        mOut.write(uint8);
        return this;
    }

    @Override public DataBuffer writeUInt16(int uint16) {
        startWrite();
        if (mIsLE) {
            mOut.write(uint16); mOut.write(uint16 >> 8);}
        else       {
            mOut.write(uint16 >> 8); mOut.write(uint16);}
        return this;
    }

    @Override public DataBuffer writeUInt32(long uint32) {
        startWrite();
        if (mIsLE) {writeUInt16((int) (uint32 & 0x0000FFFF)); writeUInt16((int) (uint32 >> 16));}
        else       {writeUInt16((int) (uint32 >> 16)); writeUInt16((int) (uint32 & 0x0000FFFF));}
        return this;
    }

    @Override public DataBuffer writeUInt64(long uint64) {
        startWrite();
        if (mIsLE) {writeUInt32(uint64 & 0xFFFFFFFF); writeUInt32(uint64 >> 32);}
        else       {writeUInt32(uint64 >> 32); writeUInt32(uint64 & 0xFFFFFFFF);}
        return this;
    }

    public DataBuffer writeUtf16String(String string) {
        startWrite();
        for (int i = 0; i < string.length(); i++) writeUInt16(string.charAt(i));
        writeUInt16(0);
        return this;
    }

    @Override public DataBuffer writeObject(byte[] byteArray) {
        startWrite();
        if (byteArray == null) return this;
        mOut.write(byteArray, 0, byteArray.length);
        return this;
    }

    @Override public short readUInt8() throws PtpIpExceptions.MalformedPacket {
        startRead(1);
        return (short) mIn.read();
    }

    @Override public int readUInt16() throws PtpIpExceptions.MalformedPacket {
        startRead(2);
        int first = mIn.read();
        int second = mIn.read();
        if (mIsLE) {return first + (second << 8);}
        else       {return (first << 8) + second;}
    }

    @Override public long readUInt32() throws PtpIpExceptions.MalformedPacket {
        startRead(4);
        long first = readUInt16();
        long second = readUInt16();
        if (mIsLE) {return first + (second << 16);}
        else       {return (first << 16) + second;}
    }

    @Override public long readUInt64() throws PtpIpExceptions.MalformedPacket {
        startRead(4);
        long first = readUInt32();
        long second = readUInt32();
        if (mIsLE) {return first + (second << 32);}
        else       {return (first << 32) + second;}
    }

    public String readUtf16String() throws PtpIpExceptions.MalformedPacket {
        startRead(2);
        char[] buffer = new char[100];
        int offset = 0;
        while (offset < buffer.length) {
            if (mIn.available() < 2) throw new PtpIpExceptions.MalformedPacket("PtpIp-string not null-terminated!");
            buffer[offset] = (char)readUInt16();
            if (buffer[offset] == 0) return new String(buffer, 0, offset);
            offset++;
        }
        throw new PtpIpExceptions.MalformedPacket("PtpIp-string longer than internal limit of 100 chars!");
    }

    @Override public byte[] readObject() {
        try {startRead(0);} catch (PtpIpExceptions.MalformedPacket e) {} //can't happen
        byte[] byteArray = new byte[mIn.available()];
        mIn.read(byteArray, 0, byteArray.length);
        return byteArray;
    }
}
