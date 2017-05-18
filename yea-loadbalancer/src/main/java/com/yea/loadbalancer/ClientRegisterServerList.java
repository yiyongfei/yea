package com.yea.loadbalancer;

import java.util.Collection;

import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.remote.client.ClientRegister;
import com.yea.loadbalancer.config.IClientConfig;

/**
 * 增加：从ClientRegister获取最新的服务器列表
 * @author yiyongfei
 *
 */
public class ClientRegisterServerList extends AbstractNodeList<BalancingNode> {

	private IClientConfig clientConfig;
	private String registerName;
	@Override
	public Collection<BalancingNode> getInitialListOfNodes() {
		return getUpdatedListOfNodes();
	}

	@Override
	public Collection<BalancingNode> getUpdatedListOfNodes() {
		return ClientRegister.getInstance().getAllBalancingNode(this.registerName);
	}

	@Override
	public void initWithNiwsConfig(IClientConfig clientConfig) {
		this.clientConfig = clientConfig;
		this.registerName = this.clientConfig.getClientName();
	}

}
