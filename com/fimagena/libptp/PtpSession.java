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


public class PtpSession {

    public interface DataLoadListener {void onDataLoaded(long loaded, long expected);}

    private PtpConnection mConnection;
    private PtpTransport.Session mSession;


    protected PtpSession(PtpConnection connection, PtpTransport.Session session) {
        mConnection = connection;
        mSession = session;
    }
    // TODO..: check deviceInfo which functions are allowed

    public void close() throws PtpTransport.TransportError, PtpExceptions.PtpProtocolViolation {
        mSession.close();
        mConnection.onSessionClosed(this);
    }

    // -----------------------------------------------------------------------------------------
    // PTP transaction
    // TODO: asynchronous requests should return transactionID as well

    public PtpConnection getConnection() {return mConnection;}

    public PtpDataType.StorageID[] getStorageIDs() throws PtpTransport.TransportError, PtpExceptions.PtpProtocolViolation, PtpExceptions.OperationFailed {
        PtpOperation.Response response = mSession.executeTransaction(PtpOperation.createRequest(PtpOperation.OPSCODE_GetStorageIDs));
        response.validate();
        if (!response.isSuccess())
            throw new PtpExceptions.OperationFailed("GetStorageIds", response.getResponseCode());
        return ((PtpDataType.StorageIdArray) response.getData()).mArrayData;
    }

    public PtpDataType.StorageInfoDataSet getStorageInfo(PtpDataType.StorageID storageId) throws PtpTransport.TransportError, PtpExceptions.PtpProtocolViolation, PtpExceptions.OperationFailed {
        PtpOperation.Request request = PtpOperation.createRequest(PtpOperation.OPSCODE_GetStorageInfo);
        request.mParameters = new long[]{storageId.mValue};
        PtpOperation.Response response = mSession.executeTransaction(request);
        response.validate();
        if (!response.isSuccess())
            throw new PtpExceptions.OperationFailed("GetStorageInfo", response.getResponseCode());
        return (PtpDataType.StorageInfoDataSet) response.getData();
    }

    public long getNumObjects(PtpDataType.StorageID storageId) throws PtpTransport.TransportError, PtpExceptions.PtpProtocolViolation, PtpExceptions.OperationFailed {
        return getNumObjects(storageId, new PtpDataType.ObjectFormatCode(), new PtpDataType.ObjectHandle());
    }

    public long getNumObjects(PtpDataType.StorageID storageId, PtpDataType.ObjectHandle associationHandle) throws PtpTransport.TransportError, PtpExceptions.PtpProtocolViolation, PtpExceptions.OperationFailed {
        return getNumObjects(storageId, new PtpDataType.ObjectFormatCode(), associationHandle);
    }

    public long getNumObjects(PtpDataType.StorageID storageId, PtpDataType.ObjectFormatCode objectFormat) throws PtpTransport.TransportError, PtpExceptions.PtpProtocolViolation, PtpExceptions.OperationFailed {
        return getNumObjects(storageId, objectFormat, new PtpDataType.ObjectHandle());
    }

    public long getNumObjects(PtpDataType.StorageID storageId, PtpDataType.ObjectFormatCode objectFormat, PtpDataType.ObjectHandle associationHandle) throws PtpTransport.TransportError, PtpExceptions.PtpProtocolViolation, PtpExceptions.OperationFailed {
        PtpOperation.Request request = PtpOperation.createRequest(PtpOperation.OPSCODE_GetNumObjects);
        request.mParameters = new long[]{storageId.mValue, objectFormat.mValue, associationHandle.mValue};
        PtpOperation.Response response = mSession.executeTransaction(request);
        response.validate();
        if (!response.isSuccess())
            throw new PtpExceptions.OperationFailed("GetNumObjects", response.getResponseCode());
        return response.mParameters[0];
    }

    public PtpDataType.ObjectHandle[] getObjectHandles(PtpDataType.StorageID storageId) throws PtpTransport.TransportError, PtpExceptions.PtpProtocolViolation, PtpExceptions.OperationFailed {
        return getObjectHandles(storageId, new PtpDataType.ObjectFormatCode(), new PtpDataType.ObjectHandle());
    }

    public PtpDataType.ObjectHandle[] getObjectHandles(PtpDataType.StorageID storageId, PtpDataType.ObjectHandle associationHandle) throws PtpTransport.TransportError, PtpExceptions.PtpProtocolViolation, PtpExceptions.OperationFailed {
        return getObjectHandles(storageId, new PtpDataType.ObjectFormatCode(), associationHandle);
    }

    public PtpDataType.ObjectHandle[] getObjectHandles(PtpDataType.StorageID storageId, PtpDataType.ObjectFormatCode objectFormat) throws PtpTransport.TransportError, PtpExceptions.PtpProtocolViolation, PtpExceptions.OperationFailed {
        return getObjectHandles(storageId, objectFormat, new PtpDataType.ObjectHandle());
    }

