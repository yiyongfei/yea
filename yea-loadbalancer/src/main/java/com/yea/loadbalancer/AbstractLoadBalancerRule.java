package com.yea.loadbalancer;

import com.yea.core.loadbalancer.ILoadBalancer;
import com.yea.core.loadbalancer.IRule;

public abstract class AbstractLoadBalancerRule implements IRule, IClientConfigAware {

	private ILoadBalancer lb;

	@Override
	public void setLoadBalancer(ILoadBalancer lb) {
		this.lb = lb;
	}

	@Override
	public ILoadBalancer getLoadBalancer() {
		return lb;
	}
	
	@Override
	public void nodeUpdate() {
		
	}
}
