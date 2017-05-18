package com.yea.core.loadbalancer;

public interface IRule{
    /*
     * choose one alive server from lb.allServers or
     * lb.upServers according to key
     * 
     * @return choosen Server object. NULL is returned if none
     *  server is available 
     */

    public BalancingNode choose(Object key);
    
    public void setLoadBalancer(ILoadBalancer lb);
    
    public void nodeUpdate();
    
    public ILoadBalancer getLoadBalancer();    
}
