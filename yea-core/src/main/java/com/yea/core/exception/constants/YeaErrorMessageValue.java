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
public class YeaErrorMessageValue extends HashMap<Long, String> implements java.io.Serializable {
    /**  */
    private static final long serialVersionUID = 2248665365767171324L;

    private YeaErrorMessageValue() {
        super();
        put(YeaErrorMessage.ERR_PLATFORM, "Platform Layer Exception");
        put(YeaErrorMessage.ERR_FOUNDATION, "Foundation Layer Exception");
        put(YeaErrorMessage.ERR_APPLICATION, "Application Layer Exception");
        put(YeaErrorMessage.ERR_BUSINESS, "Business Layer Exception");

        // the following is for demonstration purpose only...
        put(YeaErrorMessage.ERR_BUSINESS_SAMPLE_APP, "Sample Application Exception");
    }

    public static String getErrorMsg(long code) {
        try {
            String msg = (String) msgs.get(code);
            return msg;
        } catch (Exception ex) {
            return "Couldn't find error message";
        }
    }

    private static YeaErrorMessageValue msgs = new YeaErrorMessageValue();

}
