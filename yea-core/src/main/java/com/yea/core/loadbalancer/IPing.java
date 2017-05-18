package com.yea.core.loadbalancer;

public interface IPing {
    
    public boolean isAlive(BalancingNode node);
}
