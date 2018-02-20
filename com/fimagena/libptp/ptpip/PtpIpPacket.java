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


public abstract class PtpIpPacket {

    protected static final int PKT_Invalid              = 0x00000000;
    protected static final int PKT_InitCommandRequest   = 0x00000001;
    protected static final int PKT_InitCommandAck       = 0x00000002;
    protected static final int PKT_InitEventRequest     = 0x00000003;
    protected static final int PKT_InitEventAck         = 0x00000004;
    protected static final int PKT_InitFail             = 0x00000005;
    protected static final int PKT_OperationRequest     = 0x00000006;
    protected static final int PKT_OperationResponse    = 0x00000007;
    protected static final int PKT_Event                = 0x00000008;
    protected static final int PKT_StartData            = 0x00000009;
    protected static final int PKT_Data                 = 0x0000000a;
    protected static final int PKT_Cancel               = 0x0000000b;
    protected static final int PKT_EndData              = 0x0000000c;
    protected static final int PKT_ProbeRequest         = 0x0000000d;
    protected static final int PKT_ProbeResponse        = 0x0000000e;

    private   static final int DATAPHASE_NOOUT          = 0x00000001;
    private   static final int DATAPHASE_OUT            = 0x00000002;
    private   static final int DATAPHASE_UNKNOWN        = 0x00000003; // guessed value - not yet observed (but we're not using it anyway)

    protected long mLength;                                             // uint32
    protected long mPacketType;                                         // uint32
    private TcpConnection mSourceConnection;

    protected void setSourceConnection(TcpConnection tcpConnection) {mSourceConnection = tcpConnection;}
    protected TcpConnection getSourceConnection() {return mSourceConnection;}

    protected void writePayload(DataBuffer out) {}
    protected void readPayload(DataBuffer in) throws PtpIpExceptions.MalformedPacket {}
    public String toString() {return "[Length: " + mLength + ", PacketType: " + String.format("0x%08x", mPacketType) + " (" + this.getClass().getName().substring(this.getClass().getName().lastIndexOf("$") + 1) + ")]";}

    public byte[] serializePacket() {
        DataBuffer packetBuffer = new DataBuffer();
        packetBuffer.writeUInt32(0).writeUInt32(mPacketType);
        writePayload(packetBuffer);

        byte[] packetData = packetBuffer.getData();
        System.arraycopy(new DataBuffer(4).writeUInt32(packetData.length).getData(), 0, packetData, 0, 4);

        return packetData;
    }


    interface ReadingListener {void onLoaded(PtpIpPacket packet, int loadedBytes);}
    public static PtpIpPacket readPacket(InputStream in) throws IOException, PtpIpExceptions.MalformedPacket {return readPacket(in, null);}
    public static PtpIpPacket readPacket(InputStream in, final ReadingListener listener) throws IOException, PtpIpExceptions.MalformedPacket {
        // -----------------------------------------------------------------------------------------
        // Read 8-byte PtpIp-header and instantiate correct packet

        DataBuffer ptpIpHeader = new DataBuffer(8);
        ptpIpHeader.fill(in, 8);

        long packetLength = ptpIpHeader.readUInt32();
        if (packetLength < 8) throw new PtpIpExceptions.MalformedPacket("PTP/IP PacketLength header < 8 bytes (" + packetLength + ")");

        long packetType = ptpIpHeader.readUInt32();
        final PtpIpPacket packet;
        switch ((int) packetType) {
            case PKT_InitCommandRequest:packet = new InitCommandRequest();  break;
            case PKT_InitCommandAck:    packet = new InitCommandAck();      break;
            case PKT_InitEventRequest:  packet = new InitEventRequest();    break;
            case PKT_InitEventAck:      packet = new InitEventAck();        break;
            case PKT_InitFail:          packet = new InitFail();            break;
            case PKT_OperationRequest:  packet = new OperationRequest();    break;
            case PKT_OperationResponse: packet = new OperationResponse();   break;
            case PKT_Event:             packet = new Event();               break;
            case PKT_StartData:         packet = new StartData();           break;
            case PKT_Data:              packet = new Data();                break;
            case PKT_Cancel:            packet = new Cancel();              break;
            case PKT_EndData:           packet = new EndData();             break;
            case PKT_ProbeRequest:      packet = new ProbeRequest();        break;
            case PKT_ProbeResponse:     packet = new ProbeResponse();       break;
            default: throw new PtpIpExceptions.MalformedPacket("Unknown packet type: " + String.format("0x%04x", packetType));
        }
        packet.mLength = packetLength;

        // -----------------------------------------------------------------------------------------
        // Read payload and create packet

        DataBuffer.LoadingListener loadListener = listener == null ? null : new DataBuffer.LoadingListener() {
            @Override public void onLoaded(int loadedBytes) {listener.onLoaded(packet, loadedBytes + 8);}
        };

        DataBuffer ptpIpPayload = new DataBuffer((int) packetLength - 8);
        ptpIpPayload.fill(in, (int) packetLength - 8, loadListener);
        packet.readPayload(ptpIpPayload);

        return packet;
    }

