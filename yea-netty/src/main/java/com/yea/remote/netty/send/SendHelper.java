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
package com.yea.remote.netty.send;

import java.util.Date;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.yea.core.base.id.UUIDGenerator;
import com.yea.core.loadbalancer.AbstractBalancingNode;
import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.struct.Message;
import com.yea.core.util.ScheduledExecutor;
import com.yea.remote.netty.client.promise.AwaitPromise;
import com.yea.remote.netty.exception.WriteRejectException;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;

/**
 * 发送助手，Netty客户端或服务端通过该助手将消息发往对方
 * 
 * @author yiyongfei
 *
 */
public class SendHelper {
	static final Logger LOGGER = LoggerFactory.getLogger(SendHelper.class);
	private Channel channel = null;
	private AbstractBalancingNode node;
	private IUnavailableSend unavailableSend;
	private final Queue<Info> sendQueue = new ConcurrentLinkedQueue<Info>();// 发送队列

	SendHelper(Channel channel, AbstractBalancingNode node, IUnavailableSend unavailableSend) {
		this.channel = channel;
		this.node = node;
		this.unavailableSend = unavailableSend;
	}

	public Queue<Info> getSendQueue() {
		return sendQueue;
	}

	/**
	 * 将消息存在队列里，然后调用readyWriteAndFlush方法将消息发往接受方
	 * 如果往队列填充消息时，Channel.isWritable为False，则直接抛出异常。
	 * 
	 * @param nettyMessage
	 * @param observer
	 */
	public void send(Message nettyMessage, AwaitPromise<?> observer) {
		if (!this.channel.isWritable()) {
			throw new WriteRejectException();
		}

		sendQueue.add(new Info(nettyMessage, observer));

		readyWriteAndFlush();
	}

	/**
	 * 该值的设置与WRITE_SPIN_COUNT有一定的关连性，还要考虑WRITE_BUFFER_HIGH_WATER_MARK的大小，设置时需要注意
	 * channel.write()时，会检查写链表里的总字节数，如果大于等于WRITE_BUFFER_HIGH_WATER_MARK时，会改变通道的是否可写状态，设置batchSize时要考虑该值与消息的大小
	 * 每次通过Socket发消息时，消息数不会超出WRITE_SPIN_COUNT，如果待发送消息数大于WRITE_SPIN_COUNT则会放入到任务队列中，等该IO线程处理完其他任务再运行，写链表的总字节数会恶性循环
	 * 
	 */
	private final int DEQUE_CHUNK_SIZE = 16;
	private int batchSize = DEQUE_CHUNK_SIZE;
	public SendHelper setBatchSize(int batchSize) {
		this.batchSize = batchSize;
		return this;
	}
	
	
	private final AtomicBoolean scheduled = new AtomicBoolean(false);
	private final ExecutorService executor = Executors
			.newSingleThreadExecutor(ScheduledExecutor.getThreadFactory("NettySend"));

