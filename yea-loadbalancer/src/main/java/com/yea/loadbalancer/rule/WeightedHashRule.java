package com.yea.loadbalancer.rule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yea.core.base.id.UUIDGenerator;
import com.yea.core.hash.DefaultHashAlgorithm;
import com.yea.core.hash.HashAlgorithm;
import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.ILoadBalancer;
import com.yea.loadbalancer.AbstractLoadBalancer;
import com.yea.loadbalancer.AbstractLoadBalancerRule;
import com.yea.loadbalancer.BaseLoadBalancer;
import com.yea.loadbalancer.LoadBalancerStats;
import com.yea.loadbalancer.ServerComparator;
import com.yea.loadbalancer.ServerStats;
import com.yea.loadbalancer.config.IClientConfig;
import com.yea.loadbalancer.config.IClientConfigKey;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 一致性哈希规则（与响应时间权重相关）
 * 响应时间的权重计算：
 * 所有服务器平均响应时间作为基准，每个服务器平均响应时间与基准比较，平均响应时间越少，权重越大，服务器选中的可能性就越高。
 * (基准时间/单机平均响应时间)
 * @author yiyongfei
 *
 */
public class WeightedHashRule extends AbstractLoadBalancerRule {

	public static final IClientConfigKey<Integer> WEIGHT_TASK_TIMER_INTERVAL_CONFIG_KEY = new IClientConfigKey<Integer>() {
		@Override
		public String key() {
			return "ServerWeightTaskTimerInterval";
		}

		@Override
		public String toString() {
			return key();
		}

		@Override
		public Class<Integer> type() {
			return Integer.class;
		}
	};

	public static final int DEFAULT_TIMER_INTERVAL = 30 * 1000;

	private int serverWeightTaskTimerInterval = DEFAULT_TIMER_INTERVAL;

	private static final Logger logger = LoggerFactory.getLogger(WeightedHashRule.class);

	private volatile Map<BalancingNode, Double> accumulatedWeights = new ConcurrentSkipListMap<BalancingNode, Double>();

	protected Timer serverWeightTimer = null;

	protected AtomicBoolean serverWeightAssignmentInProgress = new AtomicBoolean(false);

	String name = "unknown";

	public WeightedHashRule() {
		super();
	}

	public WeightedHashRule(ILoadBalancer lb) {
		this();
		setLoadBalancer(lb);
	}

	public WeightedHashRule setHashAlgorithm(HashAlgorithm hashAlgorithm) {
		this.hashAlg = hashAlgorithm;
		return this;
	}

	public WeightedHashRule setRepetitions(Integer repetitions) {
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
		if (serverWeightTimer != null) {
			logger.debug("Stopping NFLoadBalancer-serverWeightTimer-" + name);
			serverWeightTimer.cancel();
		}
		serverWeightTimer = new Timer("NFLoadBalancer-serverWeightTimer-" + name, true);
		serverWeightTimer.schedule(new DynamicServerWeightTask(), 0, serverWeightTaskTimerInterval);

		buildKetamaNodes();

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				logger.debug("Stopping NFLoadBalancer-serverWeightTimer-" + name);
				serverWeightTimer.cancel();
			}
		}));
	}

	public void shutdown() {
		if (serverWeightTimer != null) {
			logger.debug("Stopping NFLoadBalancer-serverWeightTimer-" + name);
			serverWeightTimer.cancel();
		}
	}

	protected BalancingNode choose(ILoadBalancer lb, Object key) {
		if (lb == null) {
			return null;
		}
		BalancingNode server = null;
		while (server == null) {
			if (Thread.interrupted()) {
				return null;
			}
			List<BalancingNode> allServerList = lb.getAllNodes();
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

			if (server == null) {
				Thread.yield();
				continue;
			}

			if (!allServerList.contains(server)) {
				Thread.yield();
				server = null;
				continue;
			}

			if (server.isAlive() && (!server.isSuspended())) {
				return (server);
			} else {
				key = UUIDGenerator.generate();
			}

			server = null;
		}
		return server;
	}

	private Integer repetitions = 256;
	private HashAlgorithm hashAlg = DefaultHashAlgorithm.KETAMA_HASH;

	private ConcurrentSkipListMap<Long, BalancingNode> hashNodes;

	/**
	 * Setup the KetamaNodeLocator with the list of nodes it should use.
	 *
	 * @param nodes
	 *            a List of MemcachedNodes for this KetamaNodeLocator to use in
	 *            its continuum
	 */
	void buildKetamaNodes() {
		BalancingNode[] tmpNodes = getNodes().toArray(new BalancingNode[0]);
		ConcurrentSkipListMap<Long, BalancingNode> nodeMap = new ConcurrentSkipListMap<Long, BalancingNode>();
		int numReps = 0;
		for (BalancingNode node : tmpNodes) {
			if (accumulatedWeights.isEmpty()) {
				numReps = repetitions;
			} else {
				numReps = (int) Math.floor((repetitions * accumulatedWeights.get(node)) / 4) * 4;
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
	}

	BalancingNode getPrimary(long hash) {
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

	class DynamicServerWeightTask extends TimerTask {
		public void run() {
			ServerWeight serverWeight = new ServerWeight();
			try {
				serverWeight.maintainWeights();
			} catch (Exception e) {
				logger.error("Error running DynamicServerWeightTask for {}", name, e);
			}
		}
	}

	class ServerWeight {

		public void maintainWeights() {
			logger.debug("Weight adjusting job started");
			ILoadBalancer lb = getLoadBalancer();
			if (lb == null) {
				return;
			}

			if (!serverWeightAssignmentInProgress.compareAndSet(false, true)) {
				serverWeightAssignmentInProgress.set(false);
				return;
			}
			
			try {
				AbstractLoadBalancer nlb = (AbstractLoadBalancer) lb;
				LoadBalancerStats stats = nlb.getLoadBalancerStats();
				if (stats == null) {
					return;
				}
				double totalResponseTime = 0;
				for (BalancingNode server : nlb.getAllNodes()) {
					ServerStats ss = stats.getSingleServerStat(server);
					totalResponseTime += ss.getResponseTimeAvg() == 0 ? 0.001 : ss.getResponseTimeAvg();
				}
				Double avgResponseTime = totalResponseTime / nlb.getAllNodes().size();
				Map<BalancingNode, Double> finalWeights = new ConcurrentSkipListMap<BalancingNode, Double>(new ServerComparator());
				for (BalancingNode server : nlb.getAllNodes()) {
					ServerStats ss = stats.getSingleServerStat(server);
					double weight = avgResponseTime / (ss.getResponseTimeAvg() == 0 ? 0.001 : ss.getResponseTimeAvg());
					finalWeights.put(server, weight);
				}
				setWeights(finalWeights);
			} catch (Exception e) {
				logger.error("Error calculating server weights", e);
			} finally {
				buildKetamaNodes();
				serverWeightAssignmentInProgress.set(false);
			}

		}
	}

	void setWeights(Map<BalancingNode, Double> weights) {
		this.accumulatedWeights = weights;
	}

	Collection<BalancingNode> getNodes() {
		return getLoadBalancer().getAllNodes();
	}

	@Override
	public void initWithNiwsConfig(IClientConfig clientConfig) {
		serverWeightTaskTimerInterval = clientConfig.get(WEIGHT_TASK_TIMER_INTERVAL_CONFIG_KEY, DEFAULT_TIMER_INTERVAL);
	}

	@Override
	public BalancingNode choose(Object key) {
		return choose(getLoadBalancer(), key);
	}

}