    protected PtpIpPacket(long packetType) {mPacketType = packetType;}


    // ---------------------------------------------------------------------------------------------
    // Helper functions

    private static String arrayToString(long[] params) {
        String output = "{ ";
        for (long l : params) output += String.format("0x%08x, ", l);
        output += "}";
        return output;
    }


    // ---------------------------------------------------------------------------------------------
    // Init packet classes


    protected abstract static class InitPacket extends PtpIpPacket {
        protected InitPacket(int packetType) {super(packetType);}
    }


    public static class InitCommandRequest extends InitPacket {
        protected short[] mGuid = new short[16];                        // uint8[16]
        protected String mFriendlyName;                                 // uint16[< 39 + 1]
        protected int mProtVersionMajor, mProtVersionMinor;             // uint32

        @Override protected void writePayload(DataBuffer out) {
            for (short s : mGuid) out.writeUInt8(s);
            out.writeUtf16String(mFriendlyName);
            out.writeUInt32(mProtVersionMinor + (mProtVersionMajor << 16));
        }
        @Override protected void readPayload(DataBuffer in) throws PtpIpExceptions.MalformedPacket {
            for (int i = 0; i < 16; i++) mGuid[i] = in.readUInt8();
            mFriendlyName = in.readUtf16String();
            mProtVersionMinor = in.readUInt16();
            mProtVersionMajor = in.readUInt16();
        }
        @Override public String toString() {
            String guidString = ""; for (int i = 0; i < 16; i++) guidString += Integer.toHexString(mGuid[i]) + ":";
            return super.toString() + ":[GUID: " + guidString + ", FriendlyName: " + mFriendlyName + ", Version: " + mProtVersionMajor + "." + mProtVersionMinor + "]";
        }

        public InitCommandRequest() {super(PKT_InitCommandRequest);}
        public InitCommandRequest(PtpIpConnection.PtpIpHostId hostId) {
            this();
            mFriendlyName = hostId.mFriendlyName;
            mProtVersionMajor = hostId.mVersionMajor;
            mProtVersionMinor = hostId.mVersionMinor;
            for (int i = 0; (i < mGuid.length) && (i < hostId.mGuid.length); i++) mGuid[i] = hostId.mGuid[i];
        }
    }


    public static class InitCommandAck extends InitPacket {
        protected long mConnectionNumber;                               // uint32
        protected InitCommandRequest mResponseData = new InitCommandRequest();

        @Override public void writePayload(DataBuffer out) {
            out.writeUInt32(mConnectionNumber);
            mResponseData.writePayload(out);
        }
        @Override protected void readPayload(DataBuffer in) throws PtpIpExceptions.MalformedPacket {
            mConnectionNumber = in.readUInt32();
            mResponseData.readPayload(in);
        }
        @Override public String toString() {return super.toString() + ":[ConnectionNumber: " + mConnectionNumber + "]:[" + mResponseData.toString() + "]";}

        public InitCommandAck() {super(PKT_InitCommandAck);}
        public InitCommandAck(long connectionNumber, PtpIpConnection.PtpIpHostId hostId) {
            this();
            mConnectionNumber = connectionNumber;
            mResponseData = new InitCommandRequest(hostId);
        }
    }


    public static class InitEventRequest extends InitPacket {
        protected long mConnectionNumber;                               // uint32

        @Override public void writePayload(DataBuffer out) {out.writeUInt32(mConnectionNumber);}
        @Override protected void readPayload(DataBuffer in) throws PtpIpExceptions.MalformedPacket {mConnectionNumber = in.readUInt32();}

        @Override public String toString() {return super.toString() + ":[ConnectionNumber: " + mConnectionNumber + "]";}

        public InitEventRequest() {super(PKT_InitEventRequest);}
        public InitEventRequest(long connectionNumber) {this(); mConnectionNumber = connectionNumber;}
    }


    public static class InitEventAck extends InitPacket {
        public InitEventAck() {super(PKT_InitEventAck);}
    }


