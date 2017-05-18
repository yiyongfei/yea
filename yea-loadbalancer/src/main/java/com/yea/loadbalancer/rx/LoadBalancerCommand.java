package com.yea.loadbalancer.rx;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Func2;

import com.netflix.servo.monitor.Stopwatch;
import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.ILoadBalancer;
import com.yea.core.loadbalancer.IRetryHandler;
import com.yea.loadbalancer.ClientException;
import com.yea.loadbalancer.LoadBalancerContext;
import com.yea.loadbalancer.ServerStats;
import com.yea.loadbalancer.config.IClientConfig;
import com.yea.loadbalancer.rx.ExecutionListener.AbortExecutionException;

/**
 * A command that is used to produce the Observable from the load balancer execution. The load balancer is responsible for
 * the following:
 *
 * <ul>
 * <li>Choose a server</li>
 * <li>Invoke the {@link #call(com.yea.core.loadbalancer.BalancingNode.Server)} method</li>
 * <li>Invoke the {@link ExecutionListener} if any</li>
 * <li>Retry on exception, controlled by {@link com.yea.core.loadbalancer.IRetryHandler.RetryHandler}</li>
 * <li>Provide feedback to the {@link com.netflix.loadbalancer.LoadBalancerStats}</li>
 * </ul>
 *
 * @author Allen Wang
 */
public class LoadBalancerCommand<T> {
    private static final Logger logger = LoggerFactory.getLogger(LoadBalancerCommand.class);

    public static class Builder<T> {
        private IRetryHandler        retryHandler;
        private ILoadBalancer       loadBalancer;
        private IClientConfig       config;
        private LoadBalancerContext loadBalancerContext;
        private List<? extends ExecutionListener<?, T>> listeners;
        private Object              loadBalancerKey;
        private ExecutionContext<?> executionContext;
        private ExecutionContextListenerInvoker invoker;
        private URI                 loadBalancerURI;
        private BalancingNode              server;
        
        private Builder() {}
    
        public Builder<T> withLoadBalancer(ILoadBalancer loadBalancer) {
            this.loadBalancer = loadBalancer;
            return this;
        }
    
        public Builder<T> withLoadBalancerURI(URI loadBalancerURI) {
            this.loadBalancerURI = loadBalancerURI;
            return this;
        }
        
        public Builder<T> withListeners(List<? extends ExecutionListener<?, T>> listeners) {
            if (this.listeners == null) {
                this.listeners = new LinkedList<ExecutionListener<?, T>>(listeners);
            } else {
                this.listeners.addAll((Collection) listeners);
            }
            return this;
        }
    
        public Builder<T> withRetryHandler(IRetryHandler retryHandler) {
            this.retryHandler = retryHandler;
            return this;
        }
    
        public Builder<T> withClientConfig(IClientConfig config) {
            this.config = config;
            return this;
        }
    
        /**
         * Pass in an optional key object to help the load balancer to choose a specific server among its
         * server list, depending on the load balancer implementation.
         */
        public Builder<T> withServerLocator(Object key) {
            this.loadBalancerKey = key;
            return this;
        }
    
        public Builder<T> withLoadBalancerContext(LoadBalancerContext loadBalancerContext) {
            this.loadBalancerContext = loadBalancerContext;
            return this;
        }
    
        public Builder<T> withExecutionContext(ExecutionContext<?> executionContext) {
            this.executionContext = executionContext;
            return this;
        }
        
        /**
         * Pin the operation to a specific server.  Otherwise run on any server returned by the load balancer
         * 
         * @param server
         */
        public Builder<T> withServer(BalancingNode server) {
            this.server = server;
            return this;
        }
        
        public LoadBalancerCommand<T> build() {
            if (loadBalancerContext == null && loadBalancer == null) {
                throw new IllegalArgumentException("Either LoadBalancer or LoadBalancerContext needs to be set");
            }
            
            if (listeners != null && listeners.size() > 0) {
                this.invoker = new ExecutionContextListenerInvoker(executionContext, listeners, config);
            }
            
            if (loadBalancerContext == null) {
                loadBalancerContext = new LoadBalancerContext(loadBalancer, config);
            }
            
            return new LoadBalancerCommand<T>(this);
        }
    }
    
