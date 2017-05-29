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
package com.yea.remote.netty.server;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.yea.core.base.act.AbstractAct;
import com.yea.core.base.id.UUIDGenerator;
import com.yea.core.exception.constants.YeaErrorMessage;
import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.remote.AbstractServer;
import com.yea.core.remote.client.ClientRegister;
import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.exception.RemoteException;
import com.yea.core.remote.struct.Header;
import com.yea.core.remote.struct.Message;
import com.yea.core.util.NetworkUtils;
import com.yea.core.util.ScheduledExecutor;
import com.yea.loadbalancer.LoadBalancerBuilder;
import com.yea.loadbalancer.NettyPing;
import com.yea.loadbalancer.config.DefaultClientConfigImpl;
import com.yea.loadbalancer.rule.RoundRobinRule;
import com.yea.remote.netty.AbstractNettyEndpoint;
import com.yea.remote.netty.balancing.RemoteClient;
import com.yea.remote.netty.handle.NettyChannelHandler;
import com.yea.remote.netty.send.SendHelperRegister;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * 服务端启动类
 * @author yiyongfei
 * 
 */
public class NettyServer extends AbstractNettyEndpoint {
	private static final Logger LOGGER = LoggerFactory.getLogger(NettyServer.class);
    private List<Map<String, ChannelHandler>> listHandler = null;
    private Server server;
    
    public void bind() throws Throwable {
    	server = new Server();
    	if(StringUtils.isEmpty(this.getHost())){
    		this.setHost(NetworkUtils.getIp());
    	}
    	server.setHost(this.getHost());
    	server.setPort(this.getPort());
    	server.setRegisterName(this.getRegisterName());
    	server.setDispatcher(this.getDispatcher());
    	server.setApplicationContext(this.getApplicationContext());
    	server.bind();
    	ClientRegister.getInstance().registerEndpoint(getRegisterName(), this);
    }
    
    public void shutdown() throws Throwable {
        server.stop();
    	server = null;
    }

    public List<Map<String, ChannelHandler>> getListHandler() {
        return listHandler;
    }

    public void setListHandler(List<Map<String, ChannelHandler>> listHandler) {
        this.listHandler = listHandler;
    }
    
    @Override
	protected void initLoadBalancer() {
		loadBalancer = LoadBalancerBuilder.newBuilder()
				.withRule(new RoundRobinRule()).withPing(new NettyPing()).withClientConfig(DefaultClientConfigImpl
						.getClientConfigWithDefaultValues().setClientName(this.getRegisterName()))
				.buildFixedServerListLoadBalancer(getBalancingNodes());
	}
    
    @Override
	protected void registerNode(BalancingNode node) {
		super.registerNode(node);
		SendHelperRegister.getInstance(((RemoteClient) node).getChannel()).setBatchSize(20);
	}
    
    private NettyServer _server() {
    	return this;
    }
    
    final class Server extends AbstractServer {
        private ExecutorService executor = Executors.newFixedThreadPool(2, ScheduledExecutor.getThreadFactory("NettyServer.bind"));
        private EventLoopGroup bossGroup = null;
        private EventLoopGroup workerGroup = null;

        void bind() throws Throwable {
            if(!super.isBind()){
            	super._Disbind();
                //启动一个线程连接指定地址的服务器
            	//为什么新启线程：绑定成功后，服务会sync，处理线程会处于等待状况，所以需要新线程来开启服务
                executor.submit(new BindCallable());
                while(true){
                    if(super.isBind()){
                        break;
                    } else {
                        TimeUnit.MILLISECONDS.sleep(50);
                    }
                }
                //连接不成功时，判断连接是否有超时，若超时抛出异常，否则等候2秒后重连
                if(!super.isBindSuccess()){
                    TimeUnit.MILLISECONDS.sleep(10*1000);
                    
                    if(!super.isBindSuccess()){
                        LOGGER.info("绑定服务器（" + this.getHost() + ":" + this.getPort() +"）不成功，准备重新连接！");
                        if(!this.isStop()){
                            bind();
                        }
                    } else {
                    	super._Notstop();
                        LOGGER.info("绑定服务器（" + this.getHost() + ":" + this.getPort() +"）成功！");
    					if (this.getDispatcher() != null) {
    						try{
    							this.getDispatcher().register(_server());
    						} catch (Throwable ex){
    						}
    					}
                    }
                } else {
                	super._Notstop();
                    LOGGER.info("绑定服务器（" + this.getHost() + ":" + this.getPort() +"）成功！");
					if (this.getDispatcher() != null) {
						try{
							this.getDispatcher().register(_server());
						} catch (Throwable ex){
						}
					}
                }
            }
        }
        
