package com.yea.loadbalancer;

import com.yea.loadbalancer.config.IClientConfig;

public interface IClientConfigAware {

    /**
     * Concrete implementation should implement this method so that the configuration set via 
     * {@link IClientConfig} (which in turn were set via Archaius properties) will be taken into consideration
     *
     * @param clientConfig
     */
    public abstract void initWithNiwsConfig(IClientConfig clientConfig);
    
}