	private void readyWriteAndFlush() {
		if (scheduled.compareAndSet(false, true)) {
			/*若当前没有线程正在发送消息，则通过线程池执行发送动作*/
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
					LOGGER.error("发送异常", e);
				}
			}
		}
	};

	private final Queue<Info> hystrixQueue = new LinkedBlockingQueue<Info>();// 熔断队列，熔断后未处理对象的存放地

	private void flush() throws Exception {
		try {
			Info info;
			SendCommond commond = new SendCommond();
			int i = 0;
			boolean flushedOnce = false;
			while ((info = hystrixQueue.poll()) != null || (info = sendQueue.poll()) != null) {
				commond.write(info);
				if (++i == batchSize) {
					i = 0;
					flushedOnce = true;
					Boolean result = commond.execute();
					if (result) {
						//执行channel.flush后，需要间隔一段时间，以避免执行太快链表还未释放导致达到WRITE_BUFFER_HIGH_WATER_MARK上限
						Thread.sleep(36);
					} else {
						if (node.isSuspended()) {
							if (!_unavailable(sendQueue)) {
								Thread.sleep(80);
							}
						} else {
							Thread.sleep(80);
						}
					}
					commond = new SendCommond();
				}
			}
			
			// 最后一次flush，确保之前write的部分被flush到Socket通道里.
			if (i != 0 || !flushedOnce) {
				Boolean result = commond.execute();
				if (!result) {
					if (node.isSuspended()) {
						if (!_unavailable(hystrixQueue)) {
							Thread.sleep(80);
						}
					} else {
						Thread.sleep(80);
					}
				}
			}
		} finally {
			Thread.sleep(80);
			if (!sendQueue.isEmpty() || !hystrixQueue.isEmpty()) {
				flush();
			} else {
				scheduled.set(false);
			}
		}
	}

	/**
	 * Channel失败后的处理，将消息转发给其它Channel去发送
	 * @param queue
	 * @return
	 */
	private boolean _unavailable(Queue<Info> queue) {
		if (unavailableSend == null) {
			return false;
		}
		int i = 0;
		Info info = null;
		while ((info = queue.poll()) != null) {
			boolean result = unavailableSend.send(info.message, (AwaitPromise<?>) info.promise, node);
			if (!result) {
				queue.add(info);
				return result;
			}
			if (++i == batchSize) {
				break;
			}
		}
		return true;
	}

	final class SendCommond extends HystrixCommand<Boolean> {
		private final Queue<Info> queue = new LinkedBlockingQueue<Info>();// 发送队列

		SendCommond() {
			super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("NETTY_CONSUMER"))
					.andCommandKey(HystrixCommandKey.Factory.asKey("NETTY_" + node.getPoint().getRegisterName()))
					.andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(
							"NETTY_" + node.getPoint().getRegisterName() + "[" + channel.localAddress().toString()
									+ "->" + channel.remoteAddress().toString() + "]"))
					.andCommandPropertiesDefaults(
							HystrixCommandProperties.Setter()
									.withExecutionIsolationStrategy(
											HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
									.withExecutionTimeoutInMilliseconds(1000)
									.withCircuitBreakerSleepWindowInMilliseconds(2000).withRequestCacheEnabled(false)
									.withRequestLogEnabled(false))
					.andThreadPoolPropertiesDefaults(
							HystrixThreadPoolProperties.Setter().withCoreSize(6).withMaximumSize(18)
									.withAllowMaximumSizeToDivergeFromCoreSize(true).withKeepAliveTimeMinutes(1)));
		}

		void write(Info info) {
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
			StringBuilder sb = new StringBuilder();
			int i = 0;
			long basedate = new Date().getTime();
			try {
				Info info;
				while (channel.isWritable() && (info = queue.poll()) != null) {
					if (info.promise != null) {
						channel.write(info.message, info.promise);
					} else {
						channel.write(info.message);
					}
					sb.append("|").append(UUIDGenerator.restore(info.message.getHeader().getSessionID()));
					if (++i == batchSize) {
						break;
					}
				}
				if(sb.length() == 0) {
					sb.append("|");
				}
				if (!channel.isWritable()) {
					throw new WriteRejectException();
				}
			} finally {
				//当write消息数大于0，Flush
				if (i > 0) {
					channel.flush();
				}
				if(queue.size() + sendQueue.size() + hystrixQueue.size() > 100) {
					LOGGER.warn("将{}条信息Flush到Channel内,共用时{},尚余{}(其中:待发{}|熔断{}|外入{})条(余量过多)尚未发送,发送的消息有[{}]", i, new Date().getTime() - basedate, queue.size() + sendQueue.size() + hystrixQueue.size(), queue.size(), hystrixQueue.size(), sendQueue.size(), sb.substring(1));
				} else {
					LOGGER.debug("将{}条信息Flush到Channel内,共用时{},尚余{}(其中:待发{}|熔断{}|外入{})条尚未发送,发送的消息有[{}]", i, new Date().getTime() - basedate, queue.size() + sendQueue.size() + hystrixQueue.size(), queue.size(), hystrixQueue.size(), sendQueue.size(), sb.substring(1));
				}
				if (channel.isWritable() && !queue.isEmpty()) {
					Thread.sleep(36);
					flush();
				}
			}
		}

		@Override
		protected Boolean getFallback() {
			try {
				LOGGER.warn("进入熔断，尚未处理余量：{}，熔断原因：{}", queue.size(), executionResult);
				/* 熔断处理 */
				if (!channel.isWritable()) {
					// Netty写通道满，不能写入
					node.renewServerHealth(RemoteConstants.ServerHealthType.SEND, 0.0);
				} else {
					if (this.isResponseShortCircuited()) {
						// 短熔断打开，服务快速失败，客户端被熔断
						node.renewServerHealth(RemoteConstants.ServerHealthType.HYSTRIX, 0.0);
					} else if (this.isResponseRejected()) {
						// 被拒绝，拒绝原因可能是信号量满或者线程池满，不能处理，服务快速失败，客户端被熔断
						node.renewServerHealth(RemoteConstants.ServerHealthType.HYSTRIX, 0.0);
					}
				}
				return false;
			} finally {
				Info info;
				/* 熔断后剩余队列里的元素重新回发送队列 */
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
 * 熔断器在整个统计时间内是否开启的阀值，默认20。也就是在metricsRollingStatisticalWindowInMilliseconds（
 * 默认10s）内至少请求20次，熔断器才发挥起作用 熔断器是否开启的阀值，也就是说单位时间超过了阀值请求数，熔断器才开； private final
 * HystrixProperty circuitBreakerRequestVolumeThreshold;
 * //熔断器默认工作时间,默认:5秒.熔断器中断请求5秒后会进入半打开状态,放部分流量过去重试 private final HystrixProperty
 * circuitBreakerSleepWindowInMilliseconds; //是否启用熔断器,默认true. 启动 private final
 * HystrixProperty circuitBreakerEnabled; //默认:50%。当出错率超过50%后熔断器启动 private final
 * HystrixProperty circuitBreakerErrorThresholdPercentage;
 * //是否强制开启熔断器阻断所有请求,默认:false,不开启。置为true时，所有请求都将被拒绝，直接到fallback private final
 * HystrixProperty circuitBreakerForceOpen; //是否允许熔断器忽略错误,默认false, 不开启 private
 * final HystrixProperty circuitBreakerForceClosed;
 * //--------------信号量相关------------------ // 使用信号量隔离时，命令调用最大的并发数,默认:10 private
 * final HystrixProperty executionIsolationSemaphoreMaxConcurrentRequests;
 * //使用信号量隔离时，命令fallback(降级)调用最大的并发数,默认:10 private final HystrixProperty
 * fallbackIsolationSemaphoreMaxConcurrentRequests;
 * //--------------其他------------------
 * //使用命令调用隔离方式,默认:采用线程隔离,ExecutionIsolationStrategy.THREAD private final
 * HystrixProperty executionIsolationStrategy; //使用线程隔离时，调用超时时间，默认:1秒 private
 * final HystrixProperty executionIsolationThreadTimeoutInMilliseconds;
 * //线程池的key,用于决定命令在哪个线程池执行 private final HystrixProperty
 * executionIsolationThreadPoolKeyOverride; //是否开启fallback降级策略 默认:true private
 * final HystrixProperty fallbackEnabled;
 * //使用线程隔离时，是否对命令执行超时的线程调用中断（Thread.interrupt()）操作.默认:true private final
 * HystrixProperty executionIsolationThreadInterruptOnTimeout;
 * //是否开启请求日志,默认:true private final HystrixProperty requestLogEnabled;
 * //是否开启请求缓存,默认:true private final HystrixProperty requestCacheEnabled;
 * //Whether request caching is enabled. HystrixCollapserProperties
 * //请求合并是允许的最大请求数,默认: Integer.MAX_VALUE private final HystrixProperty
 * maxRequestsInBatch; //批处理过程中每个命令延迟的时间,默认:10毫秒 private final HystrixProperty
 * timerDelayInMilliseconds; //批处理过程中是否开启请求缓存,默认:开启 private final
 * HystrixProperty requestCacheEnabled;
 * 
 * HystrixThreadPoolProperties //配置线程池大小,默认值10个. 建议值:请求高峰时99.5%的平均响应时间 +
 * 向上预留一些即可 private final HystrixProperty corePoolSize; // 配置线程值等待队列长度,默认值:-1
 * 建议值:-1表示不等待直接拒绝,测试表明线程池使用直接决绝策略+ 合适大小的非回缩线程池效率最高.所以不建议修改此值。
 * 当使用非回缩线程池时，queueSizeRejectionThreshold,keepAliveTimeMinutes 参数无效 private
 * final HystrixProperty maxQueueSize;
 * 
 */