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
package com.yea.core.exception.constants;

import java.util.HashMap;

/**
 * 
 * @author yiyongfei
 * 
 */
public class YeaErrorTypeValue extends HashMap<Long, String> implements java.io.Serializable{

    /**  */
    private static final long serialVersionUID = -6341808892884820358L;

    private YeaErrorTypeValue() {
        super();
        put(YeaErrorType.SYSTEM_ERROR, "SYSTEM_ERROR");
        put(YeaErrorType.FIELD_VALIDATION_ERROR, "FIELD_VALIDATION_ERROR");
        put(YeaErrorType.READ_RECORD_ERROR, "READ_RECORD_ERROR");
        put(YeaErrorType.DATA_INVALID_ERROR, "DATA_INVALID_ERROR");
        put(YeaErrorType.INSERT_RECORD_ERROR, "INSERT_RECORD_ERROR");
        put(YeaErrorType.DELETE_RECORD_ERROR, "DELETE_RECORD_ERROR");
        put(YeaErrorType.UPDATE_RECORD_ERROR, "UPDATE_RECORD_ERROR");
        put(YeaErrorType.SQL_ERROR, "SQL_ERROR");
    }

    public static String getErrorTypeValue(long code) {
        try {
            String msg = (String) msgs.get(code);
            return msg;
        } catch (Exception ex) {
            return "Couldn't find error message";
        }
    }

    private static YeaErrorTypeValue msgs = new YeaErrorTypeValue();
}
