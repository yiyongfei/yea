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
package com.yea.core.dispatcher;

import java.net.SocketAddress;
import java.util.List;

import com.yea.core.remote.AbstractEndpoint;

/**
 * 服务注册、注销与服务发现
 * @author yiyongfei
 * 
 */
public interface DispatcherEndpoint {
	
	public void register(AbstractEndpoint endpoint) throws Throwable;//服务注册
	public void logout(AbstractEndpoint endpoint) throws Throwable;//服务注销
	public List<SocketAddress> discover(AbstractEndpoint endpoint) throws Throwable;//服务发现
	
}
