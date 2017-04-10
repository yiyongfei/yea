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
package com.yea.remote.netty.balancing;

import java.math.BigDecimal;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yea.core.balancing.hash.BalancingNode;
import com.yea.core.balancing.hash.KetamaNodeLocator;
import com.yea.core.balancing.hash.NodeLocator;
import com.yea.core.base.id.UUIDGenerator;
import com.yea.core.exception.constants.YeaErrorMessage;
import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.exception.RemoteException;

public class RemoteClientLocator {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteClientLocator.class);
    
	//负载均衡（哈希）
    private NodeLocator _WriteNodeLocator = new KetamaNodeLocator();
    
    //负载均衡（哈希）（写缓存已满的暂存，取Client时先从此取，若还满再从nodeLocator里取，若不满将移到nodeLocator里）
    private NodeLocator _ReadNodeLocator = new KetamaNodeLocator();
    
    public void addLocator(RemoteClient client){
    	_WriteNodeLocator.addLocator(client);
    	nodeUseCount.put(client, 0L);
    }
    
    public void removeLocator(RemoteClient client){
    	if (_WriteNodeLocator.containsLocator(client.remoteAddress)) {
			_WriteNodeLocator.removeLocator(client);
		} else if (_ReadNodeLocator.containsLocator(client.remoteAddress)) {
			_ReadNodeLocator.removeLocator(client);
		}
    	nodeUseCount.remove(client);
    }
    
    public boolean containsLocator(SocketAddress address) {
    	boolean isExist = _WriteNodeLocator.containsLocator(address);
		if (isExist) {
			return isExist;
		} else {
			return _ReadNodeLocator.containsLocator(address);
		}
    }
    
    public Collection<BalancingNode> getAll() {
    	Collection<BalancingNode> collection = this._WriteNodeLocator.getAll();
    	collection.addAll(this._ReadNodeLocator.getAll());
    	return collection;
    }
    
    public Collection<BalancingNode> getLocator(SocketAddress address) {
    	Collection<BalancingNode> collection = this._WriteNodeLocator.getLocator(address);
    	collection.addAll(this._ReadNodeLocator.getLocator(address));
    	return collection;
    }
    
    public RemoteClient getClient(byte[] sessionID) {
    	RemoteClient client = null;
		while (true) {
			if (_ReadNodeLocator.getAll().size() > 0) {
				client = (RemoteClient) _ReadNodeLocator.getPrimary(UUIDGenerator.restore(sessionID).toString());
				if (client.channel.isWritable()) {
					LOGGER.info("通道" + client.channel + "写缓存已清理到达下限禁戒线，移入写负载池！");
					this._ReadNodeLocator.removeLocator(client);
					this._WriteNodeLocator.addLocator(client);
					break;
				}
			}

			if (this._WriteNodeLocator.getAll().size() == 0) {
				throw new RemoteException(YeaErrorMessage.ERR_APPLICATION,
						RemoteConstants.ExceptionType.CONNECT.value(), "Netty的写通道缓存已满，需等候！", null);
			} else {
				client = (RemoteClient) _WriteNodeLocator.getPrimary(UUIDGenerator.restore(sessionID).toString());
				if (client.channel.isWritable()) {
					break;
				} else {
					LOGGER.info("通道" + client.channel + "写缓存已达上限禁戒线，移入读负载池！");
					this._WriteNodeLocator.removeLocator(client);
					this._ReadNodeLocator.addLocator(client);
				}
			}
		}
		nodeUseCount.put(client, nodeUseCount.get(client) + 1L);
		return client;
	}
    

    private volatile Map<BalancingNode, Long> nodeUseCount = new HashMap<BalancingNode, Long>(); 
    
    public String nodeStatistics() {
    	StringBuffer sb = new StringBuffer();
    	long count = 0;
    	Iterator<Long> it1 = nodeUseCount.values().iterator();
    	while(it1.hasNext()){
    		count += it1.next();
    	}
    	sb.append("截止").append(new Date()).append("，共调用节点").append(count).append("次。");
    	if(count > 0){
    		sb.append("其中：");
    		Iterator<BalancingNode> it2 = nodeUseCount.keySet().iterator();
        	BalancingNode node = null;
        	while(it2.hasNext()){
        		node = it2.next();
        		sb.append(node).append("调用").append(nodeUseCount.get(node)).append("次，占比：").append(new BigDecimal(nodeUseCount.get(node) * 100).divide(new BigDecimal(count), 2, BigDecimal.ROUND_HALF_DOWN)).append("%；");
        	}
    	}
    	return sb.toString();
    }
}
