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
package com.yea.dispatcher.netty.promise;

import java.net.SocketAddress;

import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.observer.Observable;
import com.yea.core.remote.observer.Observer;
import com.yea.core.remote.struct.Header;
import com.yea.remote.netty.client.NettyClient;


/**
 * 
 * @author yiyongfei
 */
public class ConsumerPromise<T> implements Observer<T> {
    private NettyClient client;
    
    public ConsumerPromise(NettyClient client){
    	this.client = client;
    }
    
    /** 
     * @see com.yea.core.remote.observer.Observer#update(com.yea.core.remote.observer.Observable, java.lang.Object)
     */
    public void update(byte[] sessionID, Header header, Observable o, T arg) {
    	if (header.getType() == RemoteConstants.MessageType.CONSUMER_LOGOUT_RESULT.value()) {
    		o.deleteObserver(header.getSessionID(), this);
    	} else if (header.getType() == RemoteConstants.MessageType.PROVIDER_REGISTER_NOTIFY.value()) {
    		try {
				client.connect((SocketAddress)arg);
			} catch (Exception e) {
			}
		} else {
		}
    }

    /** 
     * @see com.yea.core.remote.observer.Observer#hasChanged()
     */
    public boolean hasChanged(byte[] sessionID) {
        return true;
    }

}