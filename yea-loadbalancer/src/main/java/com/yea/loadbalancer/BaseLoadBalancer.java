package com.yea.loadbalancer;

import static java.util.Collections.singleton;

import java.net.SocketAddress;

import com.google.common.collect.ImmutableList;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.Monitors;
import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.INodeListChangeListener;
import com.yea.core.loadbalancer.INodeStatusChangeListener;
import com.yea.core.loadbalancer.IPing;
import com.yea.core.loadbalancer.IPingStrategy;
import com.yea.core.loadbalancer.IRule;
import com.yea.loadbalancer.config.CommonClientConfigKey;
import com.yea.loadbalancer.config.IClientConfig;
import com.yea.loadbalancer.rule.RoundRobinRule;
import com.yea.loadbalancer.util.ShutdownEnabledTimer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A basic implementation of the load balancer where an arbitrary list of
 * servers can be set as the server pool. A ping can be set to determine the
 * liveness of a server. Internally, this class maintains an "all" server list
 * and an "up" server list and use them depending on what the caller asks for.
 * 
 * @author stonse
 * 
 */
public class BaseLoadBalancer extends AbstractLoadBalancer implements IClientConfigAware {

	private static Logger logger = LoggerFactory.getLogger(BaseLoadBalancer.class);
	private final static IRule DEFAULT_RULE = new RoundRobinRule();
	private final static SerialPingStrategy DEFAULT_PING_STRATEGY = new SerialPingStrategy();
	private static final String DEFAULT_NAME = "default";
	private static final String PREFIX = "LoadBalancer_";

	protected IRule rule = DEFAULT_RULE;

	protected IPingStrategy pingStrategy = DEFAULT_PING_STRATEGY;

	protected IPing ping = null;

	@Monitor(name = PREFIX + "AllServerList", type = DataSourceType.INFORMATIONAL)
	protected volatile List<BalancingNode> allServerList = Collections.synchronizedList(new ArrayList<BalancingNode>());
	@Monitor(name = PREFIX + "UpServerList", type = DataSourceType.INFORMATIONAL)
	protected volatile List<BalancingNode> upServerList = Collections.synchronizedList(new ArrayList<BalancingNode>());

	protected ReadWriteLock allServerLock = new ReentrantReadWriteLock();
	protected ReadWriteLock upServerLock = new ReentrantReadWriteLock();

	protected String name = DEFAULT_NAME;

	protected Timer lbTimer = null;
	protected int pingIntervalSeconds = 10;
	protected int maxTotalPingTimeSeconds = 5;
	protected Comparator<BalancingNode> serverComparator = new ServerComparator();

	protected AtomicBoolean pingInProgress = new AtomicBoolean(false);

	protected LoadBalancerStats lbStats;

	private volatile Counter counter = Monitors.newCounter("LoadBalancer_ChooseServer");

	private IClientConfig config;

	private Collection<INodeListChangeListener> changeListeners = new CopyOnWriteArrayList<INodeListChangeListener>();

	private Collection<INodeStatusChangeListener> serverStatusListeners = new CopyOnWriteArrayList<INodeStatusChangeListener>();

	/**
	 * Default constructor which sets name as "default", sets null ping, and
	 * {@link RoundRobinRule} as the rule.
	 * <p>
	 * This constructor is mainly used by {@link ClientFactory}. Calling this
	 * constructor must be followed by calling {@link #init()} or
	 * {@link #initWithNiwsConfig(IClientConfig)} to complete initialization.
	 * This constructor is provided for reflection. When constructing
	 * programatically, it is recommended to use other constructors.
	 */
	public BaseLoadBalancer() {
		this.name = DEFAULT_NAME;
		this.ping = null;
		setRule(DEFAULT_RULE);
		setupPingTask();
		lbStats = new LoadBalancerStats(DEFAULT_NAME);
	}

	public BaseLoadBalancer(String lbName, IRule rule, LoadBalancerStats lbStats) {
		this(lbName, rule, lbStats, null);
	}

