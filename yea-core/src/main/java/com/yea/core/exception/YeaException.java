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
package com.yea.core.exception;

import java.io.PrintStream;
import java.io.PrintWriter;

import com.yea.core.exception.util.ExceptionUtils;

/**
 * 
 * @author yiyongfei
 * 
 */
public class YeaException extends RuntimeException {

    /**  */
    private static final long serialVersionUID = 1632780160008772553L;
    /**
     * 原因
     */
    private Throwable cause;

    /**
     * 定义未知的错误代码
     */
    public final static int UNKNOWN_ERROR = 0;

    /**
     *异常的错误代码
     *
     */
    protected int errorCode = UNKNOWN_ERROR;

    protected YeaExceptionStatus oPEStatus = null;

    protected YeaError _oPAFAError = null;

    /**
     * 默认构造函数
     */
    public YeaException() {
        this(UNKNOWN_ERROR, null, null);
    }

    /*
     * 带有初始化的错误代码的构造函数
     * @deprecated PAFA version 2.0 不支持该方法 由PAFA框架实现
     */
    public YeaException(int code) {
        this(code, null, null);
    }

    /*
     *带有初始化的错误代码和嵌套异常的构造函数
     *@deprecated PAFA version 2.0 不支持该方法 由PAFA框架实现
     */
    public YeaException(int code, Throwable inCause) {
        this(code, null, inCause);
    }

    /**
     * @param cause 原因
     * 带有嵌套异常的构造函数
     */
    public YeaException(Throwable inCause) {
        this(UNKNOWN_ERROR, null, inCause);
    }

    /*
     *带有初始化的错误代码和异常信息的构造函数
     *@deprecated PAFA version 2.0 不支持该方法 由PAFA框架实现
     */
    public YeaException(int code, String message) {
        this(code, message, null);
    }

    /**
     * @param message 异常信息
     *带有异常信息的构造函数
     */
    public YeaException(String message) {
        this(UNKNOWN_ERROR, message, null);
    }

    /*
     *带有初始化的错误代码和异常信息、嵌套异常的构造函数
     *@deprecated PAFA version 2.0 不支持该方法 由PAFA框架实现
     *
     * setup a PAFAExceptoin for PAFAExceptoin Handling
     */
    public YeaException(int inCode, String inMsg, Throwable inCause) {
        this(inMsg, 0, 0, null, inCause, YeaExceptionStatus.WARNING);
        errorCode = inCode;
    }

    /**
     * @param message 异常信息
     * @param cause   原因
     *带有异常信息和嵌套异常的构造函数
     */
    public YeaException(String message, Throwable inCause) {
        this(UNKNOWN_ERROR, message, inCause);
    }

    /**
     * 构造JyfaException
      * @param inMsg - 错误信息
      * @param inErrorType - 错误类型，见下：
      *                                    SYSTEM_ERROR
      *                                    FIELD_VALIDATION_ERROR
      *                                    DATA_INVALID_ERROR
      *                                    SQL_ERROR
      * @param inErrorMsgType - 错误原因
      * @param cause - Throwable
     */
    public YeaException(long inErrorType, long inErrMsgType, int inCode, String inMsg, Throwable inCause) {
    	this(inMsg, inErrorType, inErrMsgType, null, inCause, YeaExceptionStatus.WARNING);
        errorCode = inCode;
    }
    
