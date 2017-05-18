package com.yea.loadbalancer;

import com.yea.loadbalancer.config.IClientConfig;

public interface IVipAddressResolver {
    public String resolve(String vipAddress, IClientConfig niwsClientConfig);
}
