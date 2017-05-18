package com.yea.loadbalancer;

import java.util.Iterator;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.yea.core.loadbalancer.BalancingNode;

public class CompositePredicate extends AbstractNodePredicate {

    private AbstractNodePredicate delegate;
    
    private List<AbstractNodePredicate> fallbacks = Lists.newArrayList();
        
    private int minimalFilteredServers = 1;
    
    private float minimalFilteredPercentage = 0;    
    
    @Override
    public boolean apply(PredicateKey input) {
        return delegate.apply(input);
    }

    
    public static class Builder {
        
        private CompositePredicate toBuild;
        
        Builder(AbstractNodePredicate primaryPredicate) {
            toBuild = new CompositePredicate();    
            toBuild.delegate = primaryPredicate;                    
        }

        Builder(AbstractNodePredicate ...primaryPredicates) {
            toBuild = new CompositePredicate();
            Predicate<PredicateKey> chain = Predicates.<PredicateKey>and(primaryPredicates);
            toBuild.delegate =  AbstractNodePredicate.ofKeyPredicate(chain);                
        }

        public Builder addFallbackPredicate(AbstractNodePredicate fallback) {
            toBuild.fallbacks.add(fallback);
            return this;
        }
                
        public Builder setFallbackThresholdAsMinimalFilteredNumberOfServers(int number) {
            toBuild.minimalFilteredServers = number;
            return this;
        }
        
        public Builder setFallbackThresholdAsMinimalFilteredPercentage(float percent) {
            toBuild.minimalFilteredPercentage = percent;
            return this;
        }
        
        public CompositePredicate build() {
            return toBuild;
        }
    }
    
    public static Builder withPredicates(AbstractNodePredicate ...primaryPredicates) {
        return new Builder(primaryPredicates);
    }

    public static Builder withPredicate(AbstractNodePredicate primaryPredicate) {
        return new Builder(primaryPredicate);
    }

    /**
     * Get the filtered servers from primary predicate, and if the number of the filtered servers
     * are not enough, trying the fallback predicates  
     */
    @Override
    public List<BalancingNode> getEligibleServers(List<BalancingNode> servers, Object loadBalancerKey) {
        List<BalancingNode> result = super.getEligibleServers(servers, loadBalancerKey);
        Iterator<AbstractNodePredicate> i = fallbacks.iterator();
        while (!(result.size() >= minimalFilteredServers && result.size() > (int) (servers.size() * minimalFilteredPercentage))
                && i.hasNext()) {
            AbstractNodePredicate predicate = i.next();
            result = predicate.getEligibleServers(servers, loadBalancerKey);
        }
        return result;
    }
}
