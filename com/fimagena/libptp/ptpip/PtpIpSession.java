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

import com.fimagena.libptp.PtpEvent;
import com.fimagena.libptp.PtpExceptions;
import com.fimagena.libptp.PtpOperation;
import com.fimagena.libptp.PtpTransport;

import java.io.IOException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;


public class PtpIpSession implements PtpTransport.Session {
    private PtpIpConnection mPtpIpConnection;
    private long mLastTransactionId;
    private boolean mIsOpened = false;

    private BlockingDeque<PtpIpPacket> mTransactionPacketInQueue;

    private enum TransactionStatus {REQUEST_SENT, DATA_STARTED, DATA_ENDED, RESPONSE_RECEIVED}

    private final static Logger LOG = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);


    public PtpIpSession(PtpIpConnection ptpIpConnection, BlockingDeque<PtpIpPacket> transactionPacketInQueue) {
        mPtpIpConnection = ptpIpConnection;
        mLastTransactionId = 0;
        mTransactionPacketInQueue = transactionPacketInQueue;
    }

    protected void setOpened(boolean isOpened) {mIsOpened = isOpened;}
    protected boolean isOpened() {return mIsOpened;}

    private void testStatus(TransactionStatus currentStatus, TransactionStatus expectedStatus, PtpIpPacket packet) throws PtpIpExceptions.ProtocolViolation {
        testStatus(currentStatus, new TransactionStatus[] {expectedStatus}, packet);
    }
    private void testStatus(TransactionStatus currentStatus, TransactionStatus[] expectedStatus, PtpIpPacket packet) throws PtpIpExceptions.ProtocolViolation {
        for (TransactionStatus status : expectedStatus)
            if (currentStatus == status) return;
        throw new PtpIpExceptions.ProtocolViolation("Wrong PacketType: Received \"" + packet.getClass().getSimpleName() + "\" but currently in transaction status \"" + currentStatus.name() + "\"");
    }


    @Override public PtpOperation.Response executeTransaction(PtpOperation.Request request) throws PtpIpExceptions.MalformedPacket, PtpIpExceptions.ProtocolViolation, PtpTransport.TransportIOError, PtpIpExceptions.OperationFailed {
        return executeTransaction(request, null);
    }
    @Override public synchronized PtpOperation.Response executeTransaction(PtpOperation.Request request, DataLoadListener listener) throws PtpIpExceptions.MalformedPacket, PtpIpExceptions.ProtocolViolation, PtpTransport.TransportIOError, PtpIpExceptions.OperationFailed {
        mLastTransactionId++;
        return executeTransaction(request, mLastTransactionId, listener);
    }
    protected PtpOperation.Response executeNullTransaction(PtpOperation.Request request) throws PtpIpExceptions.MalformedPacket, PtpIpExceptions.ProtocolViolation, PtpTransport.TransportIOError, PtpIpExceptions.OperationFailed {
        return executeTransaction(request, 0, null);
    }
    private synchronized PtpOperation.Response executeTransaction(PtpOperation.Request request, long transactionId, DataLoadListener listener) throws PtpIpExceptions.MalformedPacket, PtpIpExceptions.ProtocolViolation, PtpTransport.TransportIOError, PtpIpExceptions.OperationFailed {
        // synchronized so that there's only one transaction executing at a time per session

        if (!mIsOpened && (transactionId != 0))
            throw new PtpIpExceptions.ProtocolViolation("Cannot execute transactions without opening session first!");

        // -------------------------------------------------------------------------------------
        // Send OperationRequest (+ data if required)

        LOG.info("PTP: Request out: ==> " + request.toString());

        PtpIpPacket.OperationRequest requestPacket = new PtpIpPacket.OperationRequest(request.hasData(), request.getOperationCode(), transactionId, request.getParameters());
        try {
            mPtpIpConnection.sendCommandChannelPacket(requestPacket);
            if (request.hasData()) {
                DataBuffer dataBuffer = new DataBuffer();
                request.getData().writeToBuffer(dataBuffer);
                mPtpIpConnection.sendCommandChannelPacket(new PtpIpPacket.StartData(transactionId, dataBuffer.size()));
                if (dataBuffer.size() > 0)
                    mPtpIpConnection.sendCommandChannelPacket(new PtpIpPacket.Data(transactionId, dataBuffer));
                mPtpIpConnection.sendCommandChannelPacket(new PtpIpPacket.EndData(transactionId, null));
            }
        }
        catch (IOException e) {throw new PtpIpExceptions.IOError(e);}
        TransactionStatus status = TransactionStatus.REQUEST_SENT;

        // -------------------------------------------------------------------------------------
        // Wait for and read response

        long dataRemaining = 0;
        DataBuffer dataIn = null;
        PtpOperation.Response response = PtpOperation.createResponse(request);

        while (status != TransactionStatus.RESPONSE_RECEIVED) {
            // takeBlocking
            PtpIpPacket packet = null;
            while (packet == null) {try {packet = mTransactionPacketInQueue.take();} catch (InterruptedException e) {}}

            // if Error --> throw Exception
            PtpIpExceptions.testError(packet);

            // if LoadStatus --> report onwards if we're receiving data and have a listener
            if (packet instanceof PtpIpPacket.LoadStatus) {
                PtpIpPacket subPacket = ((PtpIpPacket.LoadStatus) packet).mLoadedPacket;
                if ((subPacket instanceof PtpIpPacket.Data) || (subPacket instanceof PtpIpPacket.EndData)) {
                    testStatus(status, new TransactionStatus[]{TransactionStatus.DATA_STARTED, TransactionStatus.DATA_ENDED}, packet);
                    if (listener != null) listener.onDataLoaded(request, dataIn.size() + ((PtpIpPacket.LoadStatus) packet).mLoadedBytes - 8, dataIn.size() + dataRemaining);
                }
            }

            // if Event (must be cancel) --> abort if right transaction ID
            else if (packet instanceof PtpIpPacket.Event) {
                // this cannot happen, since we only put cancel-packets into the queue - but we'll double check anyway
                if (((PtpIpPacket.Event) packet).mEventCode != PtpEvent.EVENTCODE_CancelTransaction)
                    throw new PtpIpExceptions.ProtocolViolation("Non-cancel event packet in transaction queue!");
                if (((PtpIpPacket.Event) packet).mTransactionId > transactionId)           // this shouldn't happen but let's make sure
                    throw new PtpIpExceptions.ProtocolViolation("Received cancel-packet for future transaction!");
                if (((PtpIpPacket.Event) packet).mTransactionId < transactionId) continue; // must refer to an old transaction that finished - ignore

                throw new PtpIpExceptions.OperationFailed("Device cancelled transaction!", 0);
            }

            // if TransactionPacket --> check transactionId and process packet-types
            else if (packet instanceof PtpIpPacket.TransactionPacket) {
                if (((PtpIpPacket.TransactionPacket) packet).mTransactionId != transactionId)
                    throw new PtpIpExceptions.ProtocolViolation("Received wrong transaction-Id. Expected " + transactionId + ", received " + ((PtpIpPacket.TransactionPacket) packet).mTransactionId);

                // if StartData --> check state and move to data receiving
                if (packet instanceof PtpIpPacket.StartData) {
                    testStatus(status, TransactionStatus.REQUEST_SENT, packet);
                    if (((PtpIpPacket.StartData) packet).mDataLength == 0xFFFFFFFF)
                        throw new PtpIpExceptions.ProtocolViolation("Unknown data length (0xFFFFFFFF) currently not supported!");
                    dataRemaining = ((PtpIpPacket.StartData) packet).mDataLength;
                    dataIn = new DataBuffer((int) dataRemaining);
                    //TODO...: this can lead to OOM-situations
                    status = TransactionStatus.DATA_STARTED;
                }

                // if Data --> check state and receive
                else if (packet instanceof PtpIpPacket.Data) {
                    testStatus(status, TransactionStatus.DATA_STARTED, packet);
                    byte[] payload = (((PtpIpPacket.Data) packet).mDataPayload);
                    dataRemaining -= payload.length;
                    if (dataRemaining < 0) throw new PtpIpExceptions.ProtocolViolation("Received Data but longer than announced!");
                    dataIn.writeObject(payload);
                    mTransactionPacketInQueue.offerFirst(new PtpIpPacket.LoadStatus(packet, 0));
                }

                // if EndData --> check state and move to response expected
                else if (packet instanceof PtpIpPacket.EndData) {
                    testStatus(status, TransactionStatus.DATA_STARTED, packet);
                    byte[] payload = (((PtpIpPacket.EndData) packet).mDataPayload);
                    dataRemaining -= payload.length;
                    if (dataRemaining != 0) throw new PtpIpExceptions.ProtocolViolation("Received EndData but was expecting " + dataRemaining + " more; (EndData payload: " + payload.length);
                    dataIn.writeObject(payload);
                    status = TransactionStatus.DATA_ENDED;
                    if (payload.length != 0) mTransactionPacketInQueue.offerFirst(new PtpIpPacket.LoadStatus(packet, 0));
                }

                // if OperationResponse --> check state and complete transaction
                else if (packet instanceof PtpIpPacket.OperationResponse) {
                    testStatus(status, new TransactionStatus[] {TransactionStatus.REQUEST_SENT, TransactionStatus.DATA_ENDED}, packet);
                    PtpIpPacket.OperationResponse ptpIpResponse = (PtpIpPacket.OperationResponse) packet;
                    response.setResponseCode(ptpIpResponse.mResponseCode);
                    response.setParameters(ptpIpResponse.mParameters);
                    if (dataIn != null) response.setData(dataIn);
                    status = TransactionStatus.RESPONSE_RECEIVED;
                }
            }

            // if Other --> can't happen, there is nothing else --> Error
            else {
                LOG.severe("PTPIPsession: Encountered unknown internal packet type!");
                throw new PtpIpExceptions.MalformedPacket("Unknown internal packet type!");
            }
        }

        LOG.info("PTP: Response in: <== " + response.toString());

        return response;
    }

    @Override public void close() throws PtpTransport.TransportDataError, PtpTransport.TransportIOError, PtpTransport.TransportOperationFailed, PtpExceptions.PtpProtocolViolation {
        if (!isOpened()) return;
        PtpOperation.Response response = executeTransaction(PtpOperation.createRequest(PtpOperation.OPSCODE_CloseSession));
        response.validate();
        if (!response.isSuccess()) throw new PtpIpExceptions.OperationFailed("CloseSession", response.getResponseCode());
        mIsOpened = false;
    }
}
