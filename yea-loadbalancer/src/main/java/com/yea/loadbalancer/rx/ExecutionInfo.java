package com.yea.loadbalancer.rx;

import com.yea.core.loadbalancer.BalancingNode;

/**
 * Represents the state of execution for an instance of {@link com.netflix.loadbalancer.reactive.LoadBalancerCommand}
 * and is passed to {@link ExecutionListener}
 *
 * @author Allen Wang
 */
public class ExecutionInfo {

    private final BalancingNode server;
    private final int numberOfPastAttemptsOnServer;
    private final int numberOfPastServersAttempted;

    private ExecutionInfo(BalancingNode server, int numberOfPastAttemptsOnServer, int numberOfPastServersAttempted) {
        this.server = server;
        this.numberOfPastAttemptsOnServer = numberOfPastAttemptsOnServer;
        this.numberOfPastServersAttempted = numberOfPastServersAttempted;
    }

    public static ExecutionInfo create(BalancingNode server, int numberOfPastAttemptsOnServer, int numberOfPastServersAttempted) {
        return new ExecutionInfo(server, numberOfPastAttemptsOnServer, numberOfPastServersAttempted);
    }

    public BalancingNode getServer() {
        return server;
    }

    public int getNumberOfPastAttemptsOnServer() {
        return numberOfPastAttemptsOnServer;
    }

    public int getNumberOfPastServersAttempted() {
        return numberOfPastServersAttempted;
    }

    @Override
    public String toString() {
        return "ExecutionInfo{" +
                "server=" + server +
                ", numberOfPastAttemptsOnServer=" + numberOfPastAttemptsOnServer +
                ", numberOfPastServersAttempted=" + numberOfPastServersAttempted +
                '}';
    }
}
