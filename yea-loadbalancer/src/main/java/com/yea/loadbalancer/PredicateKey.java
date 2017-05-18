package com.yea.loadbalancer;

import com.yea.core.loadbalancer.BalancingNode;

public class PredicateKey {
    private Object loadBalancerKey;
    private BalancingNode server;
    
    public PredicateKey(Object loadBalancerKey, BalancingNode server) {
        this.loadBalancerKey = loadBalancerKey;
        this.server = server;
    }

    public PredicateKey(BalancingNode server) {
        this(null, server);
    }
    
    public final Object getLoadBalancerKey() {
        return loadBalancerKey;
    }
    
    public final BalancingNode getServer() {
        return server;
    }        
}
