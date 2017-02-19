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
import java.util.concurrent.BlockingQueue;
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

    // Queues we own
    private BlockingQueue<PtpIpPacket> mIncomingPacketQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private BlockingQueue<PtpIpPacket> mInitPacketQueue     = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);

    // Queues we don't own
    private BlockingQueue<PtpIpPacket> mTransactionPacketQueue;
    private BlockingQueue<PtpEvent>    mEventQueue;

    private enum ConnectionStatus {INITIALIZED, CONNECTED, CLOSED}
    private ConnectionStatus mStatus;

    private PtpIpSession mSingleSession = new PtpIpSession(this);


    private class PtpIpListener extends Thread {

        private void putEvent(PtpEvent event) {
            if (mEventQueue == null) return;
            while (true) {
                try {mEventQueue.put(event); return;}
                catch (InterruptedException e) {}
            }
        }

        public void run() {
            PtpIpPacket packet = null;
            while (mStatus != ConnectionStatus.CLOSED) {

                // ---------------------------------------------------------------------------------
                // Check time since last traffic and send ping if over timeout value

                long remaining = PING_TIMEGAP - (System.currentTimeMillis() - Math.max(mCommandConnection.getTimestamp(), mEventConnection.getTimestamp()));
                if (remaining <= 0) {
                    // Sending ping when timing out; however, we're ignoring the pongs
                    try {mEventConnection.sendPacket(new PtpIpPacket.ProbeRequest());} catch (IOException e) {}
                    remaining = PING_TIMEGAP;
                }
                try {
                    packet = mIncomingPacketQueue.poll(remaining, TimeUnit.MILLISECONDS);
                }
                catch (InterruptedException e) {continue;}
                if (packet == null) continue;

                // ---------------------------------------------------------------------------------
                // Process packets according to type

                // if LoadStatus for a transaction --> send onwards
                if (packet instanceof PtpIpPacket.LoadStatus) {
                    if (((PtpIpPacket.LoadStatus) packet).mLoadedPacket instanceof PtpIpPacket.TransactionPacket)
                        TcpConnection.putBlocking(mTransactionPacketQueue, packet);
                }

                // if InternalPacket --> must be Error or Close --> close down
                else if (packet instanceof PtpIpPacket.InternalPacket) close();

                // if Ping --> send Pong
                else if (packet instanceof PtpIpPacket.ProbeRequest) {
                    try {packet.getSourceConnection().sendPacket(new PtpIpPacket.ProbeResponse());}
                    catch (IOException e) {}
                }

                // if Pong --> ignore
                else if (packet instanceof PtpIpPacket.ProbeResponse) {}

                // if Event --> send a layer upwards (and cancel also onwards to be sure)
                else if (packet instanceof PtpIpPacket.Event) {
                    PtpIpPacket.Event eventPacket = (PtpIpPacket.Event) packet;
                    putEvent(new PtpEvent(new PtpDataType.EventCode(eventPacket.mEventCode), new PtpDataType.UInt32(eventPacket.mTransactionId), eventPacket.mParameters));
                    if (((PtpIpPacket.Event) packet).mEventCode == PtpEvent.EVENTCODE_CancelTransaction) TcpConnection.putBlocking(mTransactionPacketQueue, packet);
                }

                // if InitPacket --> check for correct state and send onwards
                else if (packet instanceof PtpIpPacket.InitPacket) {
                    if (mStatus == ConnectionStatus.INITIALIZED) TcpConnection.putBlocking(mInitPacketQueue, packet);
                    else {
                        LOG.severe("PTPIP: Protocol violation (received InitPacket but not in Init state) - closing connection!");
                        packet = new PtpIpPacket.Error(new PtpIpExceptions.ProtocolViolation("Wrong PacketType: Received InitPacket but not in Init state!"));
                        close();
                    }
                }

                // if TransactionPacket --> check for correct state and send onwards
                else if (packet instanceof PtpIpPacket.TransactionPacket) {
                    if (mStatus == ConnectionStatus.CONNECTED)
                        TcpConnection.putBlocking(mTransactionPacketQueue, packet);
                    else {
                        LOG.severe("PTPIP: Protocol violation (received TransactionPacket but not in Connected state) - closing connection!");
                        packet = new PtpIpPacket.Error(new PtpIpExceptions.ProtocolViolation("Wrong PacketType: Received TransactionPacket but not in Connected state!"));
                        close();
                    }
                }

                // if Other --> can't happen, there is nothing else --> Error
                else {
                    LOG.severe("PTPIP: Encountered unknown internal packet type!");
                    packet = new PtpIpPacket.Error(new PtpIpExceptions.MalformedPacket("Unknown internal packet type!"));
                    close();
                }
            }

            // ---------------------------------------------------------------------------------
            // We're done - let's tell everyone

            if (!(packet instanceof PtpIpPacket.InternalPacket)) packet = new PtpIpPacket.Closed();
            TcpConnection.putBlocking(mInitPacketQueue, packet);
            TcpConnection.putBlocking(mTransactionPacketQueue, packet);
            putEvent(new PtpEvent.InternalEvent(packet instanceof PtpIpPacket.Closed ? PtpEvent.InternalEvent.Type.CLOSED : PtpEvent.InternalEvent.Type.ERROR, packet instanceof PtpIpPacket.Closed ? null : ((PtpIpPacket.Error) packet).mException));
        }
    }

    public PtpIpConnection() {
        mCommandConnection = new TcpConnection(mIncomingPacketQueue);
        mEventConnection = new TcpConnection(mIncomingPacketQueue);

        mTransactionPacketQueue = mSingleSession.getQueue();

        mStatus = ConnectionStatus.INITIALIZED;
    }

    @Override public void setEventQueue(BlockingQueue<PtpEvent> eventQueue) {mEventQueue = eventQueue;}

    @Override public PtpDataType.DeviceInfoDataSet getDeviceInfo() throws TransportDataError, TransportIOError, TransportOperationFailed, PtpExceptions.PtpProtocolViolation {
        PtpOperation.Response response = mSingleSession.executeNullTransaction(PtpOperation.createRequest(PtpOperation.OPSCODE_GetDeviceInfo));
        response.validate();
        if (!response.isSuccess()) throw new PtpIpExceptions.OperationFailed("GetDeviceInfo", response.getResponseCode());
        return (PtpDataType.DeviceInfoDataSet) response.getData();
    }

    @Override public PtpTransport.Session openSession() throws TransportDataError, TransportIOError, TransportOperationFailed, PtpExceptions.PtpProtocolViolation {return openSession(null);}
    @Override public PtpTransport.Session openSession(BlockingQueue<PtpEvent> eventQueue) throws TransportDataError, TransportIOError, TransportOperationFailed, PtpExceptions.PtpProtocolViolation {
        if (mSingleSession.isOpened()) {
            PtpIpConnection connection = new PtpIpConnection();
            connection.setEventQueue(eventQueue == null ? mEventQueue : eventQueue);
            connection.connect(mAddress, mHostId);
            return connection.openSession();
            //TODO...: if session on additional connection is closed, connection should be closed or cached for re-use
            //TODO: who closes additional PtpIpConnection eventually?
        }

        PtpOperation.Request request = PtpOperation.createRequest(PtpOperation.OPSCODE_OpenSession);
        request.setParameters(new long[]{SESSION_ID});
        PtpOperation.Response response = mSingleSession.executeNullTransaction(request);
        response.validate();
        if (!response.isSuccess()) throw new PtpIpExceptions.OperationFailed("OpenSession", response.getResponseCode());
        mSingleSession.setOpened();
        if (eventQueue != null) mEventQueue = eventQueue;
        // FIXME: if closing session with new eventqueue, connection event packets go to new queue anyway --> right semantics? --> we should probably cache connection queue? also need to check that both queues aren't identical so we're not sending double packets on one queue...

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

        PtpIpPacket packet = TcpConnection.takeBlocking(mInitPacketQueue);

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

        new PtpIpListener().start();

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
        mStatus = ConnectionStatus.CLOSED;
        mEventConnection.close();
        mCommandConnection.close();
    }
}
