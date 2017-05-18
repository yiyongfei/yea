package com.yea.core.loadbalancer;

import java.util.Collection;

/**
 * Interface that defines the methods sed to obtain the List of Servers
 * @author stonse
 *
 * @param <T>
 */
public interface INodeList<T extends BalancingNode> {

    public Collection<T> getInitialListOfNodes();
    
    /**
     * Return updated list of servers. This is called say every 30 secs
     * (configurable) by the Loadbalancer's Ping cycle
     * 
     */
    public Collection<T> getUpdatedListOfNodes();   

}
