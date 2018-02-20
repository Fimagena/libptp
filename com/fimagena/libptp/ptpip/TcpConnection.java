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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;


public class TcpConnection {
    private Socket mSocket;
    private OutputStream mOut;
    private InputStream mIn;
    private long mLastActivityTimestamp;
    private boolean mIsClosed = false;

    private final static Logger LOG = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private BlockingQueue<PtpIpPacket> mPacketOutQueue;

    private void putBlocking(PtpIpPacket packet) {
        while (true) {try {mPacketOutQueue.put(packet); return;} catch (InterruptedException e) {}}
    }


    private class TcpListener extends Thread {

        private PtpIpPacket.ReadingListener mReadingListener = new PtpIpPacket.ReadingListener() {
            @Override public void onLoaded(PtpIpPacket packet, int loadedBytes) {
                putBlocking(new PtpIpPacket.LoadStatus(packet, loadedBytes));
            }
        };

        public void run() {
            PtpIpPacket packet;
            while (true) {
                try {
                    packet = PtpIpPacket.readPacket(mIn, mReadingListener);
                    packet.setSourceConnection(TcpConnection.this);
                    mLastActivityTimestamp = System.currentTimeMillis();
                    LOG.info("PTPIP: Packet in  <== " + packet.toString());
                    putBlocking(packet);
                }
                catch (IOException | PtpIpExceptions.MalformedPacket e) {
                    // did we trigger this ourselves? --> shutdown listener and don't notify
                    if ((e instanceof IOException) && mIsClosed) return;

                    // something happened --> shutdown send error-packet upwards
                    LOG.severe("PTPIP: Error when receiving packet - closing connection! (" + e.getMessage() + ")");
                    try {mIn .close();} catch (Exception ex) {}
                    try {mOut.close();} catch (Exception ex) {}

                    packet = new PtpIpPacket.Error(e);
                    packet.setSourceConnection(TcpConnection.this);
                    putBlocking(packet);
                    return;
                }
            }
        }
    }


    public TcpConnection(BlockingQueue<PtpIpPacket> receivedPacketQueue) {
        mPacketOutQueue = receivedPacketQueue;
    }

    public void connect(InetSocketAddress server) throws IOException {
        try {
            mSocket = new Socket();
            mSocket.setSoTimeout(0);
            mSocket.setKeepAlive(true);
            mSocket.setTcpNoDelay(true);
            mSocket.connect(server);
            mOut = mSocket.getOutputStream();
            mIn = mSocket.getInputStream();
            mLastActivityTimestamp = System.currentTimeMillis();
        } catch (IOException e) {
            LOG.severe("Error on establishing TCP connection - closing! (" + e.getMessage() + ")");
            close();
            throw e;
        }
        new TcpListener().start();
    }

    public void sendPacket(PtpIpPacket packet) throws IOException {
        LOG.info("PTPIP: Packet out ==> " + packet.toString());
        if ((mSocket == null) || (mSocket.isClosed()) || (!mSocket.isConnected())) throw new IOException();
        mOut.write(packet.serializePacket());
        mOut.flush();
        mLastActivityTimestamp = System.currentTimeMillis();
    }

    public void close() {
        LOG.info("PTPIP: Closing TCP connection");
        mIsClosed = true;
        try {mIn .close();} catch (Exception e) {}
        try {mOut.close();} catch (Exception e) {}
    }

    public long getLastActivityTimestamp() {return mLastActivityTimestamp;}
}