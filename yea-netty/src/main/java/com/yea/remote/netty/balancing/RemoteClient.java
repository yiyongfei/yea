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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yea.core.balancing.hash.BalancingNode;
import com.yea.core.base.id.UUIDGenerator;
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
import io.netty.channel.DefaultChannelPromise;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * 
 * @author yiyongfei
 *
 */
public class RemoteClient implements BalancingNode {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteClient.class);
    Channel channel = null;
    AbstractEndpoint endpoint = null;

	//客户端所连接的远程服务器地址
    SocketAddress remoteAddress;
    
    
    public RemoteClient(Channel channel, AbstractEndpoint endpoint){
    	this.channel = channel;
    	this.endpoint = endpoint;
    	this.remoteAddress = channel.remoteAddress();
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <T> NettyChannelPromise<T> send(CallAct act, List<GenericFutureListener> listeners, RemoteConstants.MessageType messageType, byte[] sessionID, Object... messages) throws Exception {
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
        
        String actName = "";
        if(act != null){
        	actName = act.getActName();
        	header.getAttachment().put(NettyConstants.CALL_ACT, act.getActName());
            header.getAttachment().put(NettyConstants.CALLBACK_ACT, act.getCallbackName());
            if(act instanceof CallReflect) {
            	header.getAttachment().put(NettyConstants.CALL_REFLECT, act);
            }
        } else {
        	if(messageType.value() == RemoteConstants.MessageType.SERVICE_REQ.value()) {
        		throw new Exception("发送服务请求时请提供对方系统的Act名");
        	}
        }
        header.getAttachment().put(NettyConstants.HEADER_DATE, new Date());
        nettyMessage.setHeader(header);
        nettyMessage.setBody(messages);
        channel.pipeline().write(nettyMessage, observer);
        channel.pipeline().flush();
        
        LOGGER.info(endpoint.getHost() + ":" + endpoint.getPort() + "将向远程节点" + remoteAddress + "请求" + actName + "服务[标识:" + UUIDGenerator.restore(sessionID) + "]！");
        return observer;
    }
    
        
    /** 
     * @see com.jbs.core.balancing.hash.BalancingNode#getSocketAddress()
     */
    public SocketAddress getSocketAddress() {
        return remoteAddress;
    }

    public String toString(){
    	return channel.toString();
    }
    
    public Channel getChannel() {
		return channel;
	}
    
    public AbstractEndpoint getEndpoint() {
		return endpoint;
	}
}
