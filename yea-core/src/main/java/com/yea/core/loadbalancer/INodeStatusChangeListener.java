package com.yea.core.loadbalancer;

import java.util.Collection;

public interface INodeStatusChangeListener {

    /**
     * when server status has changed (e.g. when marked as down or found dead by ping).
     *
     * @param servers the servers that had their status changed, never {@code null}
     */
    public void nodeStatusChanged(Collection<BalancingNode> nodes);

}
