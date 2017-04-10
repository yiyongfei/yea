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

import java.io.Serializable;
import java.util.ArrayList;

/**
 * 
 * @author yiyongfei
 * 
 */
public class YeaExceptionStatus implements Serializable {

    /**  */
    private static final long serialVersionUID = -2266420384960811647L;
    /*
     * list of status codes, this list can be extended
     */
    public static final long WARNING = 5;
    public static final long FATAL = 9;

    /* The error group contains all the errors that cause the current status failure */
    protected java.util.List<YeaError> _oErrorGroup;

    /* The current status indicator, valid value for the indicator are predefined as constant, same as severity codes s*/
    protected long _lStatus;

    /**
     * Default constructor
     */
    public YeaExceptionStatus() {
        super();
        _oErrorGroup = new ArrayList<YeaError>();
    }

    /**
     * Returns the error group of the status object.
     * @return The Vector that contains all the error objects.
     */
    public java.util.List<YeaError> getErrorGroup() {
        return _oErrorGroup;
    }

    /**
     * Returns the current status indicator
     * @return The status of the object
     */
    public long getStatus() {
        return _lStatus;
    }

    /**
     * Sets the current status indicator.
     * @param inlStatus long the status indicator whose valid values are defined as constants
     */
    public void setStatus(long inlStatus) {
        _lStatus = inlStatus;
    }

    /**
     * Adds an error to the status object. Multiple errors could be added to indicate
     * all of those errors that cause the current status, for example, during validation.
     * @param inError PAFAError
     */
    public void addError(YeaError inError) {
        _oErrorGroup.add(inError);
    }

}
