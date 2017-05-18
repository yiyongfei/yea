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

import java.net.SocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.yea.core.loadbalancer.AbstractBalancingNode;
import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.remote.AbstractEndpoint;
import com.yea.core.remote.exception.RemoteException;
import com.yea.core.remote.promise.Promise;
import com.yea.core.remote.struct.CallAct;

@SuppressWarnings("rawtypes")
public class ClientRegister<T> {
	private Map<String, AbstractEndpoint> mapEndpoint = null;
	private Map<String, List<AbstractBalancingNode>> mapBalancingNode = null;
	private Map<String, Set<String>> mapActName = null;

	private ClientRegister() {
		mapEndpoint = new ConcurrentHashMap<String, AbstractEndpoint>();
		mapBalancingNode = new ConcurrentHashMap<String, List<AbstractBalancingNode>>();
		mapActName = new ConcurrentHashMap<String, Set<String>>();
		scheduledDistribution();
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
	
	public void registerBalancingNode(String registerName, AbstractBalancingNode node) {
		if (node != null) {
			if (!mapBalancingNode.containsKey(registerName)) {
				mapBalancingNode.put(registerName, new CopyOnWriteArrayList<AbstractBalancingNode>());
			}
			mapBalancingNode.get(registerName).add(node);
		}
	}
	
	public void unregisterBalancingNode(String registerName, AbstractBalancingNode node) {
		if (node != null && mapBalancingNode.containsKey(registerName)) {
			Iterator<AbstractBalancingNode> it = mapBalancingNode.get(registerName).iterator();
			BalancingNode tmp;
			while(it.hasNext()) {
				tmp = it.next();
				if(tmp.getSocketAddress().equals(node.getSocketAddress())) {
					mapBalancingNode.get(registerName).remove(tmp);
				}
			}
		}
	}
	
	public List<BalancingNode> getAllBalancingNode(String registerName) {
		List<BalancingNode> list = new CopyOnWriteArrayList<BalancingNode>();
		if(mapBalancingNode.containsKey(registerName)) {
			list.addAll(mapBalancingNode.get(registerName));
		}
		return list;
	}
	
	public AbstractBalancingNode getBalancingNode(String registerName, SocketAddress remote) {
		if(mapBalancingNode.containsKey(registerName)) {
			List<AbstractBalancingNode> listBalancingNode = mapBalancingNode.get(registerName);
			for(AbstractBalancingNode node : listBalancingNode) {
				if(node.getSocketAddress().equals(remote)) {
					return node;
				}
			}
		}
		return null;
	}
	
	public AbstractBalancingNode getBalancingNode(SocketAddress local, SocketAddress remote) {
		for(List<AbstractBalancingNode> listBalancingNode : mapBalancingNode.values()) {
			for(AbstractBalancingNode node : listBalancingNode) {
				if(node.getLocalAddress().equals(local) && node.getSocketAddress().equals(remote)) {
					return node;
				}
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
	
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private final AtomicBoolean scheduled = new AtomicBoolean(false);
	private void scheduledDistribution() {
		if (scheduled.compareAndSet(false, true)) {
			executor.execute(resetHealthRunnable);
		}
	}
	
	private final Runnable resetHealthRunnable = new Runnable() {
		@Override
		public void run() {
			while (true) {
				try {
					for(List<AbstractBalancingNode> listBalancingNode : mapBalancingNode.values()) {
						if (listBalancingNode.size() == 0) {
							scheduled.set(false);
							break;
						}
						for (AbstractBalancingNode node : listBalancingNode) {
							if(node.isSuspended()) {
								node.resetServerHealth();
							}
						}
					}
					
					Thread.sleep(200);
				} catch (Exception e) {
				}
			}
		}
	};
}
