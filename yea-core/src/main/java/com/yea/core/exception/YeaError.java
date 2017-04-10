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

/**
 * 
 * @author yiyongfei
 * 
 */
public class YeaError implements java.io.Serializable {

    /**  */
    private static final long serialVersionUID = 8988227076480069728L;

    /**
     * 错误发生的IP地址
     */
    protected String _sIP = "";

    /**
     * 错误发生的类名
     */
    protected String _sClassName = "";
    /**
     * 错误发生的方法
     */
    protected String _sMethodName = "";
    /**
     * 错误发生时的用户
     */
    protected String _sUser = "";

    /**
     * unqiue id of the error: this could be mainly used for tracing purpose
     * current implementation is a random generated id taking a seed such as ip address
     */
    protected String _sErrorId = "";

    /**
     * error reason code of the error. A component and error code combined with a reason code
     * maps uniquely to an error message.
     */
    protected long _lErrorReasonCode;

    /**
     * code of the language that is used in messages to describe the error scenario.
     */
    protected String _sLocale = "";

    /**
     * uniqe id of the component that is involved in the error
     */
    protected long _lComponentType;

    /**
     * description of the component that is involved in the error scenario
     */
    protected String _sComponentTypeValue = "";

    /**
     * predefined message type code that used to describe the error scenario.
     */
    protected long _lErrorMessageType;

    /**
     * predefined user friendly message that used to describe the error scenario.
     */
    protected String _sErrorMessage = "";

    /**
     * id of the error type
     */
    protected long _lErrorType;

    /**
     * description of the error type
     */
    protected String _sErrorTypeValue = "";

    /**
     * id of severity.
     */
    protected long _lSeverity;

    /**
     * description of the severity
     */
    protected String _sSeverityValue = "";

    /**
     * detailed description of the error scenario (for example, in case a SQLException is thrown, document the SQL statement, parameters etc.)
     * this detail will not be presented to the end user, but maily for debugging purpose, for example: log to a file etc.
     */
    protected String _sErrorDetail = "";

    /**
     * parameters that may be used to format the error messages
     */
    protected String[] _sParametersList;

    /**
     * throwable object that caused the error
     */
    protected Throwable _oThrowable;

    /**
     * default constructor
     */
    public YeaError() {
        super();
    }

    /**
     * constructor
     */
    public YeaError(String inUser, String inClassName, String inMethodName, String inErrorId, long inComponentType, String inErrorTypeValue,
                     long inErrMsgType, String[] inParams, Throwable inTh) {
        super();
        _sUser = inUser;
        _sClassName = inClassName;
        _sMethodName = inMethodName;
        _sErrorId = inErrorId;
        _lComponentType = inComponentType;
        _sErrorTypeValue = inErrorTypeValue;
        _lErrorMessageType = inErrMsgType;
        _sParametersList = inParams;
        _oThrowable = inTh;
    }

    /**
     *
     * @return long
     */
    public long getComponentType() {
        return _lComponentType;
    }

    /**
     *
     * @return String
     */
    public String getComponentTypeValue() {
        return _sComponentTypeValue;
    }

    /**
     *
     * @return String
     */
    public String getErrorDetail() {
        return _sErrorDetail;
    }

    /**
     *
     * @return String
     */
    public String getErrorMessage() {
        return _sErrorMessage;
    }

    /**
     *
     * @return long
     */
    public long getErrorType() {
        return _lErrorType;
    }

    /**
     *
     * @return String
     */
    public String getErrorTypeValue() {
        return _sErrorTypeValue;
    }

    /**
     *
     * @return String
     */
    public String getLocale() {
        return _sLocale;
    }

    /**
     *
     * @return String[]
     */
    public String[] getParameters() {
        return _sParametersList;
    }

    /**
     *
     * @return long
     */
    public long getErrorReasonCode() {
        return _lErrorReasonCode;
    }

    /**
     * @return long
     */
    public long getSeverity() {
        return _lSeverity;
    }

    /**
     *
     * @return String
     */
    public String getSeverityValue() {
        return _sSeverityValue;
    }

    /**
     *
     * @return Throwable
     */
    public Throwable getThrowable() {
        return _oThrowable;
    }

    /**
     *
     * @param inComponentType long
     */
    public void setComponentType(long inComponentType) {
        _lComponentType = inComponentType;
    }

    /**
     *
     * @param inComponentTypeValue String
     */
    public void setComponentTypeValue(String inComponentTypeValue) {
        _sComponentTypeValue = inComponentTypeValue;
    }

    /**
     *
     * @param inErrorDetail String
     */
    public void setErrorDetail(java.lang.String inErrorDetail) {
        _sErrorDetail = inErrorDetail;
    }

    /**
     *
     * @param inErrorMessage String
     */
    public void setErrorMessage(String inErrorMessage) {
        _sErrorMessage = inErrorMessage;
    }

    /**
     *
     * @param inErrorType long
     */
    public void setErrorType(long inErrorType) {
        _lErrorType = inErrorType;
    }

    /**
     *
     * @param inErrorTypeValue String
     */
    public void setErrorTypeValue(String inErrorTypeValue) {
        _sErrorTypeValue = inErrorTypeValue;
    }

    /**
     *
     * @param inLocale String
     */
    public void setLocale(String inLocale) {
        _sLocale = inLocale;
    }

    /**
     *
     * @param inParameters String[]
     */
    public void setParameters(String[] inParameters) {
        _sParametersList = inParameters;
    }

    /**
     *
     * @param inErrorReasonCode long
     */
    public void setErrorReasonCode(long inErrorReasonCode) {
        _lErrorReasonCode = inErrorReasonCode;
    }

    /**
     *
     * @param inSeverity long
     */
    public void setSeverity(long inSeverity) {
        _lSeverity = inSeverity;
    }

    /**
     *
     * @param inSeverityValue String
     */
    public void setSeverityValue(String inSeverityValue) {
        _sSeverityValue = inSeverityValue;
    }

    /**
     *
     * @param inTh Throwable
     */
    public void setThrowable(Throwable inTh) {
        _oThrowable = inTh;
    }

    /**
     *
     * @return long
     */
    public long getErrorMessageType() {
        return _lErrorMessageType;
    }

    /**
     *
     * @param inErrorMessageType long
     */
    public void setErrorMessageType(long inErrorMessageType) {
        _lErrorMessageType = inErrorMessageType;
    }

    /**
     *
     * @return String
     */
    public String getErrorId() {
        return _sErrorId;
    }

    /**
     *
     * @param inErrorId String
     */
    public void setErrorId(String inErrorId) {
        _sErrorId = inErrorId;
    }

    /**
     * 设置类名
     * @param inClassName String
     */
    public void setClassName(String inClassName) {
        _sClassName = inClassName;
    }

    /**
     * 设置方法名
     * @param inMethodName String
     */
    public void setMethodName(String inMethodName) {
        _sMethodName = inMethodName;
    }

    /**
     * 设置用户名
     * @param inUser String
     */
    public void setUser(String inUser) {
        _sUser = inUser;
    }

    /**
     * 获取类名
     * @return String
     */
    public String getClassName() {
        return _sClassName;
    }

    /**
     * 获取用户名
     * @return String
     */
    public String getUser() {
        return _sUser;
    }

    /**
     * 获取方法名
     * @return String
     */
    public String getMethodName() {
        return _sMethodName;
    }

    /**
     * 获取IP地址
     * @return String
     */
    public String getIP() {
        return _sIP;
    }

    /**
     * 设置IP地址
     * @param inIP String
     */
    public void setIP(String inIP) {
        _sIP = inIP;
    }

}
