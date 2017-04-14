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
package com.yea.core.remote.client;

import java.util.concurrent.ForkJoinPool;

import com.yea.core.base.act.AbstractAct;
import com.yea.core.remote.AbstractEndpoint;
import com.yea.core.remote.client.promise.DefaultPromise;
import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.promise.Promise;
import com.yea.core.remote.struct.CallAct;
import com.yea.core.remote.struct.Header;

/**
 * 
 * @author yiyongfei
 * 
 */
public class DefaultClient extends AbstractEndpoint {
	private ForkJoinPool pool = new ForkJoinPool();
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public Promise send(CallAct act, Object... messages) throws Exception {
    	DefaultPromise promise = new DefaultPromise();
    	byte[] sessionID = new byte[]{0};
    	AbstractAct<?> srcAct = (AbstractAct<?>) this.getApplicationContext().getBean(act.getActName());
		AbstractAct<?> cloneAct = (AbstractAct<?>) srcAct.clone();
		cloneAct.setApplicationContext(this.getApplicationContext());
		cloneAct.setMessages(messages);
		try{
    		Object obj = pool.invoke(cloneAct);
    		Header header = new Header();
            header.setType(RemoteConstants.MessageType.SERVICE_RESP.value());
            header.setSessionID(sessionID);
            header.setResult(RemoteConstants.MessageResult.SUCCESS.value());
    		promise.update(sessionID, header, null, obj);
    	} catch (Exception ex){
    		Header header = new Header();
            header.setType(RemoteConstants.MessageType.SERVICE_RESP.value());
            header.setSessionID(sessionID);
            header.setResult(RemoteConstants.MessageResult.FAILURE.value());
    		promise.update(sessionID, header, null, ex);
    	}
    	
    	return promise;
    }
    
    public int remoteConnects() {
    	return 1;
    }
}


