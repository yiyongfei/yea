package com.yea.loadbalancer;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yea.core.loadbalancer.BalancingNode;

/**
 * A noOp Loadbalancer
 * i.e. doesnt do anything "loadbalancer like"
 * 
 * @author stonse
 *
 */
public class NoOpLoadBalancer extends AbstractLoadBalancer {

    static final Logger  logger = LoggerFactory.getLogger(NoOpLoadBalancer.class);
    

	@Override
	public void addNode(BalancingNode newServer) {
		// TODO Auto-generated method stub
		logger.info("addServer to NoOpLoadBalancer ignored");
	}
	
    @Override
    public void addNodes(Collection<BalancingNode> newServers) {
        logger.info("addServers to NoOpLoadBalancer ignored");
    }

    @Override
    public BalancingNode chooseNode(Object key) {       
        return null;
    }

    @Override
    public LoadBalancerStats getLoadBalancerStats() {        
        return null;
    }

    
    @Override
    public List<BalancingNode> getNodeList(NodeGroup serverGroup) {     
        return Collections.emptyList();
    }

    @Override
    public void markNodeDown(BalancingNode server) {
        logger.info("markServerDown to NoOpLoadBalancer ignored");
    }

	@Override
	public List<BalancingNode> getNodeList(boolean availableOnly) {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public List<BalancingNode> getReachableNodes() {
        return null;

    }

    @Override
    public List<BalancingNode> getAllNodes() {
        return null;
    }

	@Override
	public Collection<BalancingNode> chooseNode(SocketAddress address) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean contains(SocketAddress address) {
		// TODO Auto-generated method stub
		return false;
	}

}
