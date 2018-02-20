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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;


public class PtpConnection {

    public interface EventCallbacks { // FIXME: these should be connection-specific, i.e., add connection-parameter
        void onError(Exception e);
        void onEvent(PtpEvent event);
    }

    private class EventListener extends Thread {
        public void run() {
            PtpEvent event;
            while (isConnected() && !mIsClosed) {
                // we stop listening when (a) we have closed the connection, or
                // (b) we get an error-packet up, or (c) transport is closed (to be sure - shouldn't happen)

                try {event = mEventInQueue.take();}
                catch (InterruptedException e) {continue;}

                if (event instanceof PtpEvent.Error) {
                    // connection's already gone, let's clean up session and tell everyone

                    mPtpSessions.clear();
                    //TODO: programmes might still hold references to sessions. They are invalidated now, so does it matter?

                    if (mListener != null) mListener.onError(((PtpEvent.Error) event).mException);
                }
                else if (mListener != null) mListener.onEvent(event);
            }
            mEventListener = null;
        }
    }

    private boolean mIsClosed = false;
    private EventListener mEventListener;

    private EventCallbacks mListener;

    private PtpTransport mTransport;
    private PtpTransport.ResponderAddress mAddress;
    private PtpDataType.DeviceInfoDataSet mDeviceInfo;
    private BlockingQueue<PtpEvent> mEventInQueue;

    private HashSet<PtpSession> mPtpSessions = new HashSet<>();

    public PtpConnection(PtpTransport transport) {
        mTransport = transport;
        mEventInQueue = mTransport.getEventQueue();
    }

    public PtpTransport.ResponderAddress getAddress() {return mAddress;}
    public PtpDataType.DeviceInfoDataSet getDeviceInfo() {return mDeviceInfo;}

    public List<PtpSession> getSessions() {return new ArrayList<>(mPtpSessions);}

    public void registerListener(EventCallbacks listener) {mListener = listener;}

    public void connect(PtpTransport.ResponderAddress address, PtpTransport.HostId hostId)
            throws PtpTransport.TransportOperationFailed, PtpTransport.TransportDataError, PtpTransport.TransportIOError, PtpExceptions.PtpProtocolViolation {
        mTransport.connect(address, hostId);
        mEventListener = new EventListener();
        mEventListener.start();
        mDeviceInfo = mTransport.getDeviceInfo();
        mAddress = address;
    }

    public PtpSession openSession()
            throws PtpTransport.TransportOperationFailed, PtpTransport.TransportDataError, PtpTransport.TransportIOError, PtpExceptions.PtpProtocolViolation {
        PtpTransport.Session session = mTransport.openSession();

        PtpSession ptpSession = new PtpSession(this, session);
        mPtpSessions.add(ptpSession);

        return ptpSession;
    }

    protected void onSessionClosed(PtpSession session) {mPtpSessions.remove(session);}

    public boolean isConnected() {return mTransport.isConnected();}

    public void close() throws PtpTransport.TransportError, PtpExceptions.PtpProtocolViolation {
        // we're closing - immediately stop listening to events
        mIsClosed = true;
        if (mEventListener != null) mEventListener.interrupt();

        // now close all sessions
        try {
            for (PtpSession session : mPtpSessions) session.close();
            mPtpSessions.clear();
        }

        // finally close the actual connection
        finally {mTransport.close();}
    }
}