    public static <T> Builder<T> builder() {
        return new Builder<T>();
    }

    private final URI    loadBalancerURI;
    private final Object loadBalancerKey;
    
    private final LoadBalancerContext loadBalancerContext;
    private final IRetryHandler retryHandler;
    private volatile ExecutionInfo executionInfo;
    private final BalancingNode server;

    private final ExecutionContextListenerInvoker<?, T> listenerInvoker;
    
    private LoadBalancerCommand(Builder<T> builder) {
        this.loadBalancerURI     = builder.loadBalancerURI;
        this.loadBalancerKey     = builder.loadBalancerKey;
        this.loadBalancerContext = builder.loadBalancerContext;
        this.retryHandler        = builder.retryHandler != null ? builder.retryHandler : loadBalancerContext.getRetryHandler();
        this.listenerInvoker     = builder.invoker;
        this.server              = builder.server;
    }
    
    /**
     * Return an Observable that either emits only the single requested server
     * or queries the load balancer for the next server on each subscription
     */
    private Observable<BalancingNode> selectServer() {
        return Observable.create(new OnSubscribe<BalancingNode>() {
            @Override
            public void call(Subscriber<? super BalancingNode> next) {
                try {
                    BalancingNode server = loadBalancerContext.getServerFromLoadBalancer(loadBalancerURI, loadBalancerKey);   
                    next.onNext(server);
                    next.onCompleted();
                } catch (Exception e) {
                    next.onError(e);
                }
            }
        });
    }
    
    class ExecutionInfoContext {
        BalancingNode      server;
        int         serverAttemptCount = 0;
        int         attemptCount = 0;
        
        public void setServer(BalancingNode server) {
            this.server = server;
            this.serverAttemptCount++;
            
            this.attemptCount = 0;
        }
        
        public void incAttemptCount() {
            this.attemptCount++;
        }

        public int getAttemptCount() {
            return attemptCount;
        }

        public BalancingNode getServer() {
            return server;
        }

        public int getServerAttemptCount() {
            return this.serverAttemptCount;
        }

        public ExecutionInfo toExecutionInfo() {
            return ExecutionInfo.create(server, attemptCount-1, serverAttemptCount-1);
        }

        public ExecutionInfo toFinalExecutionInfo() {
            return ExecutionInfo.create(server, attemptCount, serverAttemptCount-1);
        }

    }
    
    private Func2<Integer, Throwable, Boolean> retryPolicy(final int maxRetrys, final boolean same) {
        return new Func2<Integer, Throwable, Boolean>() {
            @Override
            public Boolean call(Integer tryCount, Throwable e) {
                if (e instanceof AbortExecutionException) {
                    return false;
                }

                if (tryCount > maxRetrys) {
                    return false;
                }
                
                if (e.getCause() != null && e instanceof RuntimeException) {
                    e = e.getCause();
                }
                
                return retryHandler.isRetriableException(e, same);
            }
        };
    }

