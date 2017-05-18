package com.yea.loadbalancer;

import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.IPing;

public abstract class AbstractLoadBalancerPing implements IPing, IClientConfigAware{

    AbstractLoadBalancer lb;
    
    @Override
    public boolean isAlive(BalancingNode server) {
        return true;
    }
    
    public void setLoadBalancer(AbstractLoadBalancer lb){
        this.lb = lb;
    }
    
    public AbstractLoadBalancer getLoadBalancer(){
        return lb;
    }

}
