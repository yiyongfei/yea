package com.yea.core.loadbalancer;

public interface IPingStrategy {

    boolean[] pingNodes(IPing ping, BalancingNode[] nodes);
}
