/**
 * Copyright 2017 伊永飞
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yea.core.remote.exception;

import com.yea.core.exception.YeaException;
import com.yea.core.exception.YeaExceptionStatus;
import com.yea.core.exception.constants.YeaErrorType;

/**
 * 
 * @author yiyongfei
 * 
 */
public class RemoteException extends YeaException {

    /**  */
    private static final long serialVersionUID = -9052492587652558470L;

    public RemoteException() {
        this(UNKNOWN_ERROR, null, null);
    }

    public RemoteException(int code) {
        this(code, null, null);
    }

    public RemoteException(int code, Throwable cause) {
        this(code, null, cause);
    }

    public RemoteException(Throwable cause) {
        this(UNKNOWN_ERROR, null, cause);
    }

    public RemoteException(int code, String message) {
        this(code, message, null);
    }

    public RemoteException(String message) {
        this(UNKNOWN_ERROR, message, null);
    }

    public RemoteException(int inCode, String inMsg, Throwable inCause) {
        this(0, inCode, inMsg, inCause);
    }

    public RemoteException(long inErrMsgType, int inCode, String inMsg, Throwable inCause) {
        this(YeaErrorType.SYSTEM_ERROR, inErrMsgType, inCode, inMsg, inCause);
    }
    
    public RemoteException(String message, Throwable cause) {
        this(UNKNOWN_ERROR, message, cause);
    }

    public RemoteException(long inErrorType, long inErrMsgType, int inCode, String inMsg, Throwable inCause) {
        this(inMsg, inErrorType, inErrMsgType, null, inCause, YeaExceptionStatus.WARNING);
        this.errorCode = inCode;
    }
    
    public RemoteException(String inMsg, long inErrorType, long inErrMsgType, String[] inErrorParams, Throwable inTh, long inStatus) {
        super(inMsg, inErrorType, inErrMsgType, inErrorParams, inTh, inStatus);
    }
}
