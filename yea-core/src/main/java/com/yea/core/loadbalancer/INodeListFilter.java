package com.yea.core.loadbalancer;

import java.util.Collection;

/**
 * This interface allows for filtering the configured or dynamically obtained
 * List of candidate servers with desirable characteristics.
 * 
 * @author stonse
 * 
 * @param <T>
 */
public interface INodeListFilter<T extends BalancingNode> {

    public Collection<T> getFilteredListOfNodes(Collection<T> nodes);

}
