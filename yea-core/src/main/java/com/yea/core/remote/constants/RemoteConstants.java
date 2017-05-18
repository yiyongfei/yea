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
package com.yea.core.remote.constants;

public class RemoteConstants {

	public enum MessageResult {
        SUCCESS((byte) 1), FAILURE((byte) -1);
        private byte value;
        private MessageResult(byte value) {
            this.value = value;
        }
        public byte value() {
            return this.value;
        }
    }
    
    public enum MessageType {
        SERVICE_REQ((byte) 1), 
        SERVICE_RESP((byte) 2), 
        LOGIN_REQ((byte) 3), 
        LOGIN_RESP((byte) 4), 
        HEARTBEAT_REQ((byte) 5), 
        HEARTBEAT_RESP((byte) 6), 
        SUSPEND_REQ((byte) 7), 
        SUSPEND_RESP((byte) 8), 
        ONE_WAY((byte) 9),
        ACTLOOKUP_REQ((byte)10),
        ACTLOOKUP_RESP((byte)11),
        PING_REQ((byte)12),
        PING_RESP((byte)13),
        CONSUMER_REGISTER((byte) 21), 
        CONSUMER_REGISTER_RESULT((byte) 22), 
        CONSUMER_LOGOUT((byte) 23),
        CONSUMER_LOGOUT_RESULT((byte) 24),
        PROVIDER_REGISTER((byte) 31), 
        PROVIDER_REGISTER_NOTIFY((byte) 32), 
        PROVIDER_LOGOUT((byte) 33),
        PROVIDER_DISCOVER((byte) 34), 
        PROVIDER_DISCOVER_RESULT((byte) 35), 
        STOP((byte) 99);
        private byte value;
        private MessageType(byte value) {
            this.value = value;
        }
        public byte value() {
            return this.value;
        }
    }
    
    public enum ExceptionType {
        CONNECT(100), //尚未建立连接
        SETTING(200), //设置错误
        TIMEOUT(300), //超时错误
        BUSINESS(400), //业务错误
        OTHER(900); 
        
        private int value;
        private ExceptionType(int value) {
            this.value = value;
        }
        public int value() {
            return this.value;
        }
    }
    
    public enum ConnectResult {
        SUCCESS(true), FAILURE(false);
        private boolean value;
        private ConnectResult(boolean value) {
            this.value = value;
        }
        public boolean value() {
            return this.value;
        }
    }
    
    public enum BindResult {
        SUCCESS(true), FAILURE(false);
        private boolean value;
        private BindResult(boolean value) {
            this.value = value;
        }
        public boolean value() {
            return this.value;
        }
    }
    
    public enum DispatchType {
    	CONSUMER("CONSUMER"), PROVIDER("PROVIDER");
        private String value;
        private DispatchType(String value) {
            this.value = value;
        }
        public String value() {
            return this.value;
        }
    }
    

    public enum ServerHealthType {
        SEND(Byte.parseByte("1")), SLOW(Byte.parseByte("2")), HYSTRIX(Byte.parseByte("3"));
        
        private byte value;
        private ServerHealthType(byte value) {
            this.value = value;
        }
        public byte value() {
            return this.value;
        }
    }
}
