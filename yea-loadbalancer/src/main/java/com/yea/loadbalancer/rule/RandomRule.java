package com.yea.loadbalancer.rule;

import java.util.List;
import java.util.Random;

import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.ILoadBalancer;
import com.yea.loadbalancer.AbstractLoadBalancerRule;
import com.yea.loadbalancer.config.IClientConfig;

/**
 * 随机数规则
 * @author yiyongfei
 *
 */
public class RandomRule extends AbstractLoadBalancerRule {
    Random rand;

    public RandomRule() {
        rand = new Random();
    }

    /**
     * Randomly choose from all living servers
     */
    public BalancingNode choose(ILoadBalancer lb, Object key) {
        if (lb == null) {
            return null;
        }
        BalancingNode server = null;

        while (server == null) {
            if (Thread.interrupted()) {
                return null;
            }
            List<BalancingNode> upList = lb.getReachableNodes();
            List<BalancingNode> allList = lb.getAllNodes();

            int serverCount = allList.size();
            if (serverCount == 0) {
                /*
                 * No servers. End regardless of pass, because subsequent passes
                 * only get more restrictive.
                 */
                return null;
            }

            int index = rand.nextInt(serverCount);
            server = upList.get(index);

            if (server == null) {
                /*
                 * The only time this should happen is if the server list were
                 * somehow trimmed. This is a transient condition. Retry after
                 * yielding.
                 */
                Thread.yield();
                continue;
            }

            if (server.isAlive() && (!server.isSuspended())) {
                return (server);
            }

            // Shouldn't actually happen.. but must be transient or a bug.
            server = null;
            Thread.yield();
        }

        return server;

    }

	@Override
	public BalancingNode choose(Object key) {
		return choose(getLoadBalancer(), key);
	}

	@Override
	public void initWithNiwsConfig(IClientConfig clientConfig) {
		// TODO Auto-generated method stub
		
	}
}
