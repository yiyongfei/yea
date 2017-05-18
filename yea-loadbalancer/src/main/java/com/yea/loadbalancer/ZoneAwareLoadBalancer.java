package com.yea.loadbalancer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicDoubleProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.INodeList;
import com.yea.core.loadbalancer.INodeListFilter;
import com.yea.core.loadbalancer.INodeListUpdater;
import com.yea.core.loadbalancer.IPing;
import com.yea.core.loadbalancer.IRule;
import com.yea.loadbalancer.config.IClientConfig;
import com.yea.loadbalancer.rule.AvailabilityFilteringRule;
import com.yea.loadbalancer.rule.ZoneAvoidanceRule;

/**
 * Load balancer that can avoid a zone as a whole when choosing server.
 * <p>
 * The key metric used to measure the zone condition is Average Active Requests,
 * which is aggregated per rest client per zone. It is the total outstanding
 * requests in a zone divided by number of available targeted instances
 * (excluding circuit breaker tripped instances). This metric is very effective
 * when timeout occurs slowly on a bad zone.
 * <p>
 * The LoadBalancer will calculate and examine zone stats of all available
 * zones. If the Average Active Requests for any zone has reached a configured
 * threshold, this zone will be dropped from the active server list. In case
 * more than one zone has reached the threshold, the zone with the most active
 * requests per server will be dropped. Once the the worst zone is dropped, a
 * zone will be chosen among the rest with the probability proportional to its
 * number of instances. A server will be returned from the chosen zone with a
 * given Rule (A Rule is a load balancing strategy, for example
 * {@link AvailabilityFilteringRule}) For each request, the steps above will be
 * repeated. That is to say, each zone related load balancing decisions are made
 * at real time with the up-to-date statistics aiding the choice.
 * 
 * @author awang
 *
 * @param <T>
 */
public class ZoneAwareLoadBalancer<T extends BalancingNode> extends DynamicServerListLoadBalancer<T> {

	private ConcurrentHashMap<String, BaseLoadBalancer> balancers = new ConcurrentHashMap<String, BaseLoadBalancer>();

	private static final Logger logger = LoggerFactory.getLogger(ZoneAwareLoadBalancer.class);

	private volatile DynamicDoubleProperty triggeringLoad;

	private volatile DynamicDoubleProperty triggeringBlackoutPercentage;

	private static final DynamicBooleanProperty ENABLED = DynamicPropertyFactory.getInstance()
			.getBooleanProperty("ZoneAwareNIWSDiscoveryLoadBalancer.enabled", true);

	void setUpServerList(Collection<BalancingNode> upServerList) {
		this.upServerList.clear();
		this.upServerList.addAll(upServerList);
	}

	public ZoneAwareLoadBalancer() {
		super();
	}

	@Deprecated
	public ZoneAwareLoadBalancer(IClientConfig clientConfig, IRule rule, IPing ping, INodeList<T> serverList,
			INodeListFilter<T> filter) {
		super(clientConfig, rule, ping, serverList, filter);
	}

	public ZoneAwareLoadBalancer(IClientConfig clientConfig, IRule rule, IPing ping, INodeList<T> serverList,
			INodeListFilter<T> filter, INodeListUpdater serverListUpdater) {
		super(clientConfig, rule, ping, serverList, filter, serverListUpdater);
	}

	public ZoneAwareLoadBalancer(IClientConfig niwsClientConfig) {
		super(niwsClientConfig);
	}

	@Override
	protected void setServerListForZones(Map<String, Collection<BalancingNode>> zoneServersMap) {
		super.setServerListForZones(zoneServersMap);
		if (balancers == null) {
			balancers = new ConcurrentHashMap<String, BaseLoadBalancer>();
		}
		for (Map.Entry<String, Collection<BalancingNode>> entry : zoneServersMap.entrySet()) {
			String zone = entry.getKey().toLowerCase();
			getLoadBalancer(zone).setNodesList(entry.getValue());
		}
		// check if there is any zone that no longer has a server
		// and set the list to empty so that the zone related metrics does not
		// contain stale data
		for (Map.Entry<String, BaseLoadBalancer> existingLBEntry : balancers.entrySet()) {
			if (!zoneServersMap.keySet().contains(existingLBEntry.getKey())) {
				existingLBEntry.getValue().setNodesList(Collections.emptyList());
			}
		}
	}

