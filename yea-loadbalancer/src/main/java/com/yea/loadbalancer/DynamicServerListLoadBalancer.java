package com.yea.loadbalancer;

import com.google.common.annotations.VisibleForTesting;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.INodeList;
import com.yea.core.loadbalancer.INodeListFilter;
import com.yea.core.loadbalancer.INodeListUpdater;
import com.yea.core.loadbalancer.IPing;
import com.yea.core.loadbalancer.IRule;
import com.yea.loadbalancer.config.CommonClientConfigKey;
import com.yea.loadbalancer.config.DefaultClientConfigImpl;
import com.yea.loadbalancer.config.IClientConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A LoadBalancer that has the capabilities to obtain the candidate list of
 * servers using a dynamic source. i.e. The list of servers can potentially be
 * changed at Runtime. It also contains facilities wherein the list of servers
 * can be passed through a Filter criteria to filter out servers that do not
 * meet the desired criteria.
 * 
 * @author stonse
 * 
 */
public class DynamicServerListLoadBalancer<T extends BalancingNode> extends BaseLoadBalancer {
	private static final Logger LOGGER = LoggerFactory.getLogger(DynamicServerListLoadBalancer.class);

	boolean isSecure = false;
	boolean useTunnel = false;

	// to keep track of modification of server lists
	protected AtomicBoolean serverListUpdateInProgress = new AtomicBoolean(false);

	volatile INodeList<T> serverListImpl;

	volatile INodeListFilter<T> filter;

	protected final INodeListUpdater.UpdateAction updateAction = new INodeListUpdater.UpdateAction() {
		@Override
		public void doUpdate() {
			updateListOfServers();
		}
	};

	protected volatile INodeListUpdater serverListUpdater;

	public DynamicServerListLoadBalancer() {
		super();
	}

	@Deprecated
	public DynamicServerListLoadBalancer(IClientConfig clientConfig, IRule rule, IPing ping, INodeList<T> serverList,
			INodeListFilter<T> filter) {
		this(clientConfig, rule, ping, serverList, filter, new PollingServerListUpdater());
	}

	public DynamicServerListLoadBalancer(IClientConfig clientConfig, IRule rule, IPing ping, INodeList<T> serverList,
			INodeListFilter<T> filter, INodeListUpdater serverListUpdater) {
		super(clientConfig, rule, ping);
		this.serverListImpl = serverList;
		this.filter = filter;
		this.serverListUpdater = serverListUpdater;
		if (filter instanceof AbstractNodeListFilter) {
			((AbstractNodeListFilter) filter).setLoadBalancerStats(getLoadBalancerStats());
		}
		restOfInit(clientConfig);
	}

	public DynamicServerListLoadBalancer(IClientConfig clientConfig) {
		initWithNiwsConfig(clientConfig);
	}

	@Override
	public void initWithNiwsConfig(IClientConfig clientConfig) {
		try {
			super.initWithNiwsConfig(clientConfig);
			String niwsServerListClassName = clientConfig.getPropertyAsString(
					CommonClientConfigKey.NIWSServerListClassName, DefaultClientConfigImpl.DEFAULT_SEVER_LIST_CLASS);

			INodeList<T> niwsServerListImpl = (INodeList<T>) ClientFactory
					.instantiateInstanceWithClientConfig(niwsServerListClassName, clientConfig);
			this.serverListImpl = niwsServerListImpl;

			if (niwsServerListImpl instanceof AbstractNodeList) {
				AbstractNodeListFilter<T> niwsFilter = ((AbstractNodeList) niwsServerListImpl)
						.getFilterImpl(clientConfig);
				niwsFilter.setLoadBalancerStats(getLoadBalancerStats());
				this.filter = niwsFilter;
			}

			String serverListUpdaterClassName = clientConfig.getPropertyAsString(
					CommonClientConfigKey.ServerListUpdaterClassName,
					DefaultClientConfigImpl.DEFAULT_SERVER_LIST_UPDATER_CLASS);

			this.serverListUpdater = (INodeListUpdater) ClientFactory
					.instantiateInstanceWithClientConfig(serverListUpdaterClassName, clientConfig);

			restOfInit(clientConfig);
		} catch (Exception e) {
			throw new RuntimeException("Exception while initializing NIWSDiscoveryLoadBalancer:"
					+ clientConfig.getClientName() + ", niwsClientConfig:" + clientConfig, e);
		}
	}

	/**
	 * 启动服务列表定时刷新机制，默认通过PollingServerListUpdater来刷新。默认每30秒刷新一次。
	 * 并且执行一次刷新。
	 * @param clientConfig
	 */
	void restOfInit(IClientConfig clientConfig) {
		enableAndInitLearnNewServersFeature();

		updateListOfServers();
		LOGGER.info("DynamicServerListLoadBalancer for client {} initialized: {}", clientConfig.getClientName(),
				this.toString());
	}

