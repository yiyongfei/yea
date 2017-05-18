package com.yea.loadbalancer.rule;

import com.google.common.base.Optional;
import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.ILoadBalancer;
import com.yea.loadbalancer.AbstractNodePredicate;

public abstract class PredicateBasedRule extends ClientConfigEnabledRoundRobinRule {
   
    /**
     * Method that provides an instance of {@link AbstractNodePredicate} to be used by this class.
     * 
     */
    public abstract AbstractNodePredicate getPredicate();
        
    /**
     * Get a server by calling {@link AbstractNodePredicate#chooseRandomlyAfterFiltering(java.util.List, Object)}.
     * The performance for this method is O(n) where n is number of servers to be filtered.
     */
    @Override
    public BalancingNode choose(Object key) {
        ILoadBalancer lb = getLoadBalancer();
        Optional<BalancingNode> server = getPredicate().chooseRoundRobinAfterFiltering(lb.getAllNodes(), key);
        if (server.isPresent()) {
            return server.get();
        } else {
            return null;
        }       
    }
}
