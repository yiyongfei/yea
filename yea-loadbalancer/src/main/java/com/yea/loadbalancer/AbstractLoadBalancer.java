package com.yea.loadbalancer;

import java.util.Collection;

import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.ILoadBalancer;

public abstract class AbstractLoadBalancer implements ILoadBalancer {
    
    public enum NodeGroup{
        ALL,
        STATUS_UP,
        STATUS_NOT_UP        
    }
        
    /**
     * delegate to {@link #chooseNode(Object)} with parameter null.
     */
    public BalancingNode chooseNode() {
    	return chooseNode("");
    }

    
    /**
     * List of servers that this Loadbalancer knows about
     * 
     * @param serverGroup Servers grouped by status, e.g., {@link NodeGroup#STATUS_UP}
     */
    public abstract Collection<BalancingNode> getNodeList(NodeGroup nodeGroup);
    
    /**
     * Obtain LoadBalancer related Statistics
     */
    public abstract LoadBalancerStats getLoadBalancerStats();    
}
