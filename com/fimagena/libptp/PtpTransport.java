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

import java.util.concurrent.BlockingQueue;


public abstract class PtpTransport {

    public abstract static class ResponderAddress {}
    public abstract static class HostId {}


    public abstract static class TransportError extends Exception {
        public TransportError(String s) {super(s);}
        public TransportError(String s, Throwable cause) {super(s, cause);}
    }
    public abstract static class TransportOperationFailed extends TransportError {
        public TransportOperationFailed(String s) {super(s);}
        public TransportOperationFailed(String s, Throwable cause) {super(s, cause);}
    }
    public abstract static class TransportDataError extends TransportError {
        public TransportDataError(String s) {super(s);}
        public TransportDataError(String s, Throwable cause) {super(s, cause);}
    }
    public abstract static class TransportIOError extends TransportError {
        public TransportIOError(String s) {super(s);}
        public TransportIOError(String s, Throwable cause) {super(s, cause);}
    }


    public interface Session {
        public interface DataLoadListener {void onDataLoaded(PtpOperation.Request request, long loaded, long expected);}

        PtpOperation.Response executeTransaction(PtpOperation.Request request) throws TransportDataError, TransportIOError, TransportOperationFailed;
        PtpOperation.Response executeTransaction(PtpOperation.Request request, DataLoadListener listener) throws TransportDataError, TransportIOError, TransportOperationFailed;
        void close() throws TransportDataError, TransportIOError, TransportOperationFailed, PtpExceptions.PtpProtocolViolation;
    }


    public interface PayloadBuffer {
        PayloadBuffer writeUInt8 (short  value);
        PayloadBuffer writeUInt16(int    value);
        PayloadBuffer writeUInt32(long   value);
        PayloadBuffer writeUInt64(long   value);
        PayloadBuffer writeObject(byte[] object);

        short  readUInt8 () throws TransportDataError;
        int    readUInt16() throws TransportDataError;
        long   readUInt32() throws TransportDataError;
        long   readUInt64() throws TransportDataError;
        byte[] readObject();
    }


    public abstract BlockingQueue<PtpEvent> getEventQueue();

    public abstract PtpDataType.DeviceInfoDataSet getDeviceInfo() throws TransportOperationFailed, TransportDataError, TransportIOError, PtpExceptions.PtpProtocolViolation;

    public abstract Session openSession() throws TransportOperationFailed, TransportDataError, TransportIOError, PtpExceptions.PtpProtocolViolation;

    public abstract boolean isConnected();
    public abstract void connect(ResponderAddress address, HostId hostId) throws TransportOperationFailed, TransportDataError, TransportIOError;

    public abstract boolean isClosed();
    public abstract void close() throws TransportIOError;
}
