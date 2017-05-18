package com.yea.loadbalancer;

import com.yea.core.loadbalancer.BalancingNode;
import com.yea.loadbalancer.config.IClientConfig;

public class DummyPing extends AbstractLoadBalancerPing {

    public DummyPing() {
    }

    public boolean isAlive(BalancingNode server) {
        return true;
    }

    @Override
    public void initWithNiwsConfig(IClientConfig clientConfig) {
    }
}