	@Override
	public BalancingNode chooseNode(Object key) {
		if (!ENABLED.get() || getLoadBalancerStats().getAvailableZones().size() <= 1) {
			logger.debug("Zone aware logic disabled or there is only one zone");
			return super.chooseNode(key);
		}
		BalancingNode server = null;
		try {
			LoadBalancerStats lbStats = getLoadBalancerStats();
			Map<String, ZoneSnapshot> zoneSnapshot = ZoneAvoidanceRule.createSnapshot(lbStats);
			logger.debug("Zone snapshots: {}", zoneSnapshot);
			if (triggeringLoad == null) {
				triggeringLoad = DynamicPropertyFactory.getInstance().getDoubleProperty(
						"ZoneAwareNIWSDiscoveryLoadBalancer." + this.getName() + ".triggeringLoadPerServerThreshold",
						0.2d);
			}

			if (triggeringBlackoutPercentage == null) {
				triggeringBlackoutPercentage = DynamicPropertyFactory.getInstance().getDoubleProperty(
						"ZoneAwareNIWSDiscoveryLoadBalancer." + this.getName() + ".avoidZoneWithBlackoutPercetage",
						0.99999d);
			}
			Set<String> availableZones = ZoneAvoidanceRule.getAvailableZones(zoneSnapshot, triggeringLoad.get(),
					triggeringBlackoutPercentage.get());
			logger.debug("Available zones: {}", availableZones);
			if (availableZones != null && availableZones.size() < zoneSnapshot.keySet().size()) {
				String zone = ZoneAvoidanceRule.randomChooseZone(zoneSnapshot, availableZones);
				logger.debug("Zone chosen: {}", zone);
				if (zone != null) {
					BaseLoadBalancer zoneLoadBalancer = getLoadBalancer(zone);
					/**
					 * 新生成的负载均衡器的zoneLoadBalancer是没有服务器的，所以会通过父方法来选取。
					 * 但PollingServerListUpdater执行时，
					 * 会根据Zone通过setServerListForZones方法设置各个负载均衡器的服务器列表，
					 * 所以后续就没有问题。
					 */
					server = zoneLoadBalancer.chooseNode(key);
				}
			}
		} catch (Exception e) {
			logger.error("Error choosing server using zone aware logic for load balancer={}", name, e);
		}
		if (server != null) {
			return server;
		} else {
			logger.debug("Zone avoidance logic is not invoked.");
			return super.chooseNode(key);
		}
	}

	private final Lock lock = new ReentrantLock();
	@VisibleForTesting
	BaseLoadBalancer getLoadBalancer(String zone) {
		zone = zone.toLowerCase();
		BaseLoadBalancer loadBalancer = balancers.get(zone);
		if (loadBalancer == null) {
			lock.lock();
			try {
				loadBalancer = balancers.get(zone);
				if (loadBalancer == null) {
					// We need to create rule object for load balancer for each zone
					IRule rule = cloneRule(this.getRule());
					loadBalancer = new BaseLoadBalancer(this.getName() + "_" + zone, rule, this.getLoadBalancerStats());
					BaseLoadBalancer prev = balancers.putIfAbsent(zone, loadBalancer);
					if (prev != null) {
						loadBalancer = prev;
					}
				}
			} finally {
				lock.unlock();
			}
		}
		return loadBalancer;
	}

	private IRule cloneRule(IRule toClone) {
		IRule rule;
		if (toClone == null) {
			rule = new AvailabilityFilteringRule();
		} else {
			String ruleClass = toClone.getClass().getName();
			try {
				rule = (IRule) ClientFactory.instantiateInstanceWithClientConfig(ruleClass, this.getClientConfig());
			} catch (Exception e) {
				throw new RuntimeException("Unexpected exception creating rule for ZoneAwareLoadBalancer", e);
			}
		}
		return rule;
	}

	@Override
	public void setRule(IRule rule) {
		super.setRule(rule);
		if (balancers != null) {
			for (String zone : balancers.keySet()) {
				balancers.get(zone).setRule(cloneRule(rule));
			}
		}
	}
}