    public static class InitFail extends InitPacket {
        protected long mReason;                                         // uint32

        @Override public void writePayload(DataBuffer out) {out.writeUInt32(mReason);}
        @Override protected void readPayload(DataBuffer in) throws PtpIpExceptions.MalformedPacket {mReason = in.readUInt32();}

        @Override public String toString() {return super.toString() + ":[Reason: " + String.format("0x%08x", mReason) + "]";}

        public InitFail() {super(PKT_InitFail);}
        public InitFail(long reason) {this(); mReason = reason;}
    }


    // ---------------------------------------------------------------------------------------------
    // Transaction packet classes


    protected static abstract class TransactionPacket extends PtpIpPacket {
        protected long mTransactionId;                                  // uint32

        @Override protected void writePayload(DataBuffer out) {out.writeUInt32(mTransactionId);}
        @Override protected void readPayload(DataBuffer in) throws PtpIpExceptions.MalformedPacket {mTransactionId = in.readUInt32();}
        @Override public String toString() {return super.toString() + ":[TransactionId: " + mTransactionId + "]";}

        protected TransactionPacket(long packetType) {super(packetType);}
    }


    public static class OperationRequest extends TransactionPacket {
        protected long mDataPhaseInfo;                                  // uint32
        protected int mOperationCode;                                   // uint16
        protected long[] mParameters = new long[0];                     // uint32[<=5]

        @Override protected void writePayload(DataBuffer out) {
            out.writeUInt32(mDataPhaseInfo);
            out.writeUInt16(mOperationCode);
            super.writePayload(out);
            for (long l : mParameters) out.writeUInt32(l);
        }
        @Override protected void readPayload(DataBuffer in) throws PtpIpExceptions.MalformedPacket {
            mDataPhaseInfo = in.readUInt32();
            mOperationCode = in.readUInt16();
            super.readPayload(in);
            if ((in.available() > 20) || ((in.available() % 4) != 0)) throw new PtpIpExceptions.MalformedPacket("Malformed OperationsRequest parameters! (" + in.available() + "bytes)");
            mParameters = new long[in.available() / 4];
            for (int i = 0; i < mParameters.length; i++) mParameters[i] = in.readUInt32();
        }
        @Override public String toString() {return super.toString() + ":[DataPhaseInfo: " + String.format("0x%08x", mDataPhaseInfo) + ", OperationCode: " + String.format("0x%04x", mOperationCode) + ", Params: " + arrayToString(mParameters) + "]";}

        public OperationRequest() {super(PKT_OperationRequest);}
        public OperationRequest(boolean dataOut, int operationCode, long transactionId, long[] parameters) {
            this();
            mDataPhaseInfo = dataOut ? DATAPHASE_OUT : DATAPHASE_NOOUT;
            mOperationCode = operationCode;
            mTransactionId = transactionId;
            mParameters = parameters;
        }
    }


    public static class OperationResponse extends TransactionPacket {
        protected int mResponseCode;                                    // uint16
        protected long[] mParameters = new long[0];                     // uint32[<=5]

        @Override protected void writePayload(DataBuffer out) {
            out.writeUInt16(mResponseCode);
            super.writePayload(out);
            for (long l : mParameters) out.writeUInt32(l);
        }
        @Override protected void readPayload(DataBuffer in) throws PtpIpExceptions.MalformedPacket {
            mResponseCode = in.readUInt16();
            super.readPayload(in);
            if ((in.available() > 20) || ((in.available() % 4) != 0)) throw new PtpIpExceptions.MalformedPacket("Malformed OperationsResponse parameters! (" + in.available() + "bytes)");
            mParameters = new long[in.available() / 4];
            for (int i = 0; i < mParameters.length; i++) mParameters[i] = in.readUInt32();
        }
        @Override public String toString() {return super.toString() + ":[ResponseCode: " + String.format("0x%04x",mResponseCode) + ", Params: " + arrayToString(mParameters) + "]";}

        public OperationResponse() {super(PKT_OperationResponse);}
        public OperationResponse(int responseCode, long transactionId, long[] parameters) {
            this();
            mResponseCode = responseCode;
            mTransactionId = transactionId;
            mParameters = parameters;
        }
    }


    public static class Event extends TransactionPacket {
        protected int mEventCode;                                       // uint16
        protected long[] mParameters = new long[0];                     // uint32[<=3]

