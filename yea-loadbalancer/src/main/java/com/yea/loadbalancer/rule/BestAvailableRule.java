package com.yea.loadbalancer.rule;

import java.util.List;

import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.ILoadBalancer;
import com.yea.loadbalancer.AbstractLoadBalancer;
import com.yea.loadbalancer.LoadBalancerStats;
import com.yea.loadbalancer.ServerStats;

/**
 * A rule that skips servers with "tripped" circuit breaker and picks the
 * server with lowest concurrent requests.
 * <p>
 * This rule should typically work with {@link ServerListSubsetFilter} which puts a limit on the 
 * servers that is visible to the rule. This ensure that it only needs to find the minimal 
 * concurrent requests among a small number of servers. Also, each client will get a random list of 
 * servers which avoids the problem that one server with the lowest concurrent requests is 
 * chosen by a large number of clients and immediately gets overwhelmed.
 * 
 * @author awang
 *
 */
/**
 * 最少连接规则
 * @author yiyongfei
 *
 */
public class BestAvailableRule extends ClientConfigEnabledRoundRobinRule {

    private LoadBalancerStats loadBalancerStats;
    
    @Override
    public BalancingNode choose(Object key) {
        if (loadBalancerStats == null) {
            return super.choose(key);
        }
        List<BalancingNode> serverList = getLoadBalancer().getAllNodes();
        int minimalConcurrentConnections = Integer.MAX_VALUE;
        long currentTime = System.currentTimeMillis();
        BalancingNode chosen = null;
        for (BalancingNode server: serverList) {
        	if (server.isAlive() && (!server.isSuspended())) {
        		ServerStats serverStats = loadBalancerStats.getSingleServerStat(server);
                if (!serverStats.isCircuitBreakerTripped(currentTime)) {
                    int concurrentConnections = serverStats.getActiveRequestsCount(currentTime);
                    if (concurrentConnections < minimalConcurrentConnections) {
                        minimalConcurrentConnections = concurrentConnections;
                        chosen = server;
                    }
                }
            }
        }
        if (chosen == null) {
            return super.choose(key);
        } else {
            return chosen;
        }
    }

    @Override
    public void setLoadBalancer(ILoadBalancer lb) {
        super.setLoadBalancer(lb);
        if (lb instanceof AbstractLoadBalancer) {
            loadBalancerStats = ((AbstractLoadBalancer) lb).getLoadBalancerStats();            
        }
    }
    
    

}
