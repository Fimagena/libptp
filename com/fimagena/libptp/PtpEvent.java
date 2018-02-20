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

import java.util.HashMap;
import java.util.Map;


public class PtpEvent {

    public static class Error extends PtpEvent {
        protected Exception mException;

        public Error() {this(null);}
        public Error(Exception e) {
            super(new PtpDataType.EventCode(EVENTCODE_Internal));
            mException = e;
        }
    }

    public final static Map<Integer, String> EVENTCODE_DESCRIPTIONS = new HashMap<>(33);

    public static final int EVENTCODE_Undefined             = 0x4000; static {PtpEvent.EVENTCODE_DESCRIPTIONS.put(PtpEvent.EVENTCODE_Undefined             , "Undefined"            );}
    public static final int EVENTCODE_CancelTransaction     = 0x4001; static {PtpEvent.EVENTCODE_DESCRIPTIONS.put(PtpEvent.EVENTCODE_CancelTransaction     , "CancelTransaction"    );}
    public static final int EVENTCODE_ObjectAdded           = 0x4002; static {PtpEvent.EVENTCODE_DESCRIPTIONS.put(PtpEvent.EVENTCODE_ObjectAdded           , "ObjectAdded"          );}
    public static final int EVENTCODE_ObjectRemoved         = 0x4003; static {PtpEvent.EVENTCODE_DESCRIPTIONS.put(PtpEvent.EVENTCODE_ObjectRemoved         , "ObjectRemoved"        );}
    public static final int EVENTCODE_StoreAdded            = 0x4004; static {PtpEvent.EVENTCODE_DESCRIPTIONS.put(PtpEvent.EVENTCODE_StoreAdded            , "StoreAdded"           );}
    public static final int EVENTCODE_StoreRemoved          = 0x4005; static {PtpEvent.EVENTCODE_DESCRIPTIONS.put(PtpEvent.EVENTCODE_StoreRemoved          , "StoreRemove"          );}
    public static final int EVENTCODE_DevicePropChanged     = 0x4006; static {PtpEvent.EVENTCODE_DESCRIPTIONS.put(PtpEvent.EVENTCODE_DevicePropChanged     , "DevicePropChanged"    );}
    public static final int EVENTCODE_ObjectInfoChanged     = 0x4007; static {PtpEvent.EVENTCODE_DESCRIPTIONS.put(PtpEvent.EVENTCODE_ObjectInfoChanged     , "ObjectInfoChanged"    );}
    public static final int EVENTCODE_DeviceInfoChanged     = 0x4008; static {PtpEvent.EVENTCODE_DESCRIPTIONS.put(PtpEvent.EVENTCODE_DeviceInfoChanged     , "DeviceInfoChanged"    );}
    public static final int EVENTCODE_RequestObjectTransfer = 0x4009; static {PtpEvent.EVENTCODE_DESCRIPTIONS.put(PtpEvent.EVENTCODE_RequestObjectTransfer , "RequestObjectTransfer");}
    public static final int EVENTCODE_StoreFull             = 0x400a; static {PtpEvent.EVENTCODE_DESCRIPTIONS.put(PtpEvent.EVENTCODE_StoreFull             , "StoreFull"            );}
    public static final int EVENTCODE_DeviceReset           = 0x400b; static {PtpEvent.EVENTCODE_DESCRIPTIONS.put(PtpEvent.EVENTCODE_DeviceReset           , "DeviceReset"          );}
    public static final int EVENTCODE_StorageInfoChanged    = 0x400c; static {PtpEvent.EVENTCODE_DESCRIPTIONS.put(PtpEvent.EVENTCODE_StorageInfoChanged    , "StorageInfoChanged"   );}
    public static final int EVENTCODE_CaptureCompleted      = 0x400d; static {PtpEvent.EVENTCODE_DESCRIPTIONS.put(PtpEvent.EVENTCODE_CaptureCompleted      , "CaptureCompleted"     );}
    public static final int EVENTCODE_UnreportedStatus      = 0x400e; static {PtpEvent.EVENTCODE_DESCRIPTIONS.put(PtpEvent.EVENTCODE_UnreportedStatus      , "UnreportedStatus"     );}

    public static final int EVENTCODE_Internal              = 0x40ee; static {PtpEvent.EVENTCODE_DESCRIPTIONS.put(PtpEvent.EVENTCODE_Internal              , "InternalEvent"        );}

    protected PtpDataType.EventCode mEventCode = new PtpDataType.EventCode(EVENTCODE_Undefined);
    protected PtpDataType.UInt32 mTransactionId = new PtpDataType.UInt32();
    protected long[] mParameters = new long[0];


    public PtpEvent(PtpDataType.EventCode eventCode) {this(eventCode, new PtpDataType.UInt32(0xFFFFFFFF), null);}
    public PtpEvent(PtpDataType.EventCode eventCode, PtpDataType.UInt32 transactionId) {this(eventCode, transactionId, null);}
    public PtpEvent(PtpDataType.EventCode eventCode, long[] params) {this(eventCode, new PtpDataType.UInt32(0xFFFFFFFF), params);}
    public PtpEvent(PtpDataType.EventCode eventCode, PtpDataType.UInt32 transactionId, long[] params) {
        mEventCode = eventCode;
        mTransactionId = transactionId;
        mParameters = params;
    }
}
