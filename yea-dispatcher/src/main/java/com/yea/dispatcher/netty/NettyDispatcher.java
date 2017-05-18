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
package com.yea.dispatcher.netty;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yea.core.base.id.UUIDGenerator;
import com.yea.core.dispatcher.DispatcherEndpoint;
import com.yea.core.remote.AbstractEndpoint;
import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.observer.Observable;
import com.yea.dispatcher.netty.promise.ConsumerPromise;
import com.yea.remote.netty.client.NettyClient;
import com.yea.remote.netty.promise.NettyChannelPromise;
import com.yea.remote.netty.server.NettyServer;

import io.netty.channel.ChannelHandler;

/**
 * Netty实现的服务注册、注销与服务发现
 * @author yiyongfei
 *
 */
public class NettyDispatcher implements DispatcherEndpoint {
	private static final Logger LOGGER = LoggerFactory.getLogger(NettyDispatcher.class);
	private NettyClient dispatchClient;
	
	@Override
	public void register(AbstractEndpoint endpoint) throws Throwable {
//		RemoteClient remoteClient = (RemoteClient) dispatchClient.remoteLocator().getClient(UUIDGenerator.generate());
		String registerName = endpoint.getRegisterName();
		SocketAddress socketAddress = new InetSocketAddress(endpoint.getHost(), endpoint.getPort());
		byte[] sessionID = UUIDGenerator.generate();
		try {
			if (endpoint instanceof NettyClient) {
				dispatchClient.send(null, null, RemoteConstants.MessageType.CONSUMER_REGISTER, sessionID, registerName, socketAddress);
				@SuppressWarnings("rawtypes")
				ConsumerPromise observer = new ConsumerPromise((NettyClient)endpoint);
				Observable handle = null;
				Iterator<Map<String, ChannelHandler>> itHandle = dispatchClient.getListHandler().iterator();
		        while(itHandle.hasNext()){
		            Map<String, ChannelHandler> map = itHandle.next();
		            for(Map.Entry<String, ChannelHandler> entry : map.entrySet()) {
		            	if(entry.getValue() instanceof Observable){
			                handle = (Observable) entry.getValue();
			                handle.addObserver(sessionID, observer);
			            }
		            }
		        }
				
				LOGGER.info("" + endpoint.getHost() + ":" + endpoint.getPort() +"向" + dispatchClient.getHost() + ":" + dispatchClient.getPort() +"注册("+endpoint.getRegisterName()+")服务消费者成功");
			}
			if (endpoint instanceof NettyServer) {
				dispatchClient.send(null, null, RemoteConstants.MessageType.PROVIDER_REGISTER, sessionID, registerName, socketAddress);
				LOGGER.info("" + endpoint.getHost() + ":" + endpoint.getPort() +"向" + dispatchClient.getHost() + ":" + dispatchClient.getPort() +"注册("+endpoint.getRegisterName()+")服务提供者成功");
			}
		} catch (Throwable ex) {
			LOGGER.error("" + endpoint.getHost() + ":" + endpoint.getPort() +"向" + dispatchClient.getHost() + ":" + dispatchClient.getPort() +"注册("+endpoint.getRegisterName()+")失败");
			throw ex;
		}
		
	}

	@Override
	public void logout(AbstractEndpoint endpoint) throws Throwable {
		byte[] sessionID = UUIDGenerator.generate();
		String registerName = endpoint.getRegisterName();
		SocketAddress socketAddress = new InetSocketAddress(endpoint.getHost(), endpoint.getPort());
		try {
			if (endpoint instanceof NettyClient) {
				dispatchClient.send(null, null, RemoteConstants.MessageType.CONSUMER_LOGOUT, sessionID,
						registerName, socketAddress);
				LOGGER.info("" + endpoint.getHost() + ":" + endpoint.getPort() + "向" + dispatchClient.getHost() + ":" + dispatchClient.getPort() +"注销("+endpoint.getRegisterName()+")服务消费者成功");
			}
			if (endpoint instanceof NettyServer) {
				dispatchClient.send(null, null, RemoteConstants.MessageType.PROVIDER_LOGOUT, sessionID,
						registerName, socketAddress);
				LOGGER.info("" + endpoint.getHost() + ":" + endpoint.getPort() + "向" + dispatchClient.getHost() + ":" + dispatchClient.getPort() +"注销("+endpoint.getRegisterName()+")服务提供者成功");
			}
		} catch (Throwable ex) {
			LOGGER.error("" + endpoint.getHost() + ":" + endpoint.getPort() + "向" + dispatchClient.getHost() + ":" + dispatchClient.getPort() +"注销("+endpoint.getRegisterName()+")失败");
			throw ex;
		}
		
	}

	@Override
	public List<SocketAddress> discover(AbstractEndpoint endpoint) throws Throwable {
		byte[] sessionID = UUIDGenerator.generate();
		try {
			NettyChannelPromise<List<SocketAddress>> promise = dispatchClient.send(null, null, RemoteConstants.MessageType.PROVIDER_DISCOVER,
					sessionID, endpoint.getRegisterName());
			return promise.awaitObject(1000 * 30);
		} catch (Throwable ex) {
			LOGGER.error("" + endpoint.getHost() + ":" + endpoint.getPort() + "向" + dispatchClient.getHost() + ":" + dispatchClient.getPort() +"查找("+endpoint.getRegisterName()+")失败");
			throw ex;
		}
		
	}


	public NettyClient getDispatchClient() {
		return dispatchClient;
	}

	public void setDispatchClient(NettyClient client) {
		this.dispatchClient = client;
	}
}
