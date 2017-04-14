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
package com.yea.remote.netty.constants;

/**
 * 
 * @author lenovo
 * 
 */
public class NettyConstants {
    public static final String CALL_ACT = "call_act";
    public static final String CALLBACK_ACT = "callback_act";
    public static final String HEADER_DATE = "header_date";
    
    
    public enum ThreadPool {
        SERVICE_SERVER_HANDLER(3), SERVICE_CLIENT_HANDLER(3);
        
        private int value;
        private ThreadPool(int value) {
            this.value = value;
        }
        public int value() {
            return this.value;
        }
    }
    
    public enum HEARTBEAT {
        TIMEOUT(60*1000), IDLETIME_LIMIT(60*1000), RETRY(6);
        
        
        private long value;
        private HEARTBEAT(long value) {
            this.value = value;
        }
        public long value() {
            return this.value;
        }
    }
    
    public enum LoginAuth {
        USERNAME("USERNAME"), PASSWORD("PASSWORD");
        private String value;
        private LoginAuth(String value) {
            this.value = value;
        }
        public String value() {
            return this.value;
        }
    }
    
}