    /**
     * 构造JyfaException
      * @param inMsg - 错误信息
      * @param inErrorType - 错误类型，见下：
      *                                    SYSTEM_ERROR
      *                                    FIELD_VALIDATION_ERROR
      *                                    DATA_INVALID_ERROR
      *                                    SQL_ERROR
      * @param inErrorMsgType - 错误原因
      * @param inErrorParams - 错误细节
      * @param inTh - Throwable
      * @param inStatus - 错误级别，见下：
      *                   WARNING
      *                   FATAL
     */
    public YeaException(String inMsg, long inErrorType, long inErrMsgType, String[] inErrorParams, Throwable inTh, long inStatus) {
        super(inMsg);
        this.cause = inTh;

        String sClassName = "";
        String sMethodName = "";

        _oPAFAError = ExceptionUtils.createYFAError("", sClassName, sMethodName, 0L, inErrorType, inErrMsgType, inErrorParams, inMsg, inTh);

        if (inTh != null && inTh instanceof YeaException && (oPEStatus = ((YeaException) inTh).getPAFAExceptionStatus()) != null) {
            oPEStatus.addError(_oPAFAError);
        } else {
            oPEStatus = new YeaExceptionStatus();
            oPEStatus.addError(_oPAFAError);
            oPEStatus.setStatus(inStatus);
        }
    }

    /**
     * Prints the stack backtrace.
     * 打印堆栈信息到System.err
     * If an exception occurred during class loading it prints that
     * exception's stack trace, or else prints the stack backtrace of
     * this exception.
     */
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    /**
     * Prints the stack backtrace to the specified print stream.
     * 打印堆栈信息到PrintStream
     * If an exception occurred during class loading it prints that
     * exception's stack trace, or else prints the stack backtrace of
     * this exception.
     * @param inPS PrintStream
     */
    public void printStackTrace(PrintStream inPS) {
        if (inPS == null) {
            throw new NullPointerException("PrintStream can't be NULL");
        }
        
        synchronized (inPS) {
            super.printStackTrace(inPS);
            if (cause != null) {
                inPS.println("caused by:");
                cause.printStackTrace(inPS);
            }
        }

    }

    /**
     * Prints the stack backtrace to the specified print writer.
     * 打印堆栈信息到PrintWriter
     * If an exception occurred during class loading it prints that
     * exception's stack trace, or else prints the stack backtrace of
     * this exception.
     * @param inPW PrintWriter
     */
    public void printStackTrace(PrintWriter inPW) {
        if (inPW == null) {
            throw new NullPointerException("PrintWriter can't be NULL");
        }
        
        synchronized (inPW) {
            super.printStackTrace(inPW);
            if (cause != null) {
                inPW.println("caused by:");
                cause.printStackTrace(inPW);
            }
        }

    }

    /*
     *
     *@deprecated JYFA version 2.0
     *@see getCause()
     */
    public Exception getBaseException() {
        return (Exception) cause;
    }

    /**
     * Get Cause
     * 获取嵌套的异常
     * @return cause throwable
     * added by Qiao,Hua
     * referenced by jdk1.4
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * Get Initial Cause
     * 获取异常链中最初的异常.
     * @return cause throwable
     * added by Qiao,Hua
     *
     */
    public Throwable getInitialCause() {
        if (cause == null) {
            //自己是错误的原因
            return this;
        } else {
            if (cause instanceof YeaException) {
                //递归查找错误的原因
                return ((YeaException) cause).getInitialCause();
            } else {
                //保存的异常是错误的原因
                return cause;
            }
        }

    }

    /**
     * get error code
     * 获取错误代码
     * @return 错误代码
     */
    public int getErrorCode() {
        return this.errorCode;
    }

    /**
     * set error code
     * 设置错误代码
     * @param code 错误代码
     */
    public void setErrorCode(int code) {
        this.errorCode = code;
    }

    /**
     * Constructs exception with exception status
     * @param inPafaStatus JyfaExceptionStatus
     */
    public YeaException(YeaExceptionStatus inPafaStatus) {
        this();
        if (inPafaStatus != null) {
            this.oPEStatus = inPafaStatus;
        }
    }

    /**
     * Returns the exception status object of this exception.
     *
     * @return JyfaExceptionStatus the status object of this exception
     */
    public YeaExceptionStatus getPAFAExceptionStatus() {
        return oPEStatus;
    }

    /**
     * 获取error
     * @return JyfaError
     */
    public YeaError getPAFAError() {
        return _oPAFAError;
    }
}
