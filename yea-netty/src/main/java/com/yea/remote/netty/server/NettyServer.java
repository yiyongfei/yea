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

import com.yea.core.balancing.hash.BalancingNode;
import com.yea.core.base.id.UUIDGenerator;
import com.yea.core.exception.constants.YeaErrorMessage;
import com.yea.core.remote.AbstractEndpoint;
import com.yea.core.remote.AbstractServer;
import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.exception.RemoteException;
import com.yea.core.remote.promise.Promise;
import com.yea.core.remote.struct.CallAct;
import com.yea.core.remote.struct.Header;
import com.yea.core.remote.struct.Message;
import com.yea.core.util.NetworkUtils;
import com.yea.remote.netty.balancing.RemoteClient;
import com.yea.remote.netty.balancing.RemoteClientLocator;
import com.yea.remote.netty.handle.NettyChannelHandler;
import com.yea.remote.netty.promise.NettyChannelPromise;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * 服务端启动类
 * @author yiyongfei
 * 
 */
public class NettyServer extends AbstractEndpoint {
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
    }

    public <T> NettyChannelPromise<T> send(CallAct act, Object... messages) throws Throwable {
        return server.send(act, null, messages);
    }
    
    @SuppressWarnings("rawtypes")
    public <T> NettyChannelPromise<T> send(CallAct act, List<GenericFutureListener> listeners, Object... messages) throws Throwable {
    	return server.send(act, listeners, messages);
    }
    
    public int remoteConnects() {
    	return server.remoteConnects();
    }
    
    public String useStatistics() {
    	return server.useStatistics();
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
    
    private NettyServer _server() {
    	return this;
    }
    
    class Server extends AbstractServer {
        private ExecutorService executor = Executors.newFixedThreadPool(2);
        private EventLoopGroup bossGroup = null;
        private EventLoopGroup workerGroup = null;
        
        //负载均衡（哈希）
        private RemoteClientLocator remoteClientLocator = new RemoteClientLocator();
        
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
        

		@Override
		public <T> Promise<T> send(CallAct act, Object... messages) throws Exception {
			// TODO Auto-generated method stub
			return send(act, null, messages);
		}
		
        @SuppressWarnings("rawtypes")
        <T> NettyChannelPromise<T> send(CallAct act, List<GenericFutureListener> listeners, Object... messages) throws Exception {
        	if(remoteClientLocator.getAll().size() == 0) {
        	    throw new RemoteException(YeaErrorMessage.ERR_APPLICATION, RemoteConstants.ExceptionType.CONNECT.value() ,"未发现连接，请先连接服务器后发送！", null);
        	}
        	byte[] sessionID = UUIDGenerator.generate();
        	RemoteClient client = remoteClientLocator.getClient(sessionID);
        	NettyChannelPromise<T> future = client.send(act, listeners, RemoteConstants.MessageType.SERVICE_REQ, sessionID, messages);
            return future;
        }
        
        String useStatistics() {
        	return remoteClientLocator.nodeStatistics();
        }
        
        public int remoteConnects() {
        	return remoteClientLocator.getAll().size();
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
        	
        	/*通知调度中心服务注销*/
        	if (this.getDispatcher() != null) {
				try{
					this.getDispatcher().logout(_server());
				} catch (Throwable ex){
				}
			}
        	
        	/*通知连接该服务端的所有客户端，服务将关闭，让客户端主动关闭连接*/
        	Collection<BalancingNode> clients = remoteClientLocator.getAll();
        	LOGGER.info("服务器（" + this.getHost() + ":" + this.getPort() +"）共连接了"+clients.size()+"个客户端！");
			for (BalancingNode client : clients) {
				try{
					Message nettyMessage = new Message();
			        Header header = new Header();
			        header.setType(RemoteConstants.MessageType.STOP.value());
			        header.setSessionID(UUIDGenerator.generate());
			        nettyMessage.setHeader(header);
			        ((RemoteClient)client).getChannel().pipeline().write(nettyMessage);
			        ((RemoteClient)client).getChannel().pipeline().flush();
				} catch (Exception ex){
					/*通知客户端发生异常时，将继续通知下一个客户端*/
					LOGGER.error("通知客户端关闭时发生异常", ex);
				}
			}
			
			/*所有客户端都关闭连接或超时10秒，服务端将关闭连接*/
			long startTime = new Date().getTime();
			while (true) {
				if(remoteClientLocator.getAll().size() > 0){
					if (new Date().getTime() - startTime > 1000 * 60) {
						break;
					}
					continue;
				} else {
					break;
				}
			}
			LOGGER.info("服务器（" + this.getHost() + ":" + this.getPort() +"）仍有"+clients.size()+"个客户端未被关闭！");
        	shutdown();
        }
        
        void _bind() throws Exception {
        	super.getConnectLock().lock();
        	try{
        		// 配置服务端的NIO线程组
                bossGroup = new NioEventLoopGroup();
                workerGroup = new NioEventLoopGroup();

                ServerBootstrap bootstrap = new ServerBootstrap();
                /**
                 * 1、通过NoDelay禁用Nagle,使消息立即发出去，不用等待到一定的数据量才发出去
                 * 2、通过Keepalive，保持长连接
                 */
                /**
                 * 
                 * backlog指定了内核为此套接口排队的最大连接个数，对于给定的监听套接口，内核要维护两个队列，未链接队列和已连接队列，根据TCP三路握手过程中三个分节来分隔这两个队列。服务器处于listen状态时收到客户端syn 分节(connect)时在未完成队列中创建一个新的条目，然后用三路握手的第二个分节即服务器的syn 响应及对客户端syn的ack,此条目在第三个分节到达前(客户端对服务器syn的ack)一直保留在未完成连接队列中，如果三路握手完成，该条目将从未完成连接队列搬到已完成连接队列尾部。当进程调用accept时，从已完成队列中的头部取出一个条目给进程，当已完成队列为空时进程将睡眠，直到有条目在已完成连接队列中才唤醒。backlog被规定为两个队列总和的最大值，大多数实现默认值为5，但在高并发web服务器中此值显然不够，lighttpd中此值达到128*8 。需要设置此值更大一些的原因是未完成连接队列的长度可能因为客户端SYN的到达及等待三路握手第三个分节的到达延时而增大。Netty默认的backlog为100，当然，用户可以修改默认值，用户需要根据实际场景和网络状况进行灵活设置。
                 */
                bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 256).option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.SO_RCVBUF, 128 * 1024).option(ChannelOption.SO_SNDBUF, 128 * 1024)
                    .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 128 * 1024).option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 64 * 1024)
                    .option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_KEEPALIVE, true).childOption(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT).childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT).handler(new LoggingHandler(LogLevel.INFO))
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
                                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
										if (!isStop()) {
											super.channelActive(ctx);
	                                        RemoteClient remoteClient = new RemoteClient(ctx.channel(), _instance());
	                                        remoteClientLocator.addLocator(remoteClient);
	                                        LOGGER.info("远程节点" + remoteClient.getSocketAddress() + "已加入本地节点"+_instance().getHost()+":"+_instance().getPort()+"负载均衡池！");
	                                    } else {
											/* 如果服务中止标志已打上，此时客户端的请求连接一律拒绝 */
											ctx.close();
										}
                                    }

                                    @Override
                                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                        super.channelInactive(ctx);
                                        Collection<BalancingNode> collection = remoteClientLocator.getLocator(ctx.channel().remoteAddress());
										for (BalancingNode node : collection) {
											remoteClientLocator.removeLocator((RemoteClient)node);
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
