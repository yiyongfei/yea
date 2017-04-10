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
package com.yea.core.exception.util;

import java.net.InetAddress;

import com.yea.core.exception.YeaError;
import com.yea.core.exception.YeaException;
import com.yea.core.exception.YeaExceptionStatus;
import com.yea.core.exception.constants.YeaErrorMessageValue;
import com.yea.core.exception.constants.YeaErrorTypeValue;

/**
 * 
 * @author yiyongfei
 * 
 */
public class ExceptionUtils {
    /**
     * 构造函数
     */
    public ExceptionUtils() {
        super();
    }

    /**
     * cache host
     */
    private static String _sHost;
    static {
        try {
            _sHost = InetAddress.getLocalHost().getHostAddress();
        } catch (java.net.UnknownHostException uhe) {
            _sHost = "UnknownHost";
        }
    }

    /**
     * 创建YeaError
     * @param inUser String
     * @param inClassName String
     * @param inMethodName String
     * @param inComponentType long
     * @param inErrorType long
     * @param inErrMsgType long
     * @param inErrorParams String[]
     * @param inErrorDetail String
     * @param inTh Throwable
     * @return PAFAError
     */
    public static YeaError createYFAError(String inUser, String inClassName, String inMethodName, long inComponentType, long inErrorType, long inErrMsgType, String[] inErrorParams, String inErrorDetail, Throwable inTh) {
        String errorId = generateErrorId();

        // create error
        YeaError error = new YeaError();

        String sIP = null;

        sIP = _sHost;

        error.setIP(sIP);
        error.setUser(inUser);
        error.setClassName(inClassName);
        error.setMethodName(inMethodName);
        error.setErrorId(errorId);
        error.setComponentType(inComponentType);
        error.setErrorType(inErrorType);
        error.setErrorTypeValue(YeaErrorTypeValue.getErrorTypeValue(inErrorType));
        error.setErrorMessageType(inErrMsgType);
        error.setErrorMessage(YeaErrorMessageValue.getErrorMsg(inErrMsgType));
        if (inErrorParams == null) {
            inErrorParams = new String[0];
        }
        error.setParameters(inErrorParams);
        error.setErrorDetail(inErrorDetail);
        error.setThrowable(inTh);
        return error;
    }

    /**
     * 处理pafa异常
     * @param inUser String
     * @param inClassName String
     * @param inMethodName String
     * @param inComponentType long
     * @param inErrorType long
     * @param inErrMsgType long
     * @param inErrorParams String[]
     * @param inErrorDetail String
     * @param inPE PafaException
     * @return PAFAExceptionStatus
     */
    public static YeaExceptionStatus handleBoundaryYFAException(String inUser, String inClassName, String inMethodName, long inComponentType, long inErrorType, long inErrMsgType, String[] inErrorParams, String inErrorDetail, YeaException inPE) {

        // get exception status
        YeaExceptionStatus status = inPE.getPAFAExceptionStatus();

        YeaError pe = createYFAError(inUser, inClassName, inMethodName, inComponentType, inErrorType, inErrMsgType, inErrorParams, inErrorDetail, inPE);
        status.addError(pe);
        return status;
    }

    /**创建一个错误的编号
     * 由当前的毫秒数+1000内的随机数构成
     * @return String
     */
    public static String generateErrorId() {
        StringBuffer sb = new StringBuffer();

        Long id = new Long(System.currentTimeMillis());
        sb.append(id.longValue());

        // random id
        Double randomId = new Double(Math.floor(Math.random() * 1000));
        sb.append(randomId.intValue());

        return sb.toString();
    }
}
