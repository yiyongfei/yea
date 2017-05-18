/**
 * Copyright 2017 伊永飞
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yea.remote.netty.balancing;

import java.net.SocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.yea.core.base.id.UUIDGenerator;
import com.yea.core.loadbalancer.AbstractBalancingNode;
import com.yea.core.remote.AbstractEndpoint;
import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.observer.Observable;
import com.yea.core.remote.struct.CallAct;
import com.yea.core.remote.struct.CallReflect;
import com.yea.core.remote.struct.Header;
import com.yea.core.remote.struct.Message;
import com.yea.remote.netty.client.promise.AwaitPromise;
import com.yea.remote.netty.constants.NettyConstants;
import com.yea.remote.netty.exception.WriteRejectException;
import com.yea.remote.netty.promise.NettyChannelPromise;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * 
 * @author yiyongfei
 *
 */
public class RemoteClient extends AbstractBalancingNode {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteClient.class);
    private Channel channel = null;
    private final Queue<Info> sendQueue = new ConcurrentLinkedQueue<Info>();//发送队列
	
	public RemoteClient(Channel channel, AbstractEndpoint endpoint){
		super(endpoint, channel.remoteAddress());
    	this.channel = channel;
    }
	
	/**
	 * 重置熔断器权重：
	 * 曾经熔断，并且队列为空时，取消熔断的权重
	 */
	@Override
	protected boolean resetHystrixHealth() {
		if (hystrix == 0 && sendQueue.isEmpty()) {
			hystrix = 1.0;
			return true;
		}
		return false;
	}
	
	/**
	 * 重置Channel发送(Write)权重
	 */
	@Override
	protected boolean resetSendHealth() {
		if (unwrite == 0 && channel.isWritable()) {
			unwrite = 1.0;
			return true;
		}
		if (unwrite == 1 && !channel.isWritable()) {
			unwrite = 0.0;
			return true;
		}
		return false;
	}
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <T> NettyChannelPromise<T> send(CallAct act, List<GenericFutureListener> listeners, RemoteConstants.MessageType messageType, byte[] sessionID, Object[] messages) throws Exception {
		if (!this.channel.isWritable()) {
			throw new WriteRejectException();
		}
		AwaitPromise observer = new AwaitPromise(new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE));
        if(listeners != null && listeners.size() > 0){
            for(GenericFutureListener listener : listeners){
            	observer.addListener(listener);
            }
        }
        Observable handle = null;
        Iterator<Map.Entry<String, ChannelHandler>> itHandle = channel.pipeline().iterator();
        while(itHandle.hasNext()){
            Map.Entry<String, ChannelHandler> entry = itHandle.next();
            if(entry.getValue() instanceof Observable){
                handle = (Observable) entry.getValue();
                handle.addObserver(sessionID, observer);
            }
        }
        
        Message nettyMessage = new Message();
        Header header = new Header();
        header.setType(messageType.value());
        header.setSessionID(sessionID);
        header.setAttachment(new HashMap<String, Object>());
        if(act != null){
        	header.getAttachment().put(NettyConstants.MessageHeaderAttachment.CALL_ACT.value(), act.getActName());
			if (!StringUtils.isEmpty(act.getCallbackName())) {
				header.getAttachment().put(NettyConstants.MessageHeaderAttachment.CALLBACK_ACT.value(), act.getCallbackName());
			}
            if(act instanceof CallReflect) {
            	header.getAttachment().put(NettyConstants.MessageHeaderAttachment.CALL_REFLECT.value(), act);
            }
        } else {
        	if(messageType.value() == RemoteConstants.MessageType.SERVICE_REQ.value()) {
        		throw new Exception("发送服务请求时请提供对方系统的Act名");
        	}
        }
        nettyMessage.setHeader(header);
        nettyMessage.setBody(messages);
        
		try {
			sendQueue.add(new Info(nettyMessage, observer));
		} catch (Exception ex) {
			Thread.sleep(200);
			throw ex;
		}
        
        readyWriteAndFlush();
        
        LOGGER.debug(this.getLocalAddress() + "将向远程节点" + this.getRemoteAddress() + "请求" + act.getActName() + "服务[标识:" + UUIDGenerator.restore(sessionID) + "]！");
        return observer;
    }
    
    @Override
	public void ping(int timeout) throws Throwable {
		if (!this.channel.isWritable()) {
			throw new WriteRejectException();
		}
		byte[] sessionID = UUIDGenerator.generate();
		AwaitPromise<Boolean> promise = new AwaitPromise<Boolean>(new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE));
		Observable handle = null;
		Iterator<Map.Entry<String, ChannelHandler>> itHandle = channel.pipeline().iterator();
		while (itHandle.hasNext()) {
			Map.Entry<String, ChannelHandler> entry = itHandle.next();
			if (entry.getValue() instanceof Observable) {
				handle = (Observable) entry.getValue();
				handle.addObserver(sessionID, promise);
			}
		}
		Message nettyMessage = new Message();
		Header header = new Header();
		header.setType(RemoteConstants.MessageType.PING_REQ.value());
		header.setSessionID(sessionID);
		nettyMessage.setHeader(header);
		channel.writeAndFlush(nettyMessage, promise);
		promise.awaitObject(timeout);
	}
    
	private final AtomicBoolean scheduled = new AtomicBoolean(false);
	private static final int DEQUE_CHUNK_SIZE = 48;
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	
	private void readyWriteAndFlush() {
		if (scheduled.compareAndSet(false, true)) {
			executor.execute(flushRunnable);
		}
	}

	private final Runnable flushRunnable = new Runnable() {
		@Override
		public void run() {
			if (!sendQueue.isEmpty()) {
				try {
					flush();
				} catch (Exception e) {
				}
			}
		}
	};

	private final Queue<Info> hystrixQueue = new LinkedBlockingQueue<Info>();//熔断队列，熔断后未处理对象的存放地
	
	private void flush() throws Exception {
		try {
			Info info;
			SendCommond commond = new SendCommond();
			int i = 0;
			boolean flushedOnce = false;
			while ((info = sendQueue.poll()) != null) {
				commond.write(info);
				if (++i == DEQUE_CHUNK_SIZE) {
					i = 0;
					flushedOnce = true;
					Boolean result = commond.execute();
					if(result) {
						Thread.sleep(20);
					} else {
						Thread.sleep(20);
						long basedate = new Date().getTime();
						while((!this.channel.isWritable()) || commond.isCircuitBreakerOpen()) {
							if((new Date().getTime() - basedate) > 200) {
								break;
							}
							Thread.sleep(50);
						}
					}
					commond = new SendCommond();
				}
			}
			while ((info = hystrixQueue.poll()) != null) {
				commond.write(info);
			}
			
			// 最后一次flush，确保之前write的部分被flush到Socket通道里.
			if (i != 0 || !flushedOnce) {
				Boolean result = commond.execute();
				if(!result) {
					Thread.sleep(100);
				}
			}
		} finally {
			Thread.sleep(50);
			if (!sendQueue.isEmpty() || !hystrixQueue.isEmpty()) {
				flush();
			} else {
				scheduled.set(false);
			}
		}
	}
        
    /** 
     * @see com.jbs.core.balancing.hash.BalancingNode#getSocketAddress()
     */
    public SocketAddress getRemoteAddress() {
        return channel.remoteAddress();
    }
    
    public SocketAddress getLocalAddress() {
        return channel.localAddress();
    }

    public Channel getChannel() {
		return channel;
	}

	private class SendCommond extends HystrixCommand<Boolean> {
		private final Queue<Info> queue = new LinkedBlockingQueue<Info>();//发送队列

		SendCommond() {
			super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("NETTY_CONSUMER"))
					.andCommandKey(HystrixCommandKey.Factory.asKey("NETTY_" + getEndpoint().getRegisterName()))
					.andThreadPoolKey(HystrixThreadPoolKey.Factory
							.asKey(channel.localAddress().toString() + "->" + channel.remoteAddress().toString()))
					.andCommandPropertiesDefaults(
							HystrixCommandProperties.Setter()
									.withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
									.withExecutionTimeoutInMilliseconds(1000)
									.withCircuitBreakerSleepWindowInMilliseconds(2000)
									.withRequestCacheEnabled(false)
									.withRequestLogEnabled(false))
					.andThreadPoolPropertiesDefaults(
							HystrixThreadPoolProperties.Setter().withCoreSize(15).withMaximumSize(50)
									.withAllowMaximumSizeToDivergeFromCoreSize(true).withKeepAliveTimeMinutes(1)));
		}
		
		void write(Info info) {
			info.message.getHeader().getAttachment().put(NettyConstants.MessageHeaderAttachment.HEADER_DATE.value(), new Date());
			queue.add(info);
		}
		
		@Override
		protected Boolean run() throws Exception {
			if (channel.isWritable()) {
				flush();
				return true;
			} else {
				throw new WriteRejectException();
			}
		}
		
		protected void flush() throws Exception {
			try {
				int i = 0;
				Info info;
				while (channel.isWritable() && (info = queue.poll()) != null) {
					channel.write(info.message, info.promise);
					if (++i == DEQUE_CHUNK_SIZE) {
						break;
					}
				}
				if (!channel.isWritable()) {
					throw new WriteRejectException();
				}
			} finally {
				channel.flush();
				if (channel.isWritable() && !queue.isEmpty()) {
					flush();
				}
			}
		}

		@Override
		protected Boolean getFallback() {
			try {
				LOGGER.info("进入熔断，尚未处理余量："+queue.size()+"，熔断原因：" + this.executionResult);
				/*熔断处理*/
				if (!channel.isWritable()) {
					// Netty写通道满，不能写入
					renewServerHealth(RemoteConstants.ServerHealthType.SEND, 0.0);
				} else {
					if(this.isResponseShortCircuited()) {
						//短熔断打开，服务快速失败，客户端被熔断
						renewServerHealth(RemoteConstants.ServerHealthType.HYSTRIX, 0.0);
					} else if(this.isResponseRejected()) {
						//被拒绝，拒绝原因可能是信号量满或者线程池满，不能处理，服务快速失败，客户端被熔断
						renewServerHealth(RemoteConstants.ServerHealthType.HYSTRIX, 0.0);
					}
				}
				return false;
			} finally {
				Info info;
				/*熔断后剩余队列里的元素重新回发送队列*/
				while ((info = queue.poll()) != null) {
					hystrixQueue.add(info);
				}
			}
		}
	}
	
	private class Info {
		Message message;
		ChannelPromise promise;
		
		Info(Message message, ChannelPromise promise) {
			this.message = message;
			this.promise = promise;
		}
	}
	
}

