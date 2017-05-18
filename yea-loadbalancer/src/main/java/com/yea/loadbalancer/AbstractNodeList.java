package com.yea.loadbalancer;

import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.INodeList;
import com.yea.loadbalancer.config.CommonClientConfigKey;
import com.yea.loadbalancer.config.IClientConfig;

/**
 * The class includes an API to create a filter to be use by load balancer
 * to filter the servers returned from {@link #getUpdatedListOfNodes()} or {@link #getInitialListOfNodes()}.
 *
 */
public abstract class AbstractNodeList<T extends BalancingNode> implements INodeList<T>, IClientConfigAware {   
     
    
    /**
     * Get a ServerListFilter instance. It uses {@link ClientFactory#instantiateInstanceWithClientConfig(String, IClientConfig)}
     * which in turn uses reflection to initialize the filter instance. 
     * The filter class name is determined by the value of {@link CommonClientConfigKey#NIWSServerListFilterClassName}
     * in the {@link IClientConfig}. The default implementation is {@link ZoneAffinityServerListFilter}.
     */
    @SuppressWarnings({ "deprecation", "unchecked" })
	public AbstractNodeListFilter<T> getFilterImpl(IClientConfig niwsClientConfig) throws ClientException{
        try {
            String niwsServerListFilterClassName = niwsClientConfig
                    .getProperty(
                            CommonClientConfigKey.NIWSServerListFilterClassName,
                            ZoneAffinityServerListFilter.class.getName())
                    .toString();

            AbstractNodeListFilter<T> abstractNIWSServerListFilter = 
                    (AbstractNodeListFilter<T>) ClientFactory.instantiateInstanceWithClientConfig(niwsServerListFilterClassName, niwsClientConfig);
            return abstractNIWSServerListFilter;
        } catch (Throwable e) {
            throw new ClientException(
                    ClientException.ErrorType.CONFIGURATION,
                    "Unable to get an instance of CommonClientConfigKey.NIWSServerListFilterClassName. Configured class:"
                            + niwsClientConfig
                                    .getProperty(CommonClientConfigKey.NIWSServerListFilterClassName), e);
        }
    }
}
