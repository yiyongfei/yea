package com.yea.loadbalancer;

import java.util.Collection;

import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.ILoadBalancer;
import com.yea.core.loadbalancer.INodeList;
import com.yea.core.loadbalancer.INodeListFilter;
import com.yea.core.loadbalancer.IPing;
import com.yea.core.loadbalancer.IRule;
import com.yea.loadbalancer.config.CommonClientConfigKey;
import com.yea.loadbalancer.config.DefaultClientConfigImpl;
import com.yea.loadbalancer.config.IClientConfig;
import com.yea.loadbalancer.config.IClientConfigKey;

/**
 * 负载均衡构建器，一般如果是固定服务器可以调用buildFixedServerListLoadBalancer来生成，
 * 如果服务器是动态的可以调用buildDynamicServerListLoadBalancer来生成。
 * @author yiyongfei
 *
 * @param <T>
 */
public class LoadBalancerBuilder<T extends BalancingNode> {
    
    private IClientConfig config = DefaultClientConfigImpl.getClientConfigWithDefaultValues();
    private INodeListFilter serverListFilter;
    private IRule rule;
    private IPing ping = new DummyPing();
    private INodeList serverListImpl;
    
    
    private LoadBalancerBuilder() {
    }
    
    public static <T extends BalancingNode> LoadBalancerBuilder<T> newBuilder() {
        return new LoadBalancerBuilder<T>();
    }
    
    public LoadBalancerBuilder<T> withClientConfig(IClientConfig config) {
        this.config = config;
        return this;
    }

    public LoadBalancerBuilder<T> withRule(IRule rule) {
        this.rule = rule;
        return this;
    }
    
    public LoadBalancerBuilder<T> withPing(IPing ping) {
        this.ping = ping;
        return this;
    }
    
    public LoadBalancerBuilder<T> withDynamicServerList(INodeList<T> serverListImpl) {
        this.serverListImpl = serverListImpl;
        return this;
    }
    
    public LoadBalancerBuilder<T> withServerListFilter(INodeListFilter<T> serverListFilter) {
        this.serverListFilter = serverListFilter;
        return this;
    }

    public BaseLoadBalancer buildFixedServerListLoadBalancer(Collection<T> servers) {
        if (rule == null) {
            rule = createRuleFromConfig(config);
        }
        BaseLoadBalancer lb = new BaseLoadBalancer(config, rule, ping);
        lb.setNodesList(servers);
        return lb;
    }
    
    private static IRule createRuleFromConfig(IClientConfig config) {
        String ruleClassName = config.get(IClientConfigKey.Keys.NFLoadBalancerRuleClassName);
        if (ruleClassName == null) {
            throw new IllegalArgumentException("NFLoadBalancerRuleClassName is not specified in the config");
        }
        IRule rule;
        try {
            rule = (IRule) ClientFactory.instantiateInstanceWithClientConfig(ruleClassName, config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return rule;
    }
    
    private static INodeList<BalancingNode> createServerListFromConfig(IClientConfig config) {
        String serverListClassName = config.get(IClientConfigKey.Keys.NIWSServerListClassName);
        if (serverListClassName == null) {
            throw new IllegalArgumentException("NIWSServerListClassName is not specified in the config");
        }
        INodeList<BalancingNode> list;
        try {
            list = (INodeList<BalancingNode>) ClientFactory.instantiateInstanceWithClientConfig(serverListClassName, config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }
    
    /**
     * Build a {@link DynamicServerListLoadBalancer} with a dynamic {@link INodeList} and an {@link IRule}. The {@link INodeList} can be
     * either set in the {@link #withDynamicServerList(INodeList)} or in the {@link IClientConfig} using {@link CommonClientConfigKey#NIWSServerListClassName}.
     * The {@link IRule} can be either set by {@link #withRule(IRule)} or in the {@link IClientConfig} using
     * {@link CommonClientConfigKey#NFLoadBalancerRuleClassName}. 
     */
    public DynamicServerListLoadBalancer<T> buildDynamicServerListLoadBalancer() {
        if (serverListImpl == null) {
            serverListImpl = createServerListFromConfig(config);
        }
        if (rule == null) {
            rule = createRuleFromConfig(config);
        }
        return new DynamicServerListLoadBalancer<T>(config, rule, ping, serverListImpl, serverListFilter);
    }
    
    /**
     * Build a {@link ZoneAwareLoadBalancer} with a dynamic {@link INodeList} and an {@link IRule}. The {@link INodeList} can be
     * either set in the {@link #withDynamicServerList(INodeList)} or in the {@link IClientConfig} using {@link CommonClientConfigKey#NIWSServerListClassName}.
     * The {@link IRule} can be either set by {@link #withRule(IRule)} or in the {@link IClientConfig} using
     * {@link CommonClientConfigKey#NFLoadBalancerRuleClassName}. 
     */
    public ZoneAwareLoadBalancer<T> buildZoneAwareLoadBalancer() {
        if (serverListImpl == null) {
            serverListImpl = createServerListFromConfig(config);
        }
        if (rule == null) {
            rule = createRuleFromConfig(config);
        }
        return new ZoneAwareLoadBalancer<T>(config, rule, ping, serverListImpl, serverListFilter);
    }
    
    /**
     * Build a load balancer using the configuration from the {@link IClientConfig} only. It uses reflection to initialize necessary load balancer
     * components. 
     */
    public ILoadBalancer buildLoadBalancerFromConfigWithReflection() {
        String loadBalancerClassName = config.get(CommonClientConfigKey.NFLoadBalancerClassName);
        if (loadBalancerClassName == null) {
            throw new IllegalArgumentException("NFLoadBalancerClassName is not specified in the IClientConfig");
        }
        ILoadBalancer lb;
        try {
            lb = (ILoadBalancer) ClientFactory.instantiateInstanceWithClientConfig(loadBalancerClassName, config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return lb;
    }
}
