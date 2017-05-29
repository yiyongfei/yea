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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yea.core.loadbalancer.AbstractBalancingNode;
import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.remote.AbstractEndpoint;
import com.yea.core.remote.exception.RemoteException;
import com.yea.core.remote.promise.Promise;
import com.yea.core.remote.struct.CallAct;
import com.yea.core.util.ScheduledExecutor;

@SuppressWarnings("rawtypes")
public class ClientRegister<T> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ClientRegister.class);
	private Map<String, AbstractEndpoint> mapEndpoint = null;
	private Map<String, Set<String>> mapActName = null;
	private int _RefreshInterval = 30 * 1000;
	private Runnable _RefreshRunnable = null;

	private ClientRegister() {
		mapEndpoint = new ConcurrentHashMap<String, AbstractEndpoint>();
		mapActName = new ConcurrentHashMap<String, Set<String>>();
		
		_RefreshRunnable = new Runnable() {
			public void run() {
				try {
					LOGGER.debug("开始检查节点的健康度[检查项:写通道、熔断]");
					for (AbstractEndpoint endpoint : mapEndpoint.values()) {
						if (endpoint.getBalancingNodes().isEmpty()) {
							continue;
						}
						for (BalancingNode node : endpoint.getBalancingNodes()) {
							if (node.isSuspended()) {
								((AbstractBalancingNode) node).resetServerHealth();
							}
						}
					}
				} catch (Exception e) {
				}
			}
		};
		
		ScheduledExecutor.getScheduledExecutor().scheduleWithFixedDelay(_RefreshRunnable, 60 * 1000, _RefreshInterval, TimeUnit.MILLISECONDS);
	}

	public Promise<T> send(CallAct act, Object... messages) throws Throwable {
		if (mapActName.containsKey(act.getActName())) {
			Set<String> actset = mapActName.get(act.getActName());
			if (actset.size() == 0) {
				throw new RemoteException("未发现Act[" + act.getActName() + "]对应的注册服务，请检查！");
			} else if (actset.size() > 1) {
				throw new RemoteException("发现Act[" + act.getActName() + "]对应的注册服务超出一个，请指定注册服务发送！");
			} else {
				return send((String) actset.toArray()[0], act, messages);
			}
		} else {
			throw new RemoteException("未发现Act[" + act.getActName() + "]被注册，请检查！");
		}
	}

	public Promise<T> send(String registerName, CallAct act, Object... messages) throws Throwable {
		if (mapEndpoint.containsKey(registerName)) {
			return mapEndpoint.get(registerName).send(act, messages);
		} else {
			throw new RemoteException("未发现注册服务[" + registerName + "]的存在，请检查！");
		}
	}

	/**
	 * 注意：如果endpoint已经定义，但未被注册到ClientRegister内，可能原因是延迟加载引发的，请修改成立即加载
	 * 
	 * @param registerName
	 * @param endpoint
	 */
	public void registerEndpoint(String registerName, AbstractEndpoint endpoint) {
		if (endpoint != null) {
			mapEndpoint.put(registerName, endpoint);
		}
	}

	public AbstractEndpoint getEndpoint(String registerName) {
		return mapEndpoint.get(registerName);
	}
	
	public AbstractEndpoint getEndpoint(SocketAddress local) {
		for (AbstractEndpoint endpoint : mapEndpoint.values()) {
			if (new InetSocketAddress(endpoint.getHost(), endpoint.getPort()).equals(local)) {
				return endpoint;
			}
		}
		return null;
	}

	public void registerAct(String registerName, String[] actnames) {
		if (actnames != null && actnames.length > 0) {
			for (String actname : actnames) {
				if (!mapActName.containsKey(actname)) {
					mapActName.put(actname, new ConcurrentSkipListSet<String>());
				}
				mapActName.get(actname).add(registerName);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> ClientRegister<T> getInstance() {
		return Holder.SINGLETON;
	}

	private static class Holder {
		private static final ClientRegister SINGLETON = new ClientRegister();
	}
	
}