	public BaseLoadBalancer(IPing ping, IRule rule) {
		this(DEFAULT_NAME, rule, new LoadBalancerStats(DEFAULT_NAME), ping);
	}

	public BaseLoadBalancer(IPing ping, IRule rule, IPingStrategy pingStrategy) {
		this(DEFAULT_NAME, rule, new LoadBalancerStats(DEFAULT_NAME), ping, pingStrategy);
	}

	public BaseLoadBalancer(String name, IRule rule, LoadBalancerStats stats, IPing ping) {
		this(name, rule, stats, ping, DEFAULT_PING_STRATEGY);
	}

	public BaseLoadBalancer(String name, IRule rule, LoadBalancerStats stats, IPing ping, IPingStrategy pingStrategy) {

		logger.debug("LoadBalancer [{}]:  initialized", name);

		this.name = name;
		this.ping = ping;
		this.pingStrategy = pingStrategy;
		setRule(rule);
		setupPingTask();
		lbStats = stats;
		init();
	}

	public BaseLoadBalancer(IClientConfig config) {
		initWithNiwsConfig(config);
	}

	public BaseLoadBalancer(IClientConfig config, IRule rule, IPing ping) {
		initWithConfig(config, rule, ping);
	}

	void initWithConfig(IClientConfig clientConfig, IRule rule, IPing ping) {
		this.config = clientConfig;
		String clientName = clientConfig.getClientName();
		this.name = clientName;
		int pingIntervalTime = Integer.parseInt(""
				+ clientConfig.getProperty(CommonClientConfigKey.NFLoadBalancerPingInterval, Integer.parseInt("30")));
		int maxTotalPingTime = Integer.parseInt("" + clientConfig
				.getProperty(CommonClientConfigKey.NFLoadBalancerMaxTotalPingTime, Integer.parseInt("2")));

		setPingInterval(pingIntervalTime);
		setMaxTotalPingTime(maxTotalPingTime);

		// cross associate with each other
		// i.e. Rule,Ping meet your container LB
		// LB, these are your Ping and Rule guys ...
		setRule(rule);
		setPing(ping);
		setLoadBalancerStats(new LoadBalancerStats(clientName));
		rule.setLoadBalancer(this);
		if (ping instanceof AbstractLoadBalancerPing) {
			((AbstractLoadBalancerPing) ping).setLoadBalancer(this);
		}
		logger.info("Client: {} instantiated a LoadBalancer: {}", name, this);
		init();

	}

	@Override
	public void initWithNiwsConfig(IClientConfig clientConfig) {
		String ruleClassName = (String) clientConfig.getProperty(CommonClientConfigKey.NFLoadBalancerRuleClassName);
		String pingClassName = (String) clientConfig.getProperty(CommonClientConfigKey.NFLoadBalancerPingClassName);

		IRule rule;
		IPing ping;
		try {
			rule = (IRule) ClientFactory.instantiateInstanceWithClientConfig(ruleClassName, clientConfig);
			ping = (IPing) ClientFactory.instantiateInstanceWithClientConfig(pingClassName, clientConfig);
		} catch (Exception e) {
			throw new RuntimeException("Error initializing load balancer", e);
		}
		initWithConfig(clientConfig, rule, ping);
	}

	public void addServerListChangeListener(INodeListChangeListener listener) {
		changeListeners.add(listener);
	}

	public void removeServerListChangeListener(INodeListChangeListener listener) {
		changeListeners.remove(listener);
	}

	public void addServerStatusChangeListener(INodeStatusChangeListener listener) {
		serverStatusListeners.add(listener);
	}

	public void removeServerStatusChangeListener(INodeStatusChangeListener listener) {
		serverStatusListeners.remove(listener);
	}

	public IClientConfig getClientConfig() {
		return config;
	}

