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

import java.net.SocketAddress;
import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListSet;

import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.ServerComparator;
import com.yea.core.remote.promise.Promise;
import com.yea.core.remote.struct.CallAct;

/**
 * 
 * @author yiyongfei
 */
public abstract class AbstractEndpoint extends AbstractPoint {
	private Collection<BalancingNode> listBalancingNode = new ConcurrentSkipListSet<BalancingNode>(new ServerComparator());

	public Collection<BalancingNode> getBalancingNodes() {
		return listBalancingNode;
	}

	public BalancingNode getBalancingNode(SocketAddress address) {
		for (BalancingNode node : getBalancingNodes()) {
			if (node.getSocketAddress().equals(address)) {
				return node;
			}
		}
		return null;
	}
	
	public abstract <T> Promise<T> send(CallAct act, Object... messages) throws Throwable;
}
