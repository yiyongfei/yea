package com.yea.core.loadbalancer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class BalancingNode {

    /**
     * Additional meta information of a server, which contains
     * information of the targeting application, as well as server identification
     * specific for a deployment environment, for example, AWS.
     */
    public static interface MetaInfo {
        /**
         * @return the name of application that runs on this server, null if not available
         */
        public String getAppName();

        /**
         * @return the group of the server, for example, auto scaling group ID in AWS.
         * Null if not available
         */
        public String getServerGroup();

        /**
         * @return A virtual address used by the server to register with discovery service.
         * Null if not available
         */
        public String getServiceIdForDiscovery();

        /**
         * @return ID of the server
         */
        public String getInstanceId();
    }

    public static final String UNKNOWN_ZONE = "UNKNOWN";
    private String host;
    private int port = 80;
    private SocketAddress socketAddress;
    private volatile String id;
    private volatile boolean isAliveFlag;
    private volatile boolean isSuspendedFlag;
    private volatile boolean isDownFlag;
    private String zone = UNKNOWN_ZONE;
    private volatile boolean readyToServe = true;

    private MetaInfo simpleMetaInfo = new MetaInfo() {
        @Override
        public String getAppName() {
            return null;
        }

        @Override
        public String getServerGroup() {
            return null;
        }

        @Override
        public String getServiceIdForDiscovery() {
            return null;
        }

        @Override
        public String getInstanceId() {
            return id;
        }
    };

    public BalancingNode(String host, int port) {
        this.host = host;
        this.port = port;
        this.id = host + ":" + port;
        this.socketAddress = new InetSocketAddress(host, port);
        isAliveFlag = false;
    }

    /* host:port combination */
    public BalancingNode(String id) {
        setId(id);
        isAliveFlag = false;
    }

    // No reason to synchronize this, I believe.
    // The assignment should be atomic, and two setAlive calls
    // with conflicting results will still give nonsense(last one wins)
    // synchronization or no.

    public void setAlive(boolean isAliveFlag) {
        this.isAliveFlag = isAliveFlag;
    }

    public boolean isAlive() {
        return isAliveFlag;
    }
    
    public void setDown(boolean isDownFlag) {
        this.isDownFlag = isDownFlag;
    }

    public boolean isDown() {
        return isDownFlag;
    }
    
    public void setSuspended(boolean isSuspendedFlag) {
    	this.isSuspendedFlag = isSuspendedFlag;
    }
    
    /**
     * 临时中断服务
     * 当Netty写满、被熔断、短期内太缓慢均会临时中断
     * @return True表示临时中断，服务不可用，False正常，服务可用
     */
    public boolean isSuspended() {
    	return isSuspendedFlag;
    }

    @Deprecated
    public void setHostPort(String hostPort) {
        setId(hostPort);
    }

    static public String normalizeId(String id) {
        Pair<String, Integer> hostPort = getHostPort(id);
        if (hostPort == null) {
            return null;
        } else {
            return hostPort.first() + ":" + hostPort.second();
        }
    }

    static Pair<String, Integer> getHostPort(String id) {
        if (id != null) {
            String host = null;
            int port = 80;

            if (id.toLowerCase().startsWith("http://")) {
                id = id.substring(7);
            } else if (id.toLowerCase().startsWith("https://")) {
                id = id.substring(8);
            } else if (id.toLowerCase().startsWith("/")) {
            	id = id.substring(1);
            }

            if (id.contains("/")) {
                int slash_idx = id.indexOf("/");
                id = id.substring(0, slash_idx);
            }

            int colon_idx = id.indexOf(':');

            if (colon_idx == -1) {
                host = id; // default
                port = 80;
            } else {
                host = id.substring(0, colon_idx);
                try {
                    port = Integer.parseInt(id.substring(colon_idx + 1));
                } catch (NumberFormatException e) {
                    throw e;
                }
            }
            return new Pair<String, Integer>(host, port);
        } else {
            return null;
        }

    }

    public void setId(String id) {
        Pair<String, Integer> hostPort = getHostPort(id);
        if (hostPort != null) {
            this.id = hostPort.first() + ":" + hostPort.second();
            this.host = hostPort.first();
            this.port = hostPort.second();
            this.socketAddress = new InetSocketAddress(this.host, this.port);
        } else {
            this.id = null;
        }
    }

    public void setPort(int port) {
        this.port = port;

        if (host != null) {
            id = host + ":" + port;
        }
    }

    public void setHost(String host) {
        if (host != null) {
            this.host = host;
            id = host + ":" + port;
        }
    }

    public String getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getHostPort() {
        return host + ":" + port;
    }
    
    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    public MetaInfo getMetaInfo() {
        return simpleMetaInfo;
    }

    public void ping(int timeout) throws Throwable {
    	//由子类实现，不抛异常表示Ping成功，否则就是失败，包含超时
    }
    
    public String toString() {
        return this.getId();
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof BalancingNode))
            return false;
        BalancingNode svc = (BalancingNode) obj;
        return svc.getId().equals(this.getId());

    }

    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (null == this.getId() ? 0 : this.getId().hashCode());
        return hash;
    }

    public final String getZone() {
        return zone;
    }

    public final void setZone(String zone) {
        this.zone = zone;
    }

    public final boolean isReadyToServe() {
        return readyToServe;
    }

    public final void setReadyToServe(boolean readyToServe) {
        this.readyToServe = readyToServe;
    }

}