        void shutdown() throws Exception {
        	try{
        		if (super.isBind()) {
        			if (bossGroup != null) {
                        bossGroup.shutdownGracefully().sync();
                    }
                    if (workerGroup != null) {
                        workerGroup.shutdownGracefully().sync();
                    }
                    LOGGER.info("服务器（" + this.getHost() + ":" + this.getPort() +"）关闭完成！");
                }
        	} finally {
        		super._Disbind();
                super._BindFailure();
        	}
        }

		void stop() throws Exception {
			super._Stop();
			try {
				/* 通知连接该服务端的所有客户端，服务将关闭，让客户端主动关闭连接 */
				Collection<BalancingNode> clients = loadBalancer.getAllNodes();
				LOGGER.info("服务器（" + this.getHost() + ":" + this.getPort() + "）共连接了" + clients.size() + "个客户端！");
				if(clients.size() > 0) {
					for (BalancingNode client : clients) {
						try {
							Message nettyMessage = new Message();
							Header header = new Header();
							header.setType(RemoteConstants.MessageType.NOTIFT_STOP.value());
							header.setSessionID(UUIDGenerator.generate());
							nettyMessage.setHeader(header);
							((RemoteClient) client).getChannel().pipeline().writeAndFlush(nettyMessage);
						} catch (Exception ex) {
							/* 通知客户端发生异常时，将继续通知下一个客户端 */
							LOGGER.error("通知客户端服务端将关闭时发生异常", ex);
						}
					}
					//通知客户端后，服务端30秒后将发送关闭指令
					Thread.sleep(30 * 1000);
					for (BalancingNode client : clients) {
						try {
							Message nettyMessage = new Message();
							Header header = new Header();
							header.setType(RemoteConstants.MessageType.STOP.value());
							header.setSessionID(UUIDGenerator.generate());
							nettyMessage.setHeader(header);
							((RemoteClient) client).getChannel().pipeline().writeAndFlush(nettyMessage);
						} catch (Exception ex) {
							/* 通知客户端发生异常时，将继续通知下一个客户端 */
							LOGGER.error("发送关闭指令给客户端时发生异常", ex);
						}
					}
				}
				
				/* 所有客户端都关闭连接或超时10秒，服务端将关闭连接 */
				long startTime = new Date().getTime();
				while (true) {
					if (loadBalancer.getAllNodes().size() > 0) {
						if (new Date().getTime() - startTime > 1000 * 10) {
							break;
						}
						continue;
					} else {
						break;
					}
				}
				LOGGER.info("服务器（" + this.getHost() + ":" + this.getPort() + "）仍有" + getBalancingNodes().size() + "个客户端未被关闭！");
				shutdown();
			} finally {
				/* 通知调度中心服务注销 */
				if (this.getDispatcher() != null) {
					try {
						this.getDispatcher().logout(_server());
					} catch (Throwable ex) {
					}
				}
			}
		}
        
