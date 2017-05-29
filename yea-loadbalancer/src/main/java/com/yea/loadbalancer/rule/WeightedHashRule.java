package com.yea.loadbalancer.rule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yea.core.hash.HashAlgorithm;
import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.ILoadBalancer;
import com.yea.core.loadbalancer.ServerComparator;
import com.yea.core.util.ScheduledExecutor;
import com.yea.loadbalancer.AbstractLoadBalancer;
import com.yea.loadbalancer.LoadBalancerStats;
import com.yea.loadbalancer.ServerStats;
import com.yea.loadbalancer.config.IClientConfig;
import com.yea.loadbalancer.config.IClientConfigKey;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 一致性哈希规则（与响应时间权重相关）
 * 响应时间的权重计算：
 * 所有服务器平均响应时间作为基准，每个服务器平均响应时间与基准比较，平均响应时间越少，权重越大，服务器选中的可能性就越高。
 * (基准时间/单机平均响应时间)
 * @author yiyongfei
 *
 */
public class WeightedHashRule extends HashRule {

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

	protected Map<BalancingNode, Double> accumulatedWeights = new ConcurrentSkipListMap<BalancingNode, Double>();
	
	protected AtomicBoolean serverWeightAssignmentInProgress = new AtomicBoolean(false);
	
	public WeightedHashRule() {
		super();
		ScheduledExecutor.getScheduledExecutor().scheduleWithFixedDelay(new DynamicServerWeightRunnable(), 2 * 1000,
				serverWeightTaskTimerInterval, TimeUnit.MILLISECONDS);
	}

	public WeightedHashRule(ILoadBalancer lb) {
		super(lb);
	}

	public WeightedHashRule setHashAlgorithm(HashAlgorithm hashAlgorithm) {
		super.setHashAlgorithm(hashAlgorithm);
		return this;
	}

	public WeightedHashRule setRepetitions(Integer repetitions) {
		super.setRepetitions(repetitions);
		return this;
	}
	
	class DynamicServerWeightRunnable implements Runnable {
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
				return;
			}
			
			try {
				Collection<BalancingNode> nodes = getNodes();
				AbstractLoadBalancer nlb = (AbstractLoadBalancer) lb;
				LoadBalancerStats stats = nlb.getLoadBalancerStats();
				if (stats == null) {
					return;
				}
				
				int count = 0;
				double totalResponseTime = 0;
				for (BalancingNode server : nodes) {
					ServerStats ss = stats.getSingleServerStat(server);
					if(ss.getResponseTimeAvg() != 0) {
						totalResponseTime += ss.getResponseTimeAvg();
						count++;
					}
				}
				
				Map<BalancingNode, Double> finalWeights = new ConcurrentSkipListMap<BalancingNode, Double>(new ServerComparator());
				if (count > 0) {
					double avgResponseTime = totalResponseTime / count;
					for (BalancingNode server : nodes) {
						ServerStats ss = stats.getSingleServerStat(server);
						double weight = (avgResponseTime
								/ (ss.getResponseTimeAvg() == 0 ? avgResponseTime : ss.getResponseTimeAvg()));
						weight = Math.pow(weight, 0.5) ;
						weight = weight > 1.7 ? 1.7 : weight;
						weight = weight < 0.4 ? 0.4 : weight;
						finalWeights.put(server, weight);
					}
				} else {
					/*当所有节点都没有被使用，各节点的默认权重均为1*/
					for (BalancingNode server : nodes) {
						finalWeights.put(server, 1.0);
					}
				}
				
				setWeights(finalWeights);
			} catch (Exception e) {
				logger.error("Error calculating server weights", e);
			} finally {
				buildKetamaNodes();
				serverWeightAssignmentInProgress.set(false);
				if(hashNodes.isEmpty()){
					updateNodeInProgress.set(false);
				} else {
					updateNodeInProgress.set(true);
				}
			}

		}
	}

	void setWeights(Map<BalancingNode, Double> weights) {
		this.accumulatedWeights = weights;
	}
	
	@Override
	Map<BalancingNode, Double> getWeights() {
		return this.accumulatedWeights;
	}
	
	@Override
	public void initWithNiwsConfig(IClientConfig clientConfig) {
		serverWeightTaskTimerInterval = clientConfig.get(WEIGHT_TASK_TIMER_INTERVAL_CONFIG_KEY, DEFAULT_TIMER_INTERVAL);
	}

}
