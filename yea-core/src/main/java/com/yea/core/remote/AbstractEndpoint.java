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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.yea.core.dispatcher.DispatcherEndpoint;
import com.yea.core.remote.promise.Promise;
import com.yea.core.remote.struct.CallAct;

/**
 * 
 * @author yiyongfei
 */
public abstract class AbstractEndpoint implements ApplicationContextAware {
	private String registerName;//服务注册名
	private DispatcherEndpoint dispatcher;//服务调度中心，为null时不进行远程调度
    private String host;
    private int port;
    
    public String getHost() {
        return host;
    }
    public void setHost(String host) {
        this.host = host;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public String getRegisterName() {
		return registerName;
	}
	public void setRegisterName(String registerName) {
		this.registerName = registerName;
	}
	public DispatcherEndpoint getDispatcher() {
		return dispatcher;
	}
	public void setDispatcher(DispatcherEndpoint dispatcher) {
		this.dispatcher = dispatcher;
	}


	private ApplicationContext context;
    /** 
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    public void setApplicationContext(ApplicationContext arg0) throws BeansException {
        context = arg0;
    }
    public ApplicationContext getApplicationContext() {
        return context;
    }
    
    public abstract <T> Promise<T> send(CallAct act, Object... messages) throws Throwable;
}
