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

/**
 * 
 * @author yiyongfei
 * 
 */
public class YeaErrorType implements java.io.Serializable {

    /**  */
    private static final long serialVersionUID = 5372391190045159628L;

    public final static long SYSTEM_ERROR = 100000000;
    public final static long FIELD_VALIDATION_ERROR = 200000000;
    public final static long DATA_INVALID_ERROR = 300000000;
    public final static long READ_RECORD_ERROR = 400000000;
    public final static long INSERT_RECORD_ERROR = 500000000;
    public final static long DELETE_RECORD_ERROR = 600000000;
    public final static long UPDATE_RECORD_ERROR = 700000000;
    public final static long SQL_ERROR = 800000000;

}
