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
package com.yea.remote.netty.exception;

import com.yea.core.exception.YeaException;

/**
 * 
 * @author yiyongfei
 * 
 */
public class WriteRejectException extends YeaException {

    /**  */
    private static final long serialVersionUID = -9052492587652558470L;

    public WriteRejectException() {
        super(UNKNOWN_ERROR, "Netty已达写高水位，请稍候再试", null);
    }
}