        @Override protected void writePayload(DataBuffer out) {
            out.writeUInt16(mEventCode);
            super.writePayload(out);
            for (long l : mParameters) out.writeUInt32(l);
        }
        @Override protected void readPayload(DataBuffer in) throws PtpIpExceptions.MalformedPacket {
            mEventCode = in.readUInt16();
            super.readPayload(in);
            if ((in.available() > 12) || ((in.available() % 4) != 0)) throw new PtpIpExceptions.MalformedPacket("Malformed Event parameters! (" + in.available() + "bytes)");
            mParameters = new long[in.available() / 4];
            for (int i = 0; i < mParameters.length; i++) mParameters[i] = in.readUInt32();
        }
        @Override public String toString() {return super.toString() + ":[EventCode: " + String.format("0x%04x", mEventCode) + ", Params: " + arrayToString(mParameters) + "]";}

        public Event() {super(PKT_Event);}
        public Event(int eventCode, long transactionId, long[] parameters) {
            this();
            mEventCode = eventCode;
            mTransactionId = transactionId;
            for (int i = 0; (i < mParameters.length) && (i < parameters.length); i++) mParameters[i] = parameters[i];
        }
    }


    public static class StartData extends TransactionPacket {
        protected long mDataLength;                                     // uint64 (!! long not fully correct)

        @Override protected void writePayload(DataBuffer out) {
            super.writePayload(out);
            out.writeUInt64(mDataLength);
        }
        @Override protected void readPayload(DataBuffer in) throws PtpIpExceptions.MalformedPacket {
            super.readPayload(in);
            mDataLength = in.readUInt64();                              //FIXME: should be one 64bit (FFF... = unknown);
        }
        @Override public String toString() {return super.toString() + ":[DataLength: " + mDataLength + "]";}

        public StartData() {super(PKT_StartData);}
        public StartData(long transactionId, long dataLength) {
            this();
            mTransactionId = transactionId;
            mDataLength = dataLength;
        }
    }


    public static class Data extends TransactionPacket {
        protected byte[] mDataPayload;                                  // uint8[?]

        @Override public void writePayload(DataBuffer out) {
            super.writePayload(out);
            out.writeObject(mDataPayload);
        }
        @Override protected void readPayload(DataBuffer in) throws PtpIpExceptions.MalformedPacket {
            super.readPayload(in);
            mDataPayload = in.readObject();
        }
        @Override public String toString() {return super.toString() + ":[PayloadLength: " + mDataPayload.length + "]";}

        public Data() {super(PKT_Data);}
        public Data(long transactionId, DataBuffer dataPayload) {
            this();
            mTransactionId = transactionId;
            mDataPayload = dataPayload.getData();
        }
    }


    public static class EndData extends TransactionPacket {
        protected byte[] mDataPayload;                                  // uint8[?]

        @Override public void writePayload(DataBuffer out) {
            super.writePayload(out);
            out.writeObject(mDataPayload);
        }
        @Override protected void readPayload(DataBuffer in) throws PtpIpExceptions.MalformedPacket {
            super.readPayload(in);
            mDataPayload = in.readObject();
        }
        @Override public String toString() {return super.toString() + ":[PayloadLength: " + mDataPayload.length + "]";}

        public EndData() {super(PKT_EndData);}
        public EndData(long transactionId, DataBuffer dataPayload) {
            this();
            mTransactionId = transactionId;
            mDataPayload = dataPayload.getData();
        }
    }


    public static class Cancel extends TransactionPacket {
        public Cancel() {super(PKT_Cancel);}
        public Cancel(long transactionId) {this(); mTransactionId = transactionId;}
    }


    // ---------------------------------------------------------------------------------------------
    // Probe packet classes


    public static class ProbeRequest extends PtpIpPacket {
        public ProbeRequest() {super(PKT_ProbeRequest);}
    }


    public static class ProbeResponse extends PtpIpPacket {
        public ProbeResponse() {super(PKT_ProbeResponse);}
    }


    // ---------------------------------------------------------------------------------------------
    // Internal packet classes


    public static class InternalPacket extends PtpIpPacket {
        public InternalPacket(long packetType) {super(packetType);}
    }

    public static class Error extends InternalPacket {
        public Exception mException;

        @Override public String toString() {return super.toString() + ":[Exception: " + mException + "]";}

        public Error() {super(PKT_Invalid);}
        public Error(Exception e) {this(); mException = e;}
    }

    public static class LoadStatus extends InternalPacket {
        public PtpIpPacket mLoadedPacket;
        public int mLoadedBytes;

        public LoadStatus() {super(PKT_Invalid);}
        public LoadStatus(PtpIpPacket packet, int loadedBytes) {this(); mLoadedPacket = packet; mLoadedBytes = loadedBytes;}
    }
}