    public PtpDataType.ObjectHandle[] getObjectHandles(PtpDataType.StorageID storageId, PtpDataType.ObjectFormatCode objectFormat, PtpDataType.ObjectHandle associationHandle) throws PtpTransport.TransportError, PtpExceptions.PtpProtocolViolation, PtpExceptions.OperationFailed {
        PtpOperation.Request request = PtpOperation.createRequest(PtpOperation.OPSCODE_GetObjectHandles);
        request.mParameters = new long[]{storageId.mValue, objectFormat.mValue, associationHandle.mValue};
        PtpOperation.Response response = mSession.executeTransaction(request);
        response.validate();
        if (!response.isSuccess())
            throw new PtpExceptions.OperationFailed("GetObjectHandles", response.getResponseCode());
        return ((PtpDataType.ObjectHandleArray) response.getData()).mArrayData;
    }

    public PtpDataType.ObjectInfoDataSet getObjectInfo(PtpDataType.ObjectHandle objectHandle) throws PtpTransport.TransportError, PtpExceptions.PtpProtocolViolation, PtpExceptions.OperationFailed {
        PtpOperation.Request request = PtpOperation.createRequest(PtpOperation.OPSCODE_GetObjectInfo);
        request.mParameters = new long[]{objectHandle.mValue};
        PtpOperation.Response response = mSession.executeTransaction(request);
        response.validate();
        if (!response.isSuccess())
            throw new PtpExceptions.OperationFailed("GetObjectInfo", response.getResponseCode());
        return (PtpDataType.ObjectInfoDataSet) response.getData();
    }

    public byte[] getObject(PtpDataType.ObjectHandle objectHandle) throws PtpTransport.TransportError, PtpExceptions.PtpProtocolViolation, PtpExceptions.OperationFailed {
        return getObject(objectHandle, null);
    }
    public byte[] getObject(PtpDataType.ObjectHandle objectHandle, final DataLoadListener listener) throws PtpTransport.TransportError, PtpExceptions.PtpProtocolViolation, PtpExceptions.OperationFailed {
        PtpOperation.Request request = PtpOperation.createRequest(PtpOperation.OPSCODE_GetObject);
        request.mParameters = new long[]{objectHandle.mValue};
        PtpOperation.Response response = mSession.executeTransaction(request, listener == null ? null : new PtpTransport.Session.DataLoadListener() {
            @Override public void onDataLoaded(PtpOperation.Request request, long loaded, long expected) {listener.onDataLoaded(loaded, expected);}
        });
        response.validate();
        if (!response.isSuccess())
            throw new PtpExceptions.OperationFailed("GetObject", response.getResponseCode());
        return ((PtpDataType.Object) response.getData()).mObject;
    }

    // FIXME: need to check for IMAGE, since (Sony) camera might stop otherwise...
    public byte[] getThumb(PtpDataType.ObjectHandle objectHandle) throws PtpTransport.TransportError, PtpExceptions.PtpProtocolViolation, PtpExceptions.OperationFailed {
        return getThumb(objectHandle, null);
    }
    public byte[] getThumb(PtpDataType.ObjectHandle objectHandle, final DataLoadListener listener) throws PtpTransport.TransportError, PtpExceptions.PtpProtocolViolation, PtpExceptions.OperationFailed {
        PtpOperation.Request request = PtpOperation.createRequest(PtpOperation.OPSCODE_GetThumb);
        request.mParameters = new long[]{objectHandle.mValue};
        PtpOperation.Response response = mSession.executeTransaction(request, listener == null ? null : new PtpTransport.Session.DataLoadListener() {
            @Override public void onDataLoaded(PtpOperation.Request request, long loaded, long expected) {listener.onDataLoaded(loaded, expected);}
        });
        response.validate();
        if (!response.isSuccess())
            throw new PtpExceptions.OperationFailed("GetThumb", response.getResponseCode());
        return ((PtpDataType.Object) response.getData()).mObject;
    }

    public void initiateCapture() throws PtpTransport.TransportError, PtpExceptions.PtpProtocolViolation, PtpExceptions.OperationFailed {
        initiateCapture(new PtpDataType.StorageID(0), new PtpDataType.ObjectFormatCode(0));
    }
    public void initiateCapture(PtpDataType.StorageID storageID, PtpDataType.ObjectFormatCode objectFormatCode) throws PtpTransport.TransportError, PtpExceptions.PtpProtocolViolation, PtpExceptions.OperationFailed {
        PtpOperation.Request request = PtpOperation.createRequest(PtpOperation.OPSCODE_InitiateCapture);
        request.mParameters = new long[]{ storageID.mValue, objectFormatCode.mValue };
        PtpOperation.Response response = mSession.executeTransaction(request);
        response.validate();
        if (!response.isSuccess())
            throw new PtpExceptions.OperationFailed("GetThumb", response.getResponseCode());
    }

/*      public void deleteObject() {}
    public void sendObjectInfo() {}
    public void sendObject() {}
    public void initiateCapture() {}
    public void formatStore() {}
    public void resetDevice() {}
    public void selfTest() {}
    public void setObjectProtection() {}
    public void powerDown() {}
    public void getDevicePropDesc() {}
    public void getDevicePropValue() {}
    public void setDevicePropValue() {}
    public void resetDevicePropValue() {}
    public void terminateOpenCapture() {}
    public void moveObject() {}
    public void copyObject() {}
    public void getPartialObject() {} */
}