	/**
	 * Ping为Null或为DummyPing或NoOpPing时，不Ping
	 * @return
	 */
	private boolean canSkipPing() {
		if (ping == null || ping.getClass().getName().equals(DummyPing.class.getName()) || ping.getClass().getName().equals(NoOpPing.class.getName())) {
			// default ping, no need to set up timer
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 设置定时Ping的任务，默认每10秒Ping一次
	 */
	void setupPingTask() {
		if (canSkipPing()) {
			return;
		}
		if (lbTimer != null) {
			lbTimer.cancel();
		}
		lbTimer = new ShutdownEnabledTimer("NFLoadBalancer-PingTimer-" + name, true);
		lbTimer.schedule(new PingTask(), 0, pingIntervalSeconds * 1000);
		forceQuickPing();
	}

	/**
	 * Set the name for the load balancer. This should not be called since name
	 * should be immutable after initialization. Calling this method does not
	 * guarantee that all other data structures that depend on this name will be
	 * changed accordingly.
	 */
	void setName(String name) {
		// and register
		this.name = name;
		if (lbStats == null) {
			lbStats = new LoadBalancerStats(name);
		} else {
			lbStats.setName(name);
		}
	}

	public String getName() {
		return name;
	}

	@Override
	public LoadBalancerStats getLoadBalancerStats() {
		return lbStats;
	}

	public void setLoadBalancerStats(LoadBalancerStats lbStats) {
		this.lbStats = lbStats;
	}

	public Lock lockAllServerList(boolean write) {
		Lock aproposLock = write ? allServerLock.writeLock() : allServerLock.readLock();
		aproposLock.lock();
		return aproposLock;
	}

	public Lock lockUpServerList(boolean write) {
		Lock aproposLock = write ? upServerLock.writeLock() : upServerLock.readLock();
		aproposLock.lock();
		return aproposLock;
	}

	public void setPingInterval(int pingIntervalSeconds) {
		if (pingIntervalSeconds < 1) {
			return;
		}

		this.pingIntervalSeconds = pingIntervalSeconds;
		if (logger.isDebugEnabled()) {
			logger.debug("LoadBalancer [{}]:  pingIntervalSeconds set to {}", name, this.pingIntervalSeconds);
		}
		setupPingTask(); // since ping data changed
	}

	public int getPingInterval() {
		return pingIntervalSeconds;
	}

	/*
	 * Maximum time allowed for the ping cycle
	 */
	public void setMaxTotalPingTime(int maxTotalPingTimeSeconds) {
		if (maxTotalPingTimeSeconds < 1) {
			return;
		}
		this.maxTotalPingTimeSeconds = maxTotalPingTimeSeconds;
		logger.debug("LoadBalancer [{}]: maxTotalPingTime set to {}", name, this.maxTotalPingTimeSeconds);

	}

	public int getMaxTotalPingTime() {
		return maxTotalPingTimeSeconds;
	}

	public IPing getPing() {
		return ping;
	}

	public IRule getRule() {
		return rule;
	}

	public boolean isPingInProgress() {
		return pingInProgress.get();
	}

	/* Specify the object which is used to send pings. */
	/**
	 * 设置Ping，如果Ping为Null，则定时Ping任务取消
	 * @param rule
	 */
	public void setPing(IPing ping) {
		if (ping != null) {
			if (!ping.equals(this.ping)) {
				this.ping = ping;
				setupPingTask(); // since ping data changed
			}
		} else {
			this.ping = null;
			// cancel the timer task
			lbTimer.cancel();
		}
	}

	/* Ignore null rules */

	/**
	 * 设置负载均衡规则，默认为轮询规则
	 * @param rule
	 */
	public void setRule(IRule rule) {
		if (rule != null) {
			this.rule = rule;
		} else {
			/* default rule */
			this.rule = new RoundRobinRule();
		}
		if (this.rule.getLoadBalancer() != this) {
			this.rule.setLoadBalancer(this);
		}
	}

	/**
	 * 获得服务器总数
	 */
	/**
	 * get the count of servers.
	 * 
	 * @param onlyAvailable
	 *            if true, return only up servers.
	 */
	public int getServerCount(boolean onlyAvailable) {
		if (onlyAvailable) {
			return upServerList.size();
		} else {
			return allServerList.size();
		}
	}

	/**
	 * 增加服务节点（追加）
	 * 请注意：如果是DynamicServerListLoadBalancer，使用addNode或addNodes，它只会将节点临时增加到负载均衡列表里，最终会被PollingServerListUpdater更新
	 * @param srvString
	 */
	/**
	 * Add a server to the 'allServer' list; does not verify uniqueness, so you
	 * could give a server a greater share by adding it more than once.
	 */
	public void addNode(BalancingNode newServer) {
		if (newServer != null) {
			try {
				ArrayList<BalancingNode> newList = new ArrayList<BalancingNode>();

				newList.addAll(allServerList);
				newList.add(newServer);
				setNodesList(newList);
			} catch (Exception e) {
			}
		}
	}

	/**
	 * 批量增加服务节点（追加）
	 * @param srvString
	 */
	/**
	 * Add a list of servers to the 'allServer' list; does not verify
	 * uniqueness, so you could give a server a greater share by adding it more
	 * than once
	 */
	@Override
	public void addNodes(Collection<BalancingNode> newServers) {
		if (newServers != null && newServers.size() > 0) {
			try {
				ArrayList<BalancingNode> newList = new ArrayList<BalancingNode>();
				newList.addAll(allServerList);
				newList.addAll(newServers);
				setNodesList(newList);
			} catch (Exception e) {
				logger.error("LoadBalancer [{}]: Exception while adding Servers", name, e);
			}
		}
	}

	/**
	 * 批量增加服务节点（追加）
	 * @param srvString
	 */
	/*
	 * Add a list of servers to the 'allServer' list; does not verify
	 * uniqueness, so you could give a server a greater share by adding it more
	 * than once USED by Test Cases only for legacy reason. DO NOT USE!!
	 */
	void addNodes(Object[] newServers) {
		if ((newServers != null) && (newServers.length > 0)) {

			try {
				ArrayList<BalancingNode> newList = new ArrayList<BalancingNode>();
				newList.addAll(allServerList);

				for (Object server : newServers) {
					if (server != null) {
						if (server instanceof String) {
							server = new BalancingNode((String) server);
						}
						if (server instanceof BalancingNode) {
							newList.add((BalancingNode) server);
						}
					}
				}
				setNodesList(newList);
			} catch (Exception e) {
				logger.error("LoadBalancer [{}]: Exception while adding Servers", name, e);
			}
		}
	}

	/**
	 * 设置服务节点（全量）
	 * @param srvString
	 */
	/**
	 * Set the list of servers used as the server pool. This overrides existing
	 * server list.
	 */
	public void setNodesList(Collection lsrv) {
		Lock writeLock = allServerLock.writeLock();
		logger.debug("LoadBalancer [{}]: clearing server list (SET op)", name);

		writeLock.lock();
		try {
			ArrayList<BalancingNode> allServers = new ArrayList<BalancingNode>();
			for (Object server : lsrv) {
				if (server == null) {
					continue;
				}

				if (server instanceof String) {
					server = new BalancingNode((String) server);
				}

				if (server instanceof BalancingNode) {
					logger.debug("LoadBalancer [{}]:  addServer [{}]", name, ((BalancingNode) server).getId());
					allServers.add((BalancingNode) server);
				} else {
					throw new IllegalArgumentException(
							"Type String or Server expected, instead found:" + server.getClass());
				}

			}
			boolean listChanged = false;
			if (!allServerList.equals(allServers)) {
				listChanged = true;
				if (changeListeners != null && changeListeners.size() > 0) {
					Collection<BalancingNode> oldList = ImmutableList.copyOf(allServerList);
					Collection<BalancingNode> newList = ImmutableList.copyOf(allServers);
					for (INodeListChangeListener l : changeListeners) {
						try {
							l.nodeListChanged(oldList, newList);
						} catch (Exception e) {
							logger.error("LoadBalancer [{}]: Error invoking server list change listener", name, e);
						}
					}
				}
			}
			// This will reset readyToServe flag to true on all servers
			// regardless whether
			// previous priming connections are success or not
			allServerList = allServers;
			if (canSkipPing()) {
				for (BalancingNode s : allServerList) {
					s.setAlive(true);
				}
				upServerList = allServerList;
			} else if (listChanged) {
				forceQuickPing();
			}
		} finally {
			//当节点发生变化，通知相应的负载均衡Rule
			this.rule.nodeUpdate();
			writeLock.unlock();
		}
	}

	/**
	 * 设置服务节点（全量）
	 * @param srvString
	 */
	/* List in string form. SETS, does not add. */
	void setNodes(String srvString) {
		if (srvString != null) {

			try {
				String[] serverArr = srvString.split(",");
				ArrayList<BalancingNode> newList = new ArrayList<BalancingNode>();

				for (String serverString : serverArr) {
					if (serverString != null) {
						serverString = serverString.trim();
						if (serverString.length() > 0) {
							BalancingNode svr = new BalancingNode(serverString);
							newList.add(svr);
						}
					}
				}
				setNodesList(newList);
			} catch (Exception e) {
				logger.error("LoadBalancer [{}]: Exception while adding Servers", name, e);
			}
		}
	}

	/**
	 * return the server
	 * 
	 * @param index
	 * @param availableOnly
	 */
	public BalancingNode getServerByIndex(int index, boolean availableOnly) {
		try {
			return (availableOnly ? upServerList.get(index) : allServerList.get(index));
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public List<BalancingNode> getNodeList(boolean availableOnly) {
		return (availableOnly ? getReachableNodes() : getAllNodes());
	}

	/**
	 * 获得alive为True的服务器列表
	 */
	@Override
	public List<BalancingNode> getReachableNodes() {
		return Collections.unmodifiableList(upServerList);
	}

	/**
	 * 获得所有服务器列表
	 */
	@Override
	public List<BalancingNode> getAllNodes() {
		return Collections.unmodifiableList(allServerList);
	}

	/**
	 * 获得服务器列表
	 */
	@Override
	public Collection<BalancingNode> getNodeList(NodeGroup nodeGroup) {
		switch (nodeGroup) {
		case ALL:
			return allServerList;
		case STATUS_UP:
			return upServerList;
		case STATUS_NOT_UP:
			ArrayList<BalancingNode> notAvailableServers = new ArrayList<BalancingNode>(allServerList);
			ArrayList<BalancingNode> upServers = new ArrayList<BalancingNode>(upServerList);
			notAvailableServers.removeAll(upServers);
			return notAvailableServers;
		}
		return new ArrayList<BalancingNode>();
	}

	/**
	 * 取消Ping定时任务
	 */
	public void cancelPingTask() {
		if (lbTimer != null) {
			lbTimer.cancel();
		}
	}

	/**
	 * Ping的定时任务定义
	 */
	/**
	 * TimerTask that keeps runs every X seconds to check the status of each
	 * server/node in the Server List
	 * 
	 * @author stonse
	 * 
	 */
	class PingTask extends TimerTask {
		public void run() {
			try {
				new Pinger(pingStrategy).runPinger();
			} catch (Exception e) {
				logger.error("LoadBalancer [{}]: Error pinging", name, e);
			}
		}
	}

	/**
	 * Class that contains the mechanism to "ping" all the instances
	 * 
	 * @author stonse
	 *
	 */
	/**
	 * 运行Ping的类，根据Ping结果设置setAlive值
	 * @author yiyongfei
	 *
	 */
	class Pinger {

		private final IPingStrategy pingerStrategy;

		public Pinger(IPingStrategy pingerStrategy) {
			this.pingerStrategy = pingerStrategy;
		}

		public void runPinger() throws Exception {
			if (!pingInProgress.compareAndSet(false, true)) {
				return; // Ping in progress - nothing to do
			}

			// we are "in" - we get to Ping

			BalancingNode[] allServers = null;
			boolean[] results = null;

			Lock allLock = null;
			Lock upLock = null;

			try {
				/*
				 * The readLock should be free unless an addServer operation is
				 * going on...
				 */
				allLock = allServerLock.readLock();
				allLock.lock();
				allServers = allServerList.toArray(new BalancingNode[allServerList.size()]);
				allLock.unlock();

				int numCandidates = allServers.length;
				results = pingerStrategy.pingNodes(ping, allServers);

				final List<BalancingNode> newUpList = new ArrayList<BalancingNode>();
				final List<BalancingNode> changedServers = new ArrayList<BalancingNode>();

				for (int i = 0; i < numCandidates; i++) {
					boolean isAlive = results[i];
					BalancingNode svr = allServers[i];
					boolean oldIsAlive = svr.isAlive();

					svr.setAlive(isAlive);

					if (oldIsAlive != isAlive) {
						changedServers.add(svr);
					}

					if (isAlive) {
						newUpList.add(svr);
					}
				}
				upLock = upServerLock.writeLock();
				upLock.lock();
				upServerList = newUpList;
				upLock.unlock();

				notifyServerStatusChangeListener(changedServers);
			} finally {
				pingInProgress.set(false);
			}
		}
	}

	private void notifyServerStatusChangeListener(final Collection<BalancingNode> changedServers) {
		if (changedServers != null && !changedServers.isEmpty() && !serverStatusListeners.isEmpty()) {
			for (INodeStatusChangeListener listener : serverStatusListeners) {
				try {
					listener.nodeStatusChanged(changedServers);
				} catch (Exception e) {
					logger.error("LoadBalancer [{}]: Error invoking server status change listener", name, e);
				}
			}
		}
	}

	private final Counter createCounter() {
		return Monitors.newCounter("LoadBalancer_ChooseServer");
	}

	/**
	 * 基于Key，根据指定的负载均衡Rule获取节点
	 * @param key
	 * @return
	 */
	/*
	 * Get the alive server dedicated to key
	 * 
	 * @return the dedicated server
	 */
	public BalancingNode chooseNode(Object key) {
		if (counter == null) {
			counter = createCounter();
		}
		counter.increment();
		if (rule == null) {
			return null;
		} else {
			try {
				return rule.choose(key);
			} catch (Exception e) {
				return null;
			}
		}
	}

	/**
	 * 基于Key，根据指定的负载均衡Rule获取节点
	 * @param key
	 * @return
	 */
	/* Returns either null, or "server:port/servlet" */
	public String choose(Object key) {
		if (rule == null) {
			return null;
		} else {
			try {
				BalancingNode svr = rule.choose(key);
				return ((svr == null) ? null : svr.getId());
			} catch (Exception e) {
				logger.warn("LoadBalancer [{}]:  Error choosing server", name, e);
				return null;
			}
		}
	}

	/**
	 * 增加方法：基于远程服务器的地址直接获取节点
	 */
	public Collection<BalancingNode> chooseNode(SocketAddress address) {
		Collection<BalancingNode> tmp = new ArrayList<BalancingNode>();
		Iterator<BalancingNode> it = allServerList.iterator();
		while (it.hasNext()) {
			BalancingNode node = it.next();
			if (node.getSocketAddress().equals(address)) {
				tmp.add(node);
			}
		}
		return tmp;
	}

	/**
	 * 增加方法：根据远程服务器的地址判断该服务器是否在负载均衡内
	 */
	public boolean contains(SocketAddress address) {
		Iterator<BalancingNode> it = this.allServerList.iterator();
		while (it.hasNext()) {
			BalancingNode node = it.next();
			if (node.getSocketAddress().equals(address)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 标记服务Down
	 */
	public void markNodeDown(BalancingNode server) {
		if (server == null || !server.isAlive()) {
			return;
		}

		logger.error("LoadBalancer [{}]:  markServerDown called on [{}]", name, server.getId());
		server.setAlive(false);

		notifyServerStatusChangeListener(singleton(server));
	}

	public void markNodeDown(String id) {
		boolean triggered = false;

		id = BalancingNode.normalizeId(id);

		if (id == null) {
			return;
		}

		Lock writeLock = upServerLock.writeLock();
		writeLock.lock();
		try {
			final Collection<BalancingNode> changedServers = new ArrayList<BalancingNode>();

			for (BalancingNode svr : upServerList) {
				if (svr.isAlive() && (svr.getId().equals(id))) {
					triggered = true;
					svr.setAlive(false);
					changedServers.add(svr);
				}
			}

			if (triggered) {
				logger.error("LoadBalancer [{}]:  markServerDown called for server [{}]", name, id);
				notifyServerStatusChangeListener(changedServers);
			}

		} finally {
			writeLock.unlock();
		}
	}

	/*
	 * Force an immediate ping, if we're not currently pinging and don't have a
	 * quick-ping already scheduled.
	 */
	/**
	 * 立即Ping，检查所连接服务器的有效性
	 */
	public void forceQuickPing() {
		if (canSkipPing()) {
			return;
		}
		logger.debug("LoadBalancer [{}]:  forceQuickPing invoking", name);

		try {
			new Pinger(pingStrategy).runPinger();
		} catch (Exception e) {
			logger.error("LoadBalancer [{}]: Error running forceQuickPing()", name, e);
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{NFLoadBalancer:name=").append(this.getName()).append(",current list of Servers=")
				.append(this.allServerList).append(",/nLoad balancer stats=").append(this.lbStats.toString())
				.append("}");
		return sb.toString();
	}

	/**
	 * Register with monitors and start priming connections if it is set.
	 */
	protected void init() {
		Monitors.registerObject("LoadBalancer_" + name, this);
		// register the rule as it contains metric for available servers count
		Monitors.registerObject("Rule_" + name, this.getRule());
	}

	public void shutdown() {
		cancelPingTask();
		Monitors.unregisterObject("LoadBalancer_" + name, this);
		Monitors.unregisterObject("Rule_" + name, this.getRule());
	}

	/**
	 * Default implementation for <c>IPingStrategy</c>, performs ping serially,
	 * which may not be desirable, if your <c>IPing</c> implementation is slow,
	 * or you have large number of servers.
	 */
	/**
	 * 定期Ping 服务器，影响Server的isAlive属性
	 * @author yiyongfei
	 *
	 */
	private static class SerialPingStrategy implements IPingStrategy {

		@Override
		public boolean[] pingNodes(IPing ping, BalancingNode[] servers) {
			int numCandidates = servers.length;
			boolean[] results = new boolean[numCandidates];

			logger.debug("LoadBalancer:  PingTask executing [{}] servers configured", numCandidates);

			for (int i = 0; i < numCandidates; i++) {
				results[i] = false; /* Default answer is DEAD. */
				try {
					// NOTE: IFF we were doing a real ping
					// assuming we had a large set of servers (say 15)
					// the logic below will run them serially
					// hence taking 15 times the amount of time it takes
					// to ping each server
					// A better method would be to put this in an executor
					// pool
					// But, at the time of this writing, we dont REALLY
					// use a Real Ping (its mostly in memory eureka call)
					// hence we can afford to simplify this design and run
					// this
					// serially
					if (ping != null) {
						results[i] = ping.isAlive(servers[i]);
					}
				} catch (Exception e) {
					logger.error("Exception while pinging Server: '{}'", servers[i], e);
				}
			}
			return results;
		}
	}
}
