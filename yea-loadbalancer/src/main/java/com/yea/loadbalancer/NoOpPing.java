package com.yea.loadbalancer;

import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.IPing;

/**
 * No Op Ping
 * @author stonse
 *
 */
public class NoOpPing implements IPing {

    @Override
    public boolean isAlive(BalancingNode server) {
        return true;
    }

}
