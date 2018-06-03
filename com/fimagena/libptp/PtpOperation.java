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
import java.util.logging.Level;
import java.util.logging.Logger;


public class PtpOperation {

    private final static Logger LOG = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    static {LOG.setLevel(Level.ALL);}

    // ---------------------------------------------------------------------------------------------
    // Operation codes

    public final static Map<Integer, String> OPSCODE_DESCRIPTIONS = new HashMap<>(29);

    public final static int OPSCODE_Undefined               = 0x1000; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_Undefined             , "Undefined"           );}
    public final static int OPSCODE_GetDeviceInfo           = 0x1001; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_GetDeviceInfo         , "GetDeviceInfo"       );}
    public final static int OPSCODE_OpenSession             = 0x1002; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_OpenSession           , "OpenSession"         );}
    public final static int OPSCODE_CloseSession            = 0x1003; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_CloseSession          , "CloseSession"        );}
    public final static int OPSCODE_GetStorageIDs           = 0x1004; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_GetStorageIDs         , "GetStorageIDs"       );}
    public final static int OPSCODE_GetStorageInfo          = 0x1005; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_GetStorageInfo        , "GetStorageInfo"      );}
    public final static int OPSCODE_GetNumObjects           = 0x1006; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_GetNumObjects         , "GetNumObjects"       );}
    public final static int OPSCODE_GetObjectHandles        = 0x1007; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_GetObjectHandles      , "GetObjectHandles"    );}
    public final static int OPSCODE_GetObjectInfo           = 0x1008; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_GetObjectInfo         , "GetObjectInfo"       );}
    public final static int OPSCODE_GetObject               = 0x1009; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_GetObject             , "GetObject"           );}
    public final static int OPSCODE_GetThumb                = 0x100a; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_GetThumb              , "GetThumb"            );}
    public final static int OPSCODE_DeleteObject            = 0x100b; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_DeleteObject          , "DeleteObject"        );}
    public final static int OPSCODE_SendObjectInfo          = 0x100c; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_SendObjectInfo        , "SendObjectInfo"      );}
    public final static int OPSCODE_SendObject              = 0x100d; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_SendObject            , "SendObject"          );}
    public final static int OPSCODE_InitiateCapture         = 0x100e; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_InitiateCapture       , "InitiateCapture"     );}
    public final static int OPSCODE_FormatStore             = 0x100f; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_FormatStore           , "FormatStore"         );}
    public final static int OPSCODE_ResetDevice             = 0x1010; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_ResetDevice           , "ResetDevice"         );}
    public final static int OPSCODE_SelfTest                = 0x1011; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_SelfTest              , "SelfTest"            );}
    public final static int OPSCODE_SetObjectProtection     = 0x1012; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_SetObjectProtection   , "SetObjectProtection" );}
    public final static int OPSCODE_PowerDown               = 0x1013; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_PowerDown             , "PowerDown"           );}
    public final static int OPSCODE_GetDevicePropDesc       = 0x1014; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_GetDevicePropDesc     , "GetDevicePropDesc"   );}
    public final static int OPSCODE_GetDevicePropValue      = 0x1015; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_GetDevicePropValue    , "GetDevicePropValue"  );}
    public final static int OPSCODE_SetDevicePropValue      = 0x1016; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_SetDevicePropValue    , "SetDevicePropValue"  );}
    public final static int OPSCODE_ResetDevicePropValue    = 0x1017; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_ResetDevicePropValue  , "ResetDevicePropValue");}
    public final static int OPSCODE_TerminateOpenCapture    = 0x1018; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_TerminateOpenCapture  , "TerminateOpenCapture");}
    public final static int OPSCODE_MoveObject              = 0x1019; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_MoveObject            , "MoveObject"          );}
    public final static int OPSCODE_CopyObject              = 0x101a; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_CopyObject            , "CopyObject"          );}
    public final static int OPSCODE_GetPartialObject        = 0x101b; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_GetPartialObject      , "GetPartialObject"    );}
    public final static int OPSCODE_InitiateOpenCapture     = 0x101c; static {OPSCODE_DESCRIPTIONS.put(OPSCODE_InitiateOpenCapture   , "InitiateOpenCapture" );}

    public final static Map<Integer, String> RSPCODE_DESCRIPTIONS = new HashMap<>(33);
    
    public static final int RSPCODE_Undefined               = 0x2000; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_Undefined                 , "Undefined"               );}
    public static final int RSPCODE_OK                      = 0x2001; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_OK                        , "OK"                      );}
    public static final int RSPCODE_GeneralError            = 0x2002; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_GeneralError              , "GeneralError"            );}
    public static final int RSPCODE_SessionNotOpen          = 0x2003; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_SessionNotOpen            , "SessionNotOpen"          );}
    public static final int RSPCODE_InvalidTransactionID    = 0x2004; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_InvalidTransactionID      , "InvalidTransactionID"    );}
    public static final int RSPCODE_OperationNotSupported   = 0x2005; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_OperationNotSupported     , "OperationNotSupported"   );}
    public static final int RSPCODE_ParameterNotSupported   = 0x2006; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_ParameterNotSupported     , "ParameterNotSupported"   );}
    public static final int RSPCODE_IncompleteTransfer      = 0x2007; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_IncompleteTransfer        , "IncompleteTransfer"      );}
    public static final int RSPCODE_InvalidStorageID        = 0x2008; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_InvalidStorageID          , "InvalidStorageID"        );}
    public static final int RSPCODE_InvalidObjectHandle     = 0x2009; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_InvalidObjectHandle       , "InvalidObjectHandle"     );}
    public static final int RSPCODE_DevicePropNotSupported  = 0x200A; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_DevicePropNotSupported    , "DevicePropNotSupported"  );}
    public static final int RSPCODE_InvalidObjectFormatCode = 0x200B; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_InvalidObjectFormatCode   , "InvalidObjectFormatCode" );}
    public static final int RSPCODE_StoreFull               = 0x200C; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_StoreFull                 , "StoreFull"               );}
    public static final int RSPCODE_ObjectWriteProtected    = 0x200D; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_ObjectWriteProtected      , "ObjectWriteProtected"    );}
    public static final int RSPCODE_StoreReadOnly           = 0x200E; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_StoreReadOnly             , "StoreReadOnly"           );}
    public static final int RSPCODE_AccessDenied            = 0x200F; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_AccessDenied              , "AccessDenied"            );}
    public static final int RSPCODE_NoThumbnailPresent      = 0x2010; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_NoThumbnailPresent        , "NoThumbNailPresent"      );}
    public static final int RSPCODE_SelfTestFailed          = 0x2011; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_SelfTestFailed            , "SelfTestFailed"          );}
    public static final int RSPCODE_PartialDeletion         = 0x2012; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_PartialDeletion           , "PartialDeletion"         );}
    public static final int RSPCODE_StoreNotAvailable       = 0x2013; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_StoreNotAvailable         , "StoreNotAvailable"       );}
    public static final int RSPCODE_SpecificationByFormatUnsupported = 0x2014; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_SpecificationByFormatUnsupported, "SpecificationByFormatUnsupported");}
    public static final int RSPCODE_NoValidObjectInfo       = 0x2015; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_NoValidObjectInfo         , "NoValidObjectInfo"       );}
    public static final int RSPCODE_InvalidCodeFormat       = 0x2016; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_InvalidCodeFormat         , "InvalidCodeFormat"       );}
    public static final int RSPCODE_UnknownVendorCode       = 0x2017; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_UnknownVendorCode         , "UnknownVendorCode"       );}
    public static final int RSPCODE_CaptureAlreadyTerminated= 0x2018; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_CaptureAlreadyTerminated, "CaptureAlreadyTerminated");}
    public static final int RSPCODE_DeviceBusy              = 0x2019; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_DeviceBusy                , "DeviceBusy"              );}
    public static final int RSPCODE_InvalidParentObject     = 0x201A; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_InvalidParentObject       , "InvalidParentObject"     );}
    public static final int RSPCODE_InvalidDevicePropFormat = 0x201B; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_InvalidDevicePropFormat   , "InvalidDevicePropFormat" );}
    public static final int RSPCODE_InvalidDevicePropValue  = 0x201C; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_InvalidDevicePropValue    , "InvalidDevicePropValue"  );}
    public static final int RSPCODE_InvalidParameter        = 0x201D; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_InvalidParameter          , "InvalidParameters"       );}
    public static final int RSPCODE_SessionAlreadyOpen      = 0x201E; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_SessionAlreadyOpen        , "SessionAlreadyOpen"      );}
    public static final int RSPCODE_TransactionCancelled    = 0x201F; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_TransactionCancelled      , "TransactionCancelled"    );}
    public static final int RSPCODE_SpecificationOfDestinationUnsupported = 0x2020; static {RSPCODE_DESCRIPTIONS.put(RSPCODE_SpecificationOfDestinationUnsupported, "");}

    // ---------------------------------------------------------------------------------------------
    // Operation type definitions

    private static final PtpOperation[] PTP_OPERATIONS = {
            new PtpOperation(OPSCODE_GetDeviceInfo   , 0, 0, 0, DataFlow.DATA_IN, PtpDataType.DeviceInfoDataSet.class , new int[] {RSPCODE_OK, RSPCODE_ParameterNotSupported}),
            new PtpOperation(OPSCODE_OpenSession     , 1, 1, 0                                                        , new int[] {RSPCODE_OK, RSPCODE_ParameterNotSupported, RSPCODE_InvalidParameter, RSPCODE_SessionAlreadyOpen, RSPCODE_DeviceBusy}),
            new PtpOperation(OPSCODE_CloseSession    , 0, 0, 0                                                        , new int[] {RSPCODE_OK, RSPCODE_SessionNotOpen, RSPCODE_InvalidTransactionID, RSPCODE_ParameterNotSupported}),
            new PtpOperation(OPSCODE_GetStorageIDs   , 0, 0, 0, DataFlow.DATA_IN, PtpDataType.StorageIdArray.class    , new int[] {RSPCODE_OK, RSPCODE_OperationNotSupported, RSPCODE_SessionNotOpen, RSPCODE_InvalidTransactionID, RSPCODE_ParameterNotSupported}),
            new PtpOperation(OPSCODE_GetStorageInfo  , 1, 1, 0, DataFlow.DATA_IN, PtpDataType.StorageInfoDataSet.class, new int[] {RSPCODE_OK, RSPCODE_SessionNotOpen, RSPCODE_InvalidTransactionID, RSPCODE_AccessDenied, RSPCODE_InvalidStorageID, RSPCODE_StoreNotAvailable, RSPCODE_ParameterNotSupported}),
            new PtpOperation(OPSCODE_GetNumObjects   , 1, 3, 1                                                        , new int[] {RSPCODE_OK, RSPCODE_OperationNotSupported, RSPCODE_SessionNotOpen, RSPCODE_InvalidTransactionID, RSPCODE_InvalidStorageID, RSPCODE_StoreNotAvailable, RSPCODE_SpecificationByFormatUnsupported, RSPCODE_InvalidCodeFormat, RSPCODE_ParameterNotSupported, RSPCODE_InvalidParentObject, RSPCODE_InvalidObjectHandle, RSPCODE_InvalidParameter}),
            new PtpOperation(OPSCODE_GetObjectHandles, 1, 3, 0, DataFlow.DATA_IN, PtpDataType.ObjectHandleArray.class , new int[] {RSPCODE_OK, RSPCODE_OperationNotSupported, RSPCODE_SessionNotOpen, RSPCODE_InvalidTransactionID, RSPCODE_InvalidStorageID, RSPCODE_StoreNotAvailable, RSPCODE_InvalidObjectFormatCode, RSPCODE_SpecificationByFormatUnsupported, RSPCODE_InvalidCodeFormat, RSPCODE_InvalidObjectHandle, RSPCODE_InvalidParameter, RSPCODE_ParameterNotSupported, RSPCODE_InvalidParentObject, RSPCODE_InvalidObjectHandle}),
            new PtpOperation(OPSCODE_GetObjectInfo   , 1, 1, 0, DataFlow.DATA_IN, PtpDataType.ObjectInfoDataSet.class , new int[] {RSPCODE_OK, RSPCODE_OperationNotSupported, RSPCODE_SessionNotOpen, RSPCODE_InvalidTransactionID, RSPCODE_InvalidObjectHandle, RSPCODE_StoreNotAvailable, RSPCODE_ParameterNotSupported}),
            new PtpOperation(OPSCODE_GetObject       , 1, 1, 0, DataFlow.DATA_IN, PtpDataType.Object.class            , new int[] {RSPCODE_OK, RSPCODE_OperationNotSupported, RSPCODE_SessionNotOpen, RSPCODE_InvalidTransactionID, RSPCODE_InvalidObjectHandle, RSPCODE_InvalidParameter, RSPCODE_StoreNotAvailable, RSPCODE_ParameterNotSupported, RSPCODE_IncompleteTransfer}),
            new PtpOperation(OPSCODE_GetThumb        , 1, 1, 0, DataFlow.DATA_IN, PtpDataType.Object.class            , new int[] {RSPCODE_OK, RSPCODE_OperationNotSupported, RSPCODE_SessionNotOpen, RSPCODE_InvalidTransactionID, RSPCODE_InvalidObjectHandle, RSPCODE_NoThumbnailPresent, RSPCODE_InvalidObjectFormatCode, RSPCODE_StoreNotAvailable, RSPCODE_ParameterNotSupported}),
            new PtpOperation(OPSCODE_InitiateCapture , 2, 2, 0                                                        , new int[] {RSPCODE_OK, RSPCODE_OperationNotSupported, RSPCODE_SessionNotOpen, RSPCODE_InvalidTransactionID, RSPCODE_InvalidStorageID, RSPCODE_StoreFull, RSPCODE_InvalidObjectFormatCode, RSPCODE_InvalidParameter, RSPCODE_StoreNotAvailable, RSPCODE_InvalidCodeFormat, RSPCODE_DeviceBusy, RSPCODE_ParameterNotSupported})
    };

    // ---------------------------------------------------------------------------------------------
    // Operation definition

    protected enum DataFlow {NONE, DATA_OUT, DATA_IN}

    protected PtpDataType.OperationCode mOperationCode;
    protected int mMinNumberRequestParameters;
    protected int mMaxNumberRequestParameters;
    protected int mNumberResponseParameters;
    protected DataFlow mDataFlow;
    protected Class mDataType;
    protected int[] mAllowedRspCodes;

    public class Request {
        protected long[] mParameters = new long[0];
        protected PtpDataType mData;

        public int getOperationCode() {return mOperationCode.mValue;}
        public long[] getParameters() {return mParameters;}
        public void setParameters(long[] parameters) {mParameters = parameters;}
        public boolean hasData() {return mDataFlow == DataFlow.DATA_OUT;}
        public PtpDataType getData() {return mData;}

        public void validate() throws PtpExceptions.PtpProtocolViolation {
            if ((mParameters == null) || (mParameters.length > mMaxNumberRequestParameters) || (mParameters.length < mMinNumberRequestParameters))
                throw new PtpExceptions.PtpProtocolViolation("Invalid number of request parameters given!");
            if ((mDataFlow != DataFlow.DATA_OUT) && (mData != null))
                throw new PtpExceptions.PtpProtocolViolation("Request has data but doesn't need any!");
            if ((mDataFlow == DataFlow.DATA_OUT) && (mData == null))
                throw new PtpExceptions.PtpProtocolViolation("Request requires data but doesn't has any!");
            if ((mDataFlow == DataFlow.DATA_OUT) && !mData.getClass().equals(mDataType))
                throw new PtpExceptions.PtpProtocolViolation("Request data is of wrong class type!");
        }

        public String toString() {return "[OpsReq][OpsCode: " + mOperationCode + ", Parameters: " + arrayToString(mParameters) + ", Data: " + mData + "]";}
    }

    public class Response {
        protected PtpDataType.ResponseCode mRspCode = new PtpDataType.ResponseCode(RSPCODE_Undefined);
        protected long[] mParameters = new long[0];
        protected PtpTransport.PayloadBuffer mDataBuffer;
        private PtpDataType mData;

        public boolean isSuccess() {return mRspCode.mValue == RSPCODE_OK;}
        public int getResponseCode() {return mRspCode.mValue;}
        public void setResponseCode(int responseCode) {mRspCode = new PtpDataType.ResponseCode(responseCode);}
        public long[] getParameters() {return mParameters;}
        public void setParameters(long[] parameters) {mParameters = parameters;}
        public PtpDataType getData() {return mData;}
        public void setData(PtpTransport.PayloadBuffer buffer) {mDataBuffer = buffer;}

        public void validate() throws PtpExceptions.PtpProtocolViolation {
            if (!intArrayContains(mAllowedRspCodes, mRspCode.mValue))
                throw new PtpExceptions.PtpProtocolViolation("Invalid response code (OpsCode: " + mOperationCode + ", Rspcode: " + mRspCode + ")");
            if ((mParameters == null) || (mParameters.length != mNumberResponseParameters))
                throw new PtpExceptions.PtpProtocolViolation("Invalid number of response parameters received!");
            if ((mDataFlow != DataFlow.DATA_IN) && (mDataBuffer != null))
                throw new PtpExceptions.PtpProtocolViolation("Received data, didn't expect any!");
            if (mDataFlow == DataFlow.DATA_IN) {
                if (mDataBuffer == null) throw new PtpExceptions.PtpProtocolViolation("Expected data but didn't receive any!");
                try {mData = (PtpDataType) mDataType.newInstance();}
                catch (Exception e) {throw new PtpExceptions.PtpProtocolViolation("Error instantiating result data class!", e);}
                try {mData.read(mDataBuffer); mDataBuffer = null;}
                catch (Exception e) {throw new PtpExceptions.PtpProtocolViolation("Error parsing response data!", e);}
            }
            if (mData != null) LOG.info("PTP: Response data received: " + mData);
        }

        public String toString() {return "[OpsRsp][RpsCode: " + mRspCode + ", Parameters: " + arrayToString(mParameters) + ", Data: " + mData + "]";}
    }

    // ---------------------------------------------------------------------------------------------
    // Constructors

    private PtpOperation(int operationCode, int minNumberRequestParameters, int maxNumberRequestParameters, int numberResponseParameters, int[] allowedRspCodes) {
        this(operationCode, minNumberRequestParameters, maxNumberRequestParameters, numberResponseParameters, DataFlow.NONE, null, allowedRspCodes);
    }
    private PtpOperation(int operationCode, int minNumberRequestParameters, int maxNumberRequestParameters, int numberResponseParameters, DataFlow dataFlow, Class<? extends PtpDataType> dataType, int[] allowedRspCodes) {
        mOperationCode = new PtpDataType.OperationCode(operationCode);
        mMinNumberRequestParameters = minNumberRequestParameters;
        mMinNumberRequestParameters = maxNumberRequestParameters;
        mDataType = dataType;
        mDataFlow = dataFlow;
        mAllowedRspCodes = allowedRspCodes;
        mNumberResponseParameters = numberResponseParameters;
    }

    // ---------------------------------------------------------------------------------------------
    // Request/Response accessors

    private Request createRequest() {return new Request();}
    public static Request createRequest(int operationCode) {
        PtpOperation operation = getOperation(operationCode);
        return operation == null ? null : operation.createRequest();
    }

    private Response createRespone() {return new Response();}
    public static Response createResponse(Request request) {
        PtpOperation operation = getOperation(request.getOperationCode());
        return operation == null ? null : operation.createRespone();
    }

    // ---------------------------------------------------------------------------------------------
    // Helper functions

    private boolean intArrayContains(int[] intArray, int element) {
        for (int i : intArray) if (i == element) return true;
        return false;
    }

    private static PtpOperation getOperation(int operationCode) {
        PtpOperation operation = null;
        for (PtpOperation o : PTP_OPERATIONS)
            if (o.mOperationCode.mValue == operationCode) {operation = o; break;}
        if (operation == null) return null;
        return operation;
    }

    private static String arrayToString(long[] params) {
        String output = "{ ";
        for (long l : params) output += String.format("0x%08x, ", l);
        output += "}";
        return output;
    }
}
