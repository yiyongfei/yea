package com.yea.loadbalancer.rx;

import com.yea.core.loadbalancer.BalancingNode;

import rx.Observable;
import rx.functions.Func1;


/**
 * Provide the {@link rx.Observable} for a specified server. Used by {@link com.netflix.loadbalancer.reactive.LoadBalancerCommand}
 *
 * @param <T> Output type
 */
public interface NodeOperation<T> extends Func1<BalancingNode, Observable<T>> {
    /**
     * @return A lazy {@link Observable} for the server supplied. It is expected
     * that the actual execution is not started until the returned {@link Observable} is subscribed to.
     */
    @Override
    public Observable<T> call(BalancingNode node);
}
