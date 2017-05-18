package com.yea.core.loadbalancer;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;

public interface ILoadBalancer {

	public void addNode(BalancingNode newNode);
	/**
	 * Initial list of servers.
	 * This API also serves to add additional ones at a later time
	 * The same logical server (host:port) could essentially be added multiple times
	 * (helpful in cases where you want to give more "weightage" perhaps ..)
	 * 
	 * @param newServers new servers to add
	 */
	public void addNodes(Collection<BalancingNode> newNodes);
	
	/**
	 * Choose a server from load balancer.
	 * 
	 * @param key An object that the load balancer may use to determine which server to return. null if 
	 *         the load balancer does not use this parameter.
	 * @return server chosen
	 */
	public BalancingNode chooseNode(Object key);
	
	public Collection<BalancingNode> chooseNode(SocketAddress address) ;
	
    public boolean contains(SocketAddress address) ;
    
	/**
	 * To be called by the clients of the load balancer to notify that a Server is down
	 * else, the LB will think its still Alive until the next Ping cycle - potentially
	 * (assuming that the LB Impl does a ping)
	 * 
	 * @param server Server to mark as down
	 */
	public void markNodeDown(BalancingNode node);
	
	/**
	 * @deprecated 2016-01-20 This method is deprecated in favor of the
	 * cleaner {@link #getReachableServers} (equivalent to availableOnly=true)
	 * and {@link #getAllServers} API (equivalent to availableOnly=false).
	 *
	 * Get the current list of servers.
	 *
	 * @param availableOnly if true, only live and available servers should be returned
	 */
	@Deprecated
	public List<BalancingNode> getNodeList(boolean availableOnly);

	/**
	 * @return Only the servers that are up and reachable.
     */
    public List<BalancingNode> getReachableNodes();

    /**
     * @return All known servers, both reachable and unreachable.
     */
	public List<BalancingNode> getAllNodes();
}
