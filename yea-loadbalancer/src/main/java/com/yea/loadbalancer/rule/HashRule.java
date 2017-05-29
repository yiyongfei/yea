package com.yea.loadbalancer.rule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yea.core.base.id.UUIDGenerator;
import com.yea.core.hash.DefaultHashAlgorithm;
import com.yea.core.hash.HashAlgorithm;
import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.ILoadBalancer;
import com.yea.loadbalancer.AbstractLoadBalancerRule;
import com.yea.loadbalancer.BaseLoadBalancer;
import com.yea.loadbalancer.config.IClientConfig;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 一致性哈希规则
 * @author yiyongfei
 *
 */
public class HashRule extends AbstractLoadBalancerRule {
	private static final Logger logger = LoggerFactory.getLogger(HashRule.class);
	private Integer repetitions = 256;
	private HashAlgorithm hashAlg = DefaultHashAlgorithm.KETAMA_HASH;
	String name = "unknown";
	
	public HashRule() {
		super();
	}

	public HashRule(ILoadBalancer lb) {
		this();
		setLoadBalancer(lb);
	}

	public HashRule setHashAlgorithm(HashAlgorithm hashAlgorithm) {
		this.hashAlg = hashAlgorithm;
		return this;
	}

	public HashRule setRepetitions(Integer repetitions) {
		this.repetitions = repetitions;
		return this;
	}

	@Override
	public void setLoadBalancer(ILoadBalancer lb) {
		super.setLoadBalancer(lb);
		if (lb instanceof BaseLoadBalancer) {
			name = ((BaseLoadBalancer) lb).getName();
		}
		initialize(lb);
	}

	void initialize(ILoadBalancer lb) {
		buildKetamaNodes();
	}
	
	protected AtomicBoolean updateNodeInProgress = new AtomicBoolean(false);
	
	@Override
	public void nodeUpdate() {
		if (!updateNodeInProgress.compareAndSet(false, true)) {
			return;
		}
		try {
			buildKetamaNodes();
		} finally {
			updateNodeInProgress.set(false);
		}
	}

	protected BalancingNode choose(ILoadBalancer lb, Object key) {
		if (lb == null) {
			return null;
		}
		long basedate = new Date().getTime();
		BalancingNode server = null;
		while (server == null) {
			if (Thread.interrupted()) {
				return null;
			}
			Collection<BalancingNode> allServerList = getNodes();
			int serverCount = allServerList.size();
			if (serverCount == 0) {
				return null;
			} else {
				if (hashNodes.isEmpty()) {
					buildKetamaNodes();
				}
			}

			String loadbalancerKey = "";
			if (key instanceof String) {
				loadbalancerKey = key.toString();
			} else if (key instanceof byte[]) {
				try {
					loadbalancerKey = new String((byte[]) key, "ISO_8859_1");
				} catch (UnsupportedEncodingException e) {
				}
			} else {
				loadbalancerKey = key.toString();
			}

			try {
				server = getPrimary(hashAlg.hash(loadbalancerKey));
			} catch (Exception ex) {
				logger.error("获取Node时出现异常，异常内容如下！", ex);
			}

			if (server == null || (!allServerList.contains(server)) || (!server.isAlive() || server.isSuspended())) {
				Thread.yield();
				server = null;
				key = UUIDGenerator.generate();
				try {
					continue;
				} finally {
					if ((new Date().getTime() - basedate) > 100) {
						logger.warn("获取Node已用时:" + (new Date().getTime() - basedate) + ",失败原因可能:"
								+ (server == null ? "没有Node"
										: "Node_" + server.getHostPort() + "状态有误[是否有效:" + server.isAlive() + ",临时失效:"
												+ server.isSuspended() + "],或不在节点列表["
												+ (!allServerList.contains(server)) + "]"));
						return null;
					}
				}
			}
			
			return server;
		}
		return server;
	}

	protected ConcurrentSkipListMap<Long, BalancingNode> hashNodes;
	private AtomicBoolean buildNodesInProgress = new AtomicBoolean(false);
	/**
	 * Setup the KetamaNodeLocator with the list of nodes it should use.
	 *
	 * @param nodes
	 *            a List of MemcachedNodes for this KetamaNodeLocator to use in
	 *            its continuum
	 */
	void buildKetamaNodes() {
		if (!buildNodesInProgress.compareAndSet(false, true)) {
			return;
		}
		logger.info("开始重构Ketama节点,节点数:{},各节点权重:{}", getNodes().size(), getWeights());
		try {
			BalancingNode[] tmpNodes = getNodes().toArray(new BalancingNode[0]);
			ConcurrentSkipListMap<Long, BalancingNode> nodeMap = new ConcurrentSkipListMap<Long, BalancingNode>();
			int numReps = 0;
			for (BalancingNode node : tmpNodes) {
				if (getWeights().isEmpty() || !getWeights().containsKey(node)) {
					numReps = repetitions;
				} else {
					numReps = (int) Math.floor((repetitions * getWeights().get(node)) / 4) * 4;
				}
				if (hashAlg == DefaultHashAlgorithm.KETAMA_HASH) {
					for (int i = 0; i < numReps / 4; i++) {
						byte[] digest = DefaultHashAlgorithm.computeMd5(node.getHostPort() + "-" + i);
						for (int h = 0; h < 4; h++) {
							Long k = ((long) (digest[3 + h * 4] & 0xFF) << 24) | ((long) (digest[2 + h * 4] & 0xFF) << 16)
									| ((long) (digest[1 + h * 4] & 0xFF) << 8) | (digest[h * 4] & 0xFF);
							nodeMap.put(k, (BalancingNode) node);
						}
					}
				} else {
					for (int i = 0; i < numReps; i++) {
						nodeMap.put(hashAlg.hash(node.getHostPort() + "-" + i), (BalancingNode) node);
					}
				}
			}
			hashNodes = nodeMap;
		} catch (Exception e) {
			logger.error("重构Ketama节点时出现异常", e);
		} finally {
			buildNodesInProgress.set(false);
		}
	}

	BalancingNode getPrimary(long hash) {
		if (hashNodes.isEmpty()) {
			return null;
		}
		if (!hashNodes.containsKey(hash)) {
			// Java 1.6 adds a ceilingKey method, but I'm still stuck in 1.5
			// in a lot of places, so I'm doing this myself.
			SortedMap<Long, BalancingNode> tailMap = hashNodes.tailMap(hash);
			if (tailMap.isEmpty()) {
				hash = hashNodes.firstKey();
			} else {
				hash = tailMap.firstKey();
			}
		}
		return hashNodes.get(hash);
	}

	Collection<BalancingNode> getNodes() {
		return getLoadBalancer().getAllNodes();
	}

	Map<BalancingNode, Double> getWeights() {
		return new ConcurrentSkipListMap<BalancingNode, Double>();
	}
	
	@Override
	public void initWithNiwsConfig(IClientConfig clientConfig) {
		
	}

	@Override
	public BalancingNode choose(Object key) {
		return choose(getLoadBalancer(), key);
	}

}