/*
 * // --------------熔断器相关------------------ //
 * 熔断器在整个统计时间内是否开启的阀值，默认20。也就是在metricsRollingStatisticalWindowInMilliseconds（默认10s）内至少请求20次，熔断器才发挥起作用 
 * 熔断器是否开启的阀值，也就是说单位时间超过了阀值请求数，熔断器才开；
 * private final HystrixProperty circuitBreakerRequestVolumeThreshold;
 * //熔断器默认工作时间,默认:5秒.熔断器中断请求5秒后会进入半打开状态,放部分流量过去重试 
 * private final HystrixProperty circuitBreakerSleepWindowInMilliseconds; 
 * //是否启用熔断器,默认true. 启动 
 * private final HystrixProperty circuitBreakerEnabled; 
 * //默认:50%。当出错率超过50%后熔断器启动 
 * private final HystrixProperty circuitBreakerErrorThresholdPercentage;
 * //是否强制开启熔断器阻断所有请求,默认:false,不开启。置为true时，所有请求都将被拒绝，直接到fallback 
 * private final HystrixProperty circuitBreakerForceOpen; 
 * //是否允许熔断器忽略错误,默认false, 不开启 
 * private final HystrixProperty circuitBreakerForceClosed; 
 * //--------------信号量相关------------------ //
 * 使用信号量隔离时，命令调用最大的并发数,默认:10 
 * private final HystrixProperty executionIsolationSemaphoreMaxConcurrentRequests;
 * //使用信号量隔离时，命令fallback(降级)调用最大的并发数,默认:10 
 * private final HystrixProperty fallbackIsolationSemaphoreMaxConcurrentRequests; 
 * //--------------其他------------------
 * //使用命令调用隔离方式,默认:采用线程隔离,ExecutionIsolationStrategy.THREAD 
 * private final HystrixProperty executionIsolationStrategy; 
 * //使用线程隔离时，调用超时时间，默认:1秒 
 * private final HystrixProperty executionIsolationThreadTimeoutInMilliseconds;
 * //线程池的key,用于决定命令在哪个线程池执行 
 * private final HystrixProperty executionIsolationThreadPoolKeyOverride; 
 * //是否开启fallback降级策略 默认:true 
 * private final HystrixProperty fallbackEnabled; 
 * //使用线程隔离时，是否对命令执行超时的线程调用中断（Thread.interrupt()）操作.默认:true 
 * private final HystrixProperty executionIsolationThreadInterruptOnTimeout; 
 * //是否开启请求日志,默认:true 
 * private final HystrixProperty requestLogEnabled;
 * //是否开启请求缓存,默认:true 
 * private final HystrixProperty requestCacheEnabled; 
 * //Whether request caching is enabled. HystrixCollapserProperties
 * //请求合并是允许的最大请求数,默认: Integer.MAX_VALUE 
 * private final HystrixProperty maxRequestsInBatch; 
 * //批处理过程中每个命令延迟的时间,默认:10毫秒 
 * private final HystrixProperty timerDelayInMilliseconds; 
 * //批处理过程中是否开启请求缓存,默认:开启 
 * private final HystrixProperty requestCacheEnabled; 
 * 
 * HystrixThreadPoolProperties
 * //配置线程池大小,默认值10个. 建议值:请求高峰时99.5%的平均响应时间 + 向上预留一些即可 
 * private final HystrixProperty corePoolSize; 
 * // 配置线程值等待队列长度,默认值:-1
 * 建议值:-1表示不等待直接拒绝,测试表明线程池使用直接决绝策略+ 合适大小的非回缩线程池效率最高.所以不建议修改此值。
 * 当使用非回缩线程池时，queueSizeRejectionThreshold,keepAliveTimeMinutes 参数无效 
 * private final HystrixProperty maxQueueSize;
 * 
 */