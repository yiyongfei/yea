package com.yea.loadbalancer;

import com.yea.core.loadbalancer.BalancingNode;
import com.yea.loadbalancer.config.IClientConfig;

/**
 * 增加：通过Socket连接远程服务器，看连通性
 * 
 * @author yiyongfei
 *
 */
public class NettyPing extends AbstractLoadBalancerPing {
	private int timeout = 10 * 1000;//Ping超时时间，默认10秒

	public NettyPing() {
		super();
	}

	public boolean isAlive(BalancingNode server) {
		try {
			server.ping(timeout);
			return true;
		} catch (Throwable e) {
			/* 不能连接，返回false */
			return false;
		}
	}

	@Override
	public void initWithNiwsConfig(IClientConfig clientConfig) {
		// TODO Auto-generated method stub
	}
}
