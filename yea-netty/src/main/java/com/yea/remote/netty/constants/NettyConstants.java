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
    
    public final static long SLOW_LIMIT = 28 * 100;
    public final static int SEND_QUEUE_LIMIT = 128;//无用，原RemoteClient的发送队列阻塞值
    
    public enum MessageHeaderAttachment {
    	CALL_ACT("1"), CALL_REFLECT("2"), CALLBACK_ACT("3"),
    	HEADER_DATE("11"), SEND_DATE("12"), RECIEVE_DATE("13"), REQUEST_DATE("14"), REQUEST_SEND_DATE("15"), REQUEST_RECIEVE_DATE("16");
        
        private String value;
        private MessageHeaderAttachment(String value) {
            this.value = value;
        }
        public String value() {
            return this.value;
        }
        public String toString() {
            return this.value;
        }
    }
    
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
    
    public enum Heartbeat {
        TIMEOUT(60*1000), IDLETIME_LIMIT(60*1000), RETRY(6);
        
        
        private long value;
        private Heartbeat(long value) {
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
