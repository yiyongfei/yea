package com.yea.loadbalancer;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.ILoadBalancer;
import com.yea.core.loadbalancer.IRule;
import com.yea.loadbalancer.config.IClientConfig;

/**
 * A basic building block for server filtering logic which can be used in rules and server list filters.
 * The input object of the predicate is {@link PredicateKey}, which has Server and load balancer key
 * information. Therefore, it is possible to develop logic to filter servers by both Server and load balancer
 * key or either one of them. 
 * 
 * @author awang
 *
 */
public abstract class AbstractNodePredicate implements Predicate<PredicateKey> {
    
    protected IRule rule;
    private volatile LoadBalancerStats lbStats;
    
    private final Random random = new Random();
    
    private final AtomicInteger nextIndex = new AtomicInteger();
            
    private final Predicate<BalancingNode> serverOnlyPredicate =  new Predicate<BalancingNode>() {
        @Override
        public boolean apply(BalancingNode input) {                    
            return AbstractNodePredicate.this.apply(new PredicateKey(input));
        }
    };

    public static AbstractNodePredicate alwaysTrue() { 
        return new AbstractNodePredicate() {        
            @Override
            public boolean apply(PredicateKey input) {
                return true;
            }
        };
    }

    public AbstractNodePredicate() {
        
    }
    
    public AbstractNodePredicate(IRule rule) {
        this.rule = rule;
    }
    
    public AbstractNodePredicate(IRule rule, IClientConfig clientConfig) {
        this.rule = rule;
    }
    
    public AbstractNodePredicate(LoadBalancerStats lbStats, IClientConfig clientConfig) {
        this.lbStats = lbStats;
    }
    
    protected LoadBalancerStats getLBStats() {
        if (lbStats != null) {
            return lbStats;
        } else if (rule != null) {
            ILoadBalancer lb = rule.getLoadBalancer();
            if (lb instanceof AbstractLoadBalancer) {
                LoadBalancerStats stats =  ((AbstractLoadBalancer) lb).getLoadBalancerStats();
                setLoadBalancerStats(stats);
                return stats;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
    
    public void setLoadBalancerStats(LoadBalancerStats stats) {
        this.lbStats = stats;
    }
    
    /**
     * Get the predicate to filter list of servers. The load balancer key is treated as null
     * as the input of this predicate.
     */
    public Predicate<BalancingNode> getServerOnlyPredicate() {
        return serverOnlyPredicate;
    }
    
    /**
     * Get servers filtered by this predicate from list of servers. Load balancer key
     * is presumed to be null. 
     * 
     * @see #getEligibleServers(List, Object)
     * 
     */
    public List<BalancingNode> getEligibleServers(List<BalancingNode> servers) {
        return getEligibleServers(servers, null);
    }
 
    /**
     * Get servers filtered by this predicate from list of servers. 
     */
    public List<BalancingNode> getEligibleServers(List<BalancingNode> servers, Object loadBalancerKey) {
        if (loadBalancerKey == null) {
            return ImmutableList.copyOf(Iterables.filter(servers, this.getServerOnlyPredicate()));            
        } else {
            List<BalancingNode> results = Lists.newArrayList();
            for (BalancingNode server: servers) {
                if (this.apply(new PredicateKey(loadBalancerKey, server))) {
                    results.add(server);
                }
            }
            return results;            
        }
    }
    
    /**
     * Choose a random server after the predicate filters a list of servers. Load balancer key 
     * is presumed to be null.
     *  
     */
    public Optional<BalancingNode> chooseRandomlyAfterFiltering(List<BalancingNode> servers) {
        List<BalancingNode> eligible = getEligibleServers(servers);
        if (eligible.size() == 0) {
            return Optional.absent();
        }
        return Optional.of(eligible.get(random.nextInt(eligible.size())));
    }
    
    /**
     * Choose a server in a round robin fashion after the predicate filters a list of servers. Load balancer key 
     * is presumed to be null.
     */
    public Optional<BalancingNode> chooseRoundRobinAfterFiltering(List<BalancingNode> servers) {
        List<BalancingNode> eligible = getEligibleServers(servers);
        if (eligible.size() == 0) {
            return Optional.absent();
        }
        return Optional.of(eligible.get(nextIndex.getAndIncrement() % eligible.size()));
    }
    
    /**
     * Choose a random server after the predicate filters list of servers given list of servers and
     * load balancer key. 
     *  
     */
    public Optional<BalancingNode> chooseRandomlyAfterFiltering(List<BalancingNode> servers, Object loadBalancerKey) {
        List<BalancingNode> eligible = getEligibleServers(servers, loadBalancerKey);
        if (eligible.size() == 0) {
            return Optional.absent();
        }
        return Optional.of(eligible.get(random.nextInt(eligible.size())));
    }
    
    /**
     * Choose a server in a round robin fashion after the predicate filters a given list of servers and load balancer key. 
     */
    public Optional<BalancingNode> chooseRoundRobinAfterFiltering(List<BalancingNode> servers, Object loadBalancerKey) {
        List<BalancingNode> eligible = getEligibleServers(servers, loadBalancerKey);
        if (eligible.size() == 0) {
            return Optional.absent();
        }
        return Optional.of(eligible.get(nextIndex.getAndIncrement() % eligible.size()));
    }
        
    /**
     * Create an instance from a predicate.
     */
    public static AbstractNodePredicate ofKeyPredicate(final Predicate<PredicateKey> p) {
        return new AbstractNodePredicate() {
            @Override
            public boolean apply(PredicateKey input) {
                return p.apply(input);
            }            
        };        
    }
    
    /**
     * Create an instance from a predicate.
     */
    public static AbstractNodePredicate ofServerPredicate(final Predicate<BalancingNode> p) {
        return new AbstractNodePredicate() {
            @Override
            public boolean apply(PredicateKey input) {
                return p.apply(input.getServer());
            }            
        };        
    }
}
