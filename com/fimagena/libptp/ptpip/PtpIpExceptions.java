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

import java.io.IOException;


public class PtpIpExceptions {

    // ---------------------------------------------------------------------------------------------
    // Assert functions

    public static void testError(PtpIpPacket packet) throws IOError, MalformedPacket {
        if (packet instanceof PtpIpPacket.Error) {
            Exception e = ((PtpIpPacket.Error)packet).mException;
            if (e instanceof IOException) throw new IOError((IOException) e);
            else throw (MalformedPacket) e;
        }
    }

    public static void testType(PtpIpPacket packet, Class expectedClass) throws ProtocolViolation {
        if (!expectedClass.isAssignableFrom(packet.getClass()))
            throw new ProtocolViolation("Wrong PacketType: Expected " + expectedClass.getSimpleName() + ", received " + packet.getClass().getSimpleName());
    }

    // ---------------------------------------------------------------------------------------------
    // PtpIp Exception classes


    public static class IOError extends PtpTransport.TransportIOError {
        public IOError(IOException e) {super(e.toString(), e);}
        public IOError(String s, IOException e) {super(s, e);}
    }

    public static class OperationFailed extends PtpTransport.TransportOperationFailed {
        public OperationFailed(String s, long responseCode) {super("Responder failed on " + s + " (Response: " + String.format("0x%08x", responseCode) + ")!");}
    }

    public static class ProtocolViolation extends PtpTransport.TransportDataError {
        public ProtocolViolation(String s) {super(s);}
    }

    public static class MalformedPacket extends PtpTransport.TransportDataError {
        public MalformedPacket(String s) {super(s);}
    }
}
