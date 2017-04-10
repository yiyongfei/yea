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
public class YeaErrorMessage implements java.io.Serializable {
    /**  */
    private static final long serialVersionUID = -1628074510916482648L;

    // Platform Layer Error Codes
    public final static long ERR_PLATFORM = 100000000;

    // Foundation Layer Error Codes
    public final static long ERR_FOUNDATION = 200000000;

    // Application Layer Error Codes
    public final static long ERR_APPLICATION = 300000000;

    // Business Layer Error Codes
    public final static long ERR_BUSINESS = 400000000;

    // For demonstration purpose only...
    public final static long ERR_BUSINESS_SAMPLE_APP = 49999;

}
