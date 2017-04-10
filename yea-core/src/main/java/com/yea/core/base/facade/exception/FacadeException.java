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
package com.yea.core.base.facade.exception;

import com.yea.core.exception.YeaException;
import com.yea.core.exception.YeaExceptionStatus;
import com.yea.core.exception.constants.YeaErrorMessage;
import com.yea.core.exception.constants.YeaErrorType;

/**
 * 
 * @author yiyongfei
 * 
 */
public class FacadeException extends YeaException {

    /**  */
    private static final long serialVersionUID = -9052492587652558470L;

    public FacadeException() {
        this(UNKNOWN_ERROR, null, null);
    }

    public FacadeException(int code) {
        this(code, null, null);
    }

    public FacadeException(int code, Throwable cause) {
        this(code, null, cause);
    }

    public FacadeException(Throwable cause) {
        this(UNKNOWN_ERROR, null, cause);
    }

    public FacadeException(int code, String message) {
        this(code, message, null);
    }

    public FacadeException(String message) {
        this(UNKNOWN_ERROR, message, null);
    }
    
    public FacadeException(String message, Throwable cause) {
        this(UNKNOWN_ERROR, message, cause);
    }
    
    public FacadeException(int inCode, String inMsg, Throwable inCause) {
        super(inMsg, YeaErrorType.SYSTEM_ERROR, YeaErrorMessage.ERR_BUSINESS, null, inCause, YeaExceptionStatus.WARNING);
        this.setStackTrace(inCause.getStackTrace());
        this.errorCode = inCode;
    }
    
    public FacadeException(int inCode, long inErrorType, long inErrMsgType, String inMsg, Throwable inCause) {
        super(inMsg, inErrorType, inErrMsgType, null, inCause, YeaExceptionStatus.WARNING);
        this.setStackTrace(inCause.getStackTrace());
        this.errorCode = inCode;
    }
}
