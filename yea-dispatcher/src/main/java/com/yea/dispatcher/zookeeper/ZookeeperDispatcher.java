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
package com.yea.dispatcher.zookeeper;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooDefs.Perms;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yea.core.dispatcher.DispatcherEndpoint;
import com.yea.core.remote.AbstractEndpoint;
import com.yea.core.serializer.ISerializer;
import com.yea.core.serializer.fst.Serializer;
import com.yea.remote.netty.client.NettyClient;
import com.yea.remote.netty.server.NettyServer;

/**
 * Zookeeper实现
 * @author yiyongfei
 *
 */
public class ZookeeperDispatcher implements DispatcherEndpoint {
	private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperDispatcher.class);
	private ISerializer serializer;
	private CuratorFramework zkClient;
	
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
	
	public void init(){
		serializer = new Serializer();
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 5);
		zkClient = createWithOptions(host + ":" + port, retryPolicy, 10000, 10000);
		zkClient.start();
	}
	
	private CuratorFramework createWithOptions(String connectionString, RetryPolicy retryPolicy,
			int connectionTimeoutMs, int sessionTimeoutMs) {
		//默认创建的根节点是没有做权限控制的
		ACLProvider aclProvider = new ACLProvider() {
			private List<ACL> acl;
			@Override
			public List<ACL> getDefaultAcl() {
				if (acl == null) {
					List<ACL> acl = ZooDefs.Ids.CREATOR_ALL_ACL;
					acl.clear();
					acl.add(new ACL(Perms.ALL, new Id("auth", "admin:admin")));
					this.acl = acl;
				}
				return acl;
			}

			@Override
			public List<ACL> getAclForPath(String path) {
				return acl;
			}
		};
        byte[] auth = "admin:admin".getBytes();
        String namespace = "netty";
        return CuratorFrameworkFactory.builder().aclProvider(aclProvider).
        authorization("digest", auth).
        connectionTimeoutMs(connectionTimeoutMs).
        sessionTimeoutMs(sessionTimeoutMs).
        connectString(connectionString).
        namespace(namespace).
        retryPolicy(retryPolicy).build();		
	}
	
	@Override
	public void register(AbstractEndpoint endpoint) throws Throwable {
		String registerName = endpoint.getRegisterName();
		SocketAddress socketAddress = new InetSocketAddress(endpoint.getHost(), endpoint.getPort());
		try {
			if (endpoint instanceof NettyClient) {
				String zkPath = "/DISPATCHER/CONSUMER/"+registerName+socketAddress;
				if (zkClient.checkExists().forPath(zkPath) != null) {
					logout(endpoint);
				}
		        zkClient.create().creatingParentsIfNeeded().forPath(zkPath, serializer.serialize(socketAddress));
		        
				// 设置节点的cache
				@SuppressWarnings("resource")
				TreeCache treeCache = new TreeCache(zkClient, "/DISPATCHER/PROVIDER/"+registerName);
				TreeCacheListener listener = new ConsumerListener((NettyClient)endpoint);
				// 设置监听器和处理过程
				treeCache.getListenable().addListener(listener);
				// 开始监听
				treeCache.start();
		        
				LOGGER.info("" + endpoint.getHost() + ":" + endpoint.getPort() +"向" + getHost() + ":" + getPort() +"注册("+endpoint.getRegisterName()+")服务消费者成功");
			}
			if (endpoint instanceof NettyServer) {
				String zkPath = "/DISPATCHER/PROVIDER/"+registerName+socketAddress;
				if (zkClient.checkExists().forPath(zkPath) != null) {
					logout(endpoint);
				}
				zkClient.create().creatingParentsIfNeeded().forPath(zkPath, serializer.serialize(socketAddress));
				LOGGER.info("" + endpoint.getHost() + ":" + endpoint.getPort() +"向" + getHost() + ":" + getPort() +"注册("+endpoint.getRegisterName()+")服务提供者成功");
			}
		} catch (Throwable ex) {
			LOGGER.error("" + endpoint.getHost() + ":" + endpoint.getPort() +"向" + getHost() + ":" + getPort() +"注册("+endpoint.getRegisterName()+")失败");
			throw ex;
		}
		
	}

	@Override
	public void logout(AbstractEndpoint endpoint) throws Throwable {
		String registerName = endpoint.getRegisterName();
		SocketAddress socketAddress = new InetSocketAddress(endpoint.getHost(), endpoint.getPort());
		try {
			if (endpoint instanceof NettyClient) {
				zkClient.delete().forPath("/DISPATCHER/CONSUMER/"+registerName+socketAddress);
				LOGGER.info("" + endpoint.getHost() + ":" + endpoint.getPort() + "向" + getHost() + ":" + getPort() +"注销("+endpoint.getRegisterName()+")服务消费者成功");
			}
			if (endpoint instanceof NettyServer) {
				zkClient.delete().forPath("/DISPATCHER/PROVIDER/"+registerName+socketAddress);
				LOGGER.info("" + endpoint.getHost() + ":" + endpoint.getPort() + "向" + getHost() + ":" + getPort() +"注销("+endpoint.getRegisterName()+")服务提供者成功");
			}
		} catch (Throwable ex) {
			LOGGER.error("" + endpoint.getHost() + ":" + endpoint.getPort() + "向" + getHost() + ":" + getPort() +"注销("+endpoint.getRegisterName()+")失败");
			throw ex;
		}
		
	}

	@Override
	public List<SocketAddress> discover(AbstractEndpoint endpoint) throws Throwable {
		try {
			List<SocketAddress> listAddress = new ArrayList<SocketAddress>();
			List<String> list = zkClient.getChildren().forPath("/DISPATCHER/PROVIDER/"+endpoint.getRegisterName());
			for(String str : list) {
				String[] tmp = str.split(":");
				listAddress.add(new InetSocketAddress(tmp[0], Integer.parseInt(tmp[1])));
			}
			return listAddress;
		} catch (Throwable ex) {
			LOGGER.error("" + endpoint.getHost() + ":" + endpoint.getPort() + "向" + getHost() + ":" + getPort() +"查找("+endpoint.getRegisterName()+")失败");
			throw ex;
		}
	}
	
	class ConsumerListener implements TreeCacheListener {
		private NettyClient nettyClient;
		
		public ConsumerListener(NettyClient endpoint) {
			this.nettyClient = endpoint;
		}
		
		@Override
		public void childEvent(CuratorFramework zkClient, TreeCacheEvent zkEvent) throws Exception {
			ChildData data = zkEvent.getData();
			if (data != null && data.getData().length > 0) {
				SocketAddress socketAddress = (SocketAddress) serializer.deserialize(data.getData());
				LOGGER.info(zkEvent.getType() + data.getPath() + "  数据:" + socketAddress);
				switch (zkEvent.getType()) {
				case NODE_ADDED:
					//新增服务提供者节点时，客户端将连接新增的节点
					nettyClient.connect(socketAddress);
					break;
				default:
					break;
				}
			} else {
				LOGGER.info("zookeeper data is null : " + zkEvent.getType());
			}
		}
	}
}


