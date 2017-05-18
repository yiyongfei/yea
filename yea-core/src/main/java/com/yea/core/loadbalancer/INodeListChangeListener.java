package com.yea.core.loadbalancer;

import java.util.Collection;

public interface INodeListChangeListener {
    public void nodeListChanged(Collection<BalancingNode> oldList, Collection<BalancingNode> newList);
}