	@Override
	public void setNodesList(Collection lsrv) {
		super.setNodesList(lsrv);
		Collection<T> serverList = (Collection<T>) lsrv;
		Map<String, Collection<BalancingNode>> serversInZones = new HashMap<String, Collection<BalancingNode>>();
		for (BalancingNode server : serverList) {
			// make sure ServerStats is created to avoid creating them on hot
			// path
			getLoadBalancerStats().getSingleServerStat(server);
			String zone = server.getZone();
			if (zone != null) {
				zone = zone.toLowerCase();
				Collection<BalancingNode> servers = serversInZones.get(zone);
				if (servers == null) {
					servers = new ArrayList<BalancingNode>();
					serversInZones.put(zone, servers);
				}
				servers.add(server);
			}
		}
		setServerListForZones(serversInZones);
	}

	protected void setServerListForZones(Map<String, Collection<BalancingNode>> zoneServersMap) {
		LOGGER.debug("Setting server list for zones: {}", zoneServersMap);
		getLoadBalancerStats().updateZoneServerMapping(zoneServersMap);
	}

	public INodeList<T> getServerListImpl() {
		return serverListImpl;
	}

	public void setServerListImpl(INodeList<T> niwsServerList) {
		this.serverListImpl = niwsServerList;
	}

	public INodeListFilter<T> getFilter() {
		return filter;
	}

	public void setFilter(INodeListFilter<T> filter) {
		this.filter = filter;
	}

	@Override
	/**
	 * Makes no sense to ping an inmemory disc client
	 * 
	 */
	public void forceQuickPing() {
		// no-op
	}

	/**
	 * Feature that lets us add new instances (from AMIs) to the list of
	 * existing servers that the LB will use Call this method if you want this
	 * feature enabled
	 */
	public void enableAndInitLearnNewServersFeature() {
		LOGGER.info("Using serverListUpdater {}", serverListUpdater.getClass().getSimpleName());
		serverListUpdater.start(updateAction);
	}

	private String getIdentifier() {
		return this.getClientConfig().getClientName();
	}

	public void stopServerListRefreshing() {
		if (serverListUpdater != null) {
			serverListUpdater.stop();
		}
	}

	/**
	 * 通过INodeList获得服务器列表，设置到负载均衡列表内
	 */
	@VisibleForTesting
	public void updateListOfServers() {
		Collection<T> servers = new ArrayList<T>();
		if (serverListImpl != null) {
			servers = serverListImpl.getUpdatedListOfNodes();
			LOGGER.debug("List of Servers for {} obtained from Discovery client: {}", getIdentifier(), servers);

			if (filter != null) {
				servers = filter.getFilteredListOfNodes(servers);
				LOGGER.debug("Filtered List of Servers for {} obtained from Discovery client: {}", getIdentifier(),
						servers);
			}
		}
		updateAllServerList(servers);
	}

	/**
	 * Update the AllServer list in the LoadBalancer if necessary and enabled
	 * 
	 * @param ls
	 */
	protected void updateAllServerList(Collection<T> ls) {
		// other threads might be doing this - in which case, we pass
		if (serverListUpdateInProgress.compareAndSet(false, true)) {
			try {
				for (T s : ls) {
					s.setAlive(true); // set so that clients can start using
										// these
										// servers right away instead
										// of having to wait out the ping cycle.
				}
				setNodesList(ls);
				super.forceQuickPing();
			} finally {
				serverListUpdateInProgress.set(false);
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("DynamicServerListLoadBalancer:");
		sb.append(super.toString());
		sb.append("ServerList:" + String.valueOf(serverListImpl));
		return sb.toString();
	}

	@Override
	public void shutdown() {
		super.shutdown();
		stopServerListRefreshing();
	}

	@Monitor(name = "LastUpdated", type = DataSourceType.INFORMATIONAL)
	public String getLastUpdate() {
		return serverListUpdater.getLastUpdate();
	}

	@Monitor(name = "DurationSinceLastUpdateMs", type = DataSourceType.GAUGE)
	public long getDurationSinceLastUpdateMs() {
		return serverListUpdater.getDurationSinceLastUpdateMs();
	}

	@Monitor(name = "NumUpdateCyclesMissed", type = DataSourceType.GAUGE)
	public int getNumberMissedCycles() {
		return serverListUpdater.getNumberMissedCycles();
	}

	@Monitor(name = "NumThreads", type = DataSourceType.GAUGE)
	public int getCoreThreads() {
		return serverListUpdater.getCoreThreads();
	}
}
