package com.yea.loadbalancer.rule;

import java.util.List;

import com.google.common.collect.Collections2;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.ILoadBalancer;
import com.yea.loadbalancer.AbstractNodePredicate;
import com.yea.loadbalancer.AvailabilityPredicate;
import com.yea.loadbalancer.CompositePredicate;
import com.yea.loadbalancer.PredicateKey;
import com.yea.loadbalancer.config.IClientConfig;

/**
 * A load balancer rule that filters out servers that:
 * <ul>
 * <li> are in circuit breaker tripped state due to consecutive connection or read failures, or</li>
 * <li> have active connections that exceeds a configurable limit (default is Integer.MAX_VALUE).</li>
 * </ul>
 * The property
 * to change this limit is 
 * <pre>{@code
 * 
 * <clientName>.<nameSpace>.ActiveConnectionsLimit
 * 
 * }</pre>
 *
 * <p>
 *   
 * @author awang
 *
 */
public class AvailabilityFilteringRule extends PredicateBasedRule {    

    private AbstractNodePredicate predicate;
    
    public AvailabilityFilteringRule() {
    	super();
    	predicate = CompositePredicate.withPredicate(new AvailabilityPredicate(this, null))
                .addFallbackPredicate(AbstractNodePredicate.alwaysTrue())
                .build();
    }
    
    
    @Override
    public void initWithNiwsConfig(IClientConfig clientConfig) {
    	predicate = CompositePredicate.withPredicate(new AvailabilityPredicate(this, clientConfig))
    	            .addFallbackPredicate(AbstractNodePredicate.alwaysTrue())
    	            .build();
    }

    @Monitor(name="AvailableServersCount", type = DataSourceType.GAUGE)
    public int getAvailableServersCount() {
    	ILoadBalancer lb = getLoadBalancer();
    	List<BalancingNode> servers = lb.getAllNodes();
    	if (servers == null) {
    		return 0;
    	}
    	return Collections2.filter(servers, predicate.getServerOnlyPredicate()).size();
    }


    /**
     * This method is overridden to provide a more efficient implementation which does not iterate through
     * all servers. This is under the assumption that in most cases, there are more available instances 
     * than not. 
     */
    @Override
    public BalancingNode choose(Object key) {
        int count = 0;
        BalancingNode server = roundRobinRule.choose(key);
        while (count++ <= 10) {
            if (predicate.apply(new PredicateKey(server))) {
                return server;
            }
            server = roundRobinRule.choose(key);
        }
        return super.choose(key);
    }

    @Override
    public AbstractNodePredicate getPredicate() {
        return predicate;
    }
}