        void _bind() throws Exception {
        	super.getConnectLock().lock();
        	try{
        		int availableProcessors = Runtime.getRuntime().availableProcessors();
				if (((int) Math.floor(availableProcessors * 1.5)) == availableProcessors) {
					//当CPU核心是一核时，workerGroup的线程数为2
					availableProcessors = availableProcessors + 1;
				} else {
					availableProcessors = (int) Math.floor(availableProcessors * 1.5);
				}
        		// 配置服务端的NIO线程组
                bossGroup = new NioEventLoopGroup();
                workerGroup = new NioEventLoopGroup(availableProcessors);//默认是CPU核数 * 2
                
                ServerBootstrap bootstrap = new ServerBootstrap();
                /**
                 * 1、通过NoDelay禁用Nagle,使消息立即发出去，不用等待到一定的数据量才发出去
                 * 2、通过Keepalive，保持长连接
                 */
                /**
                 * 
                 * backlog指定了内核为此套接口排队的最大连接个数，对于给定的监听套接口，内核要维护两个队列，未链接队列和已连接队列，根据TCP三路握手过程中三个分节来分隔这两个队列。服务器处于listen状态时收到客户端syn 分节(connect)时在未完成队列中创建一个新的条目，然后用三路握手的第二个分节即服务器的syn 响应及对客户端syn的ack,此条目在第三个分节到达前(客户端对服务器syn的ack)一直保留在未完成连接队列中，如果三路握手完成，该条目将从未完成连接队列搬到已完成连接队列尾部。当进程调用accept时，从已完成队列中的头部取出一个条目给进程，当已完成队列为空时进程将睡眠，直到有条目在已完成连接队列中才唤醒。backlog被规定为两个队列总和的最大值，大多数实现默认值为5，但在高并发web服务器中此值显然不够，lighttpd中此值达到128*8 。需要设置此值更大一些的原因是未完成连接队列的长度可能因为客户端SYN的到达及等待三路握手第三个分节的到达延时而增大。Netty默认的backlog为100，当然，用户可以修改默认值，用户需要根据实际场景和网络状况进行灵活设置。
                 */
                //option()是提供给boss线程。childOption()是提供给由父管道ServerChannel接收到的连接，也就是worker线程
                bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    /*通用参数*/
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30 * 1000)//连接超时毫秒数，默认值30000毫秒
                    .option(ChannelOption.MAX_MESSAGES_PER_READ, 32)//一次Loop读取的最大消息数，对于ServerChannel或者NioByteChannel，默认值为16，其他Channel默认值为1
                    .option(ChannelOption.WRITE_SPIN_COUNT, 32)//一个Loop写操作执行的最大次数，默认值为16。对于大数据量的写操作至多进行16次，如果16次仍没有全部写完数据，此时会提交一个新的写任务给EventLoop，任务将在下次调度继续执行。(channel.flush时，会循环push数据到socket，但循环次数不大于该设定)
                    .option(ChannelOption.AUTO_READ, true)//自动读取，默认值为True。读操作，需要调用channel.read()设置关心的I/O事件为OP_READ，这样若有数据到达才能读取以供用户处理。该值为True时，每次读操作完毕后会自动调用channel.read()，从而有数据到达便能读取；否则，需要用户手动调用channel.read()
                    .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 128 * 1024)//写高水位标记，默认值64KB。如果Netty的写缓冲区中的字节超过该值，Channel的isWritable()返回False。(channel.write时，会判断已写入链表里的元素总字节数，当总字节数超出该设定，不可写)
                    .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 64 * 1024)//写低水位标记，默认值32KB。当Netty的写缓冲区中的字节超过高水位之后若下降到低水位，则Channel的isWritable()返回True
                    .option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT)//消息大小估算器，默认为DefaultMessageSizeEstimator.DEFAULT。估算ByteBuf、ByteBufHolder和FileRegion的大小，其中ByteBuf和ByteBufHolder为实际大小，FileRegion估算值为0。该值估算的字节数在计算水位时使用，FileRegion为0可知FileRegion不影响高低水位。
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT).childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)//ByteBuf的分配器，默认值为ByteBufAllocator.DEFAULT，4.0版本为UnpooledByteBufAllocator，4.1版本为PooledByteBufAllocator
                    .option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT).childOption(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT)//用于Channel分配接受Buffer的分配器，默认值为AdaptiveRecvByteBufAllocator.DEFAULT，是一个自适应的接受缓冲区分配器，能根据接受到的数据自动调节大小。可选值为FixedRecvByteBufAllocator，固定大小的接受缓冲区分配器。
                    /*ServerSocketChannel参数*/
                    .option(ChannelOption.SO_BACKLOG, 256)//服务端接受连接的队列长度，如果队列已满，客户端连接将被拒绝。默认值，Windows为200，其他为128
                    /*SocketChannel参数*/
                    .option(ChannelOption.SO_REUSEADDR, true)//地址复用，默认值False。有四种情况可以使用：(1)当有一个有相同本地地址和端口的socket1处于TIME_WAIT状态时，而你希望启动的程序的socket2要占用该地址和端口，比如重启服务且保持先前端口。(2).有多块网卡或用IP Alias技术的机器在同一端口启动多个进程，但每个进程绑定的本地IP地址不能相同。(3).单个进程绑定相同的端口到多个socket上，但每个socket绑定的ip地址不同。(4).完全相同的地址和端口的重复绑定。但这只用于UDP的多播，不用于TCP。
                    .option(ChannelOption.SO_RCVBUF, 96 * 1024)//TCP数据接收缓冲区大小。linux操作系统可使用命令：cat /proc/sys/net/ipv4/tcp_rmem查询其大小。一般情况下，该值可由用户在任意时刻设置，但当设置值超过64KB时，需要在连接到远端之前设置。低配机器可适当降低此值，高配机器适当提高此值。
                    .option(ChannelOption.SO_SNDBUF, 16 * 1024)//TCP数据发送缓冲区大小。linux操作系统可使用命令：cat /proc/sys/net/ipv4/tcp_smem查询其大小。低配机器可适当降低此值，高配机器适当提高此值。
                    .childOption(ChannelOption.TCP_NODELAY, true)//立即发送数据，默认值为True。该值设置Nagle算法的启用，该算法将小的碎片数据连接成更大的报文来最小化所发送的报文的数量，如果需要发送一些较小的报文，则需要禁用该算法。
                    .childOption(ChannelOption.SO_KEEPALIVE, true)//连接保活，默认值为False。启用该功能时，TCP会主动探测空闲连接的有效性。需注意：默认的心跳间隔是7200s
                    .option(ChannelOption.ALLOW_HALF_CLOSURE, false)//连接远端关闭时本地端是否关闭，默认值为False。值为False时，连接自动关闭；为True时，触发ChannelInboundHandler的userEventTriggered()方法，事件为ChannelInputShutdownEvent。
                    .handler(new LoggingHandler(LogLevel.WARN))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            if (listHandler != null && listHandler.size() > 0) {
                                for(Map<String, ChannelHandler> map : listHandler){
                                    Set<String> setKey = map.keySet();
                                    for(String key : setKey){
                                        if(map.get(key) instanceof NettyChannelHandler){
                                            NettyChannelHandler handler = (NettyChannelHandler) ((NettyChannelHandler)map.get(key)).clone();
                                            handler.setApplicationContext(getApplicationContext());
                                            ch.pipeline().addLast(key, handler);
                                        } else {
                                            ch.pipeline().addLast(key, map.get(key));
                                        }
                                    }
                                    
                                }
                                ch.pipeline().addLast(UUIDGenerator.generateString(), new ChannelInboundHandlerAdapter(){
									@Override
									public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
										Message message = (Message) msg;
										if (message.getHeader() != null && message.getHeader().getType() == RemoteConstants.MessageType.ACTLOOKUP_REQ.value()) {
											/* 检索当前服务所有注册的ActName */
											String[] actnames = getApplicationContext().getBeanNamesForType(AbstractAct.class, true, true);
											Message respMessage = new Message();
											Header header = new Header();
											header.setType(RemoteConstants.MessageType.ACTLOOKUP_RESP.value());
											header.setSessionID(message.getHeader().getSessionID());
											header.setResult(RemoteConstants.MessageResult.SUCCESS.value());
											header.addAttachment("registerName", getRegisterName());
											header.addAttachment("actName", actnames);
											respMessage.setHeader(header);
											ctx.write(respMessage);
											ctx.flush();
										} else {
											ctx.fireChannelRead(msg);
										}
									}
                                	
                                	@Override
                                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
										if (!isStop()) {
											super.channelActive(ctx);
	                                        RemoteClient remoteClient = new RemoteClient(ctx.channel(), _instance());
	                                        registerNode(remoteClient);
	                                        LOGGER.info("远程节点" + remoteClient.getRemoteAddress() + "已加入本地节点"+_instance().getHost()+":"+_instance().getPort()+"负载均衡池！");
	                                    } else {
											/* 如果服务中止标志已打上，此时客户端的请求连接一律拒绝 */
											ctx.close();
										}
                                    }

                                    @Override
                                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                        super.channelInactive(ctx);
										Collection<BalancingNode> collection = loadBalancer.chooseNode(ctx.channel().remoteAddress(), false);
										for (BalancingNode node : collection) {
											unregisterNode(node);
										}
										LOGGER.info("远程节点" + ctx.channel().remoteAddress() + "已从本地节点"+_instance().getHost()+":"+_instance().getPort()+"负载均衡池移除！");
                                    }
                                });
                            } else {
                                throw new RemoteException(YeaErrorMessage.ERR_FOUNDATION, RemoteConstants.ExceptionType.SETTING.value() ,"ChannelHandler设置有问题，无法连接服务器！", null);
                            }
                        }
                    });

                ChannelFuture future = null;
                // 绑定端口，同步等待成功
                if(this.getHost() != null && this.getHost().trim().length() > 0){
                    //远程模式
                    future = bootstrap.bind(this.getHost(), this.getPort());
                } else {
                    //本地模式
                    future = bootstrap.localAddress(this.getPort()).bind();
                }
                super._BindSuccess();
                super._Bind();
                future.sync();
                future.channel().closeFuture().sync();
        	} catch (Exception ex) {
                super._BindFailure();
                super._Bind();
        		throw ex;
        	} finally {
        		super.getConnectLock().unlock();
        	}
            
        }
            
        private Server _instance() {
			return this;
		}

        class BindCallable implements Callable<Boolean> {
        	
            /** 
             * @see java.lang.Runnable#run()
             */
            public Boolean call() {
                try {
                	_Bind();
                    return true;
                } catch (Throwable e) {
                    LOGGER.error("服务器绑定失败，失败原因：", e);
                    return false;
                }
            }
            
            private void _Bind() throws Throwable {
                try {
                    _bind();
                } finally {
                    //关闭服务器
                	shutdown();
                }
                //重新绑定服务器
                if(!isStop()){
                    bind();
                }
            }
        }
    }
}