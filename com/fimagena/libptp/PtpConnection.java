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

import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class PtpConnection {
    public final static int MAX_QUEUE_SIZE = 500;

    public interface EventCallbacks {
        void onError(PtpEvent.InternalEvent.Type type, Exception e);
        void onEvent(PtpEvent event);
    }

    private class EventListener extends Thread {
        public void run() {
            PtpEvent event;
            while (isConnected()) {
                try {event = mIncomingEventQueue.take();}
                catch (InterruptedException e) {continue;}

                if (event instanceof PtpEvent.InternalEvent) {
                    try {close();} catch (Exception e) {}
                    if (mListener != null) mListener.onError(((PtpEvent.InternalEvent) event).mType, ((PtpEvent.InternalEvent) event).mException);
                }
                else if (mListener != null) mListener.onEvent(event);
            }
        }
    }

    private EventCallbacks mListener;

    private PtpTransport mTransport;
    private PtpDataType.DeviceInfoDataSet mDeviceInfo;
    private BlockingQueue<PtpEvent> mIncomingEventQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);

    private HashSet<PtpSession> mPtpSessions = new HashSet<>();

    public PtpConnection(PtpTransport transport) {
        mTransport = transport;
        mTransport.setEventQueue(mIncomingEventQueue);
    }

    public void registerListener(EventCallbacks listener) {mListener = listener;}

    public void connect(PtpTransport.ResponderAddress address, PtpTransport.HostId hostId)
            throws PtpTransport.TransportOperationFailed, PtpTransport.TransportDataError, PtpTransport.TransportIOError, PtpExceptions.PtpProtocolViolation {
        mTransport.connect(address, hostId);
        new EventListener().start();
        mDeviceInfo = mTransport.getDeviceInfo();
    }

    public PtpSession openSession()
            throws PtpTransport.TransportOperationFailed, PtpTransport.TransportDataError, PtpTransport.TransportIOError, PtpExceptions.PtpProtocolViolation {
        PtpTransport.Session session = mTransport.openSession();

        PtpSession ptpSession = new PtpSession(session, mDeviceInfo);
        mPtpSessions.add(ptpSession);

        return ptpSession;
    }

    public boolean isConnected() {return mTransport.isConnected();}

    public void close() throws PtpTransport.TransportError, PtpExceptions.PtpProtocolViolation {
        try {for (PtpSession session : mPtpSessions) session.close();}
        finally {mTransport.close();}
    }
}
