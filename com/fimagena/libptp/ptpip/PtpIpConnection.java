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

import com.fimagena.libptp.PtpDataType;
import com.fimagena.libptp.PtpExceptions;
import com.fimagena.libptp.PtpOperation;
import com.fimagena.libptp.PtpEvent;
import com.fimagena.libptp.PtpTransport;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PtpIpConnection extends PtpTransport {
    public final static int MAX_QUEUE_SIZE = 500;

    private final static int PTP_VERSION_MAJOR = 1;
    private final static int PTP_VERSION_MINOR = 0;
    private final static int PTP_PORT = 15740;

    private final static int SESSION_ID = 1; // only one session per connection for PtpIp - use this id

    private final static long PING_TIMEGAP = 20000; // ping after 20 seconds of inactivity

    private final static Logger LOG = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    static {LOG.setLevel(Level.SEVERE);}


    public static class PtpIpAddress extends PtpTransport.ResponderAddress {
        protected InetSocketAddress mTcpAddress;

        public PtpIpAddress(InetAddress address) {this(address, PTP_PORT);}
        public PtpIpAddress(InetAddress address, int port) {mTcpAddress = new InetSocketAddress(address, port);}

        public String toString() {return mTcpAddress.toString();}
    }


    public static class PtpIpHostId extends PtpTransport.HostId {
        protected short[] mGuid;
        protected String mFriendlyName;
        protected int mVersionMajor, mVersionMinor;

        public PtpIpHostId(short[] guid, String friendlyName) {this(guid, friendlyName, PTP_VERSION_MAJOR, PTP_VERSION_MINOR);}
        public PtpIpHostId(short[] guid, String friendlyName, int versionMajor, int versionMinor) {
            mGuid = guid;
            mFriendlyName = friendlyName;
            mVersionMajor = versionMajor;
            mVersionMinor = versionMinor;
        }
    }


    private PtpIpAddress mAddress;
    private PtpIpHostId mHostId;

    private TcpConnection mCommandConnection;
    private TcpConnection mEventConnection;

    // TcpConnections send us PtpPackets in our incoming queue...
    private BlockingDeque<PtpIpPacket> mPacketInQueue             = new LinkedBlockingDeque<>(MAX_QUEUE_SIZE);

    // ...which we then distribute into various outgoing queues
    private BlockingQueue<PtpIpPacket> mInitPacketOutQueue        = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private BlockingDeque<PtpIpPacket> mTransactionPacketOutQueue = new LinkedBlockingDeque<>(MAX_QUEUE_SIZE);
    private BlockingQueue<PtpEvent>    mEventOutQueue             = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);

    @Override public BlockingQueue<PtpEvent> getEventQueue() {return mEventOutQueue;}

    private enum ConnectionStatus {INITIALIZED, CONNECTED, CLOSED}
    private ConnectionStatus mStatus;

    private PtpIpSession mSingleSession;

    private PtpIpPacketListener mPtpIpPacketListener;

    private class PtpIpPacketListener extends Thread {

        private void putBlocking(BlockingQueue<PtpIpPacket> queue, PtpIpPacket packet) {
            while (true) {try {queue.put(packet); return;} catch (InterruptedException e) {}}
        }

        private void putFirstBlocking(BlockingDeque<PtpIpPacket> queue, PtpIpPacket packet) {
            while (true) {try {queue.putFirst(packet); return;} catch (InterruptedException e) {}}
        }

        private void putEvent(PtpEvent event) {
            if (mEventOutQueue == null) return;
            while (true) {try {mEventOutQueue.put(event); return;} catch (InterruptedException e) {}}
        }

        public void run() {
            PtpIpPacket packet = null;
            while (mStatus != ConnectionStatus.CLOSED) {

                // ---------------------------------------------------------------------------------
                // Check time since last traffic and send ping if over timeout value

                long remaining = PING_TIMEGAP - (System.currentTimeMillis() - Math.max(mCommandConnection.getLastActivityTimestamp(), mEventConnection.getLastActivityTimestamp()));
                if (remaining <= 0) {
                    // Sending ping when timing out; however, we're ignoring the pongs
                    try {mEventConnection.sendPacket(new PtpIpPacket.ProbeRequest());} catch (IOException e) {}
                    remaining = PING_TIMEGAP;
                }
                try {packet = mPacketInQueue.poll(remaining, TimeUnit.MILLISECONDS);} catch (InterruptedException e) {continue;}
                if (packet == null) continue;

                // ---------------------------------------------------------------------------------
                // Process packets according to type

                // ---------------------------------------------------------------------------------
                // Internal packets

                // if LoadStatus for a transaction --> send onwards
                if (packet instanceof PtpIpPacket.LoadStatus) {
                    if (((PtpIpPacket.LoadStatus) packet).mLoadedPacket instanceof PtpIpPacket.TransactionPacket)
                        putBlocking(mTransactionPacketOutQueue, packet);
                }

                // if Error --> something happened, let's close down and tell everybody
                else if (packet instanceof PtpIpPacket.Error) {
                    // close downward stack
                    close();

                    mSingleSession.setOpened(false);

                    // tell upward stack
                    putBlocking(mInitPacketOutQueue, packet);
                    putBlocking(mTransactionPacketOutQueue, packet);
                    putEvent(new PtpEvent.Error(((PtpIpPacket.Error) packet).mException));
                }

                // ---------------------------------------------------------------------------------
                // Probing packets

                // if Ping --> send Pong
                else if (packet instanceof PtpIpPacket.ProbeRequest) {
                    try {packet.getSourceConnection().sendPacket(new PtpIpPacket.ProbeResponse());}
                    catch (IOException e) {}
                }

                // if Pong --> ignore
                else if (packet instanceof PtpIpPacket.ProbeResponse) {}

                // ---------------------------------------------------------------------------------
                // Init packets

                // if InitPacket --> check for correct state and send onwards
                else if (packet instanceof PtpIpPacket.InitPacket) {
                    if (mStatus == ConnectionStatus.INITIALIZED) putBlocking(mInitPacketOutQueue, packet);
                    else {
                        LOG.severe("PTPIP: Protocol violation (received InitPacket but not in Init state) - closing connection!");
                        putFirstBlocking(mPacketInQueue, new PtpIpPacket.Error(new PtpIpExceptions.ProtocolViolation("Wrong PacketType: Received InitPacket but not in Init state!")));
                    }
                }

                // ---------------------------------------------------------------------------------
                // Transaction packets

                // if Event --> send a layer upwards (and cancel also onwards to be sure)
                else if (packet instanceof PtpIpPacket.Event) {
                    PtpIpPacket.Event eventPacket = (PtpIpPacket.Event) packet;
                    putEvent(new PtpEvent(new PtpDataType.EventCode(eventPacket.mEventCode), new PtpDataType.UInt32(eventPacket.mTransactionId), eventPacket.mParameters));
                    if (eventPacket.mEventCode == PtpEvent.EVENTCODE_CancelTransaction) putBlocking(mTransactionPacketOutQueue, packet);
                }

                // if TransactionPacket --> check for correct state and send onwards
                else if (packet instanceof PtpIpPacket.TransactionPacket) {
                    if (mStatus == ConnectionStatus.CONNECTED) putBlocking(mTransactionPacketOutQueue, packet);
                    else {
                        LOG.severe("PTPIP: Protocol violation (received TransactionPacket but not in Connected state) - closing connection!");
                        putFirstBlocking(mPacketInQueue, new PtpIpPacket.Error(new PtpIpExceptions.ProtocolViolation("Wrong PacketType: Received TransactionPacket but not in Connected state!")));
                    }
                }

                // ---------------------------------------------------------------------------------
                // if Other --> can't happen, there is nothing else --> Error
                else {
                    LOG.severe("PTPIP: Encountered unknown internal packet type!");
                    putFirstBlocking(mPacketInQueue, new PtpIpPacket.Error(new PtpIpExceptions.MalformedPacket("Unknown internal packet type!")));
                }
            }

            // We're done listening, remove reference - not strictly necessary but the Thread is toast - might as well clean up resources
            mPtpIpPacketListener = null;
        }
    }

    public PtpIpConnection() {this(null);}
    public PtpIpConnection(BlockingQueue<PtpEvent> eventOutQueue){
        // we use only one packet-queue for both TCP-channels
        mCommandConnection = new TcpConnection(mPacketInQueue);
        mEventConnection   = new TcpConnection(mPacketInQueue);

        // ability to set EventOutQueue only used for add'l session based on add'l connection with same EventQueue
        if (eventOutQueue != null) mEventOutQueue = eventOutQueue;

        mSingleSession = new PtpIpSession(this, mTransactionPacketOutQueue);

        mStatus = ConnectionStatus.INITIALIZED;
    }

    @Override public PtpDataType.DeviceInfoDataSet getDeviceInfo() throws TransportDataError, TransportIOError, TransportOperationFailed, PtpExceptions.PtpProtocolViolation {
        PtpOperation.Response response = mSingleSession.executeNullTransaction(PtpOperation.createRequest(PtpOperation.OPSCODE_GetDeviceInfo));
        response.validate();
        if (!response.isSuccess()) throw new PtpIpExceptions.OperationFailed("GetDeviceInfo", response.getResponseCode());
        return (PtpDataType.DeviceInfoDataSet) response.getData();
    }

    @Override public PtpTransport.Session openSession() throws TransportDataError, TransportIOError, TransportOperationFailed, PtpExceptions.PtpProtocolViolation {
        if (mSingleSession.isOpened()) {
            PtpIpConnection connection = new PtpIpConnection(mEventOutQueue);
            connection.connect(mAddress, mHostId);
            return connection.openSession();
            //FIXME: add'l connection is never properly closed - no reference is being held
        }

        PtpOperation.Request request = PtpOperation.createRequest(PtpOperation.OPSCODE_OpenSession);
        request.setParameters(new long[]{SESSION_ID});
        PtpOperation.Response response = mSingleSession.executeNullTransaction(request);
        response.validate();
        if (!response.isSuccess()) throw new PtpIpExceptions.OperationFailed("OpenSession", response.getResponseCode());
        mSingleSession.setOpened(true);

        return mSingleSession;
    }

    protected void sendCommandChannelPacket(PtpIpPacket packet) throws IOException {mCommandConnection.sendPacket(packet);}
    protected void sendEventChannelPacket(PtpIpPacket.Event packet) throws IOException {mEventConnection.sendPacket(packet);}

    private PtpIpPacket.InitPacket connectChannel(TcpConnection tcpConnection, InetSocketAddress address, PtpIpPacket.InitPacket initPacket, Class expectedAnswer)
            throws PtpIpExceptions.IOError, PtpIpExceptions.MalformedPacket, PtpIpExceptions.ProtocolViolation, PtpIpExceptions.OperationFailed {
        try {
            tcpConnection.connect(address);
            tcpConnection.sendPacket(initPacket);
        }
        catch (IOException e) {close(); throw new PtpIpExceptions.IOError("Could not connect channel!", e);}

        // takeBlocking
        PtpIpPacket packet = null;
        while (packet == null) {try {packet = mInitPacketOutQueue.take();} catch (InterruptedException e) {}}

        if (packet instanceof PtpIpPacket.InitFail)
            throw new PtpIpExceptions.OperationFailed("InitRequest", ((PtpIpPacket.InitFail) packet).mReason);
        PtpIpExceptions.testError(packet);
        PtpIpExceptions.testType(packet, expectedAnswer);

        return (PtpIpPacket.InitPacket) packet;
    }

    @Override public boolean isConnected() {return mStatus == ConnectionStatus.CONNECTED;}
    @Override public void connect(PtpTransport.ResponderAddress address, PtpTransport.HostId hostId)
            throws PtpIpExceptions.IOError, PtpIpExceptions.MalformedPacket, PtpIpExceptions.ProtocolViolation, PtpIpExceptions.OperationFailed {
        mHostId = (PtpIpHostId) hostId;
        mAddress = (PtpIpAddress) address;

        mPtpIpPacketListener = new PtpIpPacketListener();
        mPtpIpPacketListener.start();

        // -----------------------------------------------------------------------------------------
        // Open command and event channel

        try {
            PtpIpPacket.InitPacket initAck = connectChannel(mCommandConnection, mAddress.mTcpAddress, new PtpIpPacket.InitCommandRequest(mHostId), PtpIpPacket.InitCommandAck.class);
            long mConnectionNumber = ((PtpIpPacket.InitCommandAck) initAck).mConnectionNumber;
            connectChannel(mEventConnection, mAddress.mTcpAddress, new PtpIpPacket.InitEventRequest(mConnectionNumber), PtpIpPacket.InitEventAck.class);
        }
        catch (PtpIpExceptions.IOError | PtpIpExceptions.MalformedPacket | PtpIpExceptions.ProtocolViolation e) {
            close();
            throw e;
        }
        mStatus = ConnectionStatus.CONNECTED;
    }


    @Override public boolean isClosed() {return mStatus == ConnectionStatus.CLOSED;}

    public void close() {
        // stop listening
        mStatus = ConnectionStatus.CLOSED;
        if (mPtpIpPacketListener != null) mPtpIpPacketListener.interrupt();

        mSingleSession.setOpened(false);

        // shutdown TCP connections without further noise
        mEventConnection.close();
        mCommandConnection.close();
    }
}