    /**
     * Create an {@link Observable} that once subscribed execute network call asynchronously with a server chosen by load balancer.
     * If there are any errors that are indicated as retriable by the {@link IRetryHandler}, they will be consumed internally by the
     * function and will not be observed by the {@link Observer} subscribed to the returned {@link Observable}. If number of retries has
     * exceeds the maximal allowed, a final error will be emitted by the returned {@link Observable}. Otherwise, the first successful
     * result during execution and retries will be emitted.
     */
    public Observable<T> submit(final NodeOperation<T> operation) {
        final ExecutionInfoContext context = new ExecutionInfoContext();
        
        if (listenerInvoker != null) {
            try {
                listenerInvoker.onExecutionStart();
            } catch (AbortExecutionException e) {
                return Observable.error(e);
            }
        }

        final int maxRetrysSame = retryHandler.getMaxRetriesOnSameServer();
        final int maxRetrysNext = retryHandler.getMaxRetriesOnNextServer();

        // Use the load balancer
        Observable<T> o = 
                (server == null ? selectServer() : Observable.just(server))
                .concatMap(new Func1<BalancingNode, Observable<T>>() {
                    @Override
                    // Called for each server being selected
                    public Observable<T> call(BalancingNode server) {
                        context.setServer(server);
                        final ServerStats stats = loadBalancerContext.getServerStats(server);
                        
                        // Called for each attempt and retry
                        Observable<T> o = Observable
                                .just(server)
                                .concatMap(new Func1<BalancingNode, Observable<T>>() {
                                    @Override
                                    public Observable<T> call(final BalancingNode server) {
                                        context.incAttemptCount();
                                        loadBalancerContext.noteOpenConnection(stats);
                                        
                                        if (listenerInvoker != null) {
                                            try {
                                                listenerInvoker.onStartWithServer(context.toExecutionInfo());
                                            } catch (AbortExecutionException e) {
                                                return Observable.error(e);
                                            }
                                        }
                                        
                                        final Stopwatch tracer = loadBalancerContext.getExecuteTracer().start();
                                        
                                        return operation.call(server).doOnEach(new Observer<T>() {
                                            private T entity;
                                            @Override
                                            public void onCompleted() {
                                            	recordStats(tracer, stats, entity, null);
                                                // TODO: What to do if onNext or onError are never called?
                                            }

                                            @Override
                                            public void onError(Throwable e) {
                                                recordStats(tracer, stats, null, e);
                                                logger.debug("Got error {} when executed on server {}", e, server);
                                                if (listenerInvoker != null) {
                                                    listenerInvoker.onExceptionWithServer(e, context.toExecutionInfo());
                                                }
                                            }

                                            @Override
                                            public void onNext(T entity) {
                                            	this.entity = entity;
                                                if (listenerInvoker != null) {
                                                    listenerInvoker.onExecutionSuccess(entity, context.toExecutionInfo());
                                                }
                                            }                            
                                            
                                            private void recordStats(Stopwatch tracer, ServerStats stats, Object entity, Throwable exception) {
                                                tracer.stop();
                                                loadBalancerContext.noteRequestCompletion(stats, entity, exception, tracer.getDuration(TimeUnit.MILLISECONDS), retryHandler);
                                            }
                                        });
                                    }
                                });
                        
                        if (maxRetrysSame > 0) 
                            o = o.retry(retryPolicy(maxRetrysSame, true));
                        return o;
                    }
                });
            
        if (maxRetrysNext > 0 && server == null) 
            o = o.retry(retryPolicy(maxRetrysNext, false));
        
        return o.onErrorResumeNext(new Func1<Throwable, Observable<T>>() {
            @Override
            public Observable<T> call(Throwable e) {
                if (context.getAttemptCount() > 0) {
                    if (maxRetrysNext > 0 && context.getServerAttemptCount() == (maxRetrysNext + 1)) {
                        e = new ClientException(ClientException.ErrorType.NUMBEROF_RETRIES_NEXTSERVER_EXCEEDED,
                                "Number of retries on next server exceeded max " + maxRetrysNext
                                + " retries, while making a call for: " + context.getServer(), e);
                    }
                    else if (maxRetrysSame > 0 && context.getAttemptCount() == (maxRetrysSame + 1)) {
                        e = new ClientException(ClientException.ErrorType.NUMBEROF_RETRIES_EXEEDED,
                                "Number of retries exceeded max " + maxRetrysSame
                                + " retries, while making a call for: " + context.getServer(), e);
                    }
                }
                if (listenerInvoker != null) {
                    listenerInvoker.onExecutionFailed(e, context.toFinalExecutionInfo());
                }
                return Observable.error(e);
            }
        });
    }
}
