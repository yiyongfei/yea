package com.yea.loadbalancer;

import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.INodeListFilter;

/**
 * Class that is responsible to Filter out list of servers from the ones 
 * currently available in the Load Balancer
 * @author stonse
 *
 * @param <T>
 */
public abstract class AbstractNodeListFilter<T extends BalancingNode> implements INodeListFilter<T> {

    private volatile LoadBalancerStats stats;
    
    public void setLoadBalancerStats(LoadBalancerStats stats) {
        this.stats = stats;
    }
    
    public LoadBalancerStats getLoadBalancerStats() {
        return stats;
    }

}
