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
package com.yea.core.remote;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.promise.Promise;
import com.yea.core.remote.struct.CallAct;

/**
 * 
 * @author yiyongfei
 * 
 */
public abstract class AbstractClient extends AbstractEndpoint {
    private boolean isStop = false;//是否中止（如果外部请求服务中止，该值为true，如果因系统内部导致中止该值为false）
    private boolean isConnected = false;//是否连了服务端
    private boolean connectResult = false;//是否已连上服务端
    private final Lock connectLock = new ReentrantLock();
    
    protected void _Connected() {
        this.isConnected = true;
    }
    protected void _Disconnected() {
        this.isConnected = false;
    }
    public boolean isConnected() {
        return isConnected;
    }
    
    protected Lock getConnectLock(){
        return this.connectLock;
    }

    protected void _ConnectSuccess() {
        this.connectResult = RemoteConstants.ConnectResult.SUCCESS.value();
    }
    
    protected void _ConnectFailure() {
        this.connectResult = RemoteConstants.ConnectResult.FAILURE.value();
    }
    
    public boolean isConnectSuccess() {
        return connectResult;
    }

    public boolean isStop() {
        return isStop;
    }
    
    protected void _Notstop() {
        this.isStop = false;
    }
    
    protected void _Stop() {
        this.isStop = true;
    }
    
    public <T> Promise<T> send(CallAct act, Object... messages) throws Throwable {
    	return null;
    }
    
}
