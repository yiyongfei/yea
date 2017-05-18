package com.yea.loadbalancer;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DeploymentContext.ContextKey;
import com.yea.core.loadbalancer.BalancingNode;

/**
 * A predicate the filters out servers that are not in the same zone as the client's current
 * zone. The current zone is determined from the call
 * 
 * <pre>{@code
 * ConfigurationManager.getDeploymentContext().getValue(ContextKey.zone);
 * }</pre>
 * 
 * @author awang
 *
 */
public class ZoneAffinityPredicate extends AbstractNodePredicate {

    private final String zone = ConfigurationManager.getDeploymentContext().getValue(ContextKey.zone);
    
    public ZoneAffinityPredicate() {        
    }

    @Override
    public boolean apply(PredicateKey input) {
        BalancingNode s = input.getServer();
        String az = s.getZone();
        if (az != null && zone != null && az.toLowerCase().equals(zone.toLowerCase())) {
            return true;
        } else {
            return false;
        }
    }
}
