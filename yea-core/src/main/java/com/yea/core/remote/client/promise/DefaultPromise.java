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
package com.yea.core.remote.client.promise;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.yea.core.exception.constants.YeaErrorMessage;
import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.exception.RemoteException;
import com.yea.core.remote.observer.Observable;
import com.yea.core.remote.observer.Observer;
import com.yea.core.remote.promise.Promise;
import com.yea.core.remote.struct.Header;


/**
 * 
 * @author yiyongfei
 * 
 */
@SuppressWarnings("rawtypes")
public class DefaultPromise implements Promise, Observer {
    private Object object;
    private Throwable throwable;
    private byte[] sessionID;
    private boolean isChanged;
    private boolean isSuccess;
    
    public DefaultPromise(){
        this.isChanged = false;
        this.object = null;
    }
    
    public Object awaitObject() throws Throwable{
        return awaitObject(0L);
    }
    
    public Object awaitObject(long timeout) throws Throwable{
        long startTime = new Date().getTime();
        return awaitObject(startTime, timeout);
    }
    
	private Object awaitObject(long startTime, long timeout) throws Throwable {
		if (this.hasChanged(sessionID)) {
			if (this.isSuccess) {
				return this.object;
			} else {
				throw new RemoteException(YeaErrorMessage.ERR_APPLICATION,
						RemoteConstants.ExceptionType.BUSINESS.value(), this.throwable.getMessage(), this.throwable);
			}

		} else {
			while (true) {
				TimeUnit.MILLISECONDS.sleep(25);
				if (this.hasChanged(sessionID)) {
					break;
				}
			}
			if (this.isSuccess) {
				return this.object;
			} else {
				throw new RemoteException(YeaErrorMessage.ERR_APPLICATION,
						RemoteConstants.ExceptionType.BUSINESS.value(), this.throwable.getMessage(), this.throwable);
			}
		}

	}
    
    /** 
     * @see com.yea.core.remote.observer.Observer#update(com.yea.core.remote.observer.Observable, java.lang.Object)
     */
    public void update(byte[] sessionID, Header header, Observable o, Object arg) {
        this.isChanged = true;
        this.sessionID = sessionID;
		if (RemoteConstants.MessageResult.SUCCESS.value() == header.getResult()) {
			this.isSuccess = true;
			this.object = arg;
		} else {
			this.isSuccess = false;
			this.throwable = (Throwable)arg;
		}
        
    }

    /** 
     * @see com.yea.core.remote.observer.Observer#hasChanged()
     */
    public boolean hasChanged(byte[] sessionID) {
        return this.isChanged;
    }

}