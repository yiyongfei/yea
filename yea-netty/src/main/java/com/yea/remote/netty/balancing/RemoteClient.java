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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;

import com.yea.core.base.id.UUIDGenerator;
import com.yea.core.loadbalancer.AbstractBalancingNode;
import com.yea.core.remote.AbstractPoint;
import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.exception.RemoteException;
import com.yea.core.remote.observer.Observable;
import com.yea.core.remote.struct.CallAct;
import com.yea.core.remote.struct.CallReflect;
import com.yea.core.remote.struct.Header;
import com.yea.core.remote.struct.Message;
import com.yea.remote.netty.client.promise.AwaitPromise;
import com.yea.remote.netty.constants.NettyConstants;
import com.yea.remote.netty.exception.WriteRejectException;
import com.yea.remote.netty.promise.NettyChannelPromise;
import com.yea.remote.netty.send.SendHelper;
import com.yea.remote.netty.send.SendHelperRegister;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.DefaultChannelPromise;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * 
 * @author yiyongfei
 *
 */
public class RemoteClient extends AbstractBalancingNode {
	private Channel channel = null;
    private SendHelper nettySend = null;
    
	public RemoteClient(Channel channel, AbstractPoint endpoint) {
		super(endpoint, channel.remoteAddress());
		this.channel = channel;
	}
	
	/**
	 * 重置熔断器权重：
	 * 曾经熔断，并且队列为空时，取消熔断的权重
	 */
	@Override
	protected boolean resetHystrixHealth() {
		if (hystrix == 0 && getNettySend().getSendQueue().isEmpty()) {
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
        if(act != null){
        	header.addAttachment(NettyConstants.MessageHeaderAttachment.CALL_ACT.value(), act.getActName());
			if (!StringUtils.isEmpty(act.getCallbackName())) {
				header.addAttachment(NettyConstants.MessageHeaderAttachment.CALLBACK_ACT.value(), act.getCallbackName());
			}
            if(act instanceof CallReflect) {
            	header.addAttachment(NettyConstants.MessageHeaderAttachment.CALL_REFLECT.value(), act);
            }
        } else {
        	if(messageType.value() == RemoteConstants.MessageType.SERVICE_REQ.value()) {
        		throw new Exception("发送服务请求时请提供对方系统的Act名");
        	}
        }
		header.addAttachment(NettyConstants.MessageHeaderAttachment.HEADER_DATE.value(), new Date());
        nettyMessage.setHeader(header);
        nettyMessage.setBody(messages);
        
		try {
			getNettySend().send(nettyMessage, observer);
		} catch (Exception ex) {
			Thread.sleep(200);
			throw ex;
		}
        
        return observer;
    }
    
    @Override
	public void ping(int timeout) throws Throwable {
    	if (this.isDown()) {
			throw new RemoteException("Node[" + this.channel + "]已下线！");
		}
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
    
	private SendHelper getNettySend() {
		if (nettySend == null) {
			nettySend = SendHelperRegister.getInstance(getChannel());
		}
		return nettySend;
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
}
