package com.yea.loadbalancer;

import java.util.List;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.yea.core.loadbalancer.BalancingNode;
import com.yea.loadbalancer.config.CommonClientConfigKey;
import com.yea.loadbalancer.config.IClientConfig;

/**
 * Utility class that can load the List of Servers from a Configuration (i.e
 * properties available via Archaius). The property name be defined in this format:
 * 
 * <pre>{@code
<clientName>.<nameSpace>.listOfServers=<comma delimited hostname:port strings>
}</pre>
 * 
 * @author awang
 * 
 */
/**
 * 从配置信息里获取节点列表，如果不更改配置信息，负载均衡服务器列表会被刷新成配置信息里的服务列表（如果DynamicServerListLoadBalancer）
 * @author yiyongfei
 *
 */
public class ConfigurationBasedNodeList extends AbstractNodeList<BalancingNode>  {

	private IClientConfig clientConfig;
		
	@Override
	public List<BalancingNode> getInitialListOfNodes() {
	    return getUpdatedListOfNodes();
	}

	@Override
	public List<BalancingNode> getUpdatedListOfNodes() {
        String listOfServers = clientConfig.get(CommonClientConfigKey.ListOfServers);
        return derive(listOfServers);
	}

	@Override
	public void initWithNiwsConfig(IClientConfig clientConfig) {
	    this.clientConfig = clientConfig;
	}
	
	private List<BalancingNode> derive(String value) {
	    List<BalancingNode> list = Lists.newArrayList();
		if (!Strings.isNullOrEmpty(value)) {
			for (String s: value.split(",")) {
				list.add(new BalancingNode(s.trim()));
			}
		}
        return list;
	}
}
